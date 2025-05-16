package com.rentalapp.search;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.rentalapp.R;
import com.rentalapp.bean.House;
import com.rentalapp.bean.Maintenance;
import com.rentalapp.base.DatabaseHelper;

import java.util.Calendar;

//申请维修页面
public class ApplyActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private String houseid;
    private EditText et_content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.apply);
        databaseHelper = new DatabaseHelper(this);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        }
        houseid = getIntent().getStringExtra("id");
        et_content = findViewById(R.id.et_content);
        Button submitBtn = findViewById(R.id.submitBtn);
        submitBtn.setOnClickListener(new View.OnClickListener() {
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

    private void toSubmit(){
        String content = et_content.getText().toString();
        if(content.equals("")){
            Toast.makeText(ApplyActivity.this,"Please enter the repair content",Toast.LENGTH_SHORT).show();
        }else{
            SharedPreferences sp = getSharedPreferences("userinfo", 0);
            String uid = sp.getString("uid", "");
            House house = databaseHelper.getHouse(Integer.parseInt(houseid));
            Maintenance maintenance = new Maintenance();
            maintenance.setUid(Integer.parseInt(uid));
            maintenance.setLid(house.getUid());
            maintenance.setHid(Integer.parseInt(houseid));
            maintenance.setContent(content);
            // 获取当前日期和时间
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1; // 月份是从0开始的，所以要加1
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            maintenance.setApplytime(year + "-" + month + "-" + day);
            maintenance.setStatus("wait");
            //存入数据库
            boolean flag = databaseHelper.addMaintenance(maintenance);
            if(flag){
                Toast.makeText(ApplyActivity.this,"Repair request successful",Toast.LENGTH_SHORT).show();
                finish();
            }else{
                Toast.makeText(ApplyActivity.this,"Repair request failed",Toast.LENGTH_SHORT).show();
            }
        }
    }
}