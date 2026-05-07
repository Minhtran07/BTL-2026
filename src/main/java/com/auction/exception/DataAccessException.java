package com.auction.exception;


/**
 * Ngoại lệ truy cập dữ liệu.
 */
public class DataAccessException extends AuctionException {
    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
