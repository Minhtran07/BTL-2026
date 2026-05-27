package com.auction.network.message;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Lớp cơ sở cho mọi message truyền qua TCP socket giữa client và server.
 *
 * <p>Các lớp con cụ thể (LoginRequest, PlaceBidRequest, BidUpdatePush...)
 * kế thừa và cung cấp constructor có kiểu an toàn — loại bỏ việc nhồi dữ liệu
 * vào map tùy tiện.
 *
 * @see Request
 * @see Response
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 2L;

    public enum Type {
        LOGIN, REGISTER, LOGOUT,

        CREATE_ITEM, GET_ITEM, GET_ALL_ITEMS, UPDATE_ITEM, DELETE_ITEM,

        CREATE_AUCTION, GET_AUCTION, GET_ALL_AUCTIONS, GET_ACTIVE_AUCTIONS,
        GET_SELLER_AUCTIONS, END_AUCTION, CANCEL_AUCTION,

        PLACE_BID, REGISTER_AUTO_BID, GET_BID_HISTORY,

        SUBSCRIBE_AUCTION, UNSUBSCRIBE_AUCTION,

        BID_UPDATE, AUCTION_EVENT,

        SUCCESS, ERROR,

        GET_ALL_USERS, DEACTIVATE_USER
    }

    private Type type;
    private Map<String, String> data;
    private Serializable body;

    public Message() {
        this.data = new HashMap<>();
    }

    public Message(Type type) {
        this.type = type;
        this.data = new HashMap<>();
    }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public Map<String, String> getData() { return data; }
    public void setData(Map<String, String> data) { this.data = data; }

    public void put(String key, String value) {
        data.put(key, value);
    }

    public String get(String key) {
        return data.get(key);
    }

    public Serializable getBody() { return body; }
    public void setBody(Serializable body) { this.body = body; }

    @SuppressWarnings("unchecked")
    public <T> T body() {
        return (T) body;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{type=" + type + ", data=" + data
                + (body != null ? ", body=" + body.getClass().getSimpleName() : "")
                + "}";
    }
}
