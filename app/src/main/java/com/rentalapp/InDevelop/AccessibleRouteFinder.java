// 🟣 AccessibleRouteFinder.java
package com.rentalapp.InDevelop;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用 Google Directions API 获取无障碍路径（步行模式）
 */
public class AccessibleRouteFinder {

    private static final String TAG = "AccessibleRouteFinder";
    private static final String API_KEY = "AIzaSyAtUw9mwOj2mY6vADIwtyLR5QDRdlWe0RU";
    private final RequestQueue queue;

    public AccessibleRouteFinder(Context context) {
        this.queue = Volley.newRequestQueue(context);
    }

    public interface OnRouteResult {
        void onResult(List<RouteStep> steps);
    }

    public void findAccessibleRoute(double startLat, double startLng,
                                    double endLat, double endLng,
                                    @NonNull OnRouteResult callback) {

        String url = String.format(
                "https://maps.googleapis.com/maps/api/directions/json?origin=%.6f,%.6f&destination=%.6f,%.6f&mode=walking&key=%s",
                startLat, startLng, endLat, endLng, API_KEY
        );

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    List<RouteStep> route = new ArrayList<>();
                    try {
                        JSONArray routes = response.getJSONArray("routes");
                        if (routes.length() == 0) {
                            Log.w(TAG, "❌ No route found");
                            callback.onResult(route);
                            return;
                        }

                        JSONArray legs = routes.getJSONObject(0).getJSONArray("legs");  // ✅ 修复点
                        JSONArray steps = legs.getJSONObject(0).getJSONArray("steps");  // ✅ 正确获取第一个 leg 的 steps


                        for (int i = 0; i < steps.length(); i++) {
                            JSONObject step = steps.getJSONObject(i);

                            RouteStep item = new RouteStep();
                            item.instruction = step.optString("html_instructions").replaceAll("<.*?>", "");
                            item.distance = step.getJSONObject("distance").optString("text");
                            item.startLat = step.getJSONObject("start_location").optDouble("lat");
                            item.startLng = step.getJSONObject("start_location").optDouble("lng");
                            item.endLat = step.getJSONObject("end_location").optDouble("lat");
                            item.endLng = step.getJSONObject("end_location").optDouble("lng");

                            // 标注潜在障碍关键词
                            item.hasObstacle = containsObstacle(item.instruction);

                            route.add(item);
                        }

                        callback.onResult(route);
                        Log.d(TAG, "✅ Route steps: " + route.size());

                    } catch (Exception e) {
                        Log.e(TAG, "❌ Parse route failed", e);
                        callback.onResult(route);
                    }
                },
                error -> {
                    Log.e(TAG, "❌ Request directions failed", error);
                    callback.onResult(new ArrayList<>());
                });

        queue.add(req);
    }

    private boolean containsObstacle(String text) {
        String[] dangerKeywords = {"stairs", "slope", "step", "steep", "elevated", "bridge"};
        text = text.toLowerCase();
        for (String word : dangerKeywords) {
            if (text.contains(word)) return true;
        }
        return false;
    }

    public static class RouteStep {
        public String instruction;
        public String distance;
        public double startLat, startLng;
        public double endLat, endLng;
        public boolean hasObstacle;
    }
}
