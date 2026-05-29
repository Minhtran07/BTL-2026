package com.auction.network.message.request;

import com.auction.network.message.Request;

/** Request lấy tất cả phiên đấu giá. Không cần tham số — marker class. */
public class GetAllAuctionsRequest extends Request {
    private static final long serialVersionUID = 1L;

    public GetAllAuctionsRequest() {}
}
