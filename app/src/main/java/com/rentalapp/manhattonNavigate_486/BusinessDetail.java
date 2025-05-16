
package com.rentalapp.manhattonNavigate_486;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.rentalapp.R;

public class BusinessDetail extends AppCompatActivity {
    private double mapCenterLat;
    private double mapCenterLng;
    private float mapZoom;

    /**
     * Initializes the main Google Map screen when the app starts.
     * It loads the map, handles permissions, sets up all buttons, and pulls house + business data from Firestore.
     * Also configures UI like search bars, tilt slider, disability navigation buttons, etc.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.business_detail);

        // ✅ Enable ActionBar back button
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        TextView contentView = findViewById(R.id.detailContent);
        TextView titleView = findViewById(R.id.detailTitle);

        String docId = getIntent().getStringExtra("business_id");

        // ✅ Get map state passed from Map.java
        mapCenterLat = getIntent().getDoubleExtra("map_center_lat", 0.0);
        mapCenterLng = getIntent().getDoubleExtra("map_center_lng", 0.0);
        mapZoom = getIntent().getFloatExtra("map_zoom", 16f);

        if (docId != null) {
            FirebaseFirestore.getInstance().collection("Business Site_csv")
                    .document(docId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Name: ").append(snapshot.getString("Business Name")).append("\n");
                            sb.append("Type: ").append(snapshot.getString("Business Type")).append("\n");
                            sb.append("Address: ").append(snapshot.getString("Address")).append("\n");
                            sb.append("Latitude: ").append(snapshot.getDouble("Latitude")).append("\n");
                            sb.append("Longitude: ").append(snapshot.getDouble("Longitude")).append("\n");
                            sb.append("Tactile: ").append(snapshot.getString("盲道")).append("\n");
                            sb.append("Slope: ").append(snapshot.getString("陡坡")).append("\n");
                            sb.append("Wheelchair: ").append(snapshot.getString("Wheelchair")).append("\n");
                            sb.append("Elevator: ").append(snapshot.getString("电梯")).append("\n");

                            contentView.setText(sb.toString());
                        } else {
                            contentView.setText("No business data found.");
                        }
                    });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // trigger override below
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(BusinessDetail.this, Map.class);
        intent.putExtra("map_center_lat", mapCenterLat);
        intent.putExtra("map_center_lng", mapCenterLng);
        intent.putExtra("map_zoom", mapZoom);
        startActivity(intent);
        finish(); // prevent stacking
        super.onBackPressed();
    }

}
