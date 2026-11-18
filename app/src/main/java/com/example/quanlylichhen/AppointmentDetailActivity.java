package com.example.quanlylichhen;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class AppointmentDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvLocation, tvTime, tvReminderState;
    private SwitchCompat swReminder;
    private DatabaseHelper db;
    private int appointmentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_detail);

        db = new DatabaseHelper(this);
        tvTitle = findViewById(R.id.tvTitle);
        tvLocation = findViewById(R.id.tvLocation);
        tvTime = findViewById(R.id.tvTime);
        tvReminderState = findViewById(R.id.tvReminderState);
        swReminder = findViewById(R.id.swReminder);
        Button btnBack = findViewById(R.id.btnBack);
        Button btnDelete = findViewById(R.id.btnDelete);
        TextView tvBack = findViewById(R.id.tvBack);

        // Nhận id qua Intent và truy vấn lại CSDL theo id
        appointmentId = getIntent().getIntExtra("id", -1);
        Appointment a = db.getAppointmentById(appointmentId);
        if (a == null) {
            Toast.makeText(this, "Không tìm thấy lịch hẹn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTitle.setText(a.getTitle());
        tvLocation.setText(a.getLocation());
        tvTime.setText(a.getDisplayDateTime());
        swReminder.setChecked(a.isReminder());
        updateReminderText(a.isReminder());

        // Đặt listener sau khi setChecked để không ghi DB thừa lúc khởi tạo
        swReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            db.updateReminder(appointmentId, isChecked);
            updateReminderText(isChecked);
        });

        tvBack.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> finish());
        btnDelete.setOnClickListener(v -> {
            db.deleteAppointment(appointmentId);
            Toast.makeText(this, "Đã xóa lịch hẹn", Toast.LENGTH_SHORT).show();
            finish(); // danh sách + thống kê được tải lại trong onResume()
        });
    }

    private void updateReminderText(boolean on) {
        tvReminderState.setText(on ? "Đang bật" : "Đang tắt");
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
