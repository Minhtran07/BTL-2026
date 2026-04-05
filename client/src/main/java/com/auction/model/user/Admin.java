package com.auction.model.user;

/**
 * Lớp Admin - quản trị hệ thống.
 * Kế thừa User - Polymorphism.
 */
public class Admin extends User {

    private static final long serialVersionUID = 1L;

    public Admin() {
        super();
    }

    public Admin(String username, String password, String email, String fullName) {
        super(username, password, email, fullName);
    }

    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
    }

    @Override
    public String printInfo() {
        return String.format("Admin[id=%s, username=%s]", getId(), getUsername());
    }
}
