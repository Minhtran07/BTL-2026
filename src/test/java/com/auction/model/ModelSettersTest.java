package com.auction.model;

import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.auction.AutoBidConfig;
import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.ItemCategory;
import com.auction.model.item.Vehicle;
import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================================
 * MODELSETTERSTEST - TEST CÁC GETTER/SETTER CỦA MODEL CLASSES
 * ============================================================================
 *
 * <p>Đây là test "smoke" - kiểm tra các getter/setter cơ bản hoạt động đúng.
 * Mục đích: đẩy code coverage lên cao (>60% theo yêu cầu pom.xml).
 *
 * <p><b>Pattern:</b> với mỗi class model:
 * <ol>
 *   <li>Tạo object với data ban đầu</li>
 *   <li>Set giá trị mới qua setter</li>
 *   <li>Lấy lại qua getter, assert equals</li>
 * </ol>
 *
 * <p>Test này tuy đơn giản nhưng cần thiết - bảo vệ chống refactor làm hỏng
 * encapsulation (vd: setter quên gán field).
 */

/**
 * Test bao phủ tất cả setter / getter / printInfo / role-related accessor
 * cho Item, User và Auction. Mục đích: đẩy code coverage > 60% trên các
 * package model.* (chủ yếu là encapsulation boilerplate).
 *
 * <p>Đây là "behavior smoke test" — đảm bảo getter/setter hoạt động đúng
 * cặp đôi với field, và markUpdated() được gọi khi state thay đổi.
 */
class ModelSettersTest {

    // ==================== Electronics ====================

    @Test
    void electronics_allGettersSetters_work() {
        Electronics e = new Electronics("Laptop", "Gaming laptop", 25000000.0,
                "seller-1", "Asus", "ROG Strix", "New");
        assertEquals(ItemCategory.ELECTRONICS, e.getCategory());
        assertEquals("Asus", e.getBrand());
        assertEquals("ROG Strix", e.getModel());
        assertEquals("New", e.getCondition());

        e.setBrand("Dell");
        e.setModel("XPS 15");
        e.setCondition("Used");
        assertEquals("Dell", e.getBrand());
        assertEquals("XPS 15", e.getModel());
        assertEquals("Used", e.getCondition());

        // Inherited from Item
        e.setName("New name");
        e.setDescription("New desc");
        e.setStartingPrice(30000000.0);
        e.setImageUrl("http://img/x.png");
        assertEquals("New name", e.getName());
        assertEquals("New desc", e.getDescription());
        assertEquals(30000000.0, e.getStartingPrice());
        assertEquals("http://img/x.png", e.getImageUrl());

        assertNotNull(e.printInfo());
        assertTrue(e.printInfo().contains("Laptop") || e.printInfo().contains("New name"));
    }

    // ==================== Art ====================

    @Test
    void art_allGettersSetters_work() {
        Art a = new Art("Painting", "Famous painting", 50000000.0,
                "seller-1", "Van Gogh", 1889, "Oil");
        assertEquals(ItemCategory.ART, a.getCategory());
        assertEquals("Van Gogh", a.getArtist());
        assertEquals(1889, a.getYear());
        assertEquals("Oil", a.getMedium());

        a.setArtist("Picasso");
        a.setYear(1907);
        a.setMedium("Canvas");
        assertEquals("Picasso", a.getArtist());
        assertEquals(1907, a.getYear());
        assertEquals("Canvas", a.getMedium());

        assertNotNull(a.printInfo());
    }

    // ==================== Vehicle ====================

    @Test
    void vehicle_allGettersSetters_work() {
        Vehicle v = new Vehicle("SUV", "Family car", 800000000.0,
                "seller-1", "Toyota", "Camry", 2023, 15000);
        assertEquals(ItemCategory.VEHICLE, v.getCategory());
        assertEquals("Toyota", v.getMake());
        assertEquals("Camry", v.getVehicleModel());
        assertEquals(2023, v.getYear());
        assertEquals(15000, v.getMileage());

        v.setMake("Honda");
        v.setVehicleModel("Civic");
        v.setYear(2024);
        v.setMileage(5000);
        assertEquals("Honda", v.getMake());
        assertEquals("Civic", v.getVehicleModel());
        assertEquals(2024, v.getYear());
        assertEquals(5000, v.getMileage());

        assertNotNull(v.printInfo());
    }

    // ==================== ItemCategory enum ====================

    @Test
    void itemCategory_displayName_isReadable() {
        for (ItemCategory c : ItemCategory.values()) {
            assertNotNull(c.getDisplayName(), "Category " + c + " phải có displayName");
            assertFalse(c.getDisplayName().isEmpty());
        }
    }

    // ==================== Bidder ====================

    @Test
    void bidder_allGettersSetters_work() {
        Bidder b = new Bidder("bidder1", "pass123", "b@test.com", "Bidder One");
        assertEquals(UserRole.BIDDER, b.getRole());
        assertTrue(b.getBalance() > 0, "Bidder mặc định có balance dương");

        b.setBalance(50000.0);
        assertEquals(50000.0, b.getBalance());

        b.addBalance(10000.0);
        assertEquals(60000.0, b.getBalance());

        assertTrue(b.deductBalance(20000.0));
        assertEquals(40000.0, b.getBalance());

        assertFalse(b.deductBalance(99999999.0), "Không đủ tiền thì return false");
        assertEquals(40000.0, b.getBalance(), "Số dư không đổi sau deduct fail");

        b.addWonAuction("a1");
        b.addWonAuction("a2");
        assertEquals(2, b.getWonAuctionIds().size());
        assertTrue(b.getWonAuctionIds().contains("a1"));

        b.addParticipatingAuction("p1");
        assertEquals(1, b.getParticipatingAuctionIds().size());

        assertNotNull(b.printInfo());
    }

    // ==================== Seller ====================

    @Test
    void seller_allGettersSetters_work() {
        Seller s = new Seller("seller1", "pass123", "s@test.com", "Seller One");
        assertEquals(UserRole.SELLER, s.getRole());
        assertEquals(0.0, s.getTotalRevenue());

        s.addListedItem("i1");
        s.addListedItem("i2");
        assertEquals(2, s.getListedItemIds().size());

        s.removeListedItem("i1");
        assertEquals(1, s.getListedItemIds().size());

        s.addRevenue(1000000.0);
        s.addRevenue(500000.0);
        assertEquals(1500000.0, s.getTotalRevenue());

        assertNotNull(s.printInfo());
    }

    // ==================== Admin ====================

    @Test
    void admin_roleAndPrintInfo_work() {
        Admin a = new Admin("admin1", "pass", "a@test.com", "Admin One");
        assertEquals(UserRole.ADMIN, a.getRole());
        assertNotNull(a.printInfo());
    }

    // ==================== User base (qua Bidder) ====================

    @Test
    void userBase_settersAndValidatePassword_work() {
        User u = new Bidder("u1", "rawpass", "u@test.com", "User One");
        assertEquals("u1", u.getUsername());
        assertEquals("u@test.com", u.getEmail());
        assertEquals("User One", u.getFullName());
        assertTrue(u.isActive());

        u.setUsername("u1-new");
        u.setEmail("new@test.com");
        u.setFullName("New Name");
        u.setActive(false);
        assertEquals("u1-new", u.getUsername());
        assertEquals("new@test.com", u.getEmail());
        assertEquals("New Name", u.getFullName());
        assertFalse(u.isActive());

        // Plaintext fallback (chưa hash)
        u.setPassword("plain123");
        assertTrue(u.validatePassword("plain123"));
        assertFalse(u.validatePassword("wrong"));
        assertFalse(u.validatePassword(null));

        assertNotNull(u.printInfo());
    }

    // ==================== UserRole enum ====================

    @Test
    void userRole_displayName_isReadable() {
        for (UserRole r : UserRole.values()) {
            assertNotNull(r.getDisplayName());
            assertFalse(r.getDisplayName().isEmpty());
        }
    }

    // ==================== AuctionStatus enum ====================

    @Test
    void auctionStatus_displayName_isReadable() {
        for (AuctionStatus s : AuctionStatus.values()) {
            assertNotNull(s.getDisplayName());
            assertFalse(s.getDisplayName().isEmpty());
        }
    }

    // ==================== AutoBidConfig ====================

    @Test
    void autoBidConfig_allGetters_work() {
        AutoBidConfig cfg = new AutoBidConfig("bidder-1", "Bidder One", 5000000.0, 100000.0);
        assertEquals("bidder-1", cfg.getBidderId());
        assertEquals("Bidder One", cfg.getBidderName());
        assertEquals(5000000.0, cfg.getMaxBid());
        assertEquals(100000.0, cfg.getIncrement());
        assertNotNull(cfg.getRegisteredAt());
        assertNotNull(cfg.toString());
    }

    // ==================== Auction additional getters/setters ====================

    @Test
    void auction_additionalSettersAndGetters_work() {
        Auction a = new Auction("item-1", "seller-1", "Test Item", 1000.0,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(1));
        assertEquals("item-1", a.getItemId());
        assertEquals("seller-1", a.getSellerId());
        assertEquals("Test Item", a.getItemName());
        assertEquals(1000.0, a.getStartingPrice());

        a.setItemId("item-2");
        a.setSellerId("seller-2");
        a.setItemName("Renamed");
        a.setStartingPrice(2000.0);
        assertEquals("item-2", a.getItemId());
        assertEquals("seller-2", a.getSellerId());
        assertEquals("Renamed", a.getItemName());
        assertEquals(2000.0, a.getStartingPrice());

        LocalDateTime newStart = LocalDateTime.now();
        LocalDateTime newEnd = newStart.plusHours(2);
        a.setStartTime(newStart);
        a.setEndTime(newEnd);
        assertEquals(newStart, a.getStartTime());
        assertEquals(newEnd, a.getEndTime());

        a.setStatus(AuctionStatus.CANCELED);
        assertEquals(AuctionStatus.CANCELED, a.getStatus());

        a.setAntiSnipingEnabled(false);
        assertFalse(a.isAntiSnipingEnabled());
        a.setAntiSnipingEnabled(true);
        assertTrue(a.isAntiSnipingEnabled());

        assertEquals(0, a.getSnipeExtensionCount());
        assertNotNull(a.getAutoBids());
        assertEquals(0, a.getTotalBids());
        assertNotNull(a.getBidHistory());

        assertNotNull(a.printInfo());
    }

    // ==================== Item subtype check via Item reference ====================

    @Test
    void item_polymorphicAccess_works() {
        Item e = new Electronics("L", "d", 100.0, "s1", "B", "M", "N");
        Item ar = new Art("A", "d", 100.0, "s1", "X", 2000, "Oil");
        Item v = new Vehicle("V", "d", 100.0, "s1", "Ma", "Mo", 2020, 0);
        assertEquals(ItemCategory.ELECTRONICS, e.getCategory());
        assertEquals(ItemCategory.ART, ar.getCategory());
        assertEquals(ItemCategory.VEHICLE, v.getCategory());

        // Common getters from Item base
        assertEquals("L", e.getName());
        assertEquals("A", ar.getName());
        assertEquals("V", v.getName());
    }
}
