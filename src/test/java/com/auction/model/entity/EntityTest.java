package com.auction.model.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

// Static import: cho phép viết assertEquals() thay vì Assertions.assertEquals()
import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================================
 * ENTITYTEST - UNIT TEST CHO LỚP TRỪU TƯỢNG ENTITY
 * ============================================================================
 *
 * <p>Đây là test JUnit 5 - framework chuẩn để viết unit test trong Java.
 * Mỗi method có {@code @Test} được JUnit gọi tự động khi chạy test.
 *
 * <p><b>CÁCH TEST CLASS ABSTRACT:</b> Entity là abstract → không thể new trực
 * tiếp. Phải tạo subclass stub (giả lập) trong test - xem {@link TestEntity}
 * và {@link AnotherEntity} ở bên dưới.
 *
 * <p><b>KIỂM TRA CÁC TRƯỜNG HỢP:</b>
 * <ul>
 *   <li>Sinh UUID tự động khi không truyền id</li>
 *   <li>Giữ nguyên id khi khôi phục từ persistence (constructor có id)</li>
 *   <li>{@code markUpdated()} cập nhật timestamp</li>
 *   <li>{@code equals}/{@code hashCode} dựa trên id VÀ class</li>
 *   <li>Polymorphism: {@code printInfo()} gọi đúng method subclass</li>
 * </ul>
 *
 * <p><b>NAMING CONVENTION TEST:</b> tên method theo pattern:
 * {@code methodUnderTest_scenario_expectedBehavior()} - dễ đọc khi test fail.
 */
class EntityTest {

    /**
     * Subclass STUB - dùng riêng để test Entity (vì Entity là abstract).
     * Cung cấp method touch() và setUpdatedAtTo() để test các protected method.
     */
    static class TestEntity extends Entity {
        private final String label;

        /** Constructor mặc định - dùng cho test sinh UUID tự động. */
        TestEntity() {
            super();
            this.label = "default";
        }

        /** Constructor với id - dùng cho test khôi phục từ DB. */
        TestEntity(String id) {
            super(id);
            this.label = "with-id";
        }

        @Override
        public String printInfo() {
            return "TestEntity[" + getId() + "," + label + "]";
        }

        /** Wrapper public để test gọi markUpdated() (vốn là protected). */
        public void touch() {
            markUpdated();
        }

        /** Wrapper public để test gọi setUpdatedAt(). */
        public void setUpdatedAtTo(LocalDateTime t) {
            setUpdatedAt(t);
        }
    }

    /** Subclass thứ 2 - để test equals giữa các loại Entity khác nhau. */
    static class AnotherEntity extends Entity {
        AnotherEntity(String id) {
            super(id);
        }

        @Override
        public String printInfo() {
            return "AnotherEntity[" + getId() + "]";
        }
    }

    // ========================================================================
    // CÁC TEST CASES
    // ========================================================================

    /**
     * Khi không truyền id vào constructor → tự sinh UUID.
     * Kiểm tra: id khác null, dài 36 ký tự (chuẩn UUID), timestamps được set.
     */
    @Test
    void defaultConstructor_shouldGenerateUuidAndTimestamps() {
        TestEntity e = new TestEntity();
        // assertNotNull: ném AssertionError nếu null
        assertNotNull(e.getId(), "id phải khác null");
        // UUID có format chuẩn: 8-4-4-4-12 = 36 ký tự
        assertEquals(36, e.getId().length(), "UUID dạng chuẩn dài 36 ký tự");
        assertNotNull(e.getCreatedAt());
        assertNotNull(e.getUpdatedAt());
        // Ngay khi mới tạo, updatedAt ≥ createdAt (bằng hoặc xêm xêm)
        assertFalse(e.getUpdatedAt().isBefore(e.getCreatedAt()));
    }

    /**
     * Khi truyền id vào constructor → giữ nguyên id đó.
     * Use case: khôi phục entity từ DB - cần giữ id gốc.
     */
    @Test
    void constructorWithId_shouldPreserveId() {
        TestEntity e = new TestEntity("my-fixed-id");
        assertEquals("my-fixed-id", e.getId());
    }

    /**
     * Sau khi gọi markUpdated() → updatedAt phải tiến lên.
     * Dùng Thread.sleep(5) để đảm bảo có khác biệt nano giây
     * (CPU hiện đại quá nhanh - 2 dòng code có thể xảy ra cùng nano).
     */
    @Test
    void markUpdated_shouldAdvanceUpdatedAt() throws InterruptedException {
        TestEntity e = new TestEntity();
        LocalDateTime before = e.getUpdatedAt();
        Thread.sleep(5); // ngủ 5ms để đảm bảo timestamp khác
        e.touch();
        // updatedAt mới phải >= updatedAt cũ
        assertFalse(e.getUpdatedAt().isBefore(before),
                "updatedAt sau khi markUpdated phải >= trước đó");
    }

    /** setUpdatedAt(t) phải set chính xác t (không có "smart" logic). */
    @Test
    void setUpdatedAt_shouldSetExactValue() {
        TestEntity e = new TestEntity();
        LocalDateTime target = LocalDateTime.of(2025, 1, 1, 0, 0);
        e.setUpdatedAtTo(target);
        assertEquals(target, e.getUpdatedAt());
    }

    /** Entity equals chính nó - tính chất reflexive của equals. */
    @Test
    void equals_sameInstance_shouldBeTrue() {
        TestEntity e = new TestEntity("id-1");
        assertEquals(e, e);
    }

    /**
     * Cùng class + cùng id → equals = true, hashCode phải bằng nhau.
     * Quy tắc Java: nếu a.equals(b) thì a.hashCode() == b.hashCode().
     */
    @Test
    void equals_sameClassAndId_shouldBeTrue() {
        TestEntity a = new TestEntity("id-x");
        TestEntity b = new TestEntity("id-x");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    /** Khác id → equals = false (id là identity của entity). */
    @Test
    void equals_differentId_shouldBeFalse() {
        TestEntity a = new TestEntity("id-a");
        TestEntity b = new TestEntity("id-b");
        assertNotEquals(a, b);
    }

    /**
     * Khác class cụ thể → equals = false, dù cùng id.
     * Quan trọng: tránh trường hợp Bidder.equals(Seller) trả true chỉ vì cùng id.
     */
    @Test
    void equals_differentClass_shouldBeFalse() {
        TestEntity a = new TestEntity("id-same");
        AnotherEntity b = new AnotherEntity("id-same");
        assertNotEquals(a, b, "Khác class cụ thể thì không bằng nhau dù cùng id");
    }

    /** Entity.equals(null) phải trả false (không bao giờ throw NullPointerException). */
    @Test
    void equals_null_shouldBeFalse() {
        TestEntity e = new TestEntity();
        assertNotEquals(null, e);
    }

    /**
     * Polymorphism test: gọi printInfo() trên biến kiểu Entity (cha)
     * nhưng object thực là TestEntity → phải gọi đúng implementation của TestEntity.
     */
    @Test
    void printInfo_shouldBePolymorphic() {
        Entity e = new TestEntity("abc"); // biến kiểu Entity, object là TestEntity
        // Mong đợi prefix "TestEntity[" - không phải "Entity[" hay class khác
        assertTrue(e.printInfo().startsWith("TestEntity["));
        assertTrue(e.printInfo().contains("abc"));
    }
}
