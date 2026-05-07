package com.auction.exception;


/**
 * Ngoại lệ khi đấu giá trên phiên đã đóng.
 */
public class AuctionClosedException extends AuctionException {
    public AuctionClosedException(String message) {
        super(message);
    }
}