package com.auction.network.message.request;

/**
 * Request tạo phiên đấu giá mới — chứa itemId, itemName, giá khởi điểm, thời lượng.
 * Handler: {@link com.auction.network.handler.CreateAuctionHandler}
 */
public class CreateAuctionRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String itemId;
    private final String itemName;
    private final double startingPrice;
    private final int durationMinutes;

    public CreateAuctionRequest(String itemId, String itemName,
                                double startingPrice, int durationMinutes) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.startingPrice = startingPrice;
        this.durationMinutes = durationMinutes;
    }

    public String getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public double getStartingPrice() { return startingPrice; }
    public int getDurationMinutes() { return durationMinutes; }
}
