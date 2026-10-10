# Tài liệu Thiết kế - Ứng dụng Giới hạn Thời gian

## 1. Mục tiêu và Phạm vi MVP
Xây dựng ứng dụng giúp người dùng theo dõi và kiểm soát thời gian sử dụng thiết bị. Ứng dụng chạy ngầm để tính toán thời gian dùng của các app khác, chặn truy cập bằng pop-up khi hết thời gian, và hiển thị thống kê chi tiết.

## 2. Luồng người dùng (User Flow)

### 2.1. Luồng Khởi tạo
*   **Mở app lần đầu:** Ứng dụng hiển thị màn hình yêu cầu cấp các quyền bắt buộc:
    *   **Quyền truy cập mức sử dụng (Usage Access):** Để tính toán thời gian dùng app.
    *   **Quyền vẽ lên trên ứng dụng khác (Display over other apps):** Để hiển thị pop-up chặn màn hình.
    *   **Quyền bỏ qua tối ưu hóa pin (Ignore Battery Optimization):** Để app không bị hệ thống tự động tắt khi chạy ngầm.
*   **Xử lý:** Người dùng phải cấp đủ quyền mới được sử dụng app. Nếu từ chối, hiển thị giải thích tại sao quyền này lại quan trọng.

### 2.2. Luồng Điều hướng Chính
Ứng dụng có 2 tab ở dưới cùng: **Thống kê** và **Giới hạn**.

### 2.3. Tab Thống kê (Statistics)
*   **Hiển thị mặc định:** Thời gian sử dụng của từng ứng dụng trong "Hôm nay".
*   **Thao tác chuyển đổi:** Có bộ lọc thời gian để chọn xem dữ liệu của:
    *   Hôm nay.
    *   5 ngày qua.
    *   10 ngày qua.
*   **Kết quả:** Danh sách cập nhật linh hoạt, ứng dụng nào dùng nhiều thời gian nhất xếp lên đầu.

### 2.4. Tab Giới hạn (App Limits)
*   **Màn hình chính của Tab:** Hiển thị danh sách các giới hạn đã tạo.
*   **Thao tác Thêm mới (+):**
    *   Người dùng bấm nút **+**.
    *   Hiển thị form nhập liệu gồm:
        *   Tên lựa chọn giới hạn (Tên rule, VD: "Cai nghiện mạng xã hội").
        *   Chọn Ứng dụng cần giới hạn (VD: Facebook, TikTok).
        *   Thiết lập Thời gian giới hạn (VD: 60 phút/ngày).
    *   Bấm "Lưu" -> Quay lại Tab Giới hạn và cập nhật danh sách rule vừa tạo.

### 2.5. Luồng Chặn ứng dụng (Background Flow)
*   Người dùng đang dùng một ứng dụng (VD: TikTok) và đạt đến mốc giới hạn thời gian đã cài đặt.
*   Hệ thống ngầm phát hiện và bắn một Pop-up (màn hình chặn) đè lên giao diện TikTok.
*   Người dùng không thể tiếp tục dùng TikTok cho đến qua ngày mới hoặc khi tắt giới hạn.

---

## 3. Mô hình dữ liệu (Mức Khái niệm)
Để các thành phần giao tiếp được với nhau, dữ liệu luân chuyển trong app sẽ xoay quanh 2 thực thể chính:
*   **Dữ liệu Thống kê (Usage Data):** Chứa thông tin như Tên app, Gói định danh (Package Name), Icon, Tổng thời gian sử dụng, Ngày ghi nhận.
*   **Cấu hình Giới hạn (Limit Rule):** Chứa thông tin như Tên rule, Danh sách app bị áp dụng, Thời lượng tối đa cho phép, Trạng thái Bật/Tắt.

---

## 4. Kiến trúc và Trách nhiệm các thành phần
Ứng dụng sẽ được chia làm 4 thành phần chính hoạt động độc lập:

*   **Thành phần Giao diện (UI):**
    *   Chỉ chịu trách nhiệm hiển thị 2 tab (Thống kê, Giới hạn) và Form thêm giới hạn.
    *   Nhận thao tác chạm, bấm của người dùng.
*   **Thành phần Xử lý Logic (ViewModel):**
    *   Nhận tín hiệu từ UI. Ví dụ: Khi người dùng chọn "5 ngày qua", thành phần này sẽ tính toán khoảng thời gian từ ngày nào đến ngày nào để yêu cầu lấy dữ liệu.
    *   Kiểm tra tính hợp lệ khi người dùng thêm Giới hạn mới (VD: Tên rule không được để trống).
*   **Thành phần Dữ liệu (Repository/Data Layer):**
    *   Giao tiếp với API của hệ điều hành (UsageStatsManager) để kéo dữ liệu thời gian sử dụng thực tế.
    *   Giao tiếp với Database cục bộ (Local DB) để lưu trữ, truy xuất các Rule Giới hạn do người dùng tạo.
*   **Thành phần Chạy ngầm (Service):**
    *   Đóng vai trò "Người gác cổng". Chạy liên tục (nhờ quyền bỏ tối ưu pin).
    *   So sánh thời gian dùng thực tế (từ Repository) với Rule giới hạn (từ Database).
    *   Gọi hiển thị Pop-up chặn màn hình ngay khi 2 con số bằng nhau.

---

## 5. Tiêu chí kiểm thử (Test Checklist)
- **Quyền truy cập:** App không bị crash khi người dùng từ chối quyền, hiển thị hướng dẫn cấp lại quyền.
- **Thống kê:** Chuyển đổi qua lại giữa Hôm nay, 5 ngày, 10 ngày không bị giật lag, số liệu cộng dồn chính xác.
- **Giới hạn:** Tạo một rule giới hạn thành công -> rule xuất hiện ngay lập tức trên danh sách.
- **Chạy ngầm (Core):** Cài giới hạn 1 phút cho app A. Mở app A đúng 1 phút -> Pop-up chặn chắc chắn phải xuất hiện đè lên app A.
- **Chạy ngầm (Core):** Tắt app bằng đa nhiệm, service ngầm vẫn tiếp tục theo dõi mà không bị hệ thống kill.