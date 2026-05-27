package com.auction.network.message.request;

import com.auction.network.message.Request;

public class CreateAuctionRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String itemId;
    private final String itemName;
    private final double startingPrice;
    private final int durationMinutes;

    public CreateAuctionRequest(String itemId, String itemName,
                                double startingPrice, int durationMinutes) {
        super(Type.CREATE_AUCTION);
        this.itemId = itemId;
        this.itemName = itemName;
        this.startingPrice = startingPrice;
        this.durationMinutes = durationMinutes;
        put("itemId", itemId);
        put("itemName", itemName);
        put("startingPrice", String.valueOf(startingPrice));
        put("duration", String.valueOf(durationMinutes));
    }

    public String getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public double getStartingPrice() { return startingPrice; }
    public int getDurationMinutes() { return durationMinutes; }
}
