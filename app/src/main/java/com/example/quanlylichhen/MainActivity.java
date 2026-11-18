package com.example.quanlylichhen;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private ListView lvAppointments;
    private TextView tvTotal, tvReminder;
    private AppointmentAdapter adapter;
    private final ArrayList<Appointment> data = new ArrayList<>(); // chỉ chứa kết quả truy vấn

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);
        tvTotal = findViewById(R.id.tvTotal);
        tvReminder = findViewById(R.id.tvReminder);
        lvAppointments = findViewById(R.id.lvAppointments);
        Button btnAdd = findViewById(R.id.btnAdd);

        adapter = new AppointmentAdapter(this, data);
        lvAppointments.setAdapter(adapter);
        lvAppointments.setEmptyView(findViewById(R.id.tvEmpty)); // danh sách rỗng

        lvAppointments.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, AppointmentDetailActivity.class);
            intent.putExtra("id", data.get(position).getId()); // chỉ truyền id
            startActivity(intent);
        });

        btnAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddAppointmentActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData(); // tải lại danh sách + thống kê sau khi thêm/sửa/xóa
    }

    private void loadData() {
        data.clear();
        data.addAll(db.getAllAppointments());
        adapter.notifyDataSetChanged();
        tvTotal.setText(String.valueOf(db.countAll()));
        tvReminder.setText(String.valueOf(db.countReminders()));
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    /** Adapter hiển thị mỗi dòng: tên, địa điểm, ngày giờ, nhãn Có nhắc / Không nhắc. */
    private static class AppointmentAdapter extends ArrayAdapter<Appointment> {
        AppointmentAdapter(Context context, ArrayList<Appointment> list) {
            super(context, 0, list);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_appointment, parent, false);
            }
            Appointment a = getItem(position);

            ((TextView) convertView.findViewById(R.id.tvItemTitle)).setText(a.getTitle());
            ((TextView) convertView.findViewById(R.id.tvItemLocation))
                    .setText("Địa điểm: " + a.getLocation());
            ((TextView) convertView.findViewById(R.id.tvItemTime)).setText(a.getDisplayDateTime());

            TextView tvRem = convertView.findViewById(R.id.tvItemReminder);
            if (a.isReminder()) {
                tvRem.setText("Có nhắc");
                tvRem.setBackgroundResource(R.drawable.bg_chip_on);
                tvRem.setTextColor(getContext().getColor(R.color.on_primary));
            } else {
                tvRem.setText("Không nhắc");
                tvRem.setBackgroundResource(R.drawable.bg_chip_off);
                tvRem.setTextColor(getContext().getColor(R.color.text_sub));
            }
            return convertView;
        }
    }
}
