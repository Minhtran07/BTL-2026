package com.auction.model.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public abstract class Entity implements Serializable { // Đã sửa lỗi 1 & 2

    private static final long serialVersionUID = 1L; // Đã sửa lỗi 3

    private final String id;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected Entity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    protected void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }

    public abstract String printInfo();
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()){
            return false;
        }
        Entity entity = (Entity) o;
        return Objects.equals(id, entity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return printInfo();
    }
}