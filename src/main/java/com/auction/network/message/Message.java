package com.auction.network.message;

import java.io.Serializable;

/**
 * Lớp cơ sở trừu tượng cho mọi message truyền qua TCP socket.
 *
 * <p>Sau khi refactor, lớp này KHÔNG còn chứa:
 * <ul>
 *   <li>Enum {@code Type} — phân loại bằng {@code instanceof} (đa hình)</li>
 *   <li>{@code Map<String, String> data} — dữ liệu nằm trong field của subclass</li>
 *   <li>{@code Serializable body} — body nằm trong {@link Response} generic</li>
 * </ul>
 *
 * <p>Dispatch trên server dùng {@code Map<Class<? extends Message>, RequestHandler>}
 * thay vì {@code EnumMap<Type, RequestHandler>} — tuân thủ Open/Closed Principle.
 *
 * @see Request
 * @see Response
 * @see com.auction.network.message.push.PushMessage
 */
public abstract class Message implements Serializable {

    private static final long serialVersionUID = 3L;
}
