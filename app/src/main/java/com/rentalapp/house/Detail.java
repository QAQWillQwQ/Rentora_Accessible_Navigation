// 🟣 Detail.java
package com.rentalapp.house;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.rentalapp.search.ApplyActivity;
import com.rentalapp.utils.Pay;
import com.rentalapp.utils.Pdf;
import com.rentalapp.R;
import com.rentalapp.Rent.Rent;
import com.rentalapp.utils.ImagePagerAdapter;
import com.rentalapp.base.DatabaseHelper;
import com.rentalapp.bean.House;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Detail extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private House house;
    private ViewPager viewPager;
    private TextView tv_title, tv_address, tv_housetype, tv_price, tv_uname, tv_uphone, tv_pdf, tv_area;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.house_detail);

        databaseHelper = new DatabaseHelper(this);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        }

        String houseid = getIntent().getStringExtra("id");
        house = databaseHelper.getHouse(Integer.parseInt(houseid));

        new FirestoreHouse(this).fetchHouseById(Integer.parseInt(houseid), updatedHouse -> {
            if (updatedHouse != null) {
                house = updatedHouse;
                refreshRoleBasedButtons(house);
            }
        });

        SharedPreferences sp = getSharedPreferences("userinfo", 0);

        // 多图滑动显示 ViewPager
        viewPager = findViewById(R.id.view_pager);

        List<String> imagePaths = new ArrayList<>();
        try {
            if (house.getImgpath() != null && !house.getImgpath().trim().equals("")) {
                org.json.JSONArray jsonArray = new org.json.JSONArray(house.getImgpath());
                for (int i = 0; i < jsonArray.length(); i++) {
                    imagePaths.add(jsonArray.getString(i));
                }
                Log.d("IMAGE_PARSE", "✅ Parsed image paths: " + imagePaths.size());
            }
        } catch (Exception e) {
            Log.e("IMAGE_PARSE", "❌ Failed to parse image paths", e);
            Toast.makeText(this, "Failed to load house images", Toast.LENGTH_SHORT).show();
        }


        viewPager.setAdapter(new ImagePagerAdapter(this, imagePaths));

        tv_title = findViewById(R.id.tv_title);
        tv_title.setText(house.getTitle());

        tv_address = findViewById(R.id.tv_address);
        tv_address.setText(house.getAddress());

        tv_housetype = findViewById(R.id.tv_housetype);
        tv_housetype.setText("House type: " + house.getHousetype());

        tv_price = findViewById(R.id.tv_price);
        tv_price.setText("$ " + house.getPrice());

        tv_area = findViewById(R.id.tv_area);
        tv_area.setText("area: " + house.getArea());

        tv_uname = findViewById(R.id.tv_uname);
        tv_uname.setText("landlord: " + house.getUname());

        tv_uphone = findViewById(R.id.tv_uphone);
        tv_uphone.setText("Phone: " + house.getUphone());

        tv_pdf = findViewById(R.id.tv_pdf);
        String html = "<u>Housing contract</u>";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            tv_pdf.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tv_pdf.setText(Html.fromHtml(html));
        }

        tv_pdf.setOnClickListener(view -> {
            // ✅ 打开完整PDF查看器
            Intent intent = new Intent(Detail.this, Pdf.class);
            intent.putExtra("path", house.getPdfpath());
            startActivity(intent);
        });

        Button btn_rookery = findViewById(R.id.btn_rookery);
        btn_rookery.setOnClickListener(view -> {
            Intent intent = new Intent(Detail.this, Rent.class);
            intent.putExtra("id", house.getId() + "");
            startActivity(intent);
        });

        Button btn_check = findViewById(R.id.btn_check);
        btn_check.setOnClickListener(view -> {
            new AlertDialog.Builder(Detail.this)
                    .setTitle("Review property listings")
                    .setMessage("Do you allow this property to be listed?")
                    .setPositiveButton("Agree", (dialog, which) -> {
                        boolean flag = databaseHelper.checkHouse(house.getId(), "agree");
                        myhandler.sendEmptyMessage(flag ? 1 : 0);
                    })
                    .setNegativeButton("Disagree", (dialog, which) -> {
                        boolean flag = databaseHelper.checkHouse(house.getId(), "disagree");
                        myhandler.sendEmptyMessage(flag ? 1 : 0);
                    })
                    .show();
        });

        Button btn_delete = findViewById(R.id.btn_del);
        btn_delete.setOnClickListener(view -> {
            new AlertDialog.Builder(Detail.this)
                    .setTitle("Alert")
                    .setMessage("Are you sure you want to delete?")
                    .setPositiveButton("Confirm", (dialogInterface, i) -> {
                        boolean flag = databaseHelper.delHouse(house.getId());
                        if (flag) {
                            Toast.makeText(Detail.this, "Delete Success", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(Detail.this, "Delete failed", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        Button btn_col = findViewById(R.id.btn_col);
        btn_col.setOnClickListener(view -> {
            String uid = sp.getString("uid", "");
            boolean flag = databaseHelper.addCol(Integer.parseInt(uid), house.getId(), house.getTitle(), house.getAddress());
            Toast.makeText(Detail.this, flag ? "Collection successful" : "Collection Failed", Toast.LENGTH_SHORT).show();
        });

        Button btn_repair = findViewById(R.id.btn_repair);
        btn_repair.setOnClickListener(view -> {
            Intent intent = new Intent(Detail.this, ApplyActivity.class);
            intent.putExtra("id", house.getId() + "");
            startActivity(intent);
        });

        Button btn_pay = findViewById(R.id.btn_pay);
        btn_pay.setOnClickListener(view -> {
            Intent intent = new Intent(Detail.this, Pay.class);
            intent.putExtra("id", house.getId() + "");
            startActivity(intent);
        });
    }

    private void refreshRoleBasedButtons(House house) {
        SharedPreferences sp = getSharedPreferences("userinfo", 0);
        String uid = sp.getString("uid", "");
        String role = sp.getString("role", "");

        Button btn_rookery = findViewById(R.id.btn_rookery);
        Button btn_check = findViewById(R.id.btn_check);
        Button btn_delete = findViewById(R.id.btn_del);
        Button btn_col = findViewById(R.id.btn_col);
        Button btn_repair = findViewById(R.id.btn_repair);
        Button btn_pay = findViewById(R.id.btn_pay);

        if (role == null || role.trim().isEmpty() || role.equalsIgnoreCase("guest")) {
            btn_rookery.setVisibility(View.GONE);
            btn_check.setVisibility(View.GONE);
            btn_delete.setVisibility(View.GONE);
            btn_col.setVisibility(View.GONE);
            btn_repair.setVisibility(View.GONE);
            btn_pay.setVisibility(View.GONE);
            Log.w("BUTTON_LOGIC", "👤 Guest mode: hide all buttons");
            return;
        }

        switch (role) {
            case "admin":
                btn_rookery.setVisibility(View.GONE);
                btn_check.setVisibility(View.VISIBLE);
                btn_delete.setVisibility(View.VISIBLE);
                btn_col.setVisibility(View.GONE);
                btn_repair.setVisibility(View.GONE);
                btn_pay.setVisibility(View.GONE);
                break;
            case "landlord":
                btn_rookery.setVisibility(View.GONE);
                btn_check.setVisibility(View.GONE);
                btn_delete.setVisibility(View.VISIBLE);
                btn_col.setVisibility(View.GONE);
                btn_repair.setVisibility(View.GONE);
                btn_pay.setVisibility(View.GONE);
                break;
            case "tenant":
                if ("rented".equals(house.getStatus())) {
                    if (uid.equals(house.getTenantUid())) {
                        btn_rookery.setVisibility(View.GONE);
                        btn_check.setVisibility(View.GONE);
                        btn_delete.setVisibility(View.GONE);
                        btn_col.setVisibility(View.GONE);
                        btn_repair.setVisibility(View.VISIBLE);
                        btn_pay.setVisibility(View.VISIBLE);
                        Log.d("BUTTON_LOGIC", "Tenant owns the house");
                    } else {
                        btn_rookery.setVisibility(View.GONE);
                        btn_check.setVisibility(View.GONE);
                        btn_delete.setVisibility(View.GONE);
                        btn_col.setVisibility(View.VISIBLE);
                        btn_repair.setVisibility(View.GONE);
                        btn_pay.setVisibility(View.GONE);
                        Toast.makeText(this, "😢Sorry, this house has already been rented by another tenant.", Toast.LENGTH_LONG).show();
                        Log.d("BUTTON_LOGIC", "Tenant views other's house");
                    }
                } else {
                    btn_rookery.setVisibility(View.VISIBLE);
                    btn_check.setVisibility(View.GONE);
                    btn_delete.setVisibility(View.GONE);
                    btn_col.setVisibility(View.VISIBLE);
                    btn_repair.setVisibility(View.GONE);
                    btn_pay.setVisibility(View.GONE);
                }
                break;
        }
    }

    private final Handler myhandler = new Handler(msg -> {
        if (msg.what != 0) {
            Toast.makeText(Detail.this, "Audit completed", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(Detail.this, "Audit failed", Toast.LENGTH_SHORT).show();
        }
        return true;
    });

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) finish();
        return true;
    }
}
