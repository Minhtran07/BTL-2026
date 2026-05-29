package com.auction.network.message.request;

import com.auction.network.message.Request;

/** Request lấy tất cả user (chỉ Admin). Marker class — không cần tham số. */
public class GetAllUsersRequest extends Request {
    private static final long serialVersionUID = 1L;

    public GetAllUsersRequest() {}
}
