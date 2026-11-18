package com.example.quanlylichhen;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "appointments.db";
    public static final int DB_VERSION = 1;

    // Bảng users
    public static final String TABLE_USERS = "users";
    public static final String COL_USER_ID = "id";
    public static final String COL_USERNAME = "username";
    public static final String COL_PASSWORD = "password";

    // Bảng appointments
    public static final String TABLE_APPT = "appointments";
    public static final String COL_ID = "id";
    public static final String COL_TITLE = "title";
    public static final String COL_LOCATION = "location";
    public static final String COL_DATE = "date";
    public static final String COL_TIME = "time";
    public static final String COL_REMINDER = "reminder";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " ("
                + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_USERNAME + " TEXT NOT NULL UNIQUE, "
                + COL_PASSWORD + " TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + TABLE_APPT + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TITLE + " TEXT NOT NULL, "
                + COL_LOCATION + " TEXT NOT NULL, "
                + COL_DATE + " TEXT NOT NULL, "
                + COL_TIME + " TEXT NOT NULL, "
                + COL_REMINDER + " INTEGER NOT NULL)");

        // Dữ liệu mẫu. Lưu ý: trong onCreate() phải dùng đối tượng db được truyền vào,
        // không gọi getWritableDatabase() (sẽ gây lỗi lặp vô hạn).
        insertUser(db, "admin", "123456");
        insertAppointment(db, new Appointment(0, "Họp nhóm đồ án", "Phòng B203",
                "2026-10-25", "09:00", true));
        insertAppointment(db, new Appointment(0, "Khám răng", "Nha khoa Minh Tâm",
                "2026-10-26", "14:30", true));
        insertAppointment(db, new Appointment(0, "Đi cà phê với bạn", "Highland Coffee",
                "2026-10-27", "19:00", false));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_APPT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // ===================== USERS =====================

    /** Thêm tài khoản mới, trả về id (-1 nếu lỗi, ví dụ trùng username). */
    public long insertUser(String username, String password) {
        return insertUser(getWritableDatabase(), username, password);
    }

    private long insertUser(SQLiteDatabase db, String username, String password) {
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD, password);
        return db.insert(TABLE_USERS, null, values);
    }

    /** Trả về true nếu có dòng khớp trong bảng users. */
    public boolean checkLogin(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COL_USER_ID},
                COL_USERNAME + " = ? AND " + COL_PASSWORD + " = ?",
                new String[]{username, password},
                null, null, null);
        boolean ok = cursor.getCount() > 0;
        cursor.close();
        return ok;
    }

    // ===================== APPOINTMENTS =====================

    /** Thêm lịch hẹn, trả về id vừa thêm. */
    public long insertAppointment(Appointment a) {
        return insertAppointment(getWritableDatabase(), a);
    }

    private long insertAppointment(SQLiteDatabase db, Appointment a) {
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, a.getTitle());
        values.put(COL_LOCATION, a.getLocation());
        values.put(COL_DATE, a.getDate());
        values.put(COL_TIME, a.getTime());
        values.put(COL_REMINDER, a.isReminder() ? 1 : 0);
        return db.insert(TABLE_APPT, null, values);
    }

    /** Lấy toàn bộ lịch hẹn, ORDER BY date ASC, time ASC. */
    public ArrayList<Appointment> getAllAppointments() {
        ArrayList<Appointment> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_APPT, null, null, null,
                null, null, COL_DATE + " ASC, " + COL_TIME + " ASC");
        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToAppointment(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    /** Lấy một lịch hẹn theo id, không có thì trả về null. */
    public Appointment getAppointmentById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_APPT, null,
                COL_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null);
        Appointment a = null;
        if (cursor.moveToFirst()) {
            a = cursorToAppointment(cursor);
        }
        cursor.close();
        return a;
    }

    /** Cập nhật trạng thái nhắc nhở. */
    public int updateReminder(int id, boolean on) {
        ContentValues values = new ContentValues();
        values.put(COL_REMINDER, on ? 1 : 0);
        return getWritableDatabase().update(TABLE_APPT, values,
                COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    /** Xóa lịch hẹn theo id. */
    public int deleteAppointment(int id) {
        return getWritableDatabase().delete(TABLE_APPT,
                COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    /** Tổng số lịch hẹn: COUNT(*). */
    public int countAll() {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_APPT, null);
        int n = 0;
        if (cursor.moveToFirst()) {
            n = cursor.getInt(0);
        }
        cursor.close();
        return n;
    }

    /** Số lịch hẹn đang bật nhắc nhở (reminder = 1). */
    public int countReminders() {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_APPT + " WHERE " + COL_REMINDER + " = ?",
                new String[]{"1"});
        int n = 0;
        if (cursor.moveToFirst()) {
            n = cursor.getInt(0);
        }
        cursor.close();
        return n;
    }

    private Appointment cursorToAppointment(Cursor c) {
        return new Appointment(
                c.getInt(c.getColumnIndexOrThrow(COL_ID)),
                c.getString(c.getColumnIndexOrThrow(COL_TITLE)),
                c.getString(c.getColumnIndexOrThrow(COL_LOCATION)),
                c.getString(c.getColumnIndexOrThrow(COL_DATE)),
                c.getString(c.getColumnIndexOrThrow(COL_TIME)),
                c.getInt(c.getColumnIndexOrThrow(COL_REMINDER)) == 1);
    }
}
