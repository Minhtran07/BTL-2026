package com.auction.network.server;

import com.auction.network.Message;

/**
 * ============================================================================
 * PUSHLISTENER - INTERFACE NHẬN PUSH EVENT TỪ SERVER
 * ============================================================================
 *
 * <p>Functional interface (1 method abstract) - cho phép {@link PushForwarder}
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
