package com.rentalapp.maintenance;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.rentalapp.R;
import com.rentalapp.bean.Maintenance;
import com.rentalapp.base.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

//维修管理页面
public class MaintenanceManage extends AppCompatActivity implements MaintenanceAdapter.InnerItemOnclickListener {
    private DatabaseHelper databaseHelper;
    private ListView listView;
    private List<Maintenance> list = new ArrayList<>();
    private MaintenanceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maintenance_manage);
        databaseHelper = new DatabaseHelper(this);
        SharedPreferences sp = getSharedPreferences("userinfo", 0);
        String uid = sp.getString("uid", "");
        list = databaseHelper.getMaintenances(Integer.parseInt(uid));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        }
        listView = findViewById(R.id.listView);
        adapter = new MaintenanceAdapter(this,list);
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
        if(v.getId() == R.id.makeBtn){
            boolean flag = databaseHelper.updateStatus(list.get(position).getId());
            if(flag){
                Toast.makeText(this, "Processing completed", Toast.LENGTH_SHORT).show();
                Maintenance cur = list.get(position);
                cur.setStatus("finish");
                list.remove(position);
                list.add(position,cur);
                adapter.notifyDataSetChanged();
            }else{
                Toast.makeText(this, "Processing failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}