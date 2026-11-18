package com.example.quanlylichhen;

import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import java.util.Calendar;
import java.util.Locale;

public class AddAppointmentActivity extends AppCompatActivity {

    private EditText edtTitle, edtLocation;
    private DatePicker datePicker;
    private TimePicker timePicker;
    private SwitchCompat swReminder;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_appointment);

        db = new DatabaseHelper(this);
        edtTitle = findViewById(R.id.edtTitle);
        edtLocation = findViewById(R.id.edtLocation);
        datePicker = findViewById(R.id.datePicker);
        timePicker = findViewById(R.id.timePicker);
        swReminder = findViewById(R.id.swReminder);
        Button btnSave = findViewById(R.id.btnSave);
        TextView tvBack = findViewById(R.id.tvBack);

        timePicker.setIs24HourView(true);

        tvBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> save());
    }

    private void save() {
        String title = edtTitle.getText().toString().trim();
        String location = edtLocation.getText().toString().trim();

        if (title.isEmpty()) {
            edtTitle.setError("Vui lòng nhập tên lịch hẹn");
            edtTitle.requestFocus();
            return;
        }
        if (location.isEmpty()) {
            edtLocation.setError("Vui lòng nhập địa điểm");
            edtLocation.requestFocus();
            return;
        }

        int year = datePicker.getYear();
        int month = datePicker.getMonth(); // 0 - 11
        int day = datePicker.getDayOfMonth();
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();

        // Thời điểm hẹn phải sau thời điểm hiện tại
        Calendar chosen = Calendar.getInstance();
        chosen.set(year, month, day, hour, minute, 0);
        chosen.set(Calendar.MILLISECOND, 0);
        if (!chosen.after(Calendar.getInstance())) {
            Toast.makeText(this, "Thời điểm hẹn phải sau thời điểm hiện tại",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
        String time = String.format(Locale.US, "%02d:%02d", hour, minute);

        Appointment a = new Appointment(0, title, location, date, time,
                swReminder.isChecked());
        long newId = db.insertAppointment(a);

        if (newId == -1) {
            Toast.makeText(this, "Lưu lịch hẹn thất bại", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Đã lưu lịch hẹn", Toast.LENGTH_SHORT).show();
        finish(); // quay về danh sách, onResume() sẽ tải lại
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
