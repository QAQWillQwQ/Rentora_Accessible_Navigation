// 🟣 ExcelReview.java
package com.rentalapp.house;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.rentalapp.R;
import com.rentalapp.bean.House;
import com.rentalapp.utils.ExcelParser;
import com.rentalapp.utils.Geocoding;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ExcelReview extends Activity {

    private static final String TAG = "ExcelReview";
    private List<House> houseList;
    private ListView excelListView;
    private Button approveBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.excel_review);

        excelListView = findViewById(R.id.excelListView);
        approveBtn = findViewById(R.id.approveBtn);

        houseList = new ArrayList<>();

        // ✅ 获取上传的 Excel 文件路径
        Intent intent = getIntent();

// ✅Changed: Use Uri directly and only declare inputStream once
        Uri uri = intent.getData();  // 从 setData 获取 Uri
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            houseList = new ArrayList<>(ExcelParser.parseExcel(inputStream));
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse excel: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading Excel", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ 显示内容到列表中（标题 + 地址）
        ArrayList<String> displayList = new ArrayList<>();
        for (House h : houseList) {
            displayList.add(h.getTitle() + "\n" + h.getAddress());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        excelListView.setAdapter(adapter);

        // ✅ 点击审核通过按钮
        approveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirestoreHouse service = new FirestoreHouse(ExcelReview.this);
                int successCount = 0;
                for (House h : houseList) {
                    double[] latlng = Geocoding.getLatLngFromAddress(ExcelReview.this, h.getAddress());
                    double lat = latlng[0];
                    double lng = latlng[1];

                    if (lat == 0.0 && lng == 0.0) {
                        Toast.makeText(ExcelReview.this,
                                "Failed to locate: " + h.getTitle(), Toast.LENGTH_SHORT).show();
                        continue; // ❌ 跳过这个房源
                    }

                    h.setLat(String.valueOf(lat));
                    h.setLng(String.valueOf(lng));

                    boolean ok = service.addAndSync(h);
                    if (ok) successCount++;
                }

                Toast.makeText(ExcelReview.this,
                        successCount + " houses successfully uploaded", Toast.LENGTH_LONG).show();
                finish(); // ✅ 返回管理界面
            }
        });
    }
}
