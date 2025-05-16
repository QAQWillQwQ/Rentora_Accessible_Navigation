package com.rentalapp.house;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.rentalapp.R;
import com.rentalapp.adapter.MainAdapter;
import com.rentalapp.bean.House;
import com.rentalapp.base.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

//房源管理页面
public class Manage extends AppCompatActivity implements MainAdapter.InnerItemOnclickListener {

    private ListView listView;
    private MainAdapter adapter;
    private DatabaseHelper databaseHelper;
    private List<House> list = new ArrayList<>();
    private int editindex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.house_manage);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        }
        databaseHelper = new DatabaseHelper(this);

        listView = findViewById(R.id.listView);

        SharedPreferences sp = getSharedPreferences("userinfo", 0);
        String uid = sp.getString("uid", "");
        String role = sp.getString("role", "");

        // 管理房源加载
        if (role.equals("landlord")) {
            list = databaseHelper.getHouses(uid);


// ✅ 替换为如下逻辑：
        } else if (role.equals("tenant")) {
            list = new ArrayList<>();
            // for (House h : databaseHelper.getAllHouses()) {
            // ✅ 替换为：
            for (House h : databaseHelper.getAllHousesIncludeUnchecked()) {
                Log.d("MANAGE", "🟣 tenantUid=" + h.getTenantUid() + ", uid=" + uid + ", status=" + h.getStatus());

                if ("rented".equals(h.getStatus()) && uid.equals(h.getTenantUid())) {
                    list.add(h); // ✅ 仅显示被自己租下的房源
                }
            }


        } else {
            list = databaseHelper.getHouses(uid); // ✅ admin 显示所有已通过审核的房源
        }

        adapter = new MainAdapter(this, list);
        adapter.setOnInnerItemOnClickListener(this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (!role.equals("landlord")) {
                    //跳转到房源详情页面
                    Intent intent = new Intent(Manage.this, Detail.class);
                    intent.putExtra("id", list.get(i).getId() + "");
                    startActivity(intent);
                }
            }
        });

        Button addBtn = findViewById(R.id.addBtn);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //跳转到添加房源页面
                Intent intent = new Intent(Manage.this, AddHouse.class);
                startActivityForResult(intent, 1000);
            }
        });
        if (role.equals("landlord")) {
            addBtn.setVisibility(View.VISIBLE);
        } else {
            addBtn.setVisibility(View.GONE);
        }

        Button reviewExcelBtn = findViewById(R.id.reviewExcelBtn);
        if (role.equals("admin")) {
            reviewExcelBtn.setVisibility(View.VISIBLE);
            reviewExcelBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Manage.this, ExcelReview.class);
                    startActivity(intent);
                }
            });
        } else {
            reviewExcelBtn.setVisibility(View.GONE);
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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1000 && resultCode == RESULT_OK) {
            House lastHouse = databaseHelper.getLastHouse();
            list.add(0,lastHouse);
            adapter.notifyDataSetChanged();
        } else if (requestCode==1001 && resultCode == RESULT_OK) {
            House newhouse = databaseHelper.getHouse(list.get(editindex).getId());
            list.remove(editindex);
            list.add(editindex,newhouse);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void itemClick(View v) {
        int position = (Integer) v.getTag();
        if(v.getId() == R.id.editBtn){
            //跳转到编辑房源页面
            editindex = position;
            //跳转到房源编辑页面
            Intent intent = new Intent(Manage.this, EditHouse.class);
            intent.putExtra("id", list.get(position).getId() + "");
            startActivityForResult(intent,1001);
        }

        if (v.getId() == R.id.delBtn) {
            int houseId = list.get(position).getId();
            new FirestoreHouse(this).deleteHouse(houseId); // ✅ 删除本地 + Firebase
            list.remove(position);
            adapter.notifyDataSetChanged();
        }

    }

}