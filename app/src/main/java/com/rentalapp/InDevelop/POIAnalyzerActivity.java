// 🟣 POIAnalyzerActivity.java
package com.rentalapp.InDevelop;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rentalapp.R;
import com.rentalapp.manhattonNavigate_486.OSMLoader;

import java.util.ArrayList;
import java.util.List;

public class POIAnalyzerActivity extends AppCompatActivity {

    private Button startBtn;
    private TextView statusText;
    private ProgressBar progress;
    private RecyclerView recyclerView;
    private POIReviewAdapter adapter;
    private List<AccessibilityReviewItem> reviewList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poi_analyzer);

        startBtn = findViewById(R.id.startBtn);
        statusText = findViewById(R.id.statusText);
        progress = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.reviewList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new POIReviewAdapter(reviewList);
        recyclerView.setAdapter(adapter);

        startBtn.setOnClickListener(v -> startAnalysis());
    }

    private void startAnalysis() {
        progress.setVisibility(View.VISIBLE);
        statusText.setText("⏳ Analyzing...");
        reviewList.clear();
        adapter.notifyDataSetChanged();

        OSMLoader loader = new OSMLoader(this);
        loader.fetchPOIs(
                pois -> {
                    AccessibilityBatchAnalyzer analyzer = new AccessibilityBatchAnalyzer(this);
                    analyzer.analyzeBatch(pois, 100, results -> {
                        reviewList.addAll(results);
                        adapter.notifyDataSetChanged();
                        statusText.setText("✅ Found " + results.size() + " accessible POIs.");
                        progress.setVisibility(View.GONE);
                    });
                },
                error -> {
                    Log.e("OSMLoader", "❌ Failed to load OSM POIs", error);
                    statusText.setText("❌ Failed to load POI data.");
                    progress.setVisibility(View.GONE);
                }
        );
    }

}
