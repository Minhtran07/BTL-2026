package com.auction.model.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Lớp trừu tượng cơ sở cho mọi thực thể (entity) trong domain của hệ thống
 * đấu giá trực tuyến.
 *
 * <p>Vai trò domain:
 * <ul>
 *   <li>Định nghĩa danh tính (identity) duy nhất cho mỗi entity thông qua
 *       trường {@code id} (UUID string).</li>
 *   <li>Cung cấp metadata audit ({@code createdAt}, {@code updatedAt}) để
 *       theo dõi vòng đời entity – hữu ích cho concurrency check, sync
 *       multi-JVM và logging.</li>
 *   <li>Áp dụng nguyên tắc Abstraction (phương thức {@link #printInfo()})
 *       và Encapsulation (mọi field private/protected, getter công khai).</li>
 * </ul>
 *
 * <p>Class con tiêu biểu: {@link com.auction.model.user.User},
 * {@link com.auction.model.item.Item}, {@link com.auction.model.auction.Auction},
 * {@link com.auction.model.transaction.BidTransaction}.
 *
 * <p>Invariants:
 * <ul>
 *   <li>{@code id} không bao giờ null và bất biến sau khi khởi tạo.</li>
 *   <li>{@code createdAt} bất biến; {@code updatedAt} >= {@code createdAt}.</li>
 *   <li>Equality dựa hoàn toàn vào {@code id} (không dùng các field khác).</li>
 * </ul>
 *
 * <p>Implements {@link Serializable} để hỗ trợ persistence qua file/network.
 */
public abstract class Entity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Định danh duy nhất của entity (UUID), bất biến sau khi khởi tạo. */
    private final String id;
    /** Thời điểm entity được tạo; bất biến. */
    private final LocalDateTime createdAt;
    /** Thời điểm entity được cập nhật lần gần nhất; thay đổi qua {@link #markUpdated()}. */
    private LocalDateTime updatedAt;

    /**
     * Constructor mặc định: tự sinh {@code id} mới bằng {@link UUID#randomUUID()}.
     * Dùng khi tạo entity mới hoàn toàn (chưa có id từ trước).
     */
    protected Entity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Constructor với {@code id} cho trước.
     * Dùng khi khôi phục entity từ persistence layer (file/network) — cần
     * giữ nguyên id gốc để duy trì identity xuyên các phiên làm việc.
     *
     * @param id định danh có sẵn của entity (không được null)
     */
    protected Entity(String id) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    /**
     * Trả về định danh duy nhất của entity.
     *
     * @return chuỗi UUID đại diện cho identity của entity (không null)
     */
    public String getId() {
        return id;
    }

    /**
     * Trả về thời điểm entity được tạo.
     *
     * @return thời điểm tạo, bất biến từ lúc constructor chạy
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Trả về thời điểm cập nhật gần nhất.
     *
     * @return thời điểm cập nhật mới nhất; bằng {@code createdAt} nếu chưa
     *         từng gọi {@link #markUpdated()}
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Gán trực tiếp giá trị {@code updatedAt}.
     * Dành cho subclass / merge logic cần khôi phục timestamp chính xác
     * (ví dụ khi merge state từ JVM khác).
     *
     * @param updatedAt mốc thời gian cập nhật mới
     */
    protected void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Cập nhật {@code updatedAt} về thời điểm hiện tại.
     * Subclass nên gọi sau mỗi thao tác làm thay đổi state để đảm bảo
     * audit trail chính xác.
     */
    protected void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Phương thức trừu tượng để hiển thị thông tin tóm tắt của entity.
     * Mỗi subclass tự định nghĩa định dạng riêng (Polymorphism).
     *
     * @return chuỗi mô tả ngắn gọn entity, dùng cho logging / debug / UI
     */
    public abstract String printInfo();

    /**
     * So sánh dựa trên {@code id} và class chính xác.
     * Hai entity được coi là bằng nhau khi và chỉ khi cùng class cụ thể
     * (không cho phép upcast bypass) và có cùng {@code id}.
     *
     * @param o đối tượng cần so sánh
     * @return {@code true} nếu cùng class và cùng id
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        // Yêu cầu cùng concrete class — tránh trường hợp Bidder.equals(Seller) trả true
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Entity entity = (Entity) o;
        return Objects.equals(id, entity.id);
    }

    /**
     * Hash code chỉ dựa trên {@code id} để đồng bộ với {@link #equals(Object)}.
     *
     * @return hash code dẫn xuất từ id
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * {@inheritDoc}
     * <p>Mặc định delegate sang {@link #printInfo()} để các log/print mặc
     * định luôn cho ra thông tin domain-friendly.
     */
    @Override
    public String toString() {
        return printInfo();
    }
}