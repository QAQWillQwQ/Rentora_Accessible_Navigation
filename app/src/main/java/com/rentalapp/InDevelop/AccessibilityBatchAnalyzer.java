// 🟣 AccessibilityBatchAnalyzer.java
package com.rentalapp.InDevelop;

import android.content.Context;
import android.util.Log;

import com.rentalapp.manhattonNavigate_486.OSMLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * 🧠 自动分析一批商户的无障碍评论，筛选出含有效关键词的对象
 */
public class AccessibilityBatchAnalyzer {

    private static final String TAG = "AccessibilityBatchAnalyzer";
    private final AccessibilityPOIDatabase db;
    private final AccessibilityReviewCollector collector;

    public interface OnAnalysisComplete {
        void onReady(List<AccessibilityReviewItem> usefulItems);
    }

    public AccessibilityBatchAnalyzer(Context context) {
        this.db = new AccessibilityPOIDatabase(context);
        this.collector = new AccessibilityReviewCollector(context);
    }

    /**
     * 🔁 批量执行无障碍分析
     * @param poiList 一批商户点（来自 OSMLoader 等）
     * @param maxTarget 数量限制（例如筛选出最多 100 个有效）
     */
    public void analyzeBatch(List<OSMLoader.OSMNode> poiList, int maxTarget, OnAnalysisComplete callback) {
        List<AccessibilityReviewItem> results = new ArrayList<>();
        recursiveAnalyze(poiList, 0, maxTarget, results, callback);
    }

    private void recursiveAnalyze(List<OSMLoader.OSMNode> nodes,
                                  int index,
                                  int maxCount,
                                  List<AccessibilityReviewItem> collected,
                                  OnAnalysisComplete callback) {
        if (index >= nodes.size() || collected.size() >= maxCount) {
            callback.onReady(collected);
            Log.d(TAG, "🎯 Finished analysis. Found " + collected.size() + " valid POIs.");
            return;
        }

        OSMLoader.OSMNode node = nodes.get(index);
        String name = node.name;
        double lat = node.lat;
        double lon = node.lon;

        if (db.hasCached(name, lat, lon)) {
            List<AccessibilityReviewItem> cached = db.loadCached(name, lat, lon);
            if (!cached.isEmpty()) {
                collected.addAll(cached);
            }
            recursiveAnalyze(nodes, index + 1, maxCount, collected, callback);
        } else {
            collector.collect(name, lat, lon, items -> {
                if (!items.isEmpty()) {
                    collected.addAll(items);
                    db.saveReviews(name, lat, lon, items);
                    Log.d(TAG, "🟩 Useful review: " + name);
                } else {
                    Log.d(TAG, "🟥 No relevant reviews: " + name);
                    db.saveReviews(name, lat, lon, new ArrayList<>());
                }
                recursiveAnalyze(nodes, index + 1, maxCount, collected, callback);
            });
        }
    }
}
