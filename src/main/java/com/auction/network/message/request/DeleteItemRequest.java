package com.auction.network.message.request;

import com.auction.network.message.Request;

public class DeleteItemRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String itemId;

    public DeleteItemRequest(String itemId) {
        super(Type.DELETE_ITEM);
        this.itemId = itemId;
        put("itemId", itemId);
    }

    public String getItemId() { return itemId; }
}
