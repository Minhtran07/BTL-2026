package com.auction.network.message;

import java.io.Serializable;

/**
 * Response generic trả về từ server cho client.
 *
 * <p>Tham số kiểu {@code T} cho phép ép kiểu an toàn tại compile-time:
 * <pre>
 *   Response&lt;User&gt; resp = Response.success(user);
 *   User u = resp.getBody(); // không cần cast
 * </pre>
 *
 * <p>Ba factory method {@link #success()}, {@link #success(Serializable)},
 * {@link #error(String)} thay cho constructor trực tiếp — đảm bảo trạng thái
 * hợp lệ và loại bỏ rủi ro quên set field.
 *
 * @param <T> kiểu dữ liệu body trả về (User, Item, List&lt;Auction&gt;...)
 */
public class Response<T extends Serializable> extends Message {

    private static final long serialVersionUID = 3L;

    private final boolean success;
    private final String errorMessage;
    private final T body;

    private Response(boolean success, String errorMessage, T body) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.body = body;
    }

    /** Tạo response thành công không có body (cho các thao tác void). */
    public static <T extends Serializable> Response<T> success() {
        return new Response<>(true, null, null);
    }

    /** Tạo response thành công kèm body dữ liệu. */
    public static <T extends Serializable> Response<T> success(T body) {
        return new Response<>(true, null, body);
    }

    /** Tạo response lỗi kèm thông báo. */
    public static <T extends Serializable> Response<T> error(String message) {
        return new Response<>(false, message, null);
    }

    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    public T getBody() { return body; }
}
