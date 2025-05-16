// 🟣 AccessibilityReviewCollector.java
package com.rentalapp.InDevelop;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 📡 从 Google Maps 收集 POI 的无障碍相关评论（轮椅通行、楼梯、坡道等）
 */
public class AccessibilityReviewCollector {

    private static final String TAG = "AccessibilityReviewCollector";
    private static final String GOOGLE_API_KEY = "AIzaSyAtUw9mwOj2mY6vADIwtyLR5QDRdlWe0RU";

    private final RequestQueue queue;
    private final Context context;

    private static final List<String> KEYWORDS = Arrays.asList(
            "wheelchair", "accessible", "stairs", "ramp", "elevator", "step", "slope", "curb"
    );

    public interface OnReviewResult {
        void onResult(List<AccessibilityReviewItem> matchedReviews);
    }

    public AccessibilityReviewCollector(Context context) {
        this.context = context;
        this.queue = Volley.newRequestQueue(context);
    }

    /**
     * 🚀 主入口函数：根据商户名称 + 坐标，获取并分析相关评论
     */
    public void collect(String name, double lat, double lon, OnReviewResult callback) {
        String query = String.format(Locale.US, "%s near %.6f,%.6f", name, lat, lon);
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=" +
                URLEncoder.encode(query) + "&key=" + GOOGLE_API_KEY;

        JsonObjectRequest textSearch = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        if (results.length() > 0) {
                            JSONObject first = results.getJSONObject(0);
                            String placeId = first.getString("place_id");
                            fetchReviews(placeId, name, lat, lon, callback);
                        } else {
                            Log.w(TAG, "❌ No place found for: " + name);
                            callback.onResult(new ArrayList<>());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ TextSearch parse failed", e);
                        callback.onResult(new ArrayList<>());
                    }
                },
                error -> {
                    Log.e(TAG, "❌ TextSearch failed", error);
                    callback.onResult(new ArrayList<>());
                });

        queue.add(textSearch);
    }

    /**
     * 📝 获取评论详情，并提取关键词匹配项
     */
    private void fetchReviews(String placeId, String name, double lat, double lon,
                              OnReviewResult callback) {
        String url = "https://maps.googleapis.com/maps/api/place/details/json?place_id=" +
                placeId + "&fields=name,reviews&key=" + GOOGLE_API_KEY;

        JsonObjectRequest detailRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    List<AccessibilityReviewItem> output = new ArrayList<>();
                    try {
                        JSONArray reviews = response.getJSONObject("result").optJSONArray("reviews");
                        if (reviews != null) {
                            for (int i = 0; i < reviews.length(); i++) {
                                JSONObject obj = reviews.getJSONObject(i);
                                String text = obj.optString("text", "").toLowerCase();
                                String time = obj.optString("relative_time_description", "Unknown");

                                List<String> matched = new ArrayList<>();
                                for (String key : KEYWORDS) {
                                    if (text.contains(key)) matched.add(key);
                                }

                                if (!matched.isEmpty()) {
                                    AccessibilityReviewItem item = new AccessibilityReviewItem(
                                            name, lat, lon,
                                            "Google Maps", text, matched, time
                                    );
                                    output.add(item);
                                }
                            }
                        }
                        callback.onResult(output);
                        Log.d(TAG, "✅ Matched reviews: " + output.size());
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Review parse failed", e);
                        callback.onResult(new ArrayList<>());
                    }
                },
                error -> {
                    Log.e(TAG, "❌ Review details failed", error);
                    callback.onResult(new ArrayList<>());
                });

        queue.add(detailRequest);
    }
}
