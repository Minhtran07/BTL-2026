package com.auction.network.message.request;

import com.auction.network.message.Request;

/** Request lấy phiên đang hoạt động (OPEN/RUNNING). Marker class — không cần tham số. */
public class GetActiveAuctionsRequest extends Request {
    private static final long serialVersionUID = 1L;

    public GetActiveAuctionsRequest() {}
}
