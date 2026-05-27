package com.auction.network.client;

import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.transaction.BidTransaction;
import com.auction.model.user.User;
import com.auction.network.message.Message;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ============================================================================
 * AUCTIONCLIENTSERVICE - FACADE CHO MỌI API GỌI TỚI SERVER
 * ============================================================================
 *
 * <p>Singleton này là điểm tập trung MỌI gọi network từ client. Controllers
 * KHÔNG dùng trực tiếp AuctionClient mà gọi qua service này:
 * <ul>
 *   <li>{@code service.login(...)} thay vì {@code client.sendRequest(...)}</li>
 *   <li>{@code service.placeBid(...)} với signature thân thiện thay vì Message thô</li>
 * </ul>
 *
 * <p><b>VAI TRÒ FACADE PATTERN:</b>
 * <ul>
 *   <li>Đóng gói: ẩn chi tiết Message/protocol</li>
 *   <li>Type-safe: trả về Object đúng kiểu (User, Item, List<Auction>...)</li>
 *   <li>Error handling tập trung: {@code ensureSuccess()} convert ERROR
 *       message → IOException</li>
 * </ul>
 *
 * <p><b>PUSH NOTIFICATION:</b>
 * <ul>
 *   <li>{@link #setClient(AuctionClient)}: gắn pushHandler để forward event</li>
 *   <li>Controllers gọi {@link #addPushListener(Listener)} để nhận event</li>
 *   <li>Khi server đẩy event → broadcastPush() → notify mọi listener</li>
 * </ul>
 */
public class AuctionClientService {

  private static volatile AuctionClientService instance;
  /** Client socket - thực hiện gọi network thực sự. */
  private AuctionClient client;
  /** Danh sách listener nhận push event (CopyOnWriteArrayList - thread-safe). */
  private final List<Listener> listeners = new CopyOnWriteArrayList<>();

  private AuctionClientService() {}

  public static AuctionClientService getInstance() {
    if (instance == null) {
      synchronized (AuctionClientService.class) {
        if (instance == null) {
          instance = new AuctionClientService();
        }
      }
    }
    return instance;
  }

  /**
   * Gắn AuctionClient vào service - gọi 1 lần khi MainApp khởi động.
   * Đồng thời đăng ký pushHandler để service tự forward event cho listeners.
   */
  public void setClient(AuctionClient client) {
    this.client = client;
    // Method reference: this::broadcastPush ≡ msg -> this.broadcastPush(msg)
    this.client.setPushHandler(this::broadcastPush);
  }

  /** Đăng ký listener nhận push event - controller gọi khi vào màn hình realtime. */
  public void addPushListener(Listener listener) {
    listeners.add(listener);
  }

  /** Hủy đăng ký - controller gọi khi rời màn hình (tránh memory leak). */
  public void removePushListener(Listener listener) {
    listeners.remove(listener);
  }

  /**
   * Phát push event cho tất cả listener đang đăng ký.
   * Try-catch quanh từng listener để 1 listener lỗi không làm dừng các listener khác.
   */
  public void broadcastPush(Message message) {
    for (Listener listener : listeners) {
      try {
        listener.onPush(message);
      } catch (Exception e) {
        System.err.println("[Client] Listener error: " + e.getMessage());
      }
    }
  }

  // ==========================================================================
  // AUTH APIs (LOGIN/REGISTER/LOGOUT)
  // ==========================================================================

  /**
   * Login với username/password.
   * @return User đã đăng nhập (có đầy đủ field).
   * @throws IOException nếu sai mật khẩu, network lỗi, hoặc account bị khóa
   */
  public User login(String username, String password) throws IOException {
    Message msg = new Message(Message.Type.LOGIN);
    msg.put("username", username);
    msg.put("password", password);
    Message resp = client.sendRequest(msg);
    ensureSuccess(resp);
    return resp.body();
  }
  public User register(String username, String password, String email,
                       String fullName, String role) throws IOException {
    Message msg = new Message(Message.Type.REGISTER);
    msg.put("username", username);
    msg.put("password", password);
    msg.put("email", email);
    msg.put("fullName", fullName);
    msg.put("role", role);
    Message resp = client.sendRequest(msg);
    ensureSuccess(resp);
    return resp.body();
  }

  public void logout() throws IOException {
    client.sendRequest(new Message(Message.Type.LOGOUT));
  }

  // ----- Items -----

  public String createItem(String category, String name, String description,
                           double price, Map<String, String> extra) throws IOException {
    Message msg = new Message(Message.Type.CREATE_ITEM);
    msg.put("category", category);
    msg.put("name", name);
    msg.put("description", description);
    msg.put("price", String.valueOf(price));
    if (extra != null) {
      for (Map.Entry<String, String> e : extra.entrySet()) {
        msg.put(e.getKey(), e.getValue());
      }
    }
    Message resp = client.sendRequest(msg);
    ensureSuccess(resp);
    return resp.get("itemId");
  }

  public Item getItem(String itemId) throws IOException {
    Message msg = new Message(Message.Type.GET_ITEM);
    msg.put("itemId", itemId);
    Message resp = client.sendRequest(msg);
    ensureSuccess(resp);
    return resp.body();
  }

  public List<Item> getAllItems() throws IOException {
    Message resp = client.sendRequest(new Message(Message.Type.GET_ALL_ITEMS));
    ensureSuccess(resp);
    List<Item> body = resp.body();
    return body != null ? body : List.of();
  }

  public void updateItem(Item item) throws IOException {
    Message msg = new Message(Message.Type.UPDATE_ITEM);
    msg.setBody(item);
    Message resp = client.sendRequest(msg);
    ensureSuccess(resp);
  }

  public void deleteItem(String itemId) throws IOException {
    Message msg = new Message(Message.Type.DELETE_ITEM);
    msg.put("itemId", itemId);
    Message resp = client.sendRequest(msg);
    ensureSuccess(resp);
  }

  // ----- Auctions -----

  public String createAuction(String itemId, String itemName, double startingPrice,
                              int durationMinutes) throws IOException {
    Message msg = new Message(Message.Type.CREATE_AUCTION);
    msg.put("itemId", itemId);
    msg.put("itemName", itemName);
    msg.put("startingPrice", String.valueOf(startingPrice));
    msg.put("duration", String.valueOf(durationMinutes));
    Message resp = client.sendRequest(msg);
    ensureSuccess(resp);
    return resp.get("auctionId");
  }

  public Auction getAuction(String auctionId) throws IOException {
    Message msg = new Message(Message.Type.GET_AUCTION);
    msg.put("auctionId", auctionId);
    Message resp = client.sendRequest(msg);
    ensureSuccess(resp);
    return resp.body();
  }

  public List<Auction> getAllAuctions() throws IOException {
    Message resp = client.sendRequest(new Message(Message.Type.GET_ALL_AUCTIONS));
    ensureSuccess(resp);
    List<Auction> body = resp.body();
    return body != null ? body : List.of();
  }

  public List<Auction> getActiveAuctions() throws IOException {
    Message resp = client.sendRequest(new Message(Message.Type.GET_ACTIVE_AUCTIONS));
    ensureSuccess(resp);
    List<Auction> body = resp.body();
    return body != null ? body : List.of();
  }

  public void endAuction(String auctionId) throws IOException {
    Message msg = new Message(Message.Type.END_AUCTION);
    msg.put("auctionId", auctionId);
    Message resp = client.sendRequest(msg);
    ensureSuccess(resp);
  }

  public void cancelAuction(String auctionId) throws IOException {
    Message msg = new Message(Message.Type.CANCEL_AUCTION);
    msg.put("auctionId", auctionId);
    Message resp = client.sendRequest(msg);
    ensureSuccess(resp);
  }

  // ----- Per-auction subscription (Observer Pattern) -----

  /**
   * Đăng ký nhận realtime event cho một phiên cụ thể. Sau lệnh này, server sẽ
   * push BID_UPDATE / AUCTION_EVENT của phiên đó về client; các phiên khác
   * <em>không</em> được gửi cho client này.
   */
  public void subscribeAuction(String auctionId) throws IOException {
    Message msg = new Message(Message.Type.SUBSCRIBE_AUCTION);
    msg.put("auctionId", auctionId);
    Message resp = client.sendRequest(msg);
    ensureSuccess(resp);
  }

  /** Hủy subscribe phiên — gọi khi client rời màn hình chi tiết phiên. */
  public void unsubscribeAuction(String auctionId) throws IOException {
    Message msg = new Message(Message.Type.UNSUBSCRIBE_AUCTION);
    msg.put("auctionId", auctionId);
    Message resp = client.sendRequest(msg);
    ensureSuccess(resp);
  }

  // ----- Bidding -----

  // ==========================================================================
  // BIDDING APIs
  // ==========================================================================

  /**
   * Đặt giá đấu cho 1 phiên.
   *
   * @param auctionId ID phiên đấu giá
   * @param amount    số tiền đặt (phải > giá hiện tại)
   * @throws IOException nếu bid không hợp lệ (giá thấp, phiên đã đóng...) hoặc network lỗi
   */
  public void placeBid(String auctionId, double amount) throws IOException {
    Message msg = new Message(Message.Type.PLACE_BID);
    msg.put("auctionId", auctionId);
    msg.put("amount", String.valueOf(amount));
    Message resp = client.sendRequest(msg);
    ensureSuccess(resp);
  }

  public void registerAutoBid(String auctionId, double maxBid, double increment) throws IOException {
    Message msg = new Message(Message.Type.REGISTER_AUTO_BID);
    msg.put("auctionId", auctionId);
    msg.put("maxBid", String.valueOf(maxBid));
    msg.put("increment", String.valueOf(increment));
    Message resp = client.sendRequest(msg);
    ensureSuccess(resp);
  }

  public List<BidTransaction> getBidHistory(String auctionId) throws IOException {
    Message msg = new Message(Message.Type.GET_BID_HISTORY);
    msg.put("auctionId", auctionId);
    Message resp = client.sendRequest(msg);
    ensureSuccess(resp);
    List<BidTransaction> body = resp.body();
    return body != null ? body : List.of();
  }

  // ----- Admin -----

  public List<User> getAllUsers() throws IOException {
    Message resp = client.sendRequest(new Message(Message.Type.GET_ALL_USERS));
    ensureSuccess(resp);
    List<User> body = resp.body();
    return body != null ? body : List.of();
  }

  public void deactivateUser(String userId) throws IOException {
    Message msg = new Message(Message.Type.DEACTIVATE_USER);
    msg.put("userId", userId);
    Message resp = client.sendRequest(msg);
    ensureSuccess(resp);
  }

  // ==========================================================================
  // HELPER METHODS
  // ==========================================================================

  /**
   * Kiểm tra response - ném IOException nếu là ERROR.
   *
   * <p>Helper này được mọi API public gọi để xử lý lỗi tập trung.
   * Caller (controller) bắt IOException và hiển thị message cho user.
   *
   * @param resp response từ server
   * @throws IOException nếu response null hoặc type = ERROR
   */
  private static void ensureSuccess(Message resp) throws IOException {
    if (resp == null) throw new IOException("Server không phản hồi");
    if (resp.getType() == Message.Type.ERROR) {
      String err = resp.get("error");
      throw new IOException(err != null ? err : "Lỗi không xác định");
    }
  }

  /**
   * Functional interface cho listener nhận push event từ server.
   * Controller dùng lambda: {@code service.addPushListener(msg -> {...})}
   */
  public interface Listener {
      /**
       * Callback khi có push event (BID_UPDATE, AUCTION_EVENT).
       * <b>Lưu ý:</b> được gọi từ thread network, KHÔNG được update UI trực tiếp.
       * Phải dùng Platform.runLater() để chuyển sang JavaFX thread.
       */
      void onPush(Message message);
  }
}
