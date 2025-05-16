package com.rentalapp.utils;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.github.chrisbanes.photoview.PhotoView;
import com.rentalapp.R;

import java.io.File;
import java.io.IOException;

//查看合同
public class Pdf extends AppCompatActivity {

    LayoutInflater mInflater;
    PdfRenderer mRenderer;
    ViewPager vpPdf;
    ParcelFileDescriptor mDescriptor;
    private String pdfpath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.pdf);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        }

        pdfpath = getIntent().getStringExtra("path");
        init();
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

    public void init(){
        mInflater = LayoutInflater.from(this);
        vpPdf = findViewById(R.id.vp_pdf);
        try{
            openRender();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void openRender() throws IOException {

        File file = new File(pdfpath);

        //初始化PdfRender
        mDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        if (mDescriptor != null) {
            mRenderer = new PdfRenderer(mDescriptor);
        }

        //初始化ViewPager的适配器并绑定
        MyAdapter adapter = new MyAdapter();
        vpPdf.setAdapter(adapter);
    }

    class MyAdapter extends PagerAdapter {

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public int getCount() {
            return mRenderer.getPageCount();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view==object;
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = mInflater.inflate(R.layout.item_pdf, null);

            PhotoView pvPdf = view.findViewById(R.id.iv_pdf);
            pvPdf.setEnabled(true);

            if (getCount() <= position) {
                return view;
            }

            PdfRenderer.Page currentPage = mRenderer.openPage(position);
            Bitmap bitmap = Bitmap.createBitmap(1080, 1760, Bitmap.Config.ARGB_8888);
            currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            pvPdf.setImageBitmap(bitmap);
            //关闭当前Page对象
            currentPage.close();

            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            //销毁需要销毁的视图
            container.removeView((View) object);
        }
    }

    //关闭pdf
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            closeRenderer();
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }

    //关闭pdf
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void closeRenderer() throws IOException {
        if (mRenderer != null){
            mRenderer.close();
        }
        if (mDescriptor != null){
            mDescriptor.close();
        }
    }
}