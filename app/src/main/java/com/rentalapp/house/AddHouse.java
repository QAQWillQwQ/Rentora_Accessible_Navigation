// 🟣 AddHouse.java

package com.rentalapp.house;
import com.rentalapp.house.FirestoreHouse;
import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.rentalapp.R;
import com.rentalapp.base.DatabaseHelper;
import com.rentalapp.bean.House;
import com.rentalapp.bean.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class AddHouse extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private EditText et_title, et_address, et_price, et_powerrate, et_remark, et_area;
    private Spinner sp_bedrooms, sp_bathrooms;
    private String sel_bed = "1", sel_bath = "1";
    private LinearLayout imageGallery;
    private static final int GALLERY_REQUEST_CODE = 0x01;
    private static final int CAMERA_REQUEST_CODE = 0x02;
    private View _view;
    private String mSavedPdfPath = "";
    private List<String> savedImagePaths = new ArrayList<>();
    String[] arr = {"1", "2", "4"};
    String[] arr1 = {"1", "2"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_house);
        databaseHelper = new DatabaseHelper(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        }

        et_title = findViewById(R.id.et_title);
        et_address = findViewById(R.id.et_address);
        et_price = findViewById(R.id.et_price);
        et_area = findViewById(R.id.et_area);
        et_powerrate = findViewById(R.id.et_powerrate);
        et_remark = findViewById(R.id.et_remark);
        sp_bedrooms = findViewById(R.id.sp_bedrooms);
        sp_bedrooms.setPrompt("Choose bedrooms");
        sp_bedrooms.setOnItemSelectedListener(new MySelectedListener());
        sp_bedrooms.setAdapter(new ArrayAdapter<>(this, R.layout.item_select, arr));

        sp_bathrooms = findViewById(R.id.sp_bathrooms);
        sp_bathrooms.setPrompt("Choose bathrooms");
        sp_bathrooms.setOnItemSelectedListener(new MySelectedListener1());
        sp_bathrooms.setAdapter(new ArrayAdapter<>(this, R.layout.item_select, arr1));

        imageGallery = findViewById(R.id.image_gallery);  // 🔧 添加多图显示容器
        findViewById(R.id.iv_imgs).setOnClickListener(v -> {
            _view = v;
            requestCameraPermission();
        });

        findViewById(R.id.chooseBtn).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            startActivityForResult(Intent.createChooser(intent, "Select PDF"), 1000);
        });

        findViewById(R.id.saveBtn).setOnClickListener(v -> toSubmit());
    }

    // ========== 运行时权限 ==========
    private void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
        } else {
            dissel();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // ✅ 添加这一行

        if (requestCode == 1000) {
            dissel();
        }
    }


    private void dissel() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);

        View contentView = LayoutInflater.from(this).inflate(R.layout.layout_popup_window_photo, null);
        final PopupWindow popupWindow = new PopupWindow(contentView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);

        contentView.findViewById(R.id.tv_select_photo).setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, GALLERY_REQUEST_CODE);
            popupWindow.dismiss();
        });

        contentView.findViewById(R.id.tv_take_photo).setOnClickListener(view -> {
            File targetDir = new File(getFilesDir(), "images");
            if (!targetDir.exists()) targetDir.mkdirs();

            String fileName = "img_" + System.currentTimeMillis() + ".jpg";
            File imageFile = new File(targetDir, fileName);
            savedImagePaths.add(imageFile.getAbsolutePath());

            Uri imageUri = FileProvider.getUriForFile(this, "com.rentalapp.android7.fileprovider", imageFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_REQUEST_CODE);

            popupWindow.dismiss();
        });

        popupWindow.showAtLocation(_view, Gravity.CENTER, 0, 0);
    }

    // ========== 图片、PDF 回调处理 ==========
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1000 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            handleSelectedPdf(data.getData());
        } else if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    saveImage(imageUri);
                }
            } else if (data.getData() != null) {
                saveImage(data.getData());
            }
        } else if (requestCode == CAMERA_REQUEST_CODE) {
            showImageGallery();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void saveImage(Uri imageUri) {
        try {
            File targetDir = new File(getFilesDir(), "images");
            if (!targetDir.exists()) targetDir.mkdirs();

            String fileName = "img_" + System.currentTimeMillis() + ".jpg";
            File targetFile = new File(targetDir, fileName);

            try (InputStream in = getContentResolver().openInputStream(imageUri);
                 OutputStream out = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                savedImagePaths.add(targetFile.getAbsolutePath());
            }

            showImageGallery();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showImageGallery() {
        imageGallery.removeAllViews();
        for (String path : savedImagePaths) {
            ImageView img = new ImageView(this);
            img.setLayoutParams(new LinearLayout.LayoutParams(240, 240));
            img.setPadding(10, 10, 10, 10);
            Glide.with(this).load(new File(path)).into(img);
            imageGallery.addView(img);
        }
    }

    private void handleSelectedPdf(Uri uri) {
        File targetDir = new File(getFilesDir(), "pdfs");
        if (!targetDir.exists()) targetDir.mkdirs();

        String fileName = "contract_" + System.currentTimeMillis() + ".pdf";
        File targetFile = new File(targetDir, fileName);

        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            out.flush();
            mSavedPdfPath = targetFile.getAbsolutePath();
        } catch (Exception e) {
            Toast.makeText(this, "PDF copy failed", Toast.LENGTH_SHORT).show();
        }
    }

    // ========== 表单提交 ==========
    private void toSubmit() {
        String title = et_title.getText().toString();
        String address = et_address.getText().toString();
        String price = et_price.getText().toString();
        String area = et_area.getText().toString();
        String powerrate = et_powerrate.getText().toString();
        String remark = et_remark.getText().toString();

        if (title.isEmpty() || address.isEmpty() || price.isEmpty() || powerrate.isEmpty() || area.isEmpty()) {
            Toast.makeText(this, "Please provide complete information", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sp = getSharedPreferences("userinfo", 0);
        String uid = sp.getString("uid", "");
        String email = sp.getString("email", "");
        User user = databaseHelper.getUserInfo(email);

        House house = new House();
        house.setUid(Integer.parseInt(uid));
        house.setTitle(title);
        house.setAddress(address);
        house.setPrice(Integer.parseInt(price));
        house.setArea(Integer.parseInt(area));
        house.setPowerrate(powerrate);
        house.setPdfpath(mSavedPdfPath);
        house.setHousetype(sel_bed + " bedrooms  " + sel_bath + " bathrooms");
        house.setUname(user.getTruename());
        house.setUphone(user.getPhone());
        house.setUmail(email);
        house.setRemark(remark);
        house.setStatus("nocheck");

        JSONArray jsonImgs = new JSONArray();
        for (String path : savedImagePaths) jsonImgs.put(path);
        house.setImgpath(jsonImgs.toString());

        new Thread(() -> {
            String result = getLatLongFromAddress(address);
            try {
                JSONObject json = new JSONObject(result);
                JSONArray locations = json.getJSONArray("results");
                JSONObject loc = locations.getJSONObject(0).getJSONObject("geometry").getJSONObject("location");
                house.setLat(loc.getDouble("lat") + "");
                house.setLng(loc.getDouble("lng") + "");
            } catch (Exception e) {
                house.setLat("38.27");
                house.setLng("80.32");
            }

            boolean flag = new com.rentalapp.house.FirestoreHouse(this).addAndSync(house);
            myhandler.sendEmptyMessage(flag ? 1 : 0);
        }).start();
    }

    private String getLatLongFromAddress(String address) {
        StringBuilder result = new StringBuilder();
        try {
            String urlStr = "https://maps.googleapis.com/maps/api/geocode/json?address=" + address +
                    "&key=AIzaSyAtUw9mwOj2mY6vADIwtyLR5QDRdlWe0RU";
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) result.append(line);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    class MySelectedListener implements AdapterView.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> adapter, View view, int pos, long id) {
            sel_bed = arr[pos];
        }

        public void onNothingSelected(AdapterView<?> adapter) {}
    }

    class MySelectedListener1 implements AdapterView.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> adapter, View view, int pos, long id) {
            sel_bath = arr1[pos];
        }

        public void onNothingSelected(AdapterView<?> adapter) {}
    }

    private Handler myhandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                Toast.makeText(AddHouse.this, "Add Success", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(AddHouse.this, "Add Failed", Toast.LENGTH_SHORT).show();
            }
        }
    };

}
