package com.auction.network.client;

import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.transaction.BidTransaction;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Facade cho mọi API gọi tới server.
 *
 * <p>Mỗi method tạo một đối tượng Request cụ thể (LoginRequest,
 * PlaceBidRequest...) thay vì nhồi dữ liệu vào map chung — đảm bảo
 * kiểu an toàn tại thời điểm biên dịch.
 */
public class AuctionClientService {

    private static volatile AuctionClientService instance;
    private AuctionClient client;
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

    public void setClient(AuctionClient client) {
        this.client = client;
        this.client.setPushHandler(this::broadcastPush);
    }

    public void addPushListener(Listener listener) {
        listeners.add(listener);
    }

    public void removePushListener(Listener listener) {
        listeners.remove(listener);
    }

    public void broadcastPush(Message message) {
        for (Listener listener : listeners) {
            try {
                listener.onPush(message);
            } catch (Exception e) {
                System.err.println("[Client] Listener error: " + e.getMessage());
            }
        }
    }

    // ========== AUTH ==========

    public User login(String username, String password) throws IOException {
        Message resp = client.sendRequest(new LoginRequest(username, password));
        ensureSuccess(resp);
        return resp.body();
    }

    public User register(String username, String password, String email,
                         String fullName, String role) throws IOException {
        Message resp = client.sendRequest(
                new RegisterRequest(username, password, email, fullName, role));
        ensureSuccess(resp);
        return resp.body();
    }

    public void logout() throws IOException {
        client.sendRequest(new LogoutRequest());
    }

    // ========== ITEMS ==========

    public String createItem(String category, String name, String description,
                             double price, Map<String, String> extra) throws IOException {
        Message resp = client.sendRequest(
                new CreateItemRequest(category, name, description, price, extra));
        ensureSuccess(resp);
        return resp.get("itemId");
    }

    public Item getItem(String itemId) throws IOException {
        Message resp = client.sendRequest(new GetItemRequest(itemId));
        ensureSuccess(resp);
        return resp.body();
    }

    public List<Item> getAllItems() throws IOException {
        Message resp = client.sendRequest(new GetAllItemsRequest());
        ensureSuccess(resp);
        List<Item> body = resp.body();
        return body != null ? body : List.of();
    }

    public void updateItem(Item item) throws IOException {
        Message resp = client.sendRequest(new UpdateItemRequest(item));
        ensureSuccess(resp);
    }

    public void deleteItem(String itemId) throws IOException {
        Message resp = client.sendRequest(new DeleteItemRequest(itemId));
        ensureSuccess(resp);
    }

    // ========== AUCTIONS ==========

    public String createAuction(String itemId, String itemName, double startingPrice,
                                int durationMinutes) throws IOException {
        Message resp = client.sendRequest(
                new CreateAuctionRequest(itemId, itemName, startingPrice, durationMinutes));
        ensureSuccess(resp);
        return resp.get("auctionId");
    }

    public Auction getAuction(String auctionId) throws IOException {
        Message resp = client.sendRequest(new GetAuctionRequest(auctionId));
        ensureSuccess(resp);
        return resp.body();
    }

    public List<Auction> getAllAuctions() throws IOException {
        Message resp = client.sendRequest(new GetAllAuctionsRequest());
        ensureSuccess(resp);
        List<Auction> body = resp.body();
        return body != null ? body : List.of();
    }

    public List<Auction> getActiveAuctions() throws IOException {
        Message resp = client.sendRequest(new GetActiveAuctionsRequest());
        ensureSuccess(resp);
        List<Auction> body = resp.body();
        return body != null ? body : List.of();
    }

    public void endAuction(String auctionId) throws IOException {
        Message resp = client.sendRequest(new EndAuctionRequest(auctionId));
        ensureSuccess(resp);
    }

    public void cancelAuction(String auctionId) throws IOException {
        Message resp = client.sendRequest(new CancelAuctionRequest(auctionId));
        ensureSuccess(resp);
    }

    // ========== SUBSCRIPTION ==========

    public void subscribeAuction(String auctionId) throws IOException {
        Message resp = client.sendRequest(new SubscribeAuctionRequest(auctionId));
        ensureSuccess(resp);
    }

    public void unsubscribeAuction(String auctionId) throws IOException {
        Message resp = client.sendRequest(new UnsubscribeAuctionRequest(auctionId));
        ensureSuccess(resp);
    }

    // ========== BIDDING ==========

    public void placeBid(String auctionId, double amount) throws IOException {
        Message resp = client.sendRequest(new PlaceBidRequest(auctionId, amount));
        ensureSuccess(resp);
    }

    public void registerAutoBid(String auctionId, double maxBid, double increment)
            throws IOException {
        Message resp = client.sendRequest(
                new RegisterAutoBidRequest(auctionId, maxBid, increment));
        ensureSuccess(resp);
    }

    public List<BidTransaction> getBidHistory(String auctionId) throws IOException {
        Message resp = client.sendRequest(new GetBidHistoryRequest(auctionId));
        ensureSuccess(resp);
        List<BidTransaction> body = resp.body();
        return body != null ? body : List.of();
    }

    // ========== ADMIN ==========

    public List<User> getAllUsers() throws IOException {
        Message resp = client.sendRequest(new GetAllUsersRequest());
        ensureSuccess(resp);
        List<User> body = resp.body();
        return body != null ? body : List.of();
    }

    public void deactivateUser(String userId) throws IOException {
        Message resp = client.sendRequest(new DeactivateUserRequest(userId));
        ensureSuccess(resp);
    }

    // ========== HELPERS ==========

    private static void ensureSuccess(Message resp) throws IOException {
        if (resp == null) throw new IOException("Server không phản hồi");
        if (resp.getType() == Message.Type.ERROR) {
            String err = resp.get("error");
            throw new IOException(err != null ? err : "Lỗi không xác định");
        }
    }

    public interface Listener {
        void onPush(Message message);
    }
}
