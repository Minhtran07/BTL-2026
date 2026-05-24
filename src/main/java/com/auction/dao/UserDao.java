package com.auction.dao;

import com.auction.model.user.User;

import java.util.Optional;

/**
 * DAO interface cho User.
 */
public interface UserDao extends GenericDao<User> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}