package com.rentalapp.map;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.rentalapp.R;
import com.rentalapp.adapter.ColAdapter;
import com.rentalapp.bean.ColHouse;
import com.rentalapp.base.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

//收藏夹页面
public class Collect extends AppCompatActivity implements ColAdapter.InnerItemOnclickListener{

    List<ColHouse> list = new ArrayList<>();
    private DatabaseHelper databaseHelper;
    private ListView listView;
    private ColAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collect);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        }
        databaseHelper = new DatabaseHelper(this);
        SharedPreferences sp = getSharedPreferences("userinfo", 0);
        String uid = sp.getString("uid", "");
        list = databaseHelper.getColHouses(uid);

        listView = findViewById(R.id.listView);
        adapter = new ColAdapter(this,list);
        adapter.setOnInnerItemOnClickListener(this);
        listView.setAdapter(adapter);
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

    @Override
    public void itemClick(View v) {
        int position = (Integer) v.getTag();
        if(v.getId() == R.id.delBtn){
            boolean flag = databaseHelper.delColHouse(list.get(position).getId());
            if (flag) {
                Toast.makeText(Collect.this, "Delete successful", Toast.LENGTH_SHORT).show();
                list.remove(position);
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(Collect.this, "Delete failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}