package com.auction.network.message.request;

/** Request lấy 1 sản phẩm theo itemId. Public — không cần đăng nhập. */
public class GetItemRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String itemId;

    public GetItemRequest(String itemId) {
        this.itemId = itemId;
    }

    public String getItemId() { return itemId; }
}
