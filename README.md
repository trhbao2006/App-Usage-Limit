# App-Usage-Limit
## Chi tiết Triển khai Hàm theo Luồng (Method Specifications)

Tài liệu kỹ thuật mô tả trách nhiệm, tham số và logic xử lý của từng hàm theo luồng người dùng. Toàn bộ thành viên tuân thủ đúng tên hàm, kiểu dữ liệu và từ điển biến chung của dự án.

---

### Luồng 1: Khởi tạo (Xin quyền)

#### `requestMissingPermissions()`
- **Vị trí:** `ui.onboarding.PermissionViewModel` (hoặc UI Component)
- **Mục đích:** Rà soát và yêu cầu các quyền hệ thống bắt buộc trước khi cho phép dùng ứng dụng.
- **Logic thực thi:**
  1. Lần lượt kiểm tra 3 quyền cốt lõi:
     - Usage Access (`AppOpsManager`) : Truy vấn thời gian sử dụng
     - Display Over Other Apps (`Settings.canDrawOverlays(context)`) : Hiển thị trên app khác
     - Ignore Battery Optimization (`PowerManager.isIgnoringBatteryOptimizations()`) : Bỏ qua tối ưu pin 
  2. Với mỗi quyền còn thiếu, kích hoạt `Intent` tương ứng đưa người dùng trực tiếp đến trang Cài đặt hệ thống để cấp quyền.

#### `onPermissionsGranted()`
- **Vị trí:** `ui.onboarding.PermissionViewModel`
- **Mục đích:** Xử lý điều hướng và khởi tạo dịch vụ khi hoàn tất cấp đủ quyền.
- **Logic thực thi:**
  1. Cập nhật trạng thái `hasUsageAccess`, `hasOverlayPerm` sang `true`.
  2. Bắn sự kiện điều hướng UI chuyển từ màn hình Onboarding sang Tab Thống kê.
  3. Gọi khởi chạy tiến trình ngầm `UsageMonitorService`.

---

### Luồng 2: Tab Thống kê (Statistics)

#### `onFilterChanged(timeFilter: TimeFilter)`
- **Vị trí:** `ui.stats.StatsViewModel`
- **Mục đích:** Tiếp nhận tương tác thay đổi mốc thời gian xem thống kê từ người dùng (Hôm nay, 5 ngày qua, 10 ngày qua).
- **Logic thực thi:**
  1. Lưu giá trị `timeFilter` được chọn để cập nhật trạng thái UI (highlight tab/chip).
  2. Gửi tín hiệu gọi hàm `UsageRepository.getUsageStats(timeFilter)` để nạp lại danh sách dữ liệu.

#### `getUsageStats(timeFilter: TimeFilter): List<UsageData>`
- **Vị trí:** `data.repository.UsageRepository`
- **Mục đích:** Thu thập và tổng hợp dữ liệu thời lượng sử dụng app từ Android OS.
- **Logic thực thi:**
  1. Quy đổi `timeFilter` thành cặp timestamp mili-giây: `startTime` và `endTime`.
  2. Truy vấn dữ liệu thực tế thông qua `UsageStatsManager.queryUsageStats()`.
  3. Lọc bỏ các gói ứng dụng hệ thống (system apps/launchers không cần thiết).
  4. Gom nhóm, tính tổng `usedTimeMillis` theo từng `packageName`.
  5. Sắp xếp danh sách giảm dần theo thời gian sử dụng và trả về danh sách `List<UsageData>`.

---

### Luồng 3: Tab Giới hạn (App Limits)

#### `saveNewLimit(ruleName: String, packageName: String, timeLimitMinutes: Int)`
- **Vị trí:** `ui.limits.LimitsViewModel`
- **Mục đích:** Xác thực và xử lý nghiệp vụ khi người dùng tạo quy tắc giới hạn mới từ Form.
- **Logic thực thi:**
  1. **Validation:**
     - `ruleName` không được để trống hoặc chỉ chứa khoảng trắng.
     - `packageName` phải là một app hợp lệ đã chọn.
     - `timeLimitMinutes` phải lớn hơn 0.
     - Báo lỗi qua `errorMessage` lên UI nếu dữ liệu không hợp lệ.
  2. **Mapping:** Quy đổi `timeLimitMinutes` sang `limitDurationMillis` ($timeLimitMinutes \times 60 \times 1000$).
  3. Đóng gói dữ liệu thành entity `LimitRule` (mặc định `isActive = true`).
  4. Gọi `LimitRepository.insertRule(rule)`.

#### `insertRule(rule: LimitRule)`
- **Vị trí:** `data.repository.LimitRepository`
- **Mục đích:** Ghi nhận cấu hình giới hạn vào cơ sở dữ liệu cục bộ.
- **Logic thực thi:**
  1. Thực thi bất đồng bộ trên Background Thread / Coroutine IO context.
  2. Gọi `LimitRuleDao.insert(rule)` để ghi thực thể xuống Room DB.
  3. Phát tín hiệu hoàn tất để UI đóng Form nhập và làm mới danh sách quy tắc giới hạn.

---

### Luồng 4: Chặn ứng dụng (Background Flow)

#### `getCurrentAppUsage(): Pair<String, Long>?`
- **Vị trí:** `data.repository.UsageRepository` (được gọi định kỳ bởi Service)
- **Mục đích:** Xác định app đang hiển thị trên màn hình và thời lượng đã sử dụng trong ngày.
- **Logic thực thi:**
  1. Sử dụng `UsageStatsManager.queryEvents()` để bắt sự kiện `UsageEvents.Event.ACTIVITY_RESUMED` hoặc `MOVE_TO_FOREGROUND` mới nhất nhằm lấy `packageName` đang tương tác.
  2. Tính tổng `usedTimeMillis` của `packageName` đó từ đầu ngày (00:00:00) đến thời điểm hiện tại.
  3. Trả về cặp thông tin `(packageName, usedTimeMillis)`.

#### `getActiveRules(): List<LimitRule>`
- **Vị trí:** `data.repository.LimitRepository`
- **Mục đích:** Lấy toàn bộ các cấu hình giới hạn đang có hiệu lực.
- **Logic thực thi:**
  1. Truy vấn Room Database thông qua DAO:
     ```sql
     SELECT * FROM limit_rules WHERE isActive = 1
     ```
  2. Trả về danh sách `List<LimitRule>` để Service đối chiếu.

#### `checkLimitReached(currentApp: Pair<String, Long>, rules: List<LimitRule>): Boolean`
- **Vị trí:** `service.blocker.UsageMonitorService`
- **Mục đích:** Đánh giá xem ứng dụng hiện tại đã chạm ngưỡng giới hạn thời gian hay chưa.
- **Logic thực thi:**
  1. Trích xuất `currentPackageName = currentApp.first` và `currentUsedTime = currentApp.second`.
  2. Tìm quy tắc tương ứng: `matchedRule = rules.find { it.packageName == currentPackageName }`.
  3. So sánh: Nếu tồn tại `matchedRule` và `currentUsedTime >= matchedRule.limitDurationMillis`, trả về `true` (kèm theo `ruleName` kích hoạt). Ngược lại trả về `false`.

#### `BlockerOverlayManager.showPopup(ruleName: String)`
- **Vị trí:** `service.blocker.BlockerOverlayManager`
- **Mục đích:** Vẽ giao diện đè lên màn hình để chặn người dùng thao tác khi hết thời gian cho phép.
- **Logic thực thi:**
  1. Khởi tạo giao diện cảnh báo hiển thị tên quy tắc `ruleName` và thông báo hết giờ.
  2. Cấu hình `WindowManager.LayoutParams`:
     - `type = TYPE_APPLICATION_OVERLAY`
     - `flags = FLAG_NOT_TOUCH_MODAL or FLAG_LAYOUT_IN_SCREEN`
     - `format = PixelFormat.TRANSLUCENT`
  3. Gọi `windowManager.addView(overlayView, layoutParams)` để đưa giao diện chặn lên lớp hiển thị cao nhất của hệ điều hành, vô hiệu hóa tương tác cảm ứng xuống app bên dưới.
Logic: Sử dụng WindowManager (yêu cầu quyền Draw over other apps). Khởi tạo một View (giao diện Pop-up chứa câu cảnh báo "Bạn đã hết thời gian cho: [ruleName]"). Thiết lập cờ WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL (hoặc tương đương) chặn toàn bộ thao tác cảm ứng của người dùng xuống app bên dưới. Đẩy View này lên vị trí cao nhất của màn hình.
