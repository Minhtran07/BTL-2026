# Hệ Thống Đấu Giá Trực Tuyến (Online Auction System)

Bài tập lớn môn **Lập trình nâng cao** — Hệ thống đấu giá trực tuyến theo mô hình Client-Server, cho phép người dùng đăng ký tài khoản, đăng sản phẩm, tạo phiên đấu giá và đặt giá theo thời gian thực.

## Thành viên nhóm

| Thành viên | Nhiệm vụ chính |
|---|---|
| Trần Đăng Minh | Design Patterns (Factory, Singleton, Observer, Strategy), Exception |
| Nguyễn Đình Phúc | DAO, Database (SQLite), Unit Test |
| Nguyễn Ngọc Triệu | Service layer, Network layer |
| Trịnh Tuấn Nghĩa | Controller, Giao diện JavaFX (FXML + CSS) |

> Trần Đăng Minh & Trịnh Tuấn Nghĩa cùng phối hợp phần **Model** (Entity, User, Item, Auction, Transaction) do khối lượng lớn.

## Công nghệ sử dụng

- **Ngôn ngữ:** Java 21
- **Build tool:** Apache Maven
- **Giao diện (Client):** JavaFX 21 + FXML + CSS
- **Cơ sở dữ liệu (Server):** SQLite (qua JDBC)
- **Giao tiếp mạng:** TCP Socket (Java I/O)
- **Serialization:** Gson (JSON)
- **Caching:** Google Guava Cache
- **Testing:** JUnit 5, JaCoCo (coverage tối thiểu 60%)
- **Code style:** Google Checkstyle

## Yêu cầu cài đặt

- **JDK 21** trở lên
- **Apache Maven 3.8+**
- Hệ điều hành: Windows, macOS, hoặc Linux

## Cấu trúc thư mục

```
src/main/java/com/auction/
├── MainApp.java                  # Entry point (JavaFX Application)
├── SessionManager.java           # Quản lý session đăng nhập
├── controller/                   # Các controller JavaFX (MVC)
│   ├── LoginController.java
│   ├── RegisterController.java
│   ├── DashboardController.java
│   ├── AuctionDetailController.java
│   ├── CreateAuctionController.java
│   ├── EditItemController.java
│   ├── FinanceController.java
│   └── AdminController.java
├── model/                        # Domain model
│   ├── entity/Entity.java        # Base entity
│   ├── user/                     # User, Admin, Bidder, Seller, UserRole
│   ├── item/                     # Item, Art, Electronics, Vehicle, ItemCategory
│   ├── auction/                  # Auction, AuctionStatus, AutoBidConfig
│   └── transaction/              # BidTransaction
├── dao/                          # Data Access Objects (SQLite)
│   ├── DatabaseManager.java
│   ├── GenericDao.java
│   ├── UserDao.java / UserDaoImpl.java
│   ├── AuctionDaoImpl.java
│   └── ItemDaoImpl.java
├── service/                      # Business logic
│   ├── AuctionService.java
│   ├── AuctionRegistry (in-memory cache)
│   └── UserService.java
├── network/                      # Client-Server TCP
│   ├── server/AuctionServer.java # Server đa luồng (thread pool)
│   ├── client/                   # AuctionClient, AuctionClientService
│   ├── handler/                  # 20+ RequestHandler xử lý từng loại request
│   └── message/                  # Message protocol (Request, Response, Push)
│       ├── request/              # Các lớp request (Login, PlaceBid, ...)
│       └── push/                 # Push notification (PushForwarder, PushListener)
├── pattern/                      # Design Patterns
│   ├── factory/                  # Factory Method — tạo Item theo category
│   ├── singleton/observer/       # SingletonxObserver — AuctionEventDispatcher, realtime event
│   └── strategy/                 # Strategy — BidValidationStrategy
├── exception/                    # Custom exceptions
└── util/                         # Tiện ích (PasswordUtils)

src/main/resources/com/auction/
├── view/                         # FXML layouts
└── css/                          # Stylesheet
```

## Hướng dẫn chạy

### 1. Build project

```bash
mvn clean compile
```

### 2. Chạy Server

Server lắng nghe trên port **9999** (mặc định):

```bash
mvn exec:java -Dexec.mainClass="com.auction.network.server.AuctionServer"
```

### 3. Chạy Client (JavaFX)

Mở terminal khác, chạy:

```bash
mvn javafx:run
```

Có thể mở nhiều client cùng lúc để mô phỏng nhiều người dùng đấu giá đồng thời.

### 4. Chạy test và kiểm tra chất lượng

```bash
mvn clean verify
```

Lệnh này sẽ chạy toàn bộ unit test, kiểm tra code coverage (JaCoCo ≥ 60%), và kiểm tra code style (Google Checkstyle).

## Danh sách chức năng

### Quản lý tài khoản
- Đăng ký tài khoản (Bidder / Seller)
- Đăng nhập / Đăng xuất
- Xem thông tin người dùng
- Admin vô hiệu hoá tài khoản

### Quản lý sản phẩm
- Tạo sản phẩm (Art, Electronics, Vehicle) — Factory Method Pattern
- Sửa / Xoá sản phẩm
- Xem danh sách sản phẩm

### Đấu giá
- Tạo phiên đấu giá (Seller)
- Đặt giá (Bidder) — Strategy Pattern kiểm tra giá hợp lệ
- Đấu giá tự động (Auto-Bid)
- Kết thúc / Huỷ phiên đấu giá
- Xem lịch sử đặt giá

### Realtime & Push Notification
- Cập nhật giá đấu realtime qua Observer Pattern (AuctionEventDispatcher)
- Push notification tới client qua PushForwarder + PushListener
- Subscribe / Unsubscribe theo phiên đấu giá

### Quản trị (Admin)
- Xem danh sách tất cả người dùng
- Xem tất cả phiên đấu giá
- Vô hiệu hoá tài khoản vi phạm

## Design Patterns

| Pattern | Class | Mục đích |
|---|---|---|
| **Singleton** | `AuctionEventDispatcher` | Event bus dùng chung toàn app |
| **Factory Method** | `ItemFactory`, `ItemCreator` | Tạo Item theo category (Art, Electronics, Vehicle) |
| **Observer** | `AuctionEventDispatcher`, `AuctionObserver` | Phát sự kiện đấu giá realtime |
| **Strategy** | `BidValidationStrategy` | Chiến lược validate giá đấu (Standard, ReservePrice) |
