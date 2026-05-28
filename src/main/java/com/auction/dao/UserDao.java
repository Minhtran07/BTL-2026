package com.auction.dao;

import com.auction.model.user.User;

import java.util.Optional;

/**
 * ============================================================================
 * INTERFACE USERDAO - DAO RIÊNG CHO USER
 * ============================================================================
 *
 * <p>Interface này MỞ RỘNG {@link GenericDao} bằng cách specialize generic
 * thành {@code GenericDao<User>}. Như vậy:
 * <ul>
 *   <li>Tự động có sẵn các method CRUD: save(), findById(), findAll()...</li>
 *   <li>Thêm các method CHUYÊN BIỆT cho User: tìm theo username, kiểm tra
 *       trùng username/email khi đăng ký...</li>
 * </ul>
 *
 * <p><b>Tại sao tách interface ra (không đặt method trực tiếp vào impl)?</b>
 * <ul>
 *   <li>Dễ TEST: có thể mock UserDao trong unit test</li>
 *   <li>Dễ THAY ĐỔI implementation: hôm nay dùng SQLite, mai chuyển MySQL,
 *       chỉ cần viết lại impl, code service không cần sửa</li>
 *   <li>Áp dụng nguyên tắc DEPENDENCY INVERSION (DI): code phụ thuộc vào
 *       interface, không phụ thuộc vào class cụ thể</li>
 * </ul>
 */
public interface UserDao extends GenericDao<User> {

    /**
     * Tìm user theo username.
     *
     * <p>Dùng khi đăng nhập: nhập username → lookup → so sánh password.
     *
     * @param username username cần tìm (case-sensitive)
     * @return Optional chứa User nếu tìm thấy, empty nếu không
     */
    Optional<User> findByUsername(String username);

    /**
     * Kiểm tra username đã tồn tại trong DB chưa.
     *
     * <p>Dùng khi đăng ký: nếu username đã có → từ chối đăng ký.
     * <p>Hiệu quả hơn findByUsername khi chỉ cần biết có/không (không cần load đối tượng).
     *
     * @param username username cần kiểm tra
     * @return true nếu đã tồn tại, false nếu chưa
     */
    boolean existsByUsername(String username);

    /**
     * Kiểm tra email đã tồn tại trong DB chưa.
     *
     * <p>Tương tự existsByUsername - dùng khi đăng ký để đảm bảo email duy nhất.
     *
     * @param email email cần kiểm tra
     * @return true nếu đã tồn tại, false nếu chưa
     */
    boolean existsByEmail(String email);
}
