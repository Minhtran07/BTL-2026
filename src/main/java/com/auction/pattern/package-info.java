/**
 * Tập hợp các Design Pattern áp dụng trong hệ thống.
 *
 * <ul>
 *   <li>{@link com.auction.pattern.singleton} — Singleton:
 *       {@link com.auction.pattern.singleton.AuctionManager} đảm bảo
 *       1 instance quản lý vòng đời mọi phiên.</li>
 *   <li>{@link com.auction.pattern.observer} — Observer + Event Bus
 *       (2 kênh: per-auction và global) cho realtime bid updates.</li>
 *   <li>{@link com.auction.pattern.factory} — Factory Method với registry
 *       cho phép thêm Item mới mà không sửa code cũ (OCP).</li>
 *   <li>{@link com.auction.pattern.strategy} — Strategy cho thuật toán
 *       validate giá đấu (Standard / ReservePrice).</li>
 * </ul>
 */
package com.auction.pattern;
