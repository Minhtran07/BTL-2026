package com.auction.network.message;

import java.util.UUID;

/**
 * Lớp trừu tượng cho mọi request từ client gửi lên server.
 * Mỗi request tự sinh UUID duy nhất để đối chiếu với response.
 */
public abstract class Request extends Message {

    private static final long serialVersionUID = 3L;

    private final String requestId;

    protected Request(Type type) {
        super(type);
        this.requestId = UUID.randomUUID().toString();
    }

    public String getRequestId() { return requestId; }
}
