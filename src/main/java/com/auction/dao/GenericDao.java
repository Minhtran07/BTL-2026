package com.auction.dao;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * INTERFACE GENERICDAO - MẪU CHUNG CHO TẦNG TRUY CẬP DỮ LIỆU
 * ============================================================================
 *
 * <p><b>DAO (Data Access Object)</b> là một design pattern - tách logic truy
 * cập database ra khỏi business logic. Thay vì viết SQL khắp nơi, ta gom vào
 * các DAO class.
 *
 * <p><b>GENERIC TYPE {@code <T>}:</b> Đây là "tham số kiểu" - cho phép một
 * interface dùng được cho nhiều loại entity khác nhau (User, Item, Auction...).
 * Khi implement, ta truyền kiểu cụ thể vào.
 *
 * <p>Ví dụ: {@code UserDao extends GenericDao<User>} → tất cả method dưới đây
 * trả về/nhận tham số kiểu User thay vì T.
 *
 * <p><b>CRUD operations (Create, Read, Update, Delete):</b>
 * <ul>
 *   <li>save() - CREATE: thêm mới entity vào DB</li>
 *   <li>findById(), findAll() - READ: đọc entity từ DB</li>
 *   <li>update() - UPDATE: cập nhật entity đã có</li>
 *   <li>delete() - DELETE: xóa entity</li>
 * </ul>
 *
 * <p><b>Tại sao dùng {@link Optional}?</b> Khi tìm theo id mà không có →
 * Optional.empty() rõ ràng hơn là trả null (tránh NullPointerException).
 *
 * @param <T> kiểu entity mà DAO này quản lý (User, Item, Auction...)
 */
public interface GenericDao<T> {

    /**
     * Lưu entity mới vào database.
     *
     * @param entity đối tượng cần lưu (chưa có trong DB)
     */
    void save(T entity);

    /**
     * Tìm entity theo id.
     *
     * @param id id cần tìm
     * @return Optional chứa entity nếu tìm thấy, Optional.empty() nếu không
     */
    Optional<T> findById(String id);

    /**
     * Lấy toàn bộ entity trong bảng.
     *
     * @return danh sách tất cả entity (có thể rỗng, không bao giờ null)
     */
    List<T> findAll();

    /**
     * Cập nhật entity đã tồn tại trong database.
     *
     * @param entity entity có id đã có trong DB, các field khác là giá trị mới
     */
    void update(T entity);

    /**
     * Xóa entity theo id.
     *
     * @param id id của entity cần xóa
     */
    void delete(String id);
}
