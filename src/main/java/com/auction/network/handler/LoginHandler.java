package com.auction.network.handler;

import com.auction.exception.AuthenticationException;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.LoginRequest;
import com.auction.service.UserService;

public class LoginHandler implements RequestHandler {

    private final UserService userService;
    private volatile User lastAuthenticatedUser;

    public LoginHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (!(request instanceof LoginRequest loginReq)) {
            return HandlerUtils.error("Request không hợp lệ");
        }

        try {
            User user = userService.login(loginReq.getUsername(), loginReq.getPassword());
            this.lastAuthenticatedUser = user;
            return Response.success(user);
        } catch (AuthenticationException e) {
            return Response.error(e.getMessage());
        }
    }

    public User getLastAuthenticatedUser() {
        return lastAuthenticatedUser;
    }
}
