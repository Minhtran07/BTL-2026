package com.auction.network.client;

import com.auction.network.message.Message;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * ============================================================================
 * AUCTIONCLIENT - SOCKET CLIENT KẾT NỐI TỚI AUCTIONSERVER
 * ============================================================================
 *
 * <p>Lớp này quản lý kết nối TCP socket từ phía client. Trách nhiệm:
 * <ul>
 *   <li>Mở kết nối (connect) tới server</li>
 *   <li>Gửi {@link Message} request và NHẬN response đồng bộ (synchronous)</li>
 *   <li>Lắng nghe PUSH event từ server (asynchronous) trong thread riêng</li>
 *   <li>Đóng kết nối (disconnect) gọn gàng</li>
 * </ul>
 *
 * <p><b>KIẾN TRÚC ĐỌC MESSAGE - QUAN TRỌNG:</b>
 * <p>Chỉ có DUY NHẤT 1 thread đọc từ {@code ObjectInputStream}
 * ({@code listenerThread}). Vì sao? ObjectInputStream KHÔNG thread-safe -
 * nếu nhiều thread cùng đọc → corrupt data.
 *
 * <p><b>CƠ CHẾ REQUEST-RESPONSE:</b>
 * <ol>
 *   <li>Code app gọi {@link #sendRequest}: gửi request + đợi response</li>
 *   <li>Lưu {@link CompletableFuture} vào {@link #pendingResponse}</li>
 *   <li>Thread listener đọc message từ socket:
 *     <ul>
 *       <li>Nếu là push event (BID_UPDATE/AUCTION_EVENT) → gọi pushHandler</li>
 *       <li>Nếu là response (SUCCESS/ERROR) → complete future</li>
 *     </ul>
 *   </li>
 *   <li>sendRequest unblock, trả response cho caller</li>
 * </ol>
 *
 * <p><b>TIMEOUT:</b> Mỗi request có timeout 15s - tránh treo vô hạn nếu server
 * không phản hồi (mất kết nối, server bị deadlock...).
 */
public class AuctionClient {

  // ===== HẰNG SỐ MẶC ĐỊNH =====
  private static final String DEFAULT_HOST = "localhost";
  private static final int DEFAULT_PORT = 9999;
  /**
   * Timeout 15s cho mỗi request - đủ để xử lý logic nặng.
   */
  private static final int REQUEST_TIMEOUT_SECONDS = 15;

  // ===== CONNECTION INFO =====
  private final String host;
  private final int port;
  private Socket socket;
  /**
   * Stream để gửi object (Message) ra server.
   */
  private ObjectOutputStream out;
  /**
   * Stream để nhận object từ server.
   */
  private ObjectInputStream in;
  /**
   * Cờ trạng thái - volatile để đảm bảo memory visibility giữa threads.
   */
  private volatile boolean connected;

  /**
   * Future của request đang chờ response.
   * Dùng 'volatile' để thao tác thread-safe:
   * thread sendRequest set, thread listener complete.
   */
  private volatile CompletableFuture<Message> pendingResponse = null;

  /**
   * Callback xử lý push event (BID_UPDATE, AUCTION_EVENT).
   */
  private Consumer<Message> pushHandler;
  /**
   * Thread chạy ngầm để đọc message từ server.
   */
  private Thread listenerThread;

  /**
   * Constructor mặc định - kết nối localhost:9999.
   */
  public AuctionClient() {
    this(DEFAULT_HOST, DEFAULT_PORT);
  }

  /**
   * Constructor với host/port custom (dùng cho test hoặc deploy server khác).
   */
  public AuctionClient(String host, int port) {
    this.host = host;
    this.port = port;
  }

  /**
   * Mở kết nối tới server.
   *
   * <p>Khi gọi xong:
   * <ul>
   *   <li>Socket được mở</li>
   *   <li>out/in stream đã sẵn sàng</li>
   *   <li>Thread listener chạy ngầm để nhận message</li>
   * </ul>
   *
   * @throws IOException nếu server không chạy hoặc network có lỗi
   */
  public void connect() throws IOException {
    // Mở socket TCP
    socket = new Socket(host, port);
    // QUAN TRỌNG: tạo ObjectOutputStream TRƯỚC ObjectInputStream
    // Vì OutputStream gửi header → InputStream phía bên kia mới đọc được header
    out = new ObjectOutputStream(socket.getOutputStream());
    in = new ObjectInputStream(socket.getInputStream());
    connected = true;

    // Tạo thread daemon đọc message bất đồng bộ
    // Daemon: tự động chết khi JVM thoát (không cần join thủ công)
    listenerThread = new Thread(this::listenForMessages, "AuctionClient-Listener");
    listenerThread.setDaemon(true);
    listenerThread.start();

    System.out.println("[Client] Đã kết nối tới " + host + ":" + port);
  }

  /**
   * Đặt callback xử lý các push event từ server.
   */
  public void setPushHandler(Consumer<Message> pushHandler) {
    this.pushHandler = pushHandler;
  }

  /**
   * Đóng kết nối tới server.
   * Set connected = false → thread listener sẽ thoát vòng lặp.
   */
  public void disconnect() {
    connected = false;
    try {
      if (socket != null && !socket.isClosed()) socket.close();
    } catch (IOException ignored) {
      // Đang disconnect, không cần báo lỗi
    }
    System.out.println("[Client] Đã ngắt kết nối.");
  }

  /**
   * Gửi single request lên server và đợi response.
   *
   * <p><b>synchronized:</b> Đảm bảo chỉ 1 request được gửi tại 1 thời điểm.
   * Vì 2 request gửi song song có thể nhầm response (response của A đến trước,
   * gán nhầm cho B).
   *
   * <p><b>out.reset():</b> Java Serialization có cache - nếu gửi cùng 1 object
   * 2 lần, lần thứ 2 chỉ gửi reference. Trong dự án có thể gửi cùng auction
   * object 2 lần với trạng thái khác nhau → phải reset() để gửi full data.
   *
   * @param request message gửi đi
   * @return message response từ server
   * @throws IOException nếu lỗi mạng, timeout, hoặc bị interrupt
   */
  public synchronized Message sendRequest(Message request) throws IOException {
    if (!connected) throw new IOException("Chưa kết nối tới server");

    // Tạo future để chờ response
    pendingResponse = new CompletableFuture<>();

    try {
      // Gửi request
      out.writeObject(request);
      out.flush();    // đẩy data ra socket ngay
      out.reset();    // xóa cache của ObjectOutputStream

      // Đợi response từ thread listener (với timeout)
      return pendingResponse.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

    } catch (java.util.concurrent.TimeoutException e) {
      // Server không trả lời sau 15s
      throw new IOException("Request hết thời gian chờ sau " + REQUEST_TIMEOUT_SECONDS + "s", e);
    } catch (java.util.concurrent.ExecutionException e) {
      // Future được complete với exception (vd: connection đóng)
      Throwable cause = e.getCause();
      throw new IOException("Lỗi xử lý phản hồi: " + cause.getMessage(), cause);
    } catch (InterruptedException e) {
      // Thread bị interrupt khi đang đợi
      Thread.currentThread().interrupt(); // Preserve interrupt flag
      throw new IOException("Request bị gián đoạn", e);
    } finally {
      // Luôn xóa pending sau khi xong
      pendingResponse = null;
    }
  }

  /**
   * Method chạy trong thread listener - đọc message từ server vô hạn.
   *
   * <p>Phân loại 2 loại message:
   * <ul>
   *   <li>PUSH event (AUCTION_EVENT/BID_UPDATE): gọi pushHandler ngay</li>
   *   <li>RESPONSE (SUCCESS/ERROR/data): complete future đang chờ</li>
   * </ul>
   *
   * <p>Vòng lặp thoát khi: connected = false, EOFException (server đóng),
   * hoặc lỗi khác.
   */
  private void listenForMessages() {
    while (connected) {
      try {
        // Đọc 1 object từ stream
        Object obj = in.readObject();
        // Kiểm tra kiểu (pattern matching for instanceof - Java 16+)
        if (!(obj instanceof Message message)) continue;

        Message.Type type = message.getType();
        // ===== PHÂN LOẠI MESSAGE =====
        if (type == Message.Type.AUCTION_EVENT || type == Message.Type.BID_UPDATE) {
          // Push event → forward cho pushHandler xử lý
          pushHandler.accept(message);
        } else {
          // Response của 1 request đang chờ
          if (pendingResponse != null) {
              pendingResponse.complete(message);
          } else {
            // Bất thường: nhận response mà không có request đang chờ
            System.err.println("[Client] Nhận phản hồi nhưng không có request đang chờ: " + type);
          }
        }

      } catch (EOFException e) {
        // Server đã đóng kết nối - không phải lỗi
        System.out.println("[Client] Server đã đóng kết nối.");
        break;
      } catch (Exception e) {
        // Các lỗi khác (corrupt data, network error...)
        if (connected) {
          // Chỉ log nếu KHÔNG phải do mình chủ động disconnect
          System.err.println("[Client] Lỗi nhận message: " + e.getMessage());
        }
        break;
      }
    }

    // ===== CLEANUP =====
    connected = false;
    // Nếu có future đang chờ → fail nó để caller không block vô hạn
    CompletableFuture<Message> pending = pendingResponse;
    pendingResponse = null;
    if (pending != null) {
      pending.completeExceptionally(new IOException("Kết nối bị đóng"));
    }
  }
}
