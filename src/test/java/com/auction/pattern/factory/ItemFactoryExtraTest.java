package com.auction.pattern.factory;

import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.ItemCategory;
import com.auction.model.item.Vehicle;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================================
 * ITEMFACTORYEXTRATEST - TEST FACTORY METHOD PATTERN
 * ============================================================================
 *
 * <p>Test bổ sung cho ItemFactory + 3 ConcreteCreator (Electronics/Art/Vehicle).
 *
 * <p><b>Bao phủ:</b>
 * <ul>
 *   <li>Registry lookup: factory tìm đúng creator theo category</li>
 *   <li>Exception paths: category lạ → throw IllegalArgumentException</li>
 *   <li>Default fallback: creator chấp nhận map thiếu key, dùng giá trị mặc định</li>
 * </ul>
 *
 * <p><b>Tại sao test các "extra"?</b> Đẩy code coverage lên - đặc biệt các
 * branch xử lý lỗi mà ít được trigger trong happy path.
 */
class ItemFactoryExtraTest {

    @Test
    void createItem_electronics_withAllFields() {
        Map<String, String> extra = new HashMap<>();
        extra.put("brand", "Apple");
        extra.put("model", "MacBook Pro");
        extra.put("condition", "New");

        Item item = ItemFactory.createItem(ItemCategory.ELECTRONICS,
                "MacBook", "Pro M3", 50000000.0, "seller-1", extra);

        assertInstanceOf(Electronics.class, item);
        Electronics e = (Electronics) item;
        assertEquals("Apple", e.getBrand());
        assertEquals("MacBook Pro", e.getModel());
        assertEquals("New", e.getCondition());
    }

    @Test
    void createItem_electronics_withEmptyExtra_usesDefaults() {
        Item item = ItemFactory.createItem(ItemCategory.ELECTRONICS,
                "Generic Phone", "desc", 5000000.0, "seller-1", new HashMap<>());

        assertInstanceOf(Electronics.class, item);
        Electronics e = (Electronics) item;
        // Phải có default value (không null/empty)
        assertNotNull(e.getBrand());
        assertNotNull(e.getModel());
        assertNotNull(e.getCondition());
    }

    @Test
    void createItem_art_withAllFields() {
        Map<String, String> extra = new HashMap<>();
        extra.put("artist", "Da Vinci");
        extra.put("year", "1503");
        extra.put("medium", "Oil");

        Item item = ItemFactory.createItem(ItemCategory.ART,
                "Mona Lisa", "Famous", 100000000.0, "seller-1", extra);

        assertInstanceOf(Art.class, item);
        Art a = (Art) item;
        assertEquals("Da Vinci", a.getArtist());
        assertEquals(1503, a.getYear());
        assertEquals("Oil", a.getMedium());
    }

    @Test
    void createItem_art_withInvalidYear_fallsBackToDefault() {
        Map<String, String> extra = new HashMap<>();
        extra.put("artist", "Unknown");
        extra.put("year", "not-a-number"); // invalid

        Item item = ItemFactory.createItem(ItemCategory.ART,
                "Untitled", "desc", 1000.0, "seller-1", extra);
        assertInstanceOf(Art.class, item);
        // Không crash, có default year
        assertTrue(((Art) item).getYear() > 0);
    }

    @Test
    void createItem_vehicle_withAllFields() {
        Map<String, String> extra = new HashMap<>();
        extra.put("make", "Toyota");
        extra.put("vehicleModel", "Camry");
        extra.put("year", "2024");
        extra.put("mileage", "1000");

        Item item = ItemFactory.createItem(ItemCategory.VEHICLE,
                "Camry 2024", "New car", 1000000000.0, "seller-1", extra);

        assertInstanceOf(Vehicle.class, item);
        Vehicle v = (Vehicle) item;
        assertEquals("Toyota", v.getMake());
        assertEquals("Camry", v.getVehicleModel());
        assertEquals(2024, v.getYear());
        assertEquals(1000, v.getMileage());
    }

    @Test
    void createItem_vehicle_withMissingExtra_usesDefaults() {
        Item item = ItemFactory.createItem(ItemCategory.VEHICLE,
                "Some Car", "desc", 100000.0, "seller-1", new HashMap<>());
        assertInstanceOf(Vehicle.class, item);
        Vehicle v = (Vehicle) item;
        assertNotNull(v.getMake());
        assertNotNull(v.getVehicleModel());
    }

    @Test
    void register_nullCategory_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemFactory.register(null, new ElectronicsCreator()));
    }

    @Test
    void register_nullCreator_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemFactory.register(ItemCategory.ELECTRONICS, null));
    }

    @Test
    void getRegisteredCreators_returnsAllThreeBuiltins() {
        Map<ItemCategory, ItemCreator> map = ItemFactory.getRegisteredCreators();
        assertTrue(map.containsKey(ItemCategory.ELECTRONICS));
        assertTrue(map.containsKey(ItemCategory.ART));
        assertTrue(map.containsKey(ItemCategory.VEHICLE));
    }

    @Test
    void getRegisteredCreators_isUnmodifiable() {
        Map<ItemCategory, ItemCreator> map = ItemFactory.getRegisteredCreators();
        assertThrows(UnsupportedOperationException.class,
                () -> map.put(ItemCategory.ELECTRONICS, new ElectronicsCreator()));
    }
}
