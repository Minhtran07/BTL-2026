package com.auction.network.message;

import java.io.Serializable;

/**
 * Response trả về từ server cho client.
 * Hai factory method {@link #success} và {@link #error} thay cho constructor
 * trực tiếp — đảm bảo không quên set type.
 */
public class Response extends Message {

    private static final long serialVersionUID = 3L;

    private final boolean success;
    private final String errorMessage;

    private Response(Type type, boolean success, String errorMessage) {
        super(type);
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static Response success() {
        return new Response(Type.SUCCESS, true, null);
    }

    public static Response success(Serializable body) {
        Response r = new Response(Type.SUCCESS, true, null);
        r.setBody(body);
        return r;
    }

    public static Response error(String message) {
        Response r = new Response(Type.ERROR, false, message);
        r.put("error", message);
        return r;
    }

    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
}
