package com.auction.network.message.request;

import com.auction.model.item.Item;
import com.auction.network.message.Request;

/**
 * Request cập nhật sản phẩm — gửi full Item object (Electronics/Art/Vehicle).
 * Handler: {@link com.auction.network.handler.UpdateItemHandler}
 * <p>Handler kiểm tra quyền: chỉ seller sở hữu hoặc Admin mới sửa được.
 */
public class UpdateItemRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final Item item;

    public UpdateItemRequest(Item item) {
        this.item = item;
    }

    public Item getItem() { return item; }
}
