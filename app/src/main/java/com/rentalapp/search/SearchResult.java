package com.rentalapp.search;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.rentalapp.R;
import com.rentalapp.adapter.MainAdapter;
import com.rentalapp.bean.House;
import com.rentalapp.house.Detail;
import com.rentalapp.house.EditHouse;

import java.util.ArrayList;
import java.util.List;

//检索结果页面
public class SearchResult extends AppCompatActivity {
    private List<House> list = new ArrayList<>();
    private ListView listView;
    private MainAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_result);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        }
        List<House> list = (List<House>)getIntent().getSerializableExtra("rs");
        listView = findViewById(R.id.listView);
        adapter = new MainAdapter(this, list);
        listView.setAdapter(adapter);
        SharedPreferences sp = getSharedPreferences("userinfo", 0);
        String role = sp.getString("role", "");
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (role.equals("landlord")) {
                    //跳转到房源编辑页面
                    Intent intent = new Intent(SearchResult.this, EditHouse.class);
                    intent.putExtra("id", list.get(i).getId() + "");
                    startActivity(intent);
                } else {
                    //跳转到房源详情页面
                    Intent intent = new Intent(SearchResult.this, Detail.class);
                    intent.putExtra("id", list.get(i).getId() + "");
                    startActivity(intent);
                }
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
}