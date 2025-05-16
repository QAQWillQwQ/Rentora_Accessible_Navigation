package com.rentalapp.auth;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.rentalapp.R;
import com.rentalapp.bean.User;
import com.rentalapp.base.DatabaseHelper;
import com.rentalapp.house.ExcelReview;
import com.rentalapp.utils.PersonalItemView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Intent;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;


//个人中心页面
public class MyCenter extends AppCompatActivity implements View.OnClickListener {
    private PersonalItemView item_email,item_truename,item_phone;
    private DatabaseHelper databaseHelper;
    String uid="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_center);
        databaseHelper = new DatabaseHelper(this);
        SharedPreferences sp = getSharedPreferences("userinfo", 0);
        uid = sp.getString("uid", "");
        String email = sp.getString("email","");
        User user = databaseHelper.getUserInfo(email);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        }

        item_email = findViewById(R.id.item_email);
        item_email.setData(user.getEmail());
        item_email.setOnClickListener(this);

        item_truename = findViewById(R.id.item_truename);
        item_truename.setOnClickListener(this);
        item_truename.setData(user.getTruename());

        item_phone = findViewById(R.id.item_phone);
        item_phone.setOnClickListener(this);
        item_phone.setData(user.getPhone());

        // ✅ Excel 导入
        findViewById(R.id.btn_import_excel).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            excelPicker.launch(Intent.createChooser(intent, "Select Excel File"));
        });

        // ✅ 新增：查看我已租房源
        findViewById(R.id.btn_my_rented_houses).setOnClickListener(v -> {
            Intent intent = new Intent(MyCenter.this, com.rentalapp.house.Manage.class);
            startActivity(intent); // ManageActivity 会自动根据角色 tenant 显示已租房源
        });
    }


    private final ActivityResultLauncher<Intent> excelPicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    try {
                        Intent intent = new Intent(MyCenter.this, ExcelReview.class);

                        intent.setData(uri);  // 使用 setData 方式传递 Uri

                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Excel parsing failed.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.item_email ){
            final EditText inputServer = new EditText(this);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Update Email").setIcon(android.R.drawable.ic_dialog_info).setView(inputServer)
                    .setNegativeButton("Cancel", null);
            builder.setPositiveButton("Enter", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    String curmail = inputServer.getText().toString();
                    if (isValidEmail(curmail)) {
                        boolean flag = databaseHelper.updateEmail(uid,curmail);
                        if(flag){
                            item_email.setData(curmail);
                        }
                    } else {
                        Toast.makeText(MyCenter.this, "Invalid email", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builder.show();
        }
        if(view.getId() == R.id.item_truename){
            final EditText inputServer = new EditText(this);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Change real name").setIcon(android.R.drawable.ic_dialog_info).setView(inputServer)
                    .setNegativeButton("Cancel", null);
            builder.setPositiveButton("Enter", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    String curname = inputServer.getText().toString();
                    if (curname.equals("")) {
                        Toast.makeText(MyCenter.this, "Please enter your real name", Toast.LENGTH_SHORT).show();
                    } else {
                        boolean flag = databaseHelper.updateName(uid,curname);
                        if(flag){
                            item_truename.setData(curname);
                        }
                    }
                }
            });
            builder.show();
        }
        if(view.getId() == R.id.item_phone) {
            final EditText inputServer = new EditText(this);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Update Phone").setIcon(android.R.drawable.ic_dialog_info).setView(inputServer)
                    .setNegativeButton("Cancel", null);
            builder.setPositiveButton("Enter", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    String curphone = inputServer.getText().toString();
                    if (telPhoneNumber(curphone)) {
                        boolean flag = databaseHelper.updatePhone(uid,curphone);
                        if(flag){
                            item_phone.setData(curphone);
                        }
                    } else {
                        Toast.makeText(MyCenter.this, "Invalid phone", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builder.show();
        }
    }

    /**
     * 手机号号段校验，
     * 第1位：1；
     * 第2位：{3、4、5、6、7、8}任意数字；
     * 第3—11位：0—9任意数字
     *
     * @param str
     * @return
     */
    public static boolean telPhoneNumber(String str) {
        if (str != null && str.length() == 11) {
            Pattern compile = Pattern.compile("^1[3|4|5|6|7|8][0-9]\\d{8}$");
            Matcher matcher = compile.matcher(str);
            return matcher.matches();
        }
        return false;
    }

    public boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        Pattern pattern = Pattern.compile(emailPattern);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }
}