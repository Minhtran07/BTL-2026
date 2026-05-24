package com.auction.network.server;

import com.auction.model.user.User;
import com.auction.network.Message;
import com.auction.network.handler.*;
import com.auction.pattern.observer.AuctionObserver;
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
 * {@link #handlerMap} map từ MessageType → handler. Dispatch theo bảng thay
 * vì switch case dài → dễ mở rộng, tuân thủ Open/Closed Principle.
 *
 * <p><b>OBSERVER PATTERN per-auction subscription:</b>
 * Mỗi {@link ClientHandler} là 1 {@link PushListener}. Khi client gửi
 * {@code SUBSCRIBE_AUCTION}, handler đăng ký vào {@link PushForwarder}.
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
     * Map đa hình: Message.Type → RequestHandler tương ứng.
     *
     * <p><b>Đây là COMMAND PATTERN.</b> Thay vì switch-case dài 30 case,
     * chỉ cần lookup map → gọi handle(). Thêm loại request mới chỉ cần thêm
     * 1 dòng vào {@link #buildHandlerMap()} - không sửa code khác (OCP).
     *
     * <p>{@link EnumMap}: tối ưu hơn HashMap khi key là enum.
     */
    private final Map<Message.Type, RequestHandler> handlerMap;

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
    private Map<Message.Type, RequestHandler> buildHandlerMap() {
        Map<Message.Type, RequestHandler> map = new EnumMap<>(Message.Type.class);

        // Auth
        map.put(Message.Type.LOGIN,    new LoginHandler(userService));
        map.put(Message.Type.REGISTER, new RegisterHandler(userService));
        map.put(Message.Type.LOGOUT,   new LogoutHandler());

        // Item
        map.put(Message.Type.CREATE_ITEM,  new CreateItemHandler(auctionService));
        map.put(Message.Type.GET_ITEM,     new GetItemHandler(auctionService));
        map.put(Message.Type.GET_ALL_ITEMS, new GetAllItemsHandler(auctionService));
        map.put(Message.Type.UPDATE_ITEM,  new UpdateItemHandler(auctionService));
        map.put(Message.Type.DELETE_ITEM,  new DeleteItemHandler(auctionService));

        // Auction
        map.put(Message.Type.CREATE_AUCTION,     new CreateAuctionHandler(auctionService));
        map.put(Message.Type.GET_ALL_AUCTIONS,   new GetAllAuctionsHandler(auctionService));
        map.put(Message.Type.GET_ACTIVE_AUCTIONS, new GetActiveAuctionsHandler(auctionService));
        map.put(Message.Type.GET_AUCTION,        new GetAuctionHandler(auctionService));
        map.put(Message.Type.END_AUCTION,        new EndAuctionHandler(auctionService));
        map.put(Message.Type.CANCEL_AUCTION,     new CancelAuctionHandler(auctionService));

        // Bidding — không còn truyền BidBroadcaster: AuctionService đã dispatch
        // NEW_BID event qua AuctionEventDispatcher, các ClientHandler đã subscribe
        // sẽ tự nhận và đẩy BID_UPDATE/AUCTION_EVENT về client tương ứng.
        map.put(Message.Type.PLACE_BID,          new PlaceBidHandler(auctionService));
        map.put(Message.Type.REGISTER_AUTO_BID,  new RegisterAutoBidHandler(auctionService));
        map.put(Message.Type.GET_BID_HISTORY,    new GetBidHistoryHandler(auctionService));

        // Admin
        map.put(Message.Type.GET_ALL_USERS,   new GetAllUsersHandler(userService));
        map.put(Message.Type.DEACTIVATE_USER, new DeactivateUserHandler(userService));

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
         * SUBSCRIBE/UNSUBSCRIBE được xử lý inline vì cần truy cập state của handler.
         */
        private Message processRequest(Message request) {
            if (request == null || request.getType() == null) {
                return HandlerUtils.error("Request không hợp lệ");
            }

            // Observer pattern: SUBSCRIBE/UNSUBSCRIBE thao tác trực tiếp trên handler
            switch (request.getType()) {
                case SUBSCRIBE_AUCTION:   return handleSubscribe(request);
                case UNSUBSCRIBE_AUCTION: return handleUnsubscribe(request);
                default: /* fall through to handlerMap dispatch */ break;
            }

            RequestHandler handler = handlerMap.get(request.getType());
            if (handler == null) {
                return HandlerUtils.error("Loại request không được hỗ trợ: " + request.getType());
            }

            try {
                Message response = handler.handle(request, authenticatedUser);

                // Cập nhật session state sau các thao tác thay đổi trạng thái xác thực
                if (request.getType() == Message.Type.LOGIN
                        && response.getType() == Message.Type.SUCCESS) {
                    authenticatedUser = ((LoginHandler) handler).getLastAuthenticatedUser();
                } else if (request.getType() == Message.Type.LOGOUT) {
                    authenticatedUser = null;
                    unsubscribeAll();
                }

                return response;
            } catch (Exception e) {
                return HandlerUtils.error(e.getMessage());
            }
        }

        private Message handleSubscribe(Message request) {
            String auctionId = request.get("auctionId");
            if (auctionId == null || auctionId.isBlank()) {
                return HandlerUtils.error("Thiếu auctionId để subscribe");
            }
            if (subscribedAuctions.add(auctionId)) {
                forwarder.subscribe(auctionId, this);
            }
            Message ok = new Message(Message.Type.SUCCESS);
            ok.put("auctionId", auctionId);
            ok.put("message", "Đã subscribe phiên " + auctionId);
            return ok;
        }

        private Message handleUnsubscribe(Message request) {
            String auctionId = request.get("auctionId");
            if (auctionId == null || auctionId.isBlank()) {
                return HandlerUtils.error("Thiếu auctionId để unsubscribe");
            }
            if (subscribedAuctions.remove(auctionId)) {
                forwarder.unsubscribe(auctionId, this);
            }
            Message ok = new Message(Message.Type.SUCCESS);
            ok.put("auctionId", auctionId);
            ok.put("message", "Đã unsubscribe phiên " + auctionId);
            return ok;
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
