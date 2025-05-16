package com.rentalapp.Rent;

import android.util.Log;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.rentalapp.base.FirebaseUploader;
import com.rentalapp.house.FirestoreHouse;
import com.rentalapp.manhattonNavigate_486.Map;
import com.rentalapp.R;
import com.rentalapp.bean.House;
import com.rentalapp.base.DatabaseHelper;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

//租房页面
public class Rent extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private String houseid;
    private ImageView iv_imgs;
    public static final int GALLERY_REQUEST_CODE = 0x01;
    private static final int CAMERA_REQUEST_CODE = 0x02;
    private File imgDir;
    private String mSavedImagePath = "";
    private View _view;
    private String mSavedPdfPath = "";
    private EditText et_rentaltime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rent);
        databaseHelper = new DatabaseHelper(this);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        }
        houseid = getIntent().getStringExtra("id");
        House house  = databaseHelper.getHouse(Integer.parseInt(houseid));
        et_rentaltime = findViewById(R.id.et_rentaltime);
        iv_imgs = findViewById(R.id.iv_signature);
        iv_imgs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _view = view;
                requestCameraPermission();
            }
        });
        Button chooseBtn = findViewById(R.id.chooseBtn);
        chooseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/pdf");
                startActivityForResult(Intent.createChooser(intent, "Select PDF"), 1000);
            }
        });
        Button rentingBtn = findViewById(R.id.submitBtn);
        rentingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toSubmit();
            }
        });
        Button finishBtn = findViewById(R.id.finishBtn);
        finishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toFinish();
            }
        });
        if(house.getStatus().equals("rented")){
            finishBtn.setVisibility(View.VISIBLE);
        }else{
            finishBtn.setVisibility(View.GONE);
        }
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

    private void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
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
            if (grantResults.length > 0) {
                dissel();  // ⬅️ 不是检查 grantResults[0]，直接调用 dissel()
            } else {
                Toast.makeText(Rent.this, "Please authorize", Toast.LENGTH_SHORT).show();
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

// ✅Changed: Replace entire GALLERY_REQUEST_CODE block
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                try {
                    // 生成临时文件保存图片内容
                    InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                    File cacheFile = new File(getCacheDir(), "selected_image_" + System.currentTimeMillis() + ".jpg");
                    java.nio.file.Files.copy(inputStream, cacheFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    inputStream.close();

                    mSavedImagePath = cacheFile.getAbsolutePath();
                    postMessageToServer(mSavedImagePath); // 显示图片
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "❌ Failed to process image", Toast.LENGTH_SHORT).show();
                }
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
    }

    protected void postMessageToServer(String imgpath) {
        Glide.with(this).load(imgpath).into(iv_imgs);
    }

    private void toSubmit(){

        Toast.makeText(this, "Begin to Submit! ", Toast.LENGTH_SHORT).show(); // ✅ 添加这行

        House house = databaseHelper.getHouse(Integer.parseInt(houseid));
        SharedPreferences sp = getSharedPreferences("userinfo", 0);
        Toast.makeText(this, "Processing step 1..... ", Toast.LENGTH_SHORT).show();
        String uid = sp.getString("uid", "");
        String rentaltime = et_rentaltime.getText().toString();

        Toast.makeText(this, "Processing step 2..... ", Toast.LENGTH_SHORT).show();
        // ✅ 本地更新房屋信息，嵌入租户信息
        house.setStatus("rented");
        house.setTenantUid(uid); // ✅ FIXED：参数改为 String
        house.setRentaltime(rentaltime);
        house.setContract(mSavedPdfPath);
        house.setSignature(mSavedImagePath);

        boolean flag = false;
        try {
            flag = databaseHelper.updateHouseRentalInfo(house);
            Toast.makeText(this, "✔ updateHouseRentalInfo done", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorDialog("❌ Error in update", Log.getStackTraceString(e));
        }

        try {
            // ✅Changed: 在上传 PDF 和 签名后再同步 Firestore
            FirestoreHouse service = new FirestoreHouse(this);

            FirebaseUploader.uploadFile(mSavedPdfPath, "pdfs", new FirebaseUploader.UploadCallback() {
                @Override
                public void onSuccess(String pdfUrl) {
                    house.setContract(pdfUrl);

                    FirebaseUploader.uploadFile(mSavedImagePath, "images", new FirebaseUploader.UploadCallback() {
                        @Override
                        public void onSuccess(String imgUrl) {
                            house.setSignature(imgUrl);
                            service.updateRentalFields(house);  // ✅此时再同步 Firebase
                            Toast.makeText(Rent.this, "✔ Rental info uploaded to Firebase", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(Rent.this, Map.class);
                            startActivity(intent);
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(Rent.this, "❌ Image upload failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(Rent.this, "❌ PDF upload failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ updateRentalFields failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "Processing.....step 3 ", Toast.LENGTH_SHORT).show();


        if(flag){
            // ✅ 同步到 Firebase 的 house 文档中
            new FirestoreHouse(this).updateRentalFields(house);
            Toast.makeText(this, "Processing..... ", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "Renting a house successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Rent.this, Map.class);
            startActivity(intent);
        } else {
            Toast.makeText(this,"Rental failed",Toast.LENGTH_SHORT).show();
        }
    }



    private void toFinish(){
        SharedPreferences sp = getSharedPreferences("userinfo", 0);
        String uid = sp.getString("uid", "");
        String rentaltime = et_rentaltime.getText().toString();
        Renting renting = new Renting();
        renting.setUid(Integer.parseInt(uid));
        renting.setHid(Integer.parseInt(houseid));
        renting.setSignature(mSavedImagePath);
        renting.setContract(mSavedPdfPath);
        renting.setRentaltime(rentaltime);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date currentDate = new Date();
        String currentDateTime = dateFormat.format(currentDate);
        renting.setAddtime(currentDateTime);
        renting.setStatus("finish");

        boolean flag = databaseHelper.addRenting(renting);
        if(flag){
            //变更房屋状态
            databaseHelper.updateHouseStatus(houseid,"agree");
            //检查用户是否还有租房
            List<House> list = databaseHelper.getMyHouses(uid);
            if(list.size()==0){
                //变更用户身份
                databaseHelper.updateRole(uid,"tenant");
            }

            Toast.makeText(this,"Rental ends, welcome to rent again next time",Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this,"Rental termination failed",Toast.LENGTH_SHORT).show();
        }
    }

    private void showErrorDialog(String title, String message) {
        new android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton("OK", null)
                .show();
    }

}