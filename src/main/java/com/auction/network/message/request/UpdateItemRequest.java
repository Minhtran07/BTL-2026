package com.auction.network.message.request;

import com.auction.model.item.Item;
import com.auction.network.message.Request;

public class UpdateItemRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final Item item;

    public UpdateItemRequest(Item item) {
        super(Type.UPDATE_ITEM);
        this.item = item;
        setBody(item);
    }

    public Item getItem() { return item; }
}
