// 🟣 BusinessAccessibilityAnalyzer.java
package com.rentalapp.InDevelop;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.rentalapp.manhattonNavigate_486.OSMLoader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 用于分析 Google Maps 评论中是否包含无障碍信息的模块
 */
public class BusinessAccessibilityAnalyzer {

    private static final String TAG = "AccessibilityAnalyzer";
    private static final String API_KEY = "YOUR_GOOGLE_API_KEY"; // ✅ 替换为你的真实 Key
    private final Context context;
    private final RequestQueue queue;

    // 可识别的无障碍关键词（可根据真实数据拓展）
    private static final List<String> KEYWORDS = Arrays.asList(
            "wheelchair", "accessible", "ramp", "elevator", "stairs", "curb", "step", "entrance", "slope"
    );

    public BusinessAccessibilityAnalyzer(Context context) {
        this.context = context;
        this.queue = Volley.newRequestQueue(context);
    }

    public interface OnResultListener {
        void onResult(List<AnnotatedNode> result);
    }

    /**
     * 根据 OSM 商户名称+坐标，分析无障碍评论
     */
    public void analyzeAccessibility(List<OSMLoader.OSMNode> inputNodes, OnResultListener listener) {
        List<AnnotatedNode> resultList = new ArrayList<>();

        final int[] completed = {0};
        for (OSMLoader.OSMNode node : inputNodes) {
            String query = String.format(Locale.US, "%s near %.6f,%.6f", node.name, node.lat, node.lon);
            String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=" + URLEncoder.encode(query) + "&key=" + API_KEY;

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        try {
                            JSONArray results = response.getJSONArray("results");
                            if (results.length() > 0) {
                                JSONObject place = results.getJSONObject(0);
                                String placeId = place.getString("place_id");
                                fetchPlaceDetails(placeId, node, resultList, inputNodes.size(), completed, listener);
                            } else {
                                checkCompletion(resultList, inputNodes.size(), completed, listener);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "❌ textsearch error", e);
                            checkCompletion(resultList, inputNodes.size(), completed, listener);
                        }
                    },
                    error -> {
                        Log.e(TAG, "❌ textsearch fail", error);
                        checkCompletion(resultList, inputNodes.size(), completed, listener);
                    });

            queue.add(request);
        }
    }

    private void fetchPlaceDetails(String placeId, OSMLoader.OSMNode node,
                                   List<AnnotatedNode> resultList, int total,
                                   int[] completed, OnResultListener listener) {

        String url = "https://maps.googleapis.com/maps/api/place/details/json?place_id=" + placeId +
                "&fields=review&key=" + API_KEY;

        JsonObjectRequest detailReq = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject result = response.getJSONObject("result");
                        JSONArray reviews = result.optJSONArray("reviews");

                        boolean hasKeyword = false;
                        List<String> matched = new ArrayList<>();
                        if (reviews != null) {
                            for (int i = 0; i < reviews.length(); i++) {
                                String content = reviews.getJSONObject(i).optString("text", "").toLowerCase();
                                for (String key : KEYWORDS) {
                                    if (content.contains(key)) {
                                        matched.add(key);
                                        hasKeyword = true;
                                    }
                                }
                            }
                        }

                        if (hasKeyword) {
                            AnnotatedNode annotated = new AnnotatedNode(node);
                            annotated.matchedKeywords = matched;
                            resultList.add(annotated);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "❌ details parse error", e);
                    }
                    checkCompletion(resultList, total, completed, listener);
                },
                error -> {
                    Log.e(TAG, "❌ details request fail", error);
                    checkCompletion(resultList, total, completed, listener);
                });

        queue.add(detailReq);
    }

    private void checkCompletion(List<AnnotatedNode> result, int total, int[] completed, OnResultListener listener) {
        completed[0]++;
        if (completed[0] >= total) {
            listener.onResult(result);
            Log.d(TAG, "✅ Finished analyzing " + total + " businesses");
        }
    }

    /**
     * 表示一个带无障碍信息的商户节点
     */
    public static class AnnotatedNode extends OSMLoader.OSMNode {
        public List<String> matchedKeywords;

        public AnnotatedNode(OSMLoader.OSMNode base) {
            this.id = base.id;
            this.lat = base.lat;
            this.lon = base.lon;
            this.name = base.name;
            this.type = base.type;
        }
    }
}
