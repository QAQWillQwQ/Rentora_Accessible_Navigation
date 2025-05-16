package com.rentalapp.maintenance;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.rentalapp.R;
import com.rentalapp.bean.Maintenance;

import java.util.ArrayList;
import java.util.List;

public class MaintenanceAdapter extends BaseAdapter implements View.OnClickListener {
    private Context context;
    private List<Maintenance> list = new ArrayList<>();
    private InnerItemOnclickListener mListener;

    public MaintenanceAdapter(Context context, List<Maintenance> objects) {
        super();
        this.context = context;
        this.list = objects;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        //声明布局文件
        View view;
        LayoutInflater inflater = LayoutInflater.from(context);
        view = inflater.inflate(R.layout.maintenance_list_item, null);
        Maintenance maintenance = (Maintenance) getItem(position);
        TextView tv_user = view.findViewById(R.id.tv_user);
        tv_user.setText("tenant: "+maintenance.getUname());
        TextView tv_phone = view.findViewById(R.id.tv_phone);
        tv_phone.setText("Tel: "+maintenance.getUphone());
        TextView tv_title = view.findViewById(R.id.tv_title);
        tv_title.setText(maintenance.getTitle());
        TextView tv_address = view.findViewById(R.id.tv_address);
        tv_address.setText(maintenance.getAddress());
        TextView tv_content = view.findViewById(R.id.tv_content);
        tv_content.setText("Content: "+maintenance.getContent());
        TextView tv_applytime = view.findViewById(R.id.tv_time);
        tv_applytime.setText(maintenance.getApplytime());
        TextView tv_status = view.findViewById(R.id.tv_status);
        tv_status.setText(maintenance.getStatus());
        ImageButton makeBtn=view.findViewById(R.id.makeBtn);
        makeBtn.setOnClickListener(this);
        makeBtn.setTag(position);
        if(maintenance.getStatus().equals("finish")){
            makeBtn.setVisibility(View.GONE);
        }else{
            makeBtn.setVisibility(View.VISIBLE);
        }
        return view;
    }

    public interface InnerItemOnclickListener {
        void itemClick(View v);
    }

    public void setOnInnerItemOnClickListener(InnerItemOnclickListener listener){
        this.mListener=listener;
    }

    @Override
    public void onClick(View v) {
        mListener.itemClick(v);
    }
}
