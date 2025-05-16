package com.rentalapp.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rentalapp.R;
import com.rentalapp.bean.House;

import java.util.ArrayList;
import java.util.List;

public class MainAdapter extends BaseAdapter implements View.OnClickListener {
    private Context context;
    private List<House> list = new ArrayList<>();
    private InnerItemOnclickListener mListener;

    public MainAdapter(Context context, List<House> objects) {
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
        view = inflater.inflate(R.layout.house_list_item, null);
        House house = (House) getItem(position);
        TextView tv_title = view.findViewById(R.id.tv_title);
        tv_title.setText(house.getTitle());
        TextView tv_user = view.findViewById(R.id.tv_user);
        tv_user.setText(house.getUname()+" ( "+house.getUphone()+" ) ");
        TextView tv_address = view.findViewById(R.id.tv_address);
        tv_address.setText(house.getAddress());
        LinearLayout ll = view.findViewById(R.id.ll);
        SharedPreferences sp = this.context.getSharedPreferences("userinfo", 0);
        String role = sp.getString("role", "");
        if(role.equals("landlord")){
            ll.setVisibility(View.VISIBLE);
        }else{
            ll.setVisibility(View.GONE);
        }
        ImageButton editBtn = view.findViewById(R.id.editBtn);
        editBtn.setOnClickListener(this);
        editBtn.setTag(position);
        ImageButton delBtn=view.findViewById(R.id.delBtn);
        delBtn.setOnClickListener(this);
        delBtn.setTag(position);
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
