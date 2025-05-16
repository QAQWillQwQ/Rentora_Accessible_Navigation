// 🟣 OSMLoader.java
package com.rentalapp.manhattonNavigate_486;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main class for querying business and accessibility data from OpenStreetMap via Overpass API.
 * This helps us gather POIs (Points of Interest) within a fixed region of Manhattan
 * and prepare them for navigation scoring and Firebase upload.
 */
public class OSMLoader {
    private static final String TAG = "OSMLoader";
    private final Context context;
    private static final String OVERPASS_QUERY =
            "[out:json][timeout:25];(" +
                    "node[\"shop\"](40.7397,-74.0059,40.8037,-73.9891);" +
                    "node[\"amenity\"](40.7397,-74.0059,40.8037,-73.9891);" +
                    "node[\"office\"](40.7397,-74.0059,40.8037,-73.9891);" +
                    ");out body 100;";

    /**
     * Constructor that sets up the context used for network requests.
     * This context is usually the current activity or application.
     */
    public OSMLoader(Context context) {
        this.context = context;
    }


    /**
     * Sends a custom Overpass query to fetch OSM nodes like shops and amenities.
     * Parses the returned JSON and builds a list of accessible POIs.
     * Each node is scored based on how much accessibility info it provides.
     *
     * @param onSuccess Called with top POIs when request succeeds.
     * @param onError Called if the request fails.
     */
    public void fetchFromOverpass(Response.Listener<List<OSMNode>> onSuccess,
                                  Response.ErrorListener onError) {
        Log.d(TAG, "🌐 Requesting OSM POIs from Overpass API...");

        String query = "[out:json][timeout:25];" +
                "(" +
                "node[\"shop\"](40.7397,-74.0059,40.8037,-73.9891);" +
                "node[\"amenity\"](40.7397,-74.0059,40.8037,-73.9891);" +
                ");out body;";
        String url = "https://overpass-api.de/api/interpreter?data=" + Uri.encode(query);

        RequestQueue queue = Volley.newRequestQueue(context);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray elements = response.getJSONArray("elements");
                        List<OSMNode> allNodes = new ArrayList<>();

                        for (int i = 0; i < elements.length(); i++) {
                            JSONObject item = elements.getJSONObject(i);
                            OSMNode node = new OSMNode();
                            node.id = item.optLong("id");
                            node.lat = item.optDouble("lat");
                            node.lon = item.optDouble("lon");

                            JSONObject tags = item.optJSONObject("tags");
                            if (tags != null) {
                                node.name = tags.optString("name", "Unnamed");
                                node.type = tags.optString("amenity", tags.optString("shop", "unknown"));
                                node.wheelchair = tags.optString("wheelchair", "");
                                node.kerb = tags.optString("kerb", "");
                                node.incline = tags.optString("incline", "");
                                node.surface = tags.optString("surface", "");
                                node.crossing = tags.optString("crossing", "");
                                node.tactilePaving = tags.optString("tactile_paving", "");

                                int score = 0;
                                if (!node.name.equals("Unnamed")) score++;
                                if (!node.wheelchair.isEmpty()) score++;
                                if (!node.kerb.isEmpty()) score++;
                                if (!node.incline.isEmpty()) score++;
                                if (!node.surface.isEmpty()) score++;
                                node.score = score;

                                allNodes.add(node);
                            }
                        }

                        // ✅ 按评分排序，取前100
                        allNodes.sort((a, b) -> Integer.compare(b.score, a.score));
                        List<OSMNode> top100 = allNodes.size() > 100 ? allNodes.subList(0, 100) : allNodes;

                        Log.d(TAG, "✅ Selected top " + top100.size() + " POIs from " + allNodes.size() + " candidates.");
                        onSuccess.onResponse(top100);

                    } catch (Exception e) {
                        Log.e(TAG, "❌ Failed to parse Overpass response", e);
                        onError.onErrorResponse(null);
                    }
                },
                error -> {
                    Log.e(TAG, "❌ Overpass request failed", error);
                    onError.onErrorResponse(error);
                });

        queue.add(request);
    }

    /**
     * Uploads the top 100 scored OSM POIs to Firebase Firestore.
     * Avoids uploading duplicates by checking if a document with the same ID exists.
     *
     * @param poiList List of OSMNodes (ranked) to store in the "Manhatton_Business_Site" collection.
     */
    public void saveTop100ToFirebase(List<OSMNode> poiList) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference col = db.collection("Manhatton_Business_Site");

        for (OSMNode node : poiList) {
            String docId = String.valueOf(node.id); // 用 OSM 的 ID 做唯一 key
            col.document(docId).get().addOnSuccessListener(snapshot -> {
                if (!snapshot.exists()) {
                    // 只插入不存在的
                    col.document(docId).set(nodeToMap(node))
                            .addOnSuccessListener(unused -> Log.d("UPLOAD_POI", "✅ Uploaded: " + node.name))
                            .addOnFailureListener(e -> Log.e("UPLOAD_POI", "❌ Upload failed: " + node.name, e));
                }
            });
        }
    }

    /**
     * Converts a single OSMNode into a Firebase-friendly key-value map.
     * This is needed for Firestore upload since Firestore works with maps.
     *
     * @param node The node to convert.
     * @return A map of field names and values.
     */
    private Map<String, Object> nodeToMap(OSMNode node) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", node.id);
        map.put("lat", node.lat);
        map.put("lon", node.lon);
        map.put("name", node.name);
        map.put("type", node.type);
        map.put("wheelchair", node.wheelchair);
        map.put("kerb", node.kerb);
        map.put("incline", node.incline);
        map.put("surface", node.surface);
        map.put("crossing", node.crossing);
        map.put("tactile_paving", node.tactilePaving);
        return map;
    }


    /**
     * Alternate fetch method that uses the full predefined query in OVERPASS_QUERY.
     * Also grabs POIs with tags related to accessibility. Meant for a different use case than fetchFromOverpass().
     *
     * @param onSuccess Called with list of OSM nodes if the fetch succeeds.
     * @param onError Called if the network request fails.
     */
    public void fetchPOIs(@NonNull Response.Listener<List<OSMNode>> onSuccess,
                          @NonNull Response.ErrorListener onError) {
        RequestQueue queue = Volley.newRequestQueue(context);

        String url = "https://overpass-api.de/api/interpreter?data=" + OVERPASS_QUERY;

        Log.d(TAG, "🌐 Requesting OSM POIs from Overpass API...");

        StringRequest request = new StringRequest(Request.Method.GET, url, response -> {
            try {
                JSONObject json = new JSONObject(response);
                JSONArray elements = json.getJSONArray("elements");

                List<OSMNode> nodes = new ArrayList<>();

                for (int i = 0; i < elements.length(); i++) {
                    JSONObject item = elements.getJSONObject(i);
                    JSONObject tags = item.optJSONObject("tags");
                    if (tags == null) continue;

                    OSMNode node = new OSMNode();
                    node.id = item.optLong("id");
                    node.lat = item.optDouble("lat");
                    node.lon = item.optDouble("lon");

                    node.name = tags.optString("name", "Unnamed");
                    node.type = tags.optString("shop",
                            tags.optString("amenity",
                                    tags.optString("office", "unknown")));

                    // ✅ 可达性字段（如有）
                    node.wheelchair = tags.optString("wheelchair", "unknown");
                    node.kerb = tags.optString("kerb", "");
                    node.incline = tags.optString("incline", "");
                    node.surface = tags.optString("surface", "");
                    node.crossing = tags.optString("crossing", "");
                    node.tactilePaving = tags.optString("tactile_paving", "");

                    nodes.add(node);
                }

                Log.d(TAG, "✅ Successfully loaded " + nodes.size() + " POIs");
                onSuccess.onResponse(nodes);
            } catch (Exception e) {
                Log.e(TAG, "❌ JSON parse error", e);
                onError.onErrorResponse(null);
            }

        }, error -> {
            Log.e(TAG, "❌ Network error loading POIs", error);
            onError.onErrorResponse(error);
        });

        queue.add(request);
    }

    /**
     * Represents a single OSM node object (e.g., a store, restaurant).
     * Holds both its basic info and accessibility-related metadata fields.
     */
    public static class OSMNode {
        public int score;

        public long id;
        public double lat;
        public double lon;
        public String name;
        public String type;

        public String wheelchair;
        public String kerb;
        public String incline;
        public String surface;
        public String crossing;
        public String tactilePaving;
    }
}
