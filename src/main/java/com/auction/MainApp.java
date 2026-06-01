package com.auction;

// ====== IMPORT CÁC LỚP CẦN DÙNG ======
// AuctionClient: lớp kết nối TCP socket tới server đấu giá
import com.auction.model.user.UserRole;
import com.auction.network.client.AuctionClient;
// AuctionClientService: Singleton giữ tham chiếu tới AuctionClient để mọi controller dùng chung
import com.auction.network.client.AuctionClientService;
// Application: lớp cha bắt buộc cho mọi ứng dụng JavaFX (giống main() cho JavaFX)
import com.auction.service.UserService;
import javafx.application.Application;
// FXMLLoader: dùng để nạp giao diện từ file .fxml (giao diện được vẽ trong file FXML)
import javafx.fxml.FXMLLoader;
// Parent: lớp cha của tất cả các node UI có chứa node con (giống panel chung)
import javafx.scene.Parent;
// Scene: "khung hình" chứa toàn bộ UI tại 1 thời điểm
import javafx.scene.Scene;
// Alert: hộp thoại thông báo (lỗi, cảnh báo, thông tin)
import javafx.scene.control.Alert;
// ButtonType: kiểu nút trong Alert (OK, Cancel, Yes, No...)
import javafx.scene.control.ButtonType;
// Stage: cửa sổ chính của ứng dụng JavaFX
import javafx.stage.Stage;

import java.io.IOException;

/**
 * ============================================================================
 * LỚP MAINAPP - ĐIỂM KHỞI ĐẦU CỦA ỨNG DỤNG (Entry Point)
 * ============================================================================
 *
 * <p>Đây là lớp chính khởi chạy ứng dụng JavaFX. Mọi chương trình JavaFX đều
 * phải có một lớp kế thừa từ {@link Application} và override phương thức
 * {@code start(Stage)} - đây là nơi UI đầu tiên được tạo ra.
 *
 * <p><b>Luồng hoạt động khi chạy ứng dụng:</b>
 * <ol>
 *   <li>JVM gọi {@code main()} → {@code launch(args)} khởi động JavaFX runtime</li>
 *   <li>JavaFX runtime gọi {@code start(Stage)} với cửa sổ chính (primaryStage)</li>
 *   <li>Tạo {@link AuctionClient} và kết nối tới server qua TCP socket</li>
 *   <li>Nếu kết nối thành công → hiển thị màn hình đăng nhập (login.fxml)</li>
 *   <li>Nếu thất bại → hiển thị thông báo lỗi và thoát chương trình</li>
 * </ol>
 *
 * <p><b>Lưu ý quan trọng:</b> Client KHÔNG có chế độ chạy offline. Nếu server
 * chưa khởi động thì client sẽ thoát ngay. Mọi thao tác đọc/ghi dữ liệu đều
 * phải đi qua server (kiến trúc Client-Server thuần).
 *
 * <p><b>Cách chạy ứng dụng:</b>
 * <ol>
 *   <li>Mở terminal 1, chạy server: {@code mvn exec:java}</li>
 *   <li>Mở terminal 2, chạy client (UI): {@code mvn javafx:run}</li>
 * </ol>
 */
public class MainApp extends Application {

    /**
     * Cửa sổ chính của ứng dụng JavaFX.
     *
     * <p>Khai báo {@code static} để các lớp khác (như Controller) có thể
     * gọi {@link #navigateTo(String, String)} để chuyển màn hình mà không
     * cần truyền {@code Stage} qua tham số.
     *
     * <p>Lưu ý: dùng static field cho UI là tiện nhưng làm khó test - đây là
     * trade-off chấp nhận được trong dự án nhỏ này.
     */
    private static Stage primaryStage;

    /**
     * Phương thức chính của JavaFX - được gọi tự động sau khi {@code launch()}
     * khởi động xong JavaFX runtime.
     *
     * @param stage cửa sổ chính do JavaFX tạo sẵn và truyền vào
     * @throws Exception nếu có lỗi khi nạp UI (ví dụ file FXML bị thiếu)
     */
    @Override
    public void start(Stage stage) throws Exception {
        // Lưu cửa sổ chính vào biến static để dùng ở nơi khác
        primaryStage = stage;

        // ===== CẤU HÌNH CỬA SỔ CHÍNH =====
        // Tiêu đề hiển thị trên thanh title bar
        primaryStage.setTitle("Hệ thống Đấu giá Trực tuyến");
        // Đặt kích thước tối thiểu để giao diện không bị bóp méo
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);

        // ===== KẾT NỐI TỚI SERVER =====
        // Tạo một AuctionClient mới (đại diện cho 1 kết nối socket tới server)
        AuctionClient client = new AuctionClient();
        // Đăng ký client vào service (Singleton) để controller khác dùng chung
        AuctionClientService.getInstance().setClient(client);
        try {
            // Mở kết nối TCP đến server (mặc định: localhost:9999)
            // Nếu server chưa chạy → ném IOException
            client.connect();
        } catch (IOException e) {
            // ===== XỬ LÝ KHI KẾT NỐI THẤT BẠI =====
            // Hiển thị hộp thoại lỗi cho người dùng (rõ ràng, có hướng dẫn)
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Không kết nối được tới server (localhost:9999).\n\n"
                            + "Hãy chắc chắn server đang chạy:\n"
                            + "    mvn exec:java\n\n"
                            + "Lỗi: " + e.getMessage(),
                    ButtonType.OK);
            alert.setHeaderText("Không kết nối được Auction Server");
            // showAndWait(): đợi user bấm OK rồi mới tiếp tục
            alert.showAndWait();
            // Thoát toàn bộ ứng dụng JavaFX một cách gọn gàng
            javafx.application.Platform.exit();
            return; // dừng start() ngay tại đây
        }

        // ===== ĐĂNG KÝ SỰ KIỆN ĐÓNG CỬA SỔ =====
        // Khi user bấm X (đóng cửa sổ), tự động ngắt kết nối tới server
        // để server biết và giải phóng tài nguyên (socket, thread...)
        primaryStage.setOnCloseRequest(e -> {
            try {
                client.disconnect();
            } catch (Exception ignored) {
                // Bỏ qua lỗi khi disconnect (đang thoát nên không cần báo)
            }
        });

        // ===== HIỂN THỊ MÀN HÌNH ĐĂNG NHẬP ĐẦU TIÊN =====
        navigateTo("/com/auction/view/login.fxml", "Đăng nhập");
        // Hiển thị cửa sổ (nếu không gọi show() thì cửa sổ sẽ ẩn)
        primaryStage.show();
    }

    /**
     * Chuyển sang màn hình khác (đổi Scene trong primaryStage).
     *
     * <p>Phương thức này được các Controller gọi để điều hướng giữa các màn
     * hình. Ví dụ: từ Login → Dashboard, từ Dashboard → Chi tiết phiên...
     *
     * <p>Cơ chế: nạp file FXML mới, gắn CSS, rồi đặt vào primaryStage.
     *
     * @param fxmlPath đường dẫn tới file FXML trong classpath
     *                 (ví dụ: "/com/auction/view/dashboard.fxml")
     * @param title    phần text thêm vào tiêu đề cửa sổ
     */
    public static void navigateTo(String fxmlPath, String title) {
        try {
            // Tạo loader để nạp file FXML
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(fxmlPath));
            // load() trả về node gốc (root) của giao diện - thường là VBox/HBox/AnchorPane
            Parent root = loader.load();

            // Tạo Scene mới với kích thước 1100x750
            Scene scene = new Scene(root, 1100, 750);
            // Gắn file CSS để tạo style cho toàn scene
            // toExternalForm() chuyển URL của resource sang String mà JavaFX hiểu được
            scene.getStylesheets().add(
                    MainApp.class.getResource("/com/auction/css/style.css").toExternalForm());
            // Gắn scene mới vào cửa sổ chính (thay thế scene cũ nếu có)
            primaryStage.setScene(scene);
            // Cập nhật tiêu đề cửa sổ theo màn hình hiện tại
            primaryStage.setTitle("Đấu giá Trực tuyến - " + title);
        } catch (Exception e) {
            // In stack trace ra console để dev biết lỗi gì
            e.printStackTrace();
            System.err.println("Không thể load FXML: " + fxmlPath);
        }
    }

    /**
     * Trả về một FXMLLoader đã trỏ tới file FXML chỉ định.
     *
     * <p>Khác với {@link #navigateTo(String, String)} - phương thức này
     * KHÔNG load FXML ngay mà chỉ trả về loader. Dùng khi caller cần truy
     * cập controller (qua {@code loader.getController()}) trước khi load.
     *
     * @param fxmlPath đường dẫn file FXML
     * @return FXMLLoader đã được khởi tạo
     */
    public static FXMLLoader getLoader(String fxmlPath) {
        return new FXMLLoader(MainApp.class.getResource(fxmlPath));
    }

    /**
     * Trả về cửa sổ chính của ứng dụng.
     *
     * <p>Các controller dùng phương thức này khi cần thao tác với Stage
     * (ví dụ: tạo modal dialog, đóng cửa sổ...).
     *
     * @return primaryStage hiện tại
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Điểm khởi đầu của chương trình Java (JVM gọi vào đây đầu tiên).
     *
     * <p>{@code launch()} là static method của lớp {@link Application},
     * nó sẽ khởi tạo JavaFX runtime rồi gọi {@code start(Stage)}.
     *
     * @param args tham số dòng lệnh (không sử dụng trong dự án này)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
