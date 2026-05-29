package com.auction.network.server;

import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.*;
import com.auction.network.handler.*;
import com.auction.service.AuctionService;
import com.auction.service.UserService;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

/**
 * ============================================================================
 * AUCTIONSERVER - SERVER TRUNG TÂM XỬ LÝ TẤT CẢ KẾT NỐI CLIENT
 * ============================================================================
 *
 * <p>Đây là server đa luồng (multi-threaded) - mỗi client kết nối được xử lý
 * bởi 1 thread riêng từ thread pool. Không giới hạn số client.
 *
 * <p><b>KIẾN TRÚC SERVER:</b>
 * <ol>
 *   <li>{@link ServerSocket} lắng nghe port (mặc định 9999)</li>
 *   <li>Mỗi {@code accept()} → tạo {@link ClientHandler} cho 1 client</li>
 *   <li>Thread pool ({@code ExecutorService}) chạy các handler song song</li>
 *   <li>Mỗi handler đọc request → dispatch sang {@link RequestHandler} qua map</li>
 * </ol>
 *
 * <p><b>COMMAND PATTERN (qua RequestHandler):</b>
 * Mỗi loại request có 1 handler riêng (LoginHandler, PlaceBidHandler...).
 * {@link #handlerMap} map từ {@code Class<? extends Message>} → handler.
 * Dispatch đa hình theo class thay vì switch-case → dễ mở rộng, tuân thủ
 * Open/Closed Principle.
 *
 * <p><b>OBSERVER PATTERN per-auction subscription:</b>
 * Mỗi {@link ClientHandler} là 1 {@link PushListener}. Khi client gửi
 * {@link SubscribeAuctionRequest}, handler đăng ký vào {@link PushForwarder}.
 * Khi có event → forwarder chỉ gửi cho các client đã subscribe đúng phiên đó
 * (không broadcast tất cả → tiết kiệm bandwidth).
 *
 * <p><b>CLEANUP TỰ ĐỘNG:</b>
 * Khi client UNSUBSCRIBE hoặc ngắt kết nối → handler unsubscribe khỏi mọi
 * auction để forwarder không giữ tham chiếu rác (memory leak).
 */
public class AuctionServer {

    /** Port mặc định nếu không truyền vào constructor. */
    private static final int DEFAULT_PORT = 9999;

    private final int port;
    /** Service xử lý nghiệp vụ user. */
    private final UserService userService;
    /** Service xử lý nghiệp vụ đấu giá. */
    private final AuctionService auctionService;
    /**
     * Thread pool xử lý các client.
     * CachedThreadPool: tự tạo thread khi cần, tái sử dụng thread idle.
     * Phù hợp khi số request bùng nổ rồi giảm xuống (như server đa client).
     */
    private final ExecutorService threadPool;
    /** Forwarder push event - Singleton chia sẻ cho mọi handler. */
    private final PushForwarder forwarder = PushForwarder.getInstance();

    /**
     * Danh sách client handler đang kết nối.
     * {@link CopyOnWriteArrayList}: thread-safe, tối ưu cho đọc nhiều, ghi ít.
     */
    private final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();

    /**
     * Map đa hình: {@code Class<? extends Message>} → {@link RequestHandler}.
     *
     * <p><b>Đây là COMMAND PATTERN.</b> Thay vì switch-case dài 30 case,
     * chỉ cần lookup map theo class → gọi handle(). Thêm loại request mới
     * chỉ cần thêm 1 dòng vào {@link #buildHandlerMap()} - không sửa code
     * khác (OCP).
     */
    private final Map<Class<? extends Message>, RequestHandler> handlerMap;

    /** Cờ chạy/dừng server. volatile để các thread thấy thay đổi ngay. */
    private volatile boolean running;

    public AuctionServer() {
        this(DEFAULT_PORT);
    }

    public AuctionServer(int port) {
        this.port = port;
        this.userService = new UserService();
        this.auctionService = new AuctionService();
        this.threadPool = Executors.newCachedThreadPool();
        this.handlerMap = buildHandlerMap();
    }

    /**
     * Đăng ký tất cả handler vào map — thêm loại request mới chỉ cần thêm một dòng ở đây.
     */
    private Map<Class<? extends Message>, RequestHandler> buildHandlerMap() {
        Map<Class<? extends Message>, RequestHandler> map = new HashMap<>();

        // Auth
        map.put(LoginRequest.class,    new LoginHandler(userService));
        map.put(RegisterRequest.class,  new RegisterHandler(userService));
        map.put(LogoutRequest.class,    new LogoutHandler());

        // Item
        map.put(CreateItemRequest.class,   new CreateItemHandler(auctionService));
        map.put(GetItemRequest.class,      new GetItemHandler(auctionService));
        map.put(GetAllItemsRequest.class,  new GetAllItemsHandler(auctionService));
        map.put(UpdateItemRequest.class,   new UpdateItemHandler(auctionService));
        map.put(DeleteItemRequest.class,   new DeleteItemHandler(auctionService));

        // Auction
        map.put(CreateAuctionRequest.class,      new CreateAuctionHandler(auctionService));
        map.put(GetAllAuctionsRequest.class,     new GetAllAuctionsHandler(auctionService));
        map.put(GetActiveAuctionsRequest.class,  new GetActiveAuctionsHandler(auctionService));
        map.put(GetAuctionRequest.class,         new GetAuctionHandler(auctionService));
        map.put(EndAuctionRequest.class,         new EndAuctionHandler(auctionService));
        map.put(CancelAuctionRequest.class,      new CancelAuctionHandler(auctionService));

        // Bidding
        map.put(PlaceBidRequest.class,           new PlaceBidHandler(auctionService));
        map.put(RegisterAutoBidRequest.class,    new RegisterAutoBidHandler(auctionService));
        map.put(GetBidHistoryRequest.class,      new GetBidHistoryHandler(auctionService));

        // User info (tài chính)
        map.put(GetUserInfoRequest.class,    new GetUserInfoHandler(userService));

        // Admin
        map.put(GetAllUsersRequest.class,    new GetAllUsersHandler(userService));
        map.put(DeactivateUserRequest.class, new DeactivateUserHandler(userService));

        return Collections.unmodifiableMap(map);
    }

    /**
     * Khởi động server - main loop accept connections.
     *
     * <p>Quy trình:
     * <ol>
     *   <li>Mở ServerSocket lắng nghe port</li>
     *   <li>Vòng lặp: accept() block đến khi có client → tạo ClientHandler →
     *       submit vào thread pool</li>
     *   <li>Khi running = false (shutdown) → thoát loop, dọn dẹp</li>
     * </ol>
     */
    public void start() {
        running = true;

        // try-with-resources: tự đóng ServerSocket khi thoát
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Server] Đang lắng nghe trên cổng " + port);

            // Vòng lặp accept - mỗi client kết nối → tạo handler
            while (running) {
                // accept() BLOCK đến khi có client mới
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] Client mới kết nối: " + clientSocket.getRemoteSocketAddress());

                // Tạo handler cho client + chạy trong thread riêng
                ClientHandler handler = new ClientHandler(clientSocket);
                connectedClients.add(handler);
                threadPool.execute(handler); // submit vào pool
            }
        } catch (IOException e) {
            // Nếu chủ động shutdown → bỏ qua. Nếu đang running mà lỗi → log
            if (running) {
                System.err.println("[Server] Lỗi: " + e.getMessage());
            }
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        running = false;
        for (ClientHandler client : connectedClients) {
            client.disconnect();
        }
        threadPool.shutdownNow();
        System.out.println("[Server] Đã tắt.");
    }

    // ================================================================
    //  CLIENTHANDLER - INNER CLASS XỬ LÝ MỘT CLIENT CỤ THỂ
    // ================================================================
    /**
     * Mỗi instance ClientHandler xử lý 1 client (1 connection).
     *
     * <p>Implements:
     * <ul>
     *   <li>{@link Runnable} - chạy trong thread pool</li>
     *   <li>{@link PushListener} - nhận event từ {@link PushForwarder}</li>
     * </ul>
     *
     * <p>Mỗi handler:
     * <ol>
     *   <li>Có 1 socket, 1 cặp ObjectInputStream/OutputStream</li>
     *   <li>Lưu user đã authenticated (sau LOGIN)</li>
     *   <li>Lưu danh sách auction đang subscribe</li>
     *   <li>Đọc request → process → gửi response</li>
     * </ol>
     */
    public class ClientHandler implements Runnable, PushListener {

        private final Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        /** User đã đăng nhập trong session này (null = chưa login). */
        private User authenticatedUser;
        private volatile boolean connected;

        /**
         * Các auctionId mà client này đang subscribe.
         * Thread-safe set vì có thể đăng ký/hủy từ nhiều thread.
         */
        private final Set<String> subscribedAuctions = ConcurrentHashMap.newKeySet();

        ClientHandler(Socket socket) {
            this.socket = socket;
            this.connected = true;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                while (connected) {
                    Message request = (Message) in.readObject();
                    Message response = processRequest(request);
                    if (response != null) {
                        sendMessage(response);
                    }
                }
            } catch (EOFException e) {
                // Client ngắt kết nối bình thường
            } catch (Exception e) {
                if (connected) {
                    System.err.println("[Server] Lỗi client: " + e.getMessage());
                }
            } finally {
                unsubscribeAll();
                disconnect();
                connectedClients.remove(this);
                System.out.println("[Server] Client đã ngắt kết nối.");
            }
        }

        /**
         * Xử lý request từ client – dispatch đa hình qua handlerMap.
         *
         * <p>SUBSCRIBE/UNSUBSCRIBE được xử lý inline vì cần truy cập state
         * của handler (subscribedAuctions, forwarder).
         *
         * <p>Dispatch dùng {@code request.getClass()} thay vì enum Type.
         */
        private Message processRequest(Message request) {
            if (request == null) {
                return Response.error("Request không hợp lệ");
            }

            // Dispatch subscribe/unsubscribe bằng instanceof (cần state của handler)
            if (request instanceof SubscribeAuctionRequest sub) {
                return handleSubscribe(sub);
            }
            if (request instanceof UnsubscribeAuctionRequest unsub) {
                return handleUnsubscribe(unsub);
            }

            // Dispatch đa hình qua class → handler map
            RequestHandler handler = handlerMap.get(request.getClass());
            if (handler == null) {
                return HandlerUtils.error(
                        "Loại request không được hỗ trợ: " + request.getClass().getSimpleName());
            }

            try {
                Message response = handler.handle(request, authenticatedUser);

                // Cập nhật session state sau các thao tác thay đổi trạng thái xác thực
                if (request instanceof LoginRequest
                        && response instanceof Response<?> r && r.isSuccess()) {
                    authenticatedUser = ((LoginHandler) handler).getLastAuthenticatedUser();
                } else if (request instanceof LogoutRequest) {
                    authenticatedUser = null;
                    unsubscribeAll();
                }

                return response;
            } catch (Exception e) {
                return HandlerUtils.error(e.getMessage());
            }
        }

        private Message handleSubscribe(SubscribeAuctionRequest sub) {
            String auctionId = sub.getAuctionId();
            if (auctionId == null || auctionId.isBlank()) {
                return Response.error("Thiếu auctionId để subscribe");
            }
            if (subscribedAuctions.add(auctionId)) {
                forwarder.subscribe(auctionId, this);
            }
            return Response.success();
        }

        private Message handleUnsubscribe(UnsubscribeAuctionRequest unsub) {
            String auctionId = unsub.getAuctionId();
            if (auctionId == null || auctionId.isBlank()) {
                return Response.error("Thiếu auctionId để unsubscribe");
            }
            if (subscribedAuctions.remove(auctionId)) {
                forwarder.unsubscribe(auctionId, this);
            }
            return Response.success();
        }

        private void unsubscribeAll() {
            for (String auctionId : subscribedAuctions) {
                forwarder.unsubscribe(auctionId, this);
            }
            subscribedAuctions.clear();
        }

        /**
         * Callback từ PushForwarder — server chỉ gửi message này tới
         * client đã subscribe phiên tương ứng. Đây là điểm mấu chốt của
         * Observer Pattern per-auction: không còn duyệt connectedClients.
         */
        @Override
        public void onPush(Message message) {
            if (!connected) return;
            sendMessage(message);
        }

        synchronized void sendMessage(Message message) {
            try {
                if (out != null && connected) {
                    out.writeObject(message);
                    out.flush();
                    out.reset(); // Đảm bảo gửi object mới nhất
                }
            } catch (IOException e) {
                // Client đã ngắt kết nối
                connected = false;
            }
        }

        void disconnect() {
            connected = false;
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ignored) {}
        }
    }

    // ================================================================
    //  Main – khởi chạy server standalone
    // ================================================================
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        AuctionServer server = new AuctionServer(port);
        System.out.println("=== Auction Server ===");
        server.start();
    }
}
