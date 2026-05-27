package com.auction.network.message;

public class Response<T > extends Message {
  private static final long serialVersionUID = 1L;

  private final String id;            // Trùng với ID của Request gửi lên
  private final boolean success;      // Xử lý thành công hay thất bại
  private final String errorMessage; // Thông báo lỗi nếu thất bại
  private final T body;               // Dữ liệu trả về (User, Auction, List,...)

  public Response(String requestId, T body) {
    this.id = requestId;
    this.success = true;
    this.errorMessage = null;
    this.body = body;
  }

  public Response(String requestId, String errorMessage) {
    this.id = requestId;
    this.success = false;
    this.errorMessage = errorMessage;
    this.body = null;
  }

  @Override
  public String getId() { return id; }
  public boolean isSuccess() { return success; }
  public String getErrorMessage() { return errorMessage; }
  @SuppressWarnings("unchecked")
  public <T> T body() {
    return (T) body;
  }
}
