package com.rentalapp.house;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.rentalapp.R;
import com.rentalapp.bean.House;
import com.rentalapp.base.DatabaseHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

//编辑房源页面
public class EditHouse extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private House house;
    private EditText et_title, et_address, et_price, et_powerrate, et_remark, et_area;
    private Spinner sp_bedrooms, sp_bathrooms;
    private String sel_bed = "1", sel_bath = "1";
    private ImageView iv_imgs;
    public static final int GALLERY_REQUEST_CODE = 0x01;
    private static final int CAMERA_REQUEST_CODE = 0x02;
    private File imgDir;
    private String mSavedImagePath = "";
    private View _view;
    String[] arr = {"1","2","4"};
    String[] arr1 = {"1","2"};
    private String mSavedPdfPath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_house);
        databaseHelper = new DatabaseHelper(this);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        }
        //接收参数
        String houseid = getIntent().getStringExtra("id");
        //获取房源详细信息
        house = databaseHelper.getHouse(Integer.parseInt(houseid));
        mSavedImagePath = house.getImgpath();
        mSavedPdfPath = house.getPdfpath();
        String housetype=house.getHousetype();
        String [] types = housetype.split("  ");
        String bedrooms = types[0].replace(" bedrooms","");
        String bathrooms = types[1].replace("bathrooms","");
        sel_bed = bedrooms;
        sel_bath = bathrooms;

        et_title = findViewById(R.id.et_title);
        et_title.setText(house.getTitle());
        et_address = findViewById(R.id.et_address);
        et_address.setText(house.getAddress());
        et_price = findViewById(R.id.et_price);
        et_price.setText(house.getPrice()+"");
        et_area = findViewById(R.id.et_area);
        et_area.setText(house.getArea()+"");
        et_powerrate = findViewById(R.id.et_powerrate);
        et_powerrate.setText(house.getPowerrate());
        et_remark = findViewById(R.id.et_remark);
        et_remark.setText(house.getRemark());
        sp_bedrooms = findViewById(R.id.sp_bedrooms);
        //设置下拉框的标题
        sp_bedrooms.setPrompt("Choose bedrooms");
        //给下拉框设置选择监听器，一旦用户选中某一项，就触发监听器的onItemSelected方法
        sp_bedrooms.setOnItemSelectedListener(new MySelectedListener());

        //声明一个下拉列表的数组适配器
        ArrayAdapter<String> starAdapter = new ArrayAdapter<String>(this,R.layout.item_select,arr);
        //设置数组适配器的布局样式
        starAdapter.setDropDownViewResource(R.layout.item_dropdown);
        //设置下拉框的数组适配器
        sp_bedrooms.setAdapter(starAdapter);
        if(bedrooms.equals("1")){
            sp_bedrooms.setSelection(0);
        }else if(bedrooms.equals("2")){
            sp_bedrooms.setSelection(1);
        }else{
            sp_bedrooms.setSelection(2);
        }

        sp_bathrooms = findViewById(R.id.sp_bathrooms);
        sp_bathrooms.setPrompt("Choose bedrooms");
        sp_bathrooms.setOnItemSelectedListener(new MySelectedListener1());
        ArrayAdapter<String> starAdapter1 = new ArrayAdapter<String>(this,R.layout.item_select,arr1);
        starAdapter1.setDropDownViewResource(R.layout.item_dropdown);
        sp_bathrooms.setAdapter(starAdapter1);
        if(bathrooms.equals("1")){
            sp_bathrooms.setSelection(0);
        }else{
            sp_bathrooms.setSelection(1);
        }

        iv_imgs = findViewById(R.id.iv_imgs);
        iv_imgs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _view = view;
                requestCameraPermission();
            }
        });
        Glide.with(this).load(mSavedImagePath).into(iv_imgs);
        Button chooseBtn = findViewById(R.id.chooseBtn);
        chooseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/pdf");
                startActivityForResult(Intent.createChooser(intent, "Select PDF"), 1000);
            }
        });
        Button saveBtn = findViewById(R.id.saveBtn);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toSubmit();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    class MySelectedListener implements AdapterView.OnItemSelectedListener{
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            sel_bed=arr[i];
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }

    class MySelectedListener1 implements AdapterView.OnItemSelectedListener{
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            sel_bath=arr1[i];
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }

    private void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
        } else {
            dissel();
        }
    }

    private void dissel() {
        InputMethodManager imm1 = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm1.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        View contentView = LayoutInflater.from(this).inflate(R.layout.layout_popup_window_photo, null);
        final PopupWindow popupWindow = new PopupWindow(contentView, LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setContentView(contentView);
        TextView tvSelectPhoto = contentView.findViewById(R.id.tv_select_photo);
        tvSelectPhoto.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, GALLERY_REQUEST_CODE);
            popupWindow.dismiss();
        });
        TextView tvTakePhoto = contentView.findViewById(R.id.tv_take_photo);

        tvTakePhoto.setOnClickListener(v -> {
            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");

            imgDir = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "photo");
            if (!imgDir.exists()) {
                imgDir.mkdir();
            }

            long currentTimeMillis = System.currentTimeMillis();
            Date today = new Date(currentTimeMillis);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String title = dateFormat.format(today);
            mSavedImagePath = imgDir + File.separator + title + ".jpg";

            File cameraSavePath = new File(mSavedImagePath);

            Uri imageUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                imageUri = FileProvider.getUriForFile(this, "com.rentalapp.android7.fileprovider", cameraSavePath);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                imageUri = Uri.fromFile(cameraSavePath);
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
            popupWindow.dismiss();
        });

        popupWindow.showAtLocation(_view, Gravity.CENTER, 0, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1000) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dissel();
            } else {
                Toast.makeText(EditHouse.this, "Please authorize", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            handleSelectedPdf(uri);
        }
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == this.RESULT_OK) {
            //如果用户选择了相片
            if (data != null) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = new String[]{MediaStore.Images.Media.DATA};
                //查询我们需要的数据
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();

                mSavedImagePath = picturePath;
                postMessageToServer(picturePath);
            }
        }

        if (requestCode == CAMERA_REQUEST_CODE) {
            try {
                imgDir = new File(mSavedImagePath);
                postMessageToServer(mSavedImagePath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleSelectedPdf(Uri uri) {
        mSavedPdfPath = uri.getPath();
//        // 使用ContentResolver读取PDF文件内容
//        ContentResolver contentResolver = getContentResolver();
//        try {
//            InputStream inputStream = contentResolver.openInputStream(uri);
//            if (inputStream != null) {
//                // 例如，保存到应用内部存储或外部存储
//                savePdfToLocalStorage(inputStream, getPathFromUri(uri));
//                inputStream.close();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
//
//    private String getPathFromUri(Uri uri) {
//        String path = null;
//        String[] projection = {MediaStore.Images.Media.DATA};
//        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
//        if (cursor != null && cursor.moveToFirst()) {
//            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
//            path = cursor.getString(columnIndex);
//            cursor.close();
//        }
//        return path;
//    }

//    private void savePdfToLocalStorage(InputStream inputStream, String fileName) {
//        FileOutputStream outputStream;
//        try {
//            // 获取应用的内部存储路径或外部存储路径（需要运行时权限）
//            File file = new File(getExternalFilesDir(null), fileName);
//            outputStream = new FileOutputStream(file);
//            byte[] buf = new byte[1024];
//            int len;
//            while ((len = inputStream.read(buf)) > 0) {
//                outputStream.write(buf, 0, len);
//            }
//            outputStream.close();
//            mSavedPdfPath = file.getAbsolutePath();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    protected void postMessageToServer(String imgpath) {
        Glide.with(this).load(imgpath).into(iv_imgs);
    }

    private void toSubmit() {
        String title = et_title.getText().toString();
        String address = et_address.getText().toString();
        String price = et_price.getText().toString();
        String area = et_area.getText().toString();
        String powerrate = et_powerrate.getText().toString();
        String remark = et_remark.getText().toString();
        if (title.equals("") || address.equals("") || price.equals("") || powerrate.equals("") || mSavedImagePath.equals("") || area.equals("")) {
            Toast.makeText(EditHouse.this, "Please provide complete information", Toast.LENGTH_SHORT).show();
        } else {
            house.setTitle(title);
            house.setAddress(address);
            house.setPrice(Integer.parseInt(price));
            house.setArea(Integer.parseInt(area));
            house.setPowerrate(powerrate);
            house.setImgpath(mSavedImagePath);
            house.setHousetype(sel_bed+" bedrooms  "+sel_bath+" bathrooms");
            house.setPdfpath(mSavedPdfPath);
            house.setRemark(remark);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    String result = getLatLongFromAddress(address);
                    if(!result.equals("")) {
                        try {
                            JSONObject jsonObj = new JSONObject(result);
                            JSONArray locations = jsonObj.getJSONArray("results");
                            if (locations.length() > 0) {
                                JSONObject location = locations.getJSONObject(0);
                                JSONObject geometry = location.getJSONObject("geometry");
                                JSONObject position = geometry.getJSONObject("location");
                                double latitude = position.getDouble("lat");
                                double longitude = position.getDouble("lng");
                                house.setLng(longitude + "");
                                house.setLat(latitude + "");
                            } else {
                                house.setLng("0");
                                house.setLat("0");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }else{
                        house.setLng("0");
                        house.setLat("0");
                    }
                    boolean flag = databaseHelper.updateHouse(house);
                    if(flag){
                        myhandler.sendEmptyMessage(1);
                    }else{
                        myhandler.sendEmptyMessage(0);
                    }
                }
            }).start();
        }
    }

    public String getLatLongFromAddress(String address) {
        String apiKey = "AIzaSyAtUw9mwOj2mY6vADIwtyLR5QDRdlWe0RU";
        StringBuilder result = new StringBuilder();
        try {
            String urlStr = "https://maps.googleapis.com/maps/api/geocode/json?address=" + address + "&key=" + apiKey;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            String output;
            while ((output = br.readLine()) != null) {
                result.append(output);
            }
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    private Handler myhandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                Toast.makeText(EditHouse.this, "Update Success", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK, new Intent());
                finish();
            }else{
                Toast.makeText(EditHouse.this, "Update Failed", Toast.LENGTH_SHORT).show();
            }
        }
    };
}