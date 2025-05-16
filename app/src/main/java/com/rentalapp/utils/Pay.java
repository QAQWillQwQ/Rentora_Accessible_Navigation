package com.rentalapp.utils;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.rentalapp.R;
import com.rentalapp.bean.Cost;
import com.rentalapp.base.DatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.Date;

//缴费页面
public class Pay extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private Spinner sp_cate;
    private EditText et_money;
    private EditText et_remark;
    private String houseid;
    String[] arr = {"rent","power rate"};
    private String selcate="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pay);

        databaseHelper = new DatabaseHelper(this);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        }
        //接收参数
        houseid = getIntent().getStringExtra("id");

        sp_cate = findViewById(R.id.sp_category);
        sp_cate.setPrompt("Choose payment content");
        sp_cate.setOnItemSelectedListener(new MySelectedListener());
        ArrayAdapter<String> starAdapter = new ArrayAdapter<String>(this,R.layout.item_select,arr);
        //设置数组适配器的布局样式
        starAdapter.setDropDownViewResource(R.layout.item_dropdown);
        //设置下拉框的数组适配器
        sp_cate.setAdapter(starAdapter);

        et_money = findViewById(R.id.et_money);
        et_remark = findViewById(R.id.et_remark);
        Button payBtn = findViewById(R.id.payBtn);
        payBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toPay();
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
            selcate=arr[i];
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }

    private void toPay(){
        String money = et_money.getText().toString();
        String remark = et_remark.getText().toString();
        if(money.equals("")||remark.equals("")){
            Toast.makeText(this, "All content must be entered", Toast.LENGTH_SHORT).show();
        }
        SharedPreferences sp = getSharedPreferences("userinfo", 0);
        String uid = sp.getString("uid", "");
        Cost cost = new Cost();
        cost.setUid(Integer.parseInt(uid));
        cost.setHid(Integer.parseInt(houseid));
        cost.setCategory(selcate);
        cost.setMoney(money);
        cost.setRemark(remark);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date currentDate = new Date();
        String currentDateTime = dateFormat.format(currentDate);
        cost.setAddtime(currentDateTime);
        boolean flag = databaseHelper.addCost(cost);
        if(flag){
            Toast.makeText(this,"Pay successful",Toast.LENGTH_SHORT).show();
            finish();
        }else{
            Toast.makeText(this,"Pay failed",Toast.LENGTH_SHORT).show();
        }
    }
}