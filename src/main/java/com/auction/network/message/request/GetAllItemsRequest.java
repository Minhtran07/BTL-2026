package com.auction.network.message.request;

import com.auction.network.message.Request;

public class GetAllItemsRequest extends Request {
    private static final long serialVersionUID = 1L;

    public GetAllItemsRequest() {
        super(Type.GET_ALL_ITEMS);
    }
}
