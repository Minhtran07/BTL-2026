package com.auction.model.user;

import com.auction.model.entity.Entity;

/**
 * Lớp trừu tượng User - kế thừa Entity.
 * Áp dụng Inheritance và Encapsulation.
 */

public abstract class User extends Entity{
    private static final long serialVersionUID = 1L;
    private String username;
    private String password;
    private String email;
    private String fullName;
    private boolean active;

    protected User(){
        super();
        active = true;
    }
    protected User(String username, String password, String email, String fullName){
        super();
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.active = true;
    }
    // Encapsulation - private fields with getters/setters
    public String getUsername(){
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        markUpdated();
    }
    public String getPassword(){
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
        markUpdated();
    }
    public String getFullName(){
        return fullName;
    }
    public void setFullName(String fullName){
        this.fullName = fullName;
        markUpdated();
    }
    public boolean isActive(){
        return active;
    }
    public void setActive(boolean active){
        this.active = active;
        markUpdated();
    }
    /**
     * Phương thức trừu tượng trả về vai trò người dùng - Polymorphism.
     */
    public abstract UserRole getRole();
    @Override
    public String printInfo(){
        return String.format("User[id=%s, username=%s, role=%s, email=%s, fullName=%s]",getId(), username, getRole(), email, fullName );

    }

}
