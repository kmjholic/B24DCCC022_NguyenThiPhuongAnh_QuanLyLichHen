package com.example.quanlylichhen;

/** Biểu diễn một lịch hẹn (tương ứng một dòng trong bảng appointments). */
public class Appointment {
    private int id;
    private String title;
    private String location;
    private String date;      // lưu dạng yyyy-MM-dd
    private String time;      // lưu dạng HH:mm
    private boolean reminder; // true: bật nhắc nhở

    public Appointment() {
    }

    public Appointment(int id, String title, String location,
                       String date, String time, boolean reminder) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.date = date;
        this.time = time;
        this.reminder = reminder;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public boolean isReminder() { return reminder; }
    public void setReminder(boolean reminder) { this.reminder = reminder; }

    /** Trả về chuỗi dd/MM/yyyy · HH:mm để hiển thị. */
    public String getDisplayDateTime() {
        String[] p = date.split("-"); // yyyy, MM, dd
        return p[2] + "/" + p[1] + "/" + p[0] + "  ·  " + time;
    }
}
