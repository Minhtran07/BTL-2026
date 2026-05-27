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
        String username;
        String password;

        if (request instanceof LoginRequest loginReq) {
            username = loginReq.getUsername();
            password = loginReq.getPassword();
        } else {
            username = request.get("username");
            password = request.get("password");
        }

        try {
            User user = userService.login(username, password);
            this.lastAuthenticatedUser = user;

            Response response = Response.success(user);
            response.put("userId", user.getId());
            response.put("role", user.getRole().name());
            return response;
        } catch (AuthenticationException e) {
            return Response.error(e.getMessage());
        }
    }

    public User getLastAuthenticatedUser() {
        return lastAuthenticatedUser;
    }
}
