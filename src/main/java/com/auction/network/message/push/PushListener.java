package com.auction.network.message.push;

import com.auction.network.message.Message;

/**
 * ============================================================================
 * PUSHLISTENER - INTERFACE NHẬN PUSH EVENT TỪ SERVER
 * ============================================================================
 *
 * <p>Functional interface (1 method abstract) - cho phép {@link PushBroadcaster}
 * gửi message tới các observer mà không cần biết chúng là gì.
 *
 * <p><b>Implementation chính:</b> {@code AuctionServer.ClientHandler} - mỗi
 * ClientHandler đại diện cho 1 client kết nối, implement PushListener để
 * nhận event và forward sang client của nó qua socket.
 *
 * <p>Tách interface giúp:
 * <ul>
 *   <li>Tránh circular dependency giữa PushForwarder và AuctionServer</li>
 *   <li>Dễ test (có thể mock PushListener)</li>
 *   <li>Tuân thủ Dependency Inversion Principle</li>
 * </ul>
 */
public interface PushListener {

    /**
     * Callback khi có push event cần gửi đến client.
     * @param message message cần forward sang client
     */
    void onPush(Message message);
}
