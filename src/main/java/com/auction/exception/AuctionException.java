package com.auction.exception;



/**
 * Ngoại lệ cơ sở cho hệ thống đấu giá.
 */
public class AuctionException extends Exception {
    public AuctionException(String message) {
        super(message);
    }

    public AuctionException(String message, Throwable cause) {
        super(message, cause);
    }
}