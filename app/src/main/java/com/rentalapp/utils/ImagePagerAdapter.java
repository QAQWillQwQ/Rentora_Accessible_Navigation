// 🟣 ImagePagerAdapter.java

package com.rentalapp.utils;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.viewpager.widget.PagerAdapter;

import java.io.File;
import java.util.List;

public class ImagePagerAdapter extends PagerAdapter {

    private final Context context;
    private final List<String> imagePaths;  // 本地图片路径数组

    public ImagePagerAdapter(Context context, List<String> imagePaths) {
        this.context = context;
        this.imagePaths = imagePaths;
    }

    @Override
    public int getCount() {
        return imagePaths.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    // 加载每一页的图片
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        ImageView imageView = new ImageView(context);
        String imagePath = imagePaths.get(position);
        File imageFile = new File(imagePath);

        if (imageFile.exists()) {
            imageView.setImageBitmap(BitmapFactory.decodeFile(imageFile.getAbsolutePath()));
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_report_image); // ❌图片不存在时使用默认图标
        }

        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        container.addView(imageView);
        return imageView;
    }

    // 销毁视图
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }
}
