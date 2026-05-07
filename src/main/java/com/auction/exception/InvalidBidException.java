package com.auction.exception;


/**
 * Ngoại lệ khi đặt giá không hợp lệ.
 */
public class InvalidBidException extends AuctionException {
    public InvalidBidException(String message) {
        super(message);
    }
}