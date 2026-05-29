/**
 * ============================================================================
 * PACKAGE: REQUEST CLASSES
 * ============================================================================
 *
 * <p>Chứa tất cả request message cụ thể mà client gửi lên server.
 * Mỗi class là 1 POJO immutable (final fields) kế thừa {@link com.auction.network.message.Request}.
 *
 * <h3>REFACTORING (từ Message hierarchy cũ):</h3>
 * <ul>
 *   <li><b>Trước:</b> Client tạo {@code new Message(Type.LOGIN)} rồi đặt
 *       data vào map: {@code msg.put("username", "abc")}, {@code msg.put("password", "123")}.
 *       Server đọc lại bằng {@code request.get("username")} — không type-safe,
 *       dễ sai key, thiếu field.</li>
 *   <li><b>Sau:</b> Mỗi loại request là 1 class riêng (LoginRequest, PlaceBidRequest...),
 *       chứa typed fields với getter. Constructor nhận đầy đủ tham số →
 *       compile-time check, IDE autocomplete, không bao giờ thiếu field.</li>
 * </ul>
 *
 * <h3>DISPATCH:</h3>
 * <p>Server dùng {@code Map<Class<? extends Message>, RequestHandler>} để map
 * class → handler. Khi nhận request, lookup bằng {@code request.getClass()}.
 * Handler dùng {@code instanceof} pattern matching (Java 16+) để ép kiểu.
 *
 * <h3>SERIALIZATION:</h3>
 * <p>Tất cả class đều {@code implements Serializable} (thông qua kế thừa
 * Message → Request). Mỗi class có {@code serialVersionUID = 1L}.
 * Truyền qua TCP socket bằng {@code ObjectOutputStream / ObjectInputStream}.
 *
 * <h3>DANH SÁCH REQUEST:</h3>
 * <ul>
 *   <li>Auth: LoginRequest, RegisterRequest, LogoutRequest</li>
 *   <li>Item: CreateItemRequest, UpdateItemRequest, DeleteItemRequest,
 *       GetItemRequest, GetAllItemsRequest</li>
 *   <li>Auction: CreateAuctionRequest, GetAuctionRequest, GetAllAuctionsRequest,
 *       GetActiveAuctionsRequest, EndAuctionRequest, CancelAuctionRequest</li>
 *   <li>Bid: PlaceBidRequest, RegisterAutoBidRequest, GetBidHistoryRequest</li>
 *   <li>Push: SubscribeAuctionRequest, UnsubscribeAuctionRequest</li>
 *   <li>Admin: DeactivateUserRequest, GetAllUsersRequest</li>
 * </ul>
 */
package com.auction.network.message.request;
