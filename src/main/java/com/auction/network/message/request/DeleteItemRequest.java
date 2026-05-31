package com.auction.network.message.request;

/**
 * Request xóa sản phẩm theo ID.
 * Handler: {@link com.auction.network.handler.DeleteItemHandler}
 * <p>Handler kiểm tra quyền (seller/admin) và ràng buộc FK (auction tham chiếu).
 */
public class DeleteItemRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String itemId;

    public DeleteItemRequest(String itemId) {
        this.itemId = itemId;
    }

    public String getItemId() { return itemId; }
}
