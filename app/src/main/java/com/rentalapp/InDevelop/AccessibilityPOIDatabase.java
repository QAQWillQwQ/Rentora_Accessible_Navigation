// 🟣 AccessibilityPOIDatabase.java
package com.rentalapp.InDevelop;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.*;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 💾 本地 SQLite 数据库，用于记录已采集商户及其无障碍评论
 */
public class AccessibilityPOIDatabase extends SQLiteOpenHelper {

    private static final String TAG = "AccessibilityPOIDB";
    private static final String DB_NAME = "accessibility_poi.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE = "poi_reviews";

    public AccessibilityPOIDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String create = "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "lat REAL, " +
                "lon REAL, " +
                "review_json TEXT" +
                ")";
        db.execSQL(create);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    /**
     * ✅ 插入评论列表（自动覆盖重复坐标的记录）
     */
    public void saveReviews(String name, double lat, double lon, List<AccessibilityReviewItem> list) {
        try {
            JSONArray arr = new JSONArray();
            for (AccessibilityReviewItem item : list) {
                JSONObject obj = new JSONObject();
                obj.put("source", item.source);
                obj.put("text", item.fullText);
                obj.put("matched", new JSONArray(item.matchedKeywords));
                obj.put("timestamp", item.timestamp);
                arr.put(obj);
            }

            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("lat", lat);
            values.put("lon", lon);
            values.put("review_json", arr.toString());

            db.delete(TABLE, "name=? AND lat=? AND lon=?", new String[]{name, String.valueOf(lat), String.valueOf(lon)});
            db.insert(TABLE, null, values);
            db.close();
            Log.d(TAG, "✅ Saved review cache: " + name);
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to save review cache", e);
        }
    }

    /**
     * 🔍 查询是否已经缓存过该 POI
     */
    public boolean hasCached(String name, double lat, double lon) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE + " WHERE name=? AND lat=? AND lon=?",
                new String[]{name, String.valueOf(lat), String.valueOf(lon)});
        boolean result = false;
        if (c.moveToFirst()) {
            result = c.getInt(0) > 0;
        }
        c.close();
        db.close();
        return result;
    }

    /**
     * 📤 加载缓存评论（如果存在）
     */
    public List<AccessibilityReviewItem> loadCached(String name, double lat, double lon) {
        List<AccessibilityReviewItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT review_json FROM " + TABLE + " WHERE name=? AND lat=? AND lon=?",
                new String[]{name, String.valueOf(lat), String.valueOf(lon)});
        if (c.moveToFirst()) {
            try {
                String json = c.getString(0);
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    list.add(new AccessibilityReviewItem(
                            name, lat, lon,
                            obj.optString("source"),
                            obj.optString("text"),
                            jsonArrayToList(obj.optJSONArray("matched")),
                            obj.optString("timestamp")
                    ));
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to parse review JSON", e);
            }
        }
        c.close();
        db.close();
        return list;
    }

    private List<String> jsonArrayToList(JSONArray arr) {
        List<String> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            list.add(arr.optString(i));
        }
        return list;
    }
}
