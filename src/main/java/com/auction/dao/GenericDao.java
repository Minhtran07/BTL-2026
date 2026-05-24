package com.auction.dao;

import java.util.List;
import java.util.Optional;

/**
 * Generic DAO interface - lớp trừu tượng cho truy cập dữ liệu.
 *
 * @param <T> kiểu entity
 */
public interface GenericDao<T> {

    void save(T entity);

    Optional<T> findById(String id);

    List<T> findAll();

    void update(T entity);

    void delete(String id);
}