package com.auction.pattern.observer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================================
 * AUCTIONEVENTDISPATCHERTEST - TEST OBSERVER PATTERN SUBJECT
 * ============================================================================
 *
 * <p>Test toàn bộ AuctionEventDispatcher - "trái tim" của Observer Pattern
 * trong hệ thống.
 *
 * <p><b>Concurrency test:</b> dùng CountDownLatch để đợi observer chạy xong
 * trước khi assert (vì dispatch có thể async).
 *
 * <p>Bao phủ:
 * <ul>
 *   <li>Singleton + reset cho test (mỗi test bắt đầu với state sạch)</li>
 *   <li>subscribe/unsubscribe per-auction</li>
 *   <li>subscribeGlobal/unsubscribeGlobal</li>
 *   <li>dispatch không bị crash khi observer ném exception</li>
 *   <li>clearAuctionObservers</li>
 *   <li>Thread-safety cơ bản với concurrent subscribe + dispatch</li>
 * </ul>
 */
class AuctionEventDispatcherTest {

    private AuctionEventDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        AuctionEventDispatcher.resetInstance();
        dispatcher = AuctionEventDispatcher.getInstance();
    }

    /**
     * Observer ghi lại event nhận được.
     * Dùng {@link CopyOnWriteArrayList} để thread-safe — test concurrent dispatch
     * có nhiều thread cùng gọi {@code add()}; với {@code ArrayList} thường races
     * có thể làm mất phần tử (count nhỏ hơn kỳ vọng).
     */
    static class RecordingObserver implements AuctionObserver {
        final List<AuctionEvent> received = new CopyOnWriteArrayList<>();

        @Override
        public void onAuctionEvent(AuctionEvent event) {
            received.add(event);
        }
    }

    /** Observer luôn ném RuntimeException. */
    static class FailingObserver implements AuctionObserver {
        @Override
        public void onAuctionEvent(AuctionEvent event) {
            throw new RuntimeException("intentional failure");
        }
    }

    private AuctionEvent eventFor(String auctionId) {
        return new AuctionEvent(AuctionEvent.EventType.NEW_BID, auctionId, null, "bid");
    }

    @Test
    void getInstance_shouldReturnSameInstance() {
        AuctionEventDispatcher a = AuctionEventDispatcher.getInstance();
        AuctionEventDispatcher b = AuctionEventDispatcher.getInstance();
        assertSame(a, b);
    }

    @Test
    void resetInstance_shouldGiveNewInstance() {
        AuctionEventDispatcher first = AuctionEventDispatcher.getInstance();
        AuctionEventDispatcher.resetInstance();
        AuctionEventDispatcher second = AuctionEventDispatcher.getInstance();
        assertNotSame(first, second);
    }

    @Test
    void subscribe_shouldReceiveOnlyMatchingAuctionEvents() {
        RecordingObserver obs = new RecordingObserver();
        dispatcher.subscribe("auction-1", obs);

        dispatcher.dispatch(eventFor("auction-1"));
        dispatcher.dispatch(eventFor("auction-2")); // không liên quan

        assertEquals(1, obs.received.size());
        assertEquals("auction-1", obs.received.get(0).getAuctionId());
    }

    @Test
    void unsubscribe_shouldStopReceivingEvents() {
        RecordingObserver obs = new RecordingObserver();
        dispatcher.subscribe("auction-x", obs);
        dispatcher.dispatch(eventFor("auction-x"));
        assertEquals(1, obs.received.size());

        dispatcher.unsubscribe("auction-x", obs);
        dispatcher.dispatch(eventFor("auction-x"));
        assertEquals(1, obs.received.size(), "Sau unsubscribe không nhận thêm");
    }

    @Test
    void unsubscribe_unknownAuction_shouldBeNoOp() {
        RecordingObserver obs = new RecordingObserver();
        // Không subscribe gì cả — chỉ kiểm tra không ném exception
        assertDoesNotThrow(() -> dispatcher.unsubscribe("missing", obs));
    }

    @Test
    void globalObserver_shouldReceiveAllAuctions() {
        RecordingObserver obs = new RecordingObserver();
        dispatcher.subscribeGlobal(obs);

        dispatcher.dispatch(eventFor("a1"));
        dispatcher.dispatch(eventFor("a2"));
        dispatcher.dispatch(eventFor("a3"));

        assertEquals(3, obs.received.size());
    }

    @Test
    void unsubscribeGlobal_shouldStopReceiving() {
        RecordingObserver obs = new RecordingObserver();
        dispatcher.subscribeGlobal(obs);
        dispatcher.dispatch(eventFor("a"));
        dispatcher.unsubscribeGlobal(obs);
        dispatcher.dispatch(eventFor("a"));
        assertEquals(1, obs.received.size());
    }

    @Test
    void dispatch_shouldContinueAfterObserverThrows() {
        FailingObserver failing = new FailingObserver();
        RecordingObserver ok = new RecordingObserver();
        dispatcher.subscribe("auction-1", failing);
        dispatcher.subscribe("auction-1", ok);

        assertDoesNotThrow(() -> dispatcher.dispatch(eventFor("auction-1")));
        assertEquals(1, ok.received.size(),
                "Observer 'ok' vẫn nhận được dù observer trước throw");
    }

    @Test
    void dispatch_shouldContinueAfterGlobalObserverThrows() {
        FailingObserver failing = new FailingObserver();
        RecordingObserver ok = new RecordingObserver();
        dispatcher.subscribeGlobal(failing);
        dispatcher.subscribeGlobal(ok);

        assertDoesNotThrow(() -> dispatcher.dispatch(eventFor("a")));
        assertEquals(1, ok.received.size());
    }

    @Test
    void clearAuctionObservers_shouldRemoveAllForAuction() {
        RecordingObserver obs1 = new RecordingObserver();
        RecordingObserver obs2 = new RecordingObserver();
        dispatcher.subscribe("auction-1", obs1);
        dispatcher.subscribe("auction-1", obs2);

        dispatcher.clearAuctionObservers("auction-1");
        dispatcher.dispatch(eventFor("auction-1"));

        assertEquals(0, obs1.received.size());
        assertEquals(0, obs2.received.size());
    }

    @Test
    void multipleObserversOnSameAuction_shouldAllReceive() {
        RecordingObserver a = new RecordingObserver();
        RecordingObserver b = new RecordingObserver();
        RecordingObserver c = new RecordingObserver();
        dispatcher.subscribe("auction-1", a);
        dispatcher.subscribe("auction-1", b);
        dispatcher.subscribe("auction-1", c);

        dispatcher.dispatch(eventFor("auction-1"));

        assertEquals(1, a.received.size());
        assertEquals(1, b.received.size());
        assertEquals(1, c.received.size());
    }

    @Test
    void concurrentDispatch_shouldNotThrow() throws InterruptedException {
        RecordingObserver obs = new RecordingObserver();
        dispatcher.subscribeGlobal(obs);

        int threads = 8;
        int eventsPerThread = 50;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    for (int j = 0; j < eventsPerThread; j++) {
                        dispatcher.dispatch(eventFor("a" + j));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }

        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS), "Mọi thread phải xong trong 5s");
        assertEquals(threads * eventsPerThread, obs.received.size());
    }
}
