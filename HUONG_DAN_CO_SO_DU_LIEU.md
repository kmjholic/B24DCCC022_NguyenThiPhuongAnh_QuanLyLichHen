# Hướng dẫn làm phần cơ sở dữ liệu SQLite – Ứng dụng Quản lý lịch hẹn

Làm lần lượt theo từng bước. Mã đầy đủ nằm trong `Appointment.java` và `DatabaseHelper.java`; tài liệu này giải thích **vì sao** viết như vậy để bạn tự làm lại và trả lời được khi thầy cô hỏi.

---

## Bước 0. Hiểu mô hình

- SQLite là CSDL nằm trong một file trên máy (Android tự lưu ở `/data/data/<package>/databases/appointments.db`). Tắt app mở lại thì file vẫn còn → dữ liệu được giữ.
- `SQLiteOpenHelper` là lớp Android cung cấp để: tạo file CSDL lần đầu (`onCreate`), nâng cấp khi đổi version (`onUpgrade`), và đưa ra đối tượng `SQLiteDatabase` để thao tác (`getReadableDatabase()` / `getWritableDatabase()`).
- Mỗi **bảng** ↔ một lớp Java: bảng `appointments` ↔ lớp `Appointment`. Mỗi **dòng** ↔ một đối tượng.

Cấu trúc 2 bảng theo đề:

| users | | | appointments | | |
|---|---|---|---|---|---|
| id | INTEGER | PK, AUTOINCREMENT | id | INTEGER | PK, AUTOINCREMENT |
| username | TEXT | NOT NULL, UNIQUE | title | TEXT | NOT NULL |
| password | TEXT | NOT NULL | location | TEXT | NOT NULL |
| | | | date | TEXT | NOT NULL (`yyyy-MM-dd`) |
| | | | time | TEXT | NOT NULL (`HH:mm`) |
| | | | reminder | INTEGER | NOT NULL (1 bật, 0 tắt) |

> **Vì sao ngày lưu `yyyy-MM-dd`, giờ lưu `HH:mm`?** Vì SQLite sắp xếp chuỗi theo thứ tự từng ký tự. `2026-10-25` < `2026-11-03` đúng thứ tự thời gian, còn `25/10/2026` thì sai (so ký tự đầu "25" với "03"). Đây là lý do đề yêu cầu dạng này, và `ORDER BY date ASC, time ASC` mới cho kết quả "gần nhất trước".
> **Vì sao reminder là INTEGER?** SQLite không có kiểu boolean; quy ước 1 = true, 0 = false.

---

## Bước 1. Tạo lớp `Appointment` (model)

Chuột phải package → New → Java Class → `Appointment`.

- 6 thuộc tính `private`: `id`, `title`, `location`, `date`, `time`, `reminder` (boolean).
- Constructor đầy đủ + getter/setter (Android Studio: Alt+Insert → Constructor / Getter and Setter).
- Khi thêm mới, `id` chưa có nên truyền `0`; SQLite tự sinh id nhờ AUTOINCREMENT.
- Thêm hàm tiện ích `getDisplayDateTime()` đổi `2026-10-25` + `09:00` thành `25/10/2026 · 09:00` để hiển thị (dữ liệu vẫn lưu dạng chuẩn).

---

## Bước 2. Tạo lớp `DatabaseHelper` kế thừa `SQLiteOpenHelper`

### 2.1 Khai báo hằng số và constructor

```java
public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "appointments.db";
    public static final int DB_VERSION = 1;
    // + tên bảng, tên cột: TABLE_USERS, COL_USERNAME, TABLE_APPT, COL_TITLE ...

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }
```

Dùng hằng số cho tên bảng/cột để khỏi gõ sai chính tả trong chuỗi SQL.

### 2.2 `onCreate()` – tạo bảng + chèn dữ liệu mẫu

`onCreate` chỉ chạy **một lần duy nhất**, khi file `appointments.db` chưa tồn tại.

```java
db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, "
         + "username TEXT NOT NULL UNIQUE, password TEXT NOT NULL)");

db.execSQL("CREATE TABLE appointments (id INTEGER PRIMARY KEY AUTOINCREMENT, "
         + "title TEXT NOT NULL, location TEXT NOT NULL, date TEXT NOT NULL, "
         + "time TEXT NOT NULL, reminder INTEGER NOT NULL)");
```

Sau đó chèn: tài khoản `admin / 123456` và 3 lịch hẹn mẫu (ngày `2026-10-25`, `2026-10-26`, `2026-10-27`).

> ⚠️ **Lỗi hay gặp:** trong `onCreate` **không** được gọi `getWritableDatabase()` (hoặc phương thức public nào gọi nó) vì sẽ bị gọi lồng lặp vô hạn/crash. Hãy dùng đối tượng `db` được truyền vào. Vì vậy trong code mẫu có 2 phiên bản `insertUser`: bản public (không tham số `db`, dùng cho code khác) và bản private nhận thêm `SQLiteDatabase db` (dùng trong `onCreate`).

### 2.3 `onUpgrade()`

Chạy khi `DB_VERSION` tăng. Ở bài này (version 1) chỉ cần: `DROP TABLE IF EXISTS ...` rồi gọi lại `onCreate(db)`.

---

## Bước 3. Viết 9 phương thức tối thiểu

### Quy tắc chung (đề yêu cầu)
1. Dùng **câu truy vấn có tham số**: đặt dấu `?` và truyền giá trị qua `selectionArgs` → tránh lỗi cú pháp và SQL injection (ví dụ nhập mật khẩu `' OR '1'='1`).
2. **Đóng Cursor** sau khi dùng: `cursor.close()`.
3. **Xử lý rỗng**: `if (cursor.moveToFirst()) {...}` — không có dòng thì bỏ qua, trả về danh sách rỗng / `null` / `0`.

### 3.1 `insertUser(username, password)`
Dùng `ContentValues` (giống một "map cột → giá trị") rồi `db.insert(bảng, null, values)`. Trả về id dòng mới, hoặc `-1` nếu lỗi (ví dụ trùng `username` do ràng buộc UNIQUE).

### 3.2 `checkLogin(username, password)`
```java
Cursor c = db.query("users", new String[]{"id"},
        "username = ? AND password = ?",
        new String[]{username, password}, null, null, null);
boolean ok = c.getCount() > 0;
c.close();
return ok;
```
Có ít nhất 1 dòng khớp → đăng nhập đúng. (Tương đương `SELECT id FROM users WHERE username=? AND password=?`.)

### 3.3 `insertAppointment(Appointment a)`
Đưa `title, location, date, time, reminder` vào `ContentValues` (`reminder` đổi `true→1`, `false→0`), gọi `db.insert(...)`, trả về id vừa thêm. **Không** put cột `id` vì tự tăng.

### 3.4 `getAllAppointments()`
```java
Cursor c = db.query("appointments", null, null, null, null, null,
        "date ASC, time ASC");          // tham số cuối = ORDER BY
if (c.moveToFirst()) {
    do { list.add(cursorToAppointment(c)); } while (c.moveToNext());
}
c.close();
```
Chuyển mỗi dòng Cursor thành một `Appointment` bằng hàm riêng `cursorToAppointment(c)` (đọc theo tên cột: `c.getColumnIndexOrThrow("title")` rồi `c.getString(...)`; với reminder: `c.getInt(...) == 1`). `ArrayList` ở đây **chỉ là nơi chứa kết quả truy vấn** để đưa lên ListView — đúng yêu cầu đề.

### 3.5 `getAppointmentById(int id)`
`WHERE id = ?`, `selectionArgs = {String.valueOf(id)}`. Có dòng → trả về `Appointment`; không có → `null` (Activity chi tiết phải kiểm tra `null`).

### 3.6 `updateReminder(int id, boolean on)`
```java
ContentValues v = new ContentValues();
v.put("reminder", on ? 1 : 0);
db.update("appointments", v, "id = ?", new String[]{String.valueOf(id)});
```

### 3.7 `deleteAppointment(int id)`
`db.delete("appointments", "id = ?", new String[]{String.valueOf(id)})`.

### 3.8 `countAll()`
`rawQuery("SELECT COUNT(*) FROM appointments", null)` → `cursor.getInt(0)`.

### 3.9 `countReminders()`
`rawQuery("SELECT COUNT(*) FROM appointments WHERE reminder = ?", new String[]{"1"})`.

---

## Bước 4. Nối CSDL vào các Activity

| Màn hình | Dùng phương thức nào | Ghi chú |
|---|---|---|
| `LoginActivity` | `checkLogin()` | Kiểm tra rỗng trước; đúng thì `startActivity(MainActivity)` + `finish()`; sai thì hiện "Sai tên đăng nhập hoặc mật khẩu". **Không** viết cứng `admin/123456` trong Activity. |
| `MainActivity` | `getAllAppointments()`, `countAll()`, `countReminders()` | Gọi trong `onResume()` để mỗi lần quay lại màn hình đều tải lại danh sách + thống kê. Khi bấm dòng: `intent.putExtra("id", id)`. |
| `AddAppointmentActivity` | `insertAppointment()` | Kiểm tra trống + thời điểm phải sau hiện tại → đổi ngày/giờ sang `yyyy-MM-dd`/`HH:mm` bằng `String.format("%04d-%02d-%02d", ...)` (**nhớ `month + 1`**, vì `DatePicker.getMonth()` tính từ 0) → lưu → `finish()`. |
| `AppointmentDetailActivity` | `getAppointmentById()`, `updateReminder()`, `deleteAppointment()` | Nhận id: `getIntent().getIntExtra("id", -1)`; truy vấn lại theo id. Đặt `setOnCheckedChangeListener` **sau** khi `setChecked()` ban đầu để không ghi DB thừa. |

Vì sao dùng `onResume()` mà không phải `onCreate()`? `onCreate` của MainActivity chỉ chạy 1 lần; khi bạn thêm/xóa ở màn hình khác rồi quay về, chỉ `onResume()` được gọi lại.

---

## Bước 5. Kiểm thử (theo đúng yêu cầu 3.4)

1. **Lần đầu mở app**: đăng nhập `admin / 123456` → thấy 3 lịch hẹn mẫu, thống kê `3 / 2`, thứ tự 25 → 26 → 27/10.
2. **Đăng nhập sai / bỏ trống** → báo lỗi, ở lại màn hình đăng nhập.
3. **Thêm** một lịch hẹn ngày mai → xuất hiện đúng vị trí theo thời gian, thống kê tăng.
4. **Thêm với thời điểm quá khứ** → báo lỗi, không lưu.
5. **Chi tiết → tắt nhắc nhở** → chữ đổi thành "Đang tắt"; quay lại: nhãn "Không nhắc", số "Có nhắc nhở" giảm.
6. **Xóa** → quay lại danh sách, thống kê giảm.
7. **Tắt hẳn app** (vuốt khỏi danh sách ứng dụng gần đây), mở lại → dữ liệu vừa thêm/sửa/xóa vẫn còn. Đây là bằng chứng dữ liệu nằm trong SQLite.

### Xem trực tiếp nội dung file DB trong Android Studio
`View → Tool Windows → App Inspection → Database Inspector` → chọn tiến trình app đang chạy → chọn `appointments.db` → bấm bảng `users`/`appointments` để xem dữ liệu, hoặc chạy thử câu SQL, ví dụ `SELECT * FROM appointments ORDER BY date, time;`. Rất hữu ích để chụp ảnh nộp bài.

### Muốn chạy lại `onCreate` (để nạp lại dữ liệu mẫu)
Gỡ app khỏi máy ảo/điện thoại (hoặc Settings → Apps → Clear storage), rồi chạy lại. Chỉ sửa code mà không xóa dữ liệu thì `onCreate` **không** chạy lại vì file DB đã tồn tại.

---

## Lỗi thường gặp

| Hiện tượng | Nguyên nhân / cách sửa |
|---|---|
| Sửa tên cột/dữ liệu mẫu nhưng app không đổi | File DB cũ vẫn còn → gỡ cài đặt app rồi chạy lại. |
| `no such table` / `no such column` | Sai tên bảng/cột trong chuỗi SQL, hoặc DB cũ tạo từ code cũ → gỡ app, kiểm tra hằng số. |
| `Index -1 requested` / `column does not exist` khi đọc Cursor | Tên cột trong `getColumnIndexOrThrow` không khớp tên cột lúc tạo bảng. |
| `CursorIndexOutOfBoundsException` | Đọc dữ liệu khi chưa `moveToFirst()` hoặc bảng rỗng. |
| App treo/crash ngay khi mở lần đầu | Gọi `getWritableDatabase()` bên trong `onCreate()`. |
| Danh sách không cập nhật sau khi thêm/xóa | Quên tải lại trong `onResume()`, hoặc quên `adapter.notifyDataSetChanged()`. |
| Lịch hẹn sắp xếp sai thứ tự | Lưu ngày không đúng dạng `yyyy-MM-dd` (thiếu số 0, hoặc quên `month + 1`). |
