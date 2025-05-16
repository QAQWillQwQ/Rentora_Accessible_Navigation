// 🟣 AccessibleMapActivity.java
package com.rentalapp.InDevelop;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.rentalapp.R;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.rentalapp.manhattonNavigate_486.OSMLoader;
import com.rentalapp.manhattonNavigate_486.RouteSelector;
import com.rentalapp.manhattonNavigate_486.TurnByTurnGenerator;

public class AccessibleMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private TextView instructionView;
    private static final LatLng CENTER_MANHATTAN = new LatLng(40.7445, -73.9971); // 曼哈顿中点

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accessible_map);
        instructionView = findViewById(R.id.instructionView);

        // 初始化地图
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(CENTER_MANHATTAN, 14));
        map.getUiSettings().setZoomControlsEnabled(true);

        startAccessibilityRouteFlow();
    }

    private void startAccessibilityRouteFlow() {
        OSMLoader loader = new OSMLoader(this);
        loader.fetchPOIs(osmNodes -> {
            Log.d("FLOW", "Step1 ✅ OSM nodes: " + osmNodes.size());

            BusinessAccessibilityAnalyzer analyzer = new BusinessAccessibilityAnalyzer(this);
            analyzer.analyzeAccessibility(osmNodes, annotated -> {
                Log.d("FLOW", "Step2 ✅ Accessibility nodes: " + annotated.size());

                // ✅ 1. 在地图上添加所有可达商户 marker
                for (OSMLoader.OSMNode node : annotated) {
                    LatLng pos = new LatLng(node.lat, node.lon);
                    map.addMarker(new MarkerOptions()
                            .position(pos)
                            .title("📍 " + node.name)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                }

                // ✅ 2. 自动选择 start 和 end 进行导航路线生成
                var pair = RouteSelector.selectPair(annotated);
                if (pair == null) {
                    Toast.makeText(this, "❌ No valid path within 2km", Toast.LENGTH_SHORT).show();
                    return;
                }

                map.addMarker(new MarkerOptions().position(new LatLng(pair.start.lat, pair.start.lon)).title("Start: " + pair.start.name));
                map.addMarker(new MarkerOptions().position(new LatLng(pair.end.lat, pair.end.lon)).title("End: " + pair.end.name));

                AccessibleRouteFinder finder = new AccessibleRouteFinder(this);
                finder.findAccessibleRoute(pair.start.lat, pair.start.lon, pair.end.lat, pair.end.lon,
                        steps -> {
                            Log.d("FLOW", "Step4 ✅ Route steps: " + steps.size());

                            // 绘制路径
                            PolylineOptions polylineOptions = new PolylineOptions().color(0xFF0077CC).width(10);
                            for (var s : steps) {
                                polylineOptions.add(new LatLng(s.startLat, s.startLng));
                            }
                            polylineOptions.add(new LatLng(steps.get(steps.size()-1).endLat, steps.get(steps.size()-1).endLng));
                            map.addPolyline(polylineOptions);

                            TurnByTurnGenerator generator = new TurnByTurnGenerator(this);
                            generator.generateInstructions(steps, result -> {
                                instructionView.setText("🧭 导航指令：\n" + result);
                            });
                        });
            });
        }, error -> {
            Toast.makeText(this, "❌ Failed to load OSM data", Toast.LENGTH_SHORT).show();
        });
    }
}
