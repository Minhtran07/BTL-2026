package com.auction.exception;


/**
 * Ngoại lệ xác thực người dùng.
 */
public class AuthenticationException extends AuctionException {
    public AuthenticationException(String message) {
        super(message);
    }
}