//
package com.rentalapp.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

public class FloatingTrianglesView extends View {
    private static final int TRIANGLE_COUNT = 15;
    private Triangle[] triangles;
    private Paint paint = new Paint();
    private Random random = new Random();

    public FloatingTrianglesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initTriangles();
    }

    private void initTriangles() {
        triangles = new Triangle[TRIANGLE_COUNT];
        for (int i = 0; i < TRIANGLE_COUNT; i++) {
            triangles[i] = new Triangle();
        }
        postDelayed(updateRunnable, 30);
    }

    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            for (Triangle t : triangles) {
                t.y += t.speed;
                if (t.y > getHeight()) t.y = 0;
            }
            invalidate();
            postDelayed(this, 30);
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Triangle t : triangles) {
            paint.setColor(t.color);
            paint.setAlpha(40); // 半透明
            Path path = new Path();
            path.moveTo(t.x, t.y);
            path.lineTo(t.x + t.size, t.y + t.size);
            path.lineTo(t.x - t.size, t.y + t.size);
            path.close();
            canvas.drawPath(path, paint);
        }
    }

    private class Triangle {
        float x, y, size, speed;
        int color;

        Triangle() {
            x = random.nextInt(1000);
            y = random.nextInt(2000);
            size = 30 + random.nextInt(30);
            speed = 1 + random.nextFloat() * 2;
            color = 0xFFFFFFFF; // 白色
        }
    }
}
