// 🟣 Map.java

package com.rentalapp.manhattonNavigate_486;
import android.Manifest;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rentalapp.auth.Login;
import com.rentalapp.auth.MyCenter;
import com.rentalapp.R;
import com.rentalapp.base.FirebaseMerge;
import com.rentalapp.bean.House;
import com.rentalapp.base.DatabaseHelper;
import com.rentalapp.house.Detail;
import com.rentalapp.house.Manage;
import com.rentalapp.maintenance.MaintenanceManage;
import com.rentalapp.map.Collect;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class Map extends AppCompatActivity implements OnMapReadyCallback {

    // 🔹 Used to link each business marker to its corresponding Firestore Document ID
    private java.util.Map<Marker, String> businessMarkerMap = new java.util.HashMap<>();

    private List<Marker> analyzeRouteMarkers = new ArrayList<>();
    private List<com.google.android.gms.maps.model.Polyline> analyzePolylines = new ArrayList<>();

    private int mapModeState = 0;  // 0 = normal, 1 = satellite 2D, 2 = satellite 3D

    private float savedZoomLevel = 0f;
    private LatLng savedCenterLatLng = null;

    private boolean isNavigateMode = false;
    private EditText navigateInput;
    private Button navigateGoBtn;

    private boolean ttsReady = false;

    private GoogleMap mMap;
    String role = "";
    List<House> list = new ArrayList<>();
    private DatabaseHelper databaseHelper;
    EditText et_title,et_minPrice,et_maxPrice,et_minArea,et_maxArea;
    private Spinner sp_bedrooms, sp_bathrooms;
    private String sel_bed = "", sel_bath = "";
    String[] arr = {"", "1", "2", "4"};
    String[] arr1 = {"", "1", "2"};

    private PopupWindow popupWindow;

    private boolean isSearchMode = false;
    private List<House> searchResultList = new ArrayList<>();

    private TextToSpeech tts;
    private List<RouteStep> currentSteps = new ArrayList<>();
    private int currentStepIndex = 0;

    private android.location.LocationManager locationManager;
    private android.location.LocationListener locationListener;


    /**
     * Initializes the main Google Map screen when the app starts.
     * It loads the map, handles permissions, sets up all buttons, and pulls house + business data from Firestore.
     * Also configures UI like search bars, tilt slider, disability navigation buttons, etc.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        java.util.Locale.setDefault(java.util.Locale.ENGLISH);
        android.content.res.Configuration config = getResources().getConfiguration();
        config.setLocale(java.util.Locale.ENGLISH);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        super.onCreate(savedInstanceState);

        if (!com.google.android.libraries.places.api.Places.isInitialized()) {
            com.google.android.libraries.places.api.Places.initialize(
                    getApplicationContext(),
                    "AIzaSyAtUw9mwOj2mY6vADIwtyLR5QDRdlWe0RU"
            );
            Log.d("PLACES_INIT", "✅ Google Places API initialized");
        }

        setContentView(R.layout.map);

        LinearLayout topContainer = findViewById(R.id.search_container);
        LinearLayout instructionContainer = findViewById(R.id.instructionContainer);

        Button btnHideTop = findViewById(R.id.btn_hide_top_ui);
        btnHideTop.setOnClickListener(v -> {
            if (topContainer.getVisibility() == View.VISIBLE) {
                topContainer.setVisibility(View.GONE);
                btnHideTop.setText("⬇️");
            } else {
                topContainer.setVisibility(View.VISIBLE);
                btnHideTop.setText("⬆️");
            }
        });

        Button btnHideAll = findViewById(R.id.btn_hide_all_ui);
        btnHideAll.setOnClickListener(v -> {
            boolean visible = (topContainer.getVisibility() == View.VISIBLE || instructionContainer.getVisibility() == View.VISIBLE);
            topContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
            instructionContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
            btnHideAll.setText(visible ? "⬆️ SHOW UI" : "⬇️ HIDE UI");
        });


        databaseHelper = new DatabaseHelper(this);

        new FirebaseMerge(this)
                .mergeHousesIfNotExists();

        list = databaseHelper.getAllHousesIncludeUnchecked();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 1001);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        SharedPreferences sp = getSharedPreferences("userinfo", 0);
        role = sp.getString("role", "");

        Button queryBtn = findViewById(R.id.queryBtn);
        queryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopupWindow(view);
            }
        });

        Button disabilityBtn = findViewById(R.id.btn_disability_nav);
        disabilityBtn.setOnClickListener(v -> {

            String[] types = {"🧑‍🦯 blind", "♿️ wheelchair"};
            new AlertDialog.Builder(this)
                    .setTitle("Choose Disability Type")
                    .setItems(types, (dialog, which) -> {
                        String userType = (which == 0) ? "blind" : "wheelchair";

                        String startName = ((EditText)findViewById(R.id.et_start_point)).getText().toString().trim();
                        String endName = ((EditText)findViewById(R.id.et_end_point)).getText().toString().trim();

                        if (startName.isEmpty() || endName.isEmpty()) {
                            Toast.makeText(Map.this, "Please enter both start and end point", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Toast.makeText(Map.this, "⏳ Finding path for: " + types[which], Toast.LENGTH_SHORT).show();

                        new Thread(() -> {
                            LatLng startCoord = findLatLngByName(startName);
                            if (startCoord == null) {
                                runOnUiThread(() -> Toast.makeText(Map.this, "❌ Start not found", Toast.LENGTH_LONG).show());
                                return;
                            }

                            LatLng endCoord = findLatLngByName(endName);
                            if (endCoord == null) {
                                runOnUiThread(() -> Toast.makeText(Map.this, "❌ End not found", Toast.LENGTH_LONG).show());
                                return;
                            }

                            runOnUiThread(() -> {
                                Toast.makeText(Map.this, "✅ Drawing accessible path...", Toast.LENGTH_SHORT).show();
                                drawAccessibleRoutes(startCoord, endCoord, userType);
                                instructionContainer.setVisibility(View.VISIBLE); // 确保说明区展示
                            });

                        }).start();
                    }).show();
        });


        EditText etZip = findViewById(R.id.et_zip);
        Button btnZipSearch = findViewById(R.id.btn_zip_search);
        btnZipSearch.setOnClickListener(v -> {
            String zip = etZip.getText().toString().trim();
            if (!zip.isEmpty()) {
                searchZipAndMoveCamera(zip);
            } else {
                Toast.makeText(Map.this, "Please enter ZIP code", Toast.LENGTH_SHORT).show();
            }
        });

        btnHideTop.setOnClickListener(v -> {
            if (topContainer.getVisibility() == View.VISIBLE) {
                topContainer.setVisibility(View.GONE);
                btnHideTop.setText("⬇️"); // 替代控件可用 FrameLayout 居中
            } else {
                topContainer.setVisibility(View.VISIBLE);
                btnHideTop.setText("⬆️");
            }
        });

        Button btnHideInstruction = findViewById(R.id.btn_hide_instruction);
        LinearLayout instruction = findViewById(R.id.instructionContainer);
        btnHideInstruction.setOnClickListener(v -> {
            if (instruction.getVisibility() == View.VISIBLE) {
                instruction.setVisibility(View.GONE);
                btnHideInstruction.setText("⬆️");
            } else {
                instruction.setVisibility(View.VISIBLE);
                btnHideInstruction.setText("⬇️");
            }
        });

        LinearLayout searchContainer = findViewById(R.id.search_container);


        Button toggleMapBtn = findViewById(R.id.btn_toggle_mapview);
        toggleMapBtn.setOnClickListener(v -> {
            mapModeState = (mapModeState + 1) % 3;

            if (mapModeState == 0) {
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder()
                                .target(mMap.getCameraPosition().target)
                                .zoom(mMap.getCameraPosition().zoom)
                                .tilt(0)  // 平视
                                .bearing(0)
                                .build()
                ));
                toggleMapBtn.setText("Normal Map");
            } else if (mapModeState == 1) {
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder()
                                .target(mMap.getCameraPosition().target)
                                .zoom(mMap.getCameraPosition().zoom)
                                .tilt(0)
                                .bearing(0)
                                .build()
                ));
                toggleMapBtn.setText("Satellite 2D");
            } else {
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder()
                                .target(mMap.getCameraPosition().target)
                                .zoom(mMap.getCameraPosition().zoom)
                                .tilt(60)  // ✅ 倾斜角度以展示 3D 建筑
                                .bearing(45)  // 可选旋转角度
                                .build()
                ));
                toggleMapBtn.setText("Satellite 3D");
            }
        });

        SeekBar tiltSeekBar = findViewById(R.id.tiltSeekBar);
        TextView tiltValue = findViewById(R.id.tiltValue);

        tiltSeekBar.setMax(90);
        tiltSeekBar.setProgress(0);

        tiltSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tiltValue.setText(progress + "°");

                if (mMap != null) {
                    CameraPosition current = mMap.getCameraPosition();
                    CameraPosition newPos = new CameraPosition.Builder()
                            .target(current.target)
                            .zoom(Math.max(current.zoom, 17))
                            .bearing(current.bearing)
                            .tilt(progress)
                            .build();
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(newPos));
                }
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        LinearLayout navStepInputContainer = findViewById(R.id.navStepInputContainer);
        EditText etStartPoint = findViewById(R.id.et_start_point);
        EditText etEndPoint = findViewById(R.id.et_end_point);
        Button btnStartNav = findViewById(R.id.btn_start_nav);

        btnStartNav.setOnClickListener(v -> {
            String startName = etStartPoint.getText().toString().trim();
            String endName = etEndPoint.getText().toString().trim();

            if (startName.isEmpty() || endName.isEmpty()) {
                Toast.makeText(this, "Please enter both start and end points", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "⏳ Searching start point...", Toast.LENGTH_SHORT).show();

            new Thread(() -> {
                LatLng startCoord = findLatLngByName(startName);
                if (startCoord == null) {
                    runOnUiThread(() -> Toast.makeText(Map.this, "❌ Could not find START point: " + startName, Toast.LENGTH_LONG).show());
                    return;
                }

                Log.d("NAV_START", "✅ Found start: " + startCoord.toString());
                runOnUiThread(() -> Toast.makeText(Map.this, "✅ Start found. Now searching end point...", Toast.LENGTH_SHORT).show());

                // 第二个请求等第一个完成后再发
                LatLng endCoord = findLatLngByName(endName);
                if (endCoord == null) {
                    runOnUiThread(() -> Toast.makeText(Map.this, "❌ Could not find END point: " + endName, Toast.LENGTH_LONG).show());
                    return;
                }

                Log.d("NAV_END", "✅ Found end: " + endCoord.toString());
                runOnUiThread(() -> {
                    Toast.makeText(Map.this, "✅ Both locations found! Drawing route...", Toast.LENGTH_SHORT).show();
                    drawMultipleRoutes(startCoord, endCoord);
                });

            }).start();
        });

        LinearLayout stepNavContainer = findViewById(R.id.stepNavContainer);
        TextView stepText = findViewById(R.id.stepText);
        Button btnPrevStep = findViewById(R.id.btnPrevStep);
        Button btnNextStep = findViewById(R.id.btnNextStep);

        btnPrevStep.setOnClickListener(v -> {
            if (currentStepIndex > 0) {
                currentStepIndex--;
                speakCurrentStep(stepText);
            }
        });
        btnNextStep.setOnClickListener(v -> {
            if (currentStepIndex < currentSteps.size() - 1) {
                currentStepIndex++;
                speakCurrentStep(stepText);
            }
        });

        locationManager = (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = location -> {
            if (!currentSteps.isEmpty() && currentStepIndex < currentSteps.size()) {
                RouteStep step = currentSteps.get(currentStepIndex);
                double targetLat = step.endLat;
                double targetLng = step.endLng;

                float[] result = new float[1];
                android.location.Location.distanceBetween(
                        location.getLatitude(), location.getLongitude(),
                        targetLat, targetLng, result);

                float distance = result[0];
                Log.d("GPS_STEP", "📍 Distance to step target: " + distance + " meters");

                if (distance < 25.0f) {
                    runOnUiThread(() -> {
                        currentStepIndex++;
                        if (currentStepIndex < currentSteps.size()) {
                            speakCurrentStep(findViewById(R.id.stepText));
                        } else {
                            tts.speak("🎉 You have reached your destination.", TextToSpeech.QUEUE_FLUSH, null, null);
                            Toast.makeText(this, "🎉 到达终点", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, 2000, 3, locationListener);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1002);
        }

        FirebaseFirestore.getInstance().collection("Manhatton_Business_Site")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        double lat = doc.getDouble("lat");
                        double lon = doc.getDouble("lon");
                        String name = doc.getString("name");
                        String type = doc.getString("type");

                        LatLng pos = new LatLng(lat, lon);

                        float color;
                        switch (type.toLowerCase()) {
                            case "restaurant": color = BitmapDescriptorFactory.HUE_ORANGE; break;
                            case "gym": color = BitmapDescriptorFactory.HUE_GREEN; break;
                            case "cafe": color = BitmapDescriptorFactory.HUE_BLUE; break;
                            case "pharmacy": color = BitmapDescriptorFactory.HUE_RED; break;
                            case "grocery": color = BitmapDescriptorFactory.HUE_YELLOW; break;
                            case "bookstore": color = BitmapDescriptorFactory.HUE_VIOLET; break;
                            case "bakery": color = BitmapDescriptorFactory.HUE_MAGENTA; break;
                            case "bank": color = BitmapDescriptorFactory.HUE_ROSE; break;
                            default: color = BitmapDescriptorFactory.HUE_CYAN;
                        }

                        mMap.addMarker(new MarkerOptions()
                                .position(pos)
                                .title(name)
                                .icon(BitmapDescriptorFactory.defaultMarker(color))
                                .anchor(0.5f, 1.0f));
                    }
                    Log.d("FIREBASE_POI", "✅ Loaded POIs from Firestore: " + snapshot.size());
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_POI", "❌ Failed to load POIs from Firebase", e);
                    Toast.makeText(this, "❌ Cannot load business sites", Toast.LENGTH_SHORT).show();
                });


        FirebaseFirestore.getInstance().collection("Manhatton_Business_Site")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Log.d("OSM_INIT", "📡 Firebase business POI is empty, now triggering OSM import...");
                        OSMLoader osmLoader = new OSMLoader(this);
                        osmLoader.fetchFromOverpass(top100 -> {
                            Log.d("OSM_INIT", "✅ Successfully fetched top 100 business sites from OSM");
                            osmLoader.saveTop100ToFirebase(top100);  // ✅ 保存到 Firebase
                        }, error -> Log.e("OSM_INIT", "❌ OSM fetch failed", error));
                    } else {
                        Log.d("OSM_INIT", "✅ Business POIs already exist in Firebase, skipping import.");
                    }
                });

        if (getIntent().getBooleanExtra("refreshBusinessSites", false)) {
            loadBusinessSitesFromFirestore(); // 重新加载
        }

        if (savedZoomLevel == 0f && mMap != null) {
            savedZoomLevel = mMap.getCameraPosition().zoom;
            savedCenterLatLng = mMap.getCameraPosition().target;
        }
    }

    /**
     * Speaks the current step of the navigation route using TTS (Text-to-Speech).
     * Also updates a TextView to display the step text and moves the map to the step location.
     */
    private void speakCurrentStep(TextView stepTextView) {
        if (currentSteps.isEmpty()) return;

        RouteStep step = currentSteps.get(currentStepIndex);
        String plainText = android.text.Html.fromHtml(step.htmlInstruction).toString();
        String speakText = "Step " + (currentStepIndex + 1) + ": " + plainText;

        stepTextView.setText(speakText);

        if (ttsReady) {
            Log.d("TTS_STEP", "🗣 Speaking step: " + speakText);
            tts.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            Log.e("TTS_STEP", "❌ TTS not ready when trying to speak step");
        }

        // ✅ 推进摄像头 + 添加 step marker
        LatLng target = step.end;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 18));

        mMap.addMarker(new MarkerOptions()
                .position(target)
                .title("Step " + (currentStepIndex + 1))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .anchor(0.5f, 1.0f)
                .zIndex(200));
    }

    /**
     * Tries to find a location's LatLng by using Google Places API with text input.
     * If not found via API, it checks if any local house titles match.
     */
    private LatLng findLatLngByName(String name) {
        name = name.toLowerCase();

        try {
            String apiKey = "AIzaSyAtUw9mwOj2mY6vADIwtyLR5QDRdlWe0RU";
            String urlStr = "https://maps.googleapis.com/maps/api/place/textsearch/json" +
                    "?query=" + java.net.URLEncoder.encode(name, "UTF-8") +
                    "&key=" + apiKey;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONObject response = new JSONObject(sb.toString());
            JSONArray results = response.optJSONArray("results");
            if (results != null && results.length() > 0) {
                JSONObject geometry = results.getJSONObject(0).optJSONObject("geometry");
                if (geometry != null) {
                    JSONObject loc = geometry.getJSONObject("location");
                    double lat = loc.getDouble("lat");
                    double lng = loc.getDouble("lng");
                    Log.d("PLACES_API", "✅ Found via PLACES TEXT SEARCH: " + name + " -> " + lat + ", " + lng);
                    return new LatLng(lat, lng);
                }
            } else {
                Log.w("PLACES_API", "⚠️ Places API TEXT SEARCH: No match for " + name);
            }

        } catch (Exception e) {
            Log.e("PLACES_API", "❌ Error using TEXT SEARCH", e);
        }

        for (House h : list) {
            if (h.getTitle() != null && h.getTitle().toLowerCase().contains(name)) {
                try {
                    return new LatLng(Double.parseDouble(h.getLat()), Double.parseDouble(h.getLng()));
                } catch (Exception ignored) {}
            }
        }

        return null;
    }

    private class RouteOption {
        List<LatLng> path;
        String summary;
        String duration;
        String distance;
        int color;
    }

    private class RouteStep {
        String htmlInstruction;
        String distance;
        String duration;
        LatLng start;
        LatLng end;

        double endLat;
        double endLng;
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();

        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }

    }


    /**
     * Parses a single Google Directions route and extracts all steps (with start, end, instructions).
     * These are stored as RouteStep objects for use in turn-by-turn navigation.
     */
    private List<RouteStep> parseStepsFromRoute(JSONObject route) {
        List<RouteStep> steps = new ArrayList<>();
        try {
            JSONArray legs = route.getJSONArray("legs");
            if (legs.length() == 0) return steps;

            JSONArray jsonSteps = legs.getJSONObject(0).getJSONArray("steps");

            for (int i = 0; i < jsonSteps.length(); i++) {
                JSONObject s = jsonSteps.getJSONObject(i);
                JSONObject startLoc = s.getJSONObject("start_location");
                JSONObject endLoc = s.getJSONObject("end_location");

                RouteStep step = new RouteStep();
                step.start = new LatLng(startLoc.getDouble("lat"), startLoc.getDouble("lng"));
                step.end = new LatLng(endLoc.getDouble("lat"), endLoc.getDouble("lng"));
                step.distance = s.getJSONObject("distance").getString("text");
                step.duration = s.getJSONObject("duration").getString("text");
                step.htmlInstruction = s.getString("html_instructions");

                steps.add(step);
            }

        } catch (Exception e) {
            Log.e("PARSE_STEPS", "❌ Failed to parse steps", e);
        }

        return steps;
    }

    /**
     * Converts all navigation steps into a full guide using OpenAI API.
     * The AI rewrites raw Google instructions into cleaner, human-friendly steps and reads them out loud.
     */
    private void generateStepByStepInstructions(List<RouteStep> steps) {
        StringBuilder rawInstructions = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            RouteStep s = steps.get(i);
            rawInstructions.append((i + 1) + ". " + android.text.Html.fromHtml(s.htmlInstruction).toString())
                    .append(" (").append(s.distance).append(", ").append(s.duration).append(")\n");
        }

        String prompt = "Convert these raw Google Maps walking directions into a friendly and clear navigation guide:\n\n" + rawInstructions;

        new Thread(() -> {
            try {
                URL url = new URL("https://api.openai.com/v1/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer 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");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("model", "gpt-3.5-turbo");

                JSONArray messages = new JSONArray();
                JSONObject sysMsg = new JSONObject();
                sysMsg.put("role", "system");
                sysMsg.put("content", "You are a helpful navigation assistant for pedestrians.");
                messages.put(sysMsg);

                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                userMsg.put("content", prompt);
                messages.put(userMsg);

                json.put("messages", messages);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONObject respJson = new JSONObject(response.toString());
                Log.d("OPENAI_NAV", "✅ OpenAI response: " + respJson.toString());

                if (!respJson.has("choices")) {
                    Log.e("OPENAI_NAV", "❌ No 'choices' field in response");
                    return;
                }

                JSONArray choices = respJson.getJSONArray("choices");
                if (choices.length() == 0 || !choices.getJSONObject(0).has("message")) {
                    Log.e("OPENAI_NAV", "❌ Empty 'choices' or missing 'message'");
                    return;
                }

                String content = choices.getJSONObject(0).getJSONObject("message").optString("content", "");
                if (content.isEmpty()) {
                    Log.e("OPENAI_NAV", "❌ 'content' is empty");
                    return;
                }

                runOnUiThread(() -> {
                    TextView instructionView = findViewById(R.id.instructionView);
                    instructionView.setText("🧭 Walking Guide:\n\n" + content);

                    if (ttsReady) {
                        Log.d("TTS_AI_FULL", "🗣 Now speaking AI instruction block...");
                        tts.speak(content, TextToSpeech.QUEUE_FLUSH, null, null);
                    } else {
                        Log.e("TTS_AI_FULL", "❌ TTS not ready when trying to speak AI instructions");
                    }

                    LinearLayout stepNavContainer = findViewById(R.id.stepNavContainer);
                    TextView stepText = findViewById(R.id.stepText);

                    stepNavContainer.setVisibility(View.VISIBLE);
                    currentStepIndex = 0;

                    // ✅ 再次调用 speakCurrentStep，确保朗读第一步
                    if (!currentSteps.isEmpty()) {
                        Log.d("TTS_NAV", "🗣 Speaking first step now...");
                        speakCurrentStep(stepText);  // 自动朗读第一步
                    } else {
                        Log.w("TTS_NAV", "⚠️ currentSteps is empty; nothing to speak");
                    }

                });


            } catch (Exception e) {
                Log.e("OPENAI_NAV", "❌ Failed to generate AI instructions", e);
            }
        }).start();
    }


    /**
     * Draws multiple walking routes between two points using Directions API.
     * The user can pick the best one by clicking on markers or buttons.
     * It also triggers the AI-based path generator from `MyAINavigation`.
     */
    private void drawMultipleRoutes(LatLng origin, LatLng destination) {
        String apiKey = "AIzaSyAtUw9mwOj2mY6vADIwtyLR5QDRdlWe0RU";
        String urlStr = "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&mode=walking&alternatives=true&key=" + apiKey;

        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                JSONArray routes = json.getJSONArray("routes");

                List<RouteOption> routeOptions = new ArrayList<>();
                int[] colors = {Color.BLUE, Color.RED, Color.GREEN};
                List<LatLng> allPoints = new ArrayList<>();

                for (int i = 0; i < routes.length(); i++) {
                    JSONObject route = routes.getJSONObject(i);
                    String polyline = route.getJSONObject("overview_polyline").getString("points");
                    List<LatLng> path = decodePoly(polyline);
                    allPoints.addAll(path);

                    JSONObject leg = route.getJSONArray("legs").getJSONObject(0);
                    String duration = leg.getJSONObject("duration").getString("text");
                    String distance = leg.getJSONObject("distance").getString("text");

                    RouteOption option = new RouteOption();
                    option.path = path;
                    option.duration = duration;
                    option.distance = distance;
                    option.summary = "Option " + (i + 1) + ": " + duration + " / " + distance;
                    option.color = colors[i % colors.length];

                    routeOptions.add(option);
                }

                runOnUiThread(() -> {
                    mMap.clear(); reloadMapMarkers();
                    reloadOSMMarkers();
                    loadBusinessSitesFromFirestore();

                    int fastestIndex = 0;
                    int fastestSeconds = Integer.MAX_VALUE;
                    for (int i = 0; i < routeOptions.size(); i++) {
                        try {
                            int minutes = Integer.parseInt(routeOptions.get(i).duration.replaceAll("[^0-9]", ""));
                            if (minutes < fastestSeconds) {
                                fastestSeconds = minutes;
                                fastestIndex = i;
                            }
                        } catch (Exception ignored) {}
                    }

                    LinearLayout container = findViewById(R.id.pathSelectionContainer);
                    container.removeAllViews();
                    container.setVisibility(View.VISIBLE);

                    for (int i = 0; i < routeOptions.size(); i++) {
                        RouteOption option = routeOptions.get(i);

                        List<LatLng> path = option.path;
                        PolylineOptions options = new PolylineOptions()
                                .color(option.color)
                                .width(12)
                                .clickable(true);
                        final int[] index = {0};
                        final android.os.Handler handler = new android.os.Handler();

                        Runnable drawSegment = new Runnable() {
                            @Override
                            public void run() {
                                if (index[0] < path.size()) {
                                    options.add(path.get(index[0]));
                                    mMap.addPolyline(options);
                                    index[0]++;
                                    handler.postDelayed(this, 30);  // 控制绘制速度
                                }
                            }
                        };
                        drawSegment.run();

                        LatLng midPoint = path.get(path.size() / 2);
                        boolean isFastest = (i == fastestIndex);
                        String label = isFastest ? "✅FAST " + option.duration : option.duration;
                        Bitmap bitmap = createRouteLabelBitmap(label, isFastest);

                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(midPoint)
                                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                                .anchor(0.5f, 1f)
                                .zIndex(100));

                        if (marker != null) {
                            final int selectedIndex = i;
                            marker.setTag(selectedIndex);
                            mMap.setOnMarkerClickListener(clicked -> {
                                Object tag = clicked.getTag();
                                if (tag instanceof Integer) {
                                    int idx = (Integer) tag;
                                    JSONObject selectedRoute = routes.optJSONObject(idx);
                                    if (selectedRoute != null) {
                                        List<RouteStep> selectedSteps = parseStepsFromRoute(selectedRoute);
                                        currentSteps = selectedSteps;
                                        currentStepIndex = 0;
                                        generateStepByStepInstructions(selectedSteps);
                                        Toast.makeText(this, "✅ Route " + (idx + 1) + " selected", Toast.LENGTH_SHORT).show();
                                        container.setVisibility(View.GONE);
                                    }
                                    return true;
                                }
                                return false;
                            });
                        }

                        Button pathBtn = new Button(this);
                        pathBtn.setMaxWidth(400);  // 限制最大宽度
                        pathBtn.setSingleLine(true);  // 避免换行撑大

                        pathBtn.setText("Path " + (i + 1) + ": " + option.duration + (isFastest ? " ✅FAST" : ""));
                        pathBtn.setBackgroundColor(option.color);
                        pathBtn.setTextColor(Color.WHITE);
                        pathBtn.setPadding(20, 10, 20, 10);
                        pathBtn.setTag(i);

                        pathBtn.setOnClickListener(v -> {
                            int idx = (int) v.getTag();
                            JSONObject selectedRoute = routes.optJSONObject(idx);
                            if (selectedRoute != null) {
                                List<RouteStep> selectedSteps = parseStepsFromRoute(selectedRoute);
                                currentSteps = selectedSteps;
                                currentStepIndex = 0;
                                TextView instructionView = findViewById(R.id.instructionView);
                                instructionView.setText("🛣 Navigating Path " + (idx + 1) + "...\n\nGenerating instructions...");
                                generateStepByStepInstructions(selectedSteps);
                                container.setVisibility(View.GONE);

                                // ✅ 清除所有中间 label marker（避免地图遗留蓝绿标签）
                                for (Marker m : analyzeRouteMarkers) {
                                    if (m != null) m.remove();
                                }
                                analyzeRouteMarkers.clear();
                            }
                        });

                        container.addView(pathBtn);
                    }

                    // ✅ 自动缩放视角到所有路径区域
                    if (!allPoints.isEmpty()) {
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        for (LatLng point : allPoints) builder.include(point);
                        LatLngBounds bounds = builder.build();
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120));
                    }
                });

            } catch (Exception e) {
                Log.e("DRAW_ROUTE", "❌ Failed to draw route", e);
            }
        }).start();

        MyAINavigation.generatePathsUsingAI(origin, destination, MyAINavigation.Mode.BLIND, new MyAINavigation.PathCallback() {
            @Override
            public void onPathsReady(List<List<LatLng>> aiPaths) {
                runOnUiThread(() -> {
                    drawPaths(aiPaths);
                });
            }
        });

    }

    private Bitmap createRouteLabelBitmap(String text, boolean isFastest) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(34);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(isFastest ? Color.parseColor("#388E3C") : Color.BLUE);

        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        int padding = 14;
        int width = bounds.width() + padding * 2;
        int height = bounds.height() + padding * 2;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawRoundRect(new RectF(0, 0, width, height), 20, 20, bgPaint);
        canvas.drawText(text, width / 2f, height / 2f + bounds.height() / 2f - 6, paint);
        return bitmap;
    }


    private Bitmap createAccessibleRouteLabel(String duration, int score, boolean isAccessible, boolean isBest, String userType) {
        String emoji = userType.equals("blind") ? "🧑‍🦯‍➡️" : "🧑🏼‍🦽‍➡️";
        String text = (isAccessible ? "✅" : "❌") + (isBest ? "⚜️" : "") + duration + " " + emoji + score;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(34);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(isAccessible ? Color.parseColor("#1565C0") : Color.GRAY);

        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        int padding = 14;
        int width = bounds.width() + padding * 2;
        int height = bounds.height() + padding * 2;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawRoundRect(new RectF(0, 0, width, height), 20, 20, bgPaint);
        canvas.drawText(text, width / 2f, height / 2f + bounds.height() / 2f - 6, paint);
        return bitmap;
    }

    private void drawPaths(List<List<LatLng>> paths) {
        int[] colors = {
                Color.parseColor("#00BA68C8"),
                Color.parseColor("#00BA68C8"),
                Color.parseColor("#00BA68C8")
        };

        for (int i = 0; i < paths.size(); i++) {
            List<LatLng> path = paths.get(i);
            if (path == null || path.size() < 2) continue;

            PolylineOptions polyOptions = new PolylineOptions()
                    .color(colors[i % colors.length])
                    .width(12)
                    .clickable(false)
                    .zIndex(80 + i);

            for (LatLng pt : path) polyOptions.add(pt);

            mMap.addPolyline(polyOptions);

            mMap.addMarker(new MarkerOptions()
                    .anchor(0.5f, 1.0f)
                    .zIndex(90 + i));
        }
    }

    private void reloadOSMMarkers() {
        OSMLoader osmLoader = new OSMLoader(this);
        osmLoader.fetchFromOverpass(top100 -> {
            for (OSMLoader.OSMNode node : top100) {
                LatLng pos = new LatLng(node.lat, node.lon);
                mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(node.name)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        .anchor(0.5f, 1.0f));
            }
        }, error -> {
            Log.e("OSM_RELOAD", "❌ Failed to reload OSM POIs", error);
        });
    }


    private int calculateAccessibilityScore(DocumentSnapshot doc, String userType) {
        int score = 0;

        String tactile = doc.getString("Tactile Paving");
        String slope = doc.getString("Steep Slope");
        String wheelchair = doc.getString("Wheelchair");
        String elevator = doc.getString("Elevator");

        if ("wheelchair".equals(userType)) {
            if ("Yes".equalsIgnoreCase(wheelchair)) score += 1;
            else return 0; // ❌直接不可通行

            if ("Yes".equalsIgnoreCase(tactile)) score += 1;
            else score -= 1;

            if ("No".equalsIgnoreCase(slope)) score += 1;
            else if ("Yes".equalsIgnoreCase(slope)) score -= 1;

            if ("Yes".equalsIgnoreCase(elevator)) score += 1;
            else if ("No".equalsIgnoreCase(elevator)) score -= 1;

        } else if ("blind".equals(userType)) {
            if ("Yes".equalsIgnoreCase(tactile)) score += 3;
            else return 0;

            if ("Yes".equalsIgnoreCase(wheelchair)) score += 1;
            else return 0;

            if ("No".equalsIgnoreCase(slope)) score += 1;
            else if ("Yes".equalsIgnoreCase(slope)) score -= 1;

            if ("Yes".equalsIgnoreCase(elevator)) score += 1;
            else if ("No".equalsIgnoreCase(elevator)) score -= 1;
        }

        return Math.max(score, 0);
    }

    /**
     * This gets called once the map is fully loaded and ready.
     * I use this to draw all markers: houses, POIs, custom icons, etc.
     * It also enables camera movement and click listeners for route drawing and POI analysis.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        try {
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasValidMarker = false;

        for (House h : list) {
            try {
                double lat = Double.parseDouble(h.getLat());
                double lng = Double.parseDouble(h.getLng());
                LatLng latLng = new LatLng(lat, lng);

                float hue = "rented".equalsIgnoreCase(h.getStatus()) ?
                        BitmapDescriptorFactory.HUE_VIOLET : BitmapDescriptorFactory.HUE_RED;

                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(h.getTitle())
                        .icon(BitmapDescriptorFactory.defaultMarker(hue))
                        .anchor(0.5f, 1.0f));

                if (marker != null) {
                    marker.setTag(h);  // 绑定整个 House 对象以便双击时使用位置
                    boundsBuilder.include(latLng);
                    hasValidMarker = true;
                }
            } catch (Exception ignored) {}
        }

        final Marker[] lastClickedMarker = {null};
        final long[] clickTimes = {0, 0, 0}; // 记录最近 3 次点击时间

        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();

            if (tag instanceof House) {
                House house = (House) tag;

                if (isNavigateMode) {
                    LatLng origin = new LatLng(Double.parseDouble(house.getLat()), Double.parseDouble(house.getLng()));
                    showRouteToNearbyLandmarks(origin);
                } else {
                    Intent intent = new Intent(Map.this, Detail.class);
                    intent.putExtra("id", house.getId() + "");
                    startActivity(intent);

                    // 🧠 插入 Analyze 标签
                    try {
                        LatLng origin = new LatLng(Double.parseDouble(house.getLat()), Double.parseDouble(house.getLng()));
                        Marker analyzeBtnMarker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(origin.latitude + 0.0004, origin.longitude))
                                .title("🧠 Analyze")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                                .anchor(0.5f, 1.0f));
                        if (analyzeBtnMarker != null) {
                            analyzeBtnMarker.setTag("analyze_" + house.getLat() + "_" + house.getLng());
                        }
                    } catch (Exception e) {
                        Log.e("ANALYZE_MARKER", "❌ Failed to insert analyze marker", e);
                    }
                }

                return true;
            }

            if (tag instanceof String && ((String) tag).startsWith("analyze_")) {
                try {
                    String[] parts = ((String) tag).split("_");
                    double lat = Double.parseDouble(parts[1]);
                    double lng = Double.parseDouble(parts[2]);
                    LatLng origin = new LatLng(lat, lng);
                    Toast.makeText(Map.this, "🧠 Analyzing nearby places...", Toast.LENGTH_SHORT).show();

                    String[] keywords = new String[]{
                            "restaurant", "grocery", "airport", "bank", "hospital", "school"
                    };
                    int[] colors = new int[]{
                            Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.CYAN, Color.YELLOW
                    };

                    for (int i = 0; i < keywords.length; i++) {
                        final int idx = i;
                        new android.os.Handler().postDelayed(() -> {
                            findNearbyPlace(keywords[idx], origin, placeLatLng -> {
                                if (placeLatLng != null) {
                                    drawRoute(origin, placeLatLng, colors[idx]);
                                }
                            });
                        }, i * 1500);
                    }

                } catch (Exception e) {
                    Log.e("ANALYZE_CLICK", "❌ Failed to analyze marker", e);
                }
                return true;
            }

            return false;
        });


        try {
            InputStream is = getAssets().open("manhattan_pois.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONArray poiArray = new JSONArray(sb.toString());

            for (int i = 0; i < poiArray.length(); i++) {
                JSONObject obj = poiArray.getJSONObject(i);
                String name = obj.optString("name", "Unnamed");
                double lat = obj.getDouble("lat");
                double lon = obj.getDouble("lon");
                String type = obj.optString("type", "other");

                float color;
                switch (type) {
                    case "restaurant": color = BitmapDescriptorFactory.HUE_ORANGE; break;
                    case "gym": color = BitmapDescriptorFactory.HUE_GREEN; break;
                    case "cafe": color = BitmapDescriptorFactory.HUE_BLUE; break;
                    case "pharmacy": color = BitmapDescriptorFactory.HUE_RED; break;
                    case "grocery": color = BitmapDescriptorFactory.HUE_YELLOW; break;
                    case "bookstore": color = BitmapDescriptorFactory.HUE_VIOLET; break;
                    case "bakery": color = BitmapDescriptorFactory.HUE_MAGENTA; break;
                    case "bank": color = BitmapDescriptorFactory.HUE_ROSE; break;
                    default: color = BitmapDescriptorFactory.HUE_CYAN;
                }

                LatLng latLng = new LatLng(lat, lon);
                mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(name)
                        .icon(BitmapDescriptorFactory.defaultMarker(color))
                        .anchor(0.5f, 1.0f));
            }

        } catch (Exception e) {
            Log.e("MAP_POI", "❌ Error loading POIs", e);
        }


        LatLng[] buildingPositions = new LatLng[]{
                new LatLng(39.50973, -84.73814),
                new LatLng(39.50785, -84.73510),
                new LatLng(39.50717, -84.73000),
                new LatLng(39.50815, -84.72601),
                new LatLng(39.50518, -84.73324)
        };
        String[] buildingNames = new String[]{
                "McVey Data Science Building",
                "Benton Hall",
                "Bachelor Hall",
                "Farmer Business School",
                "Art Building"
        };
        for (int i = 0; i < buildingPositions.length; i++) {
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(buildingPositions[i])
                    .title(buildingNames[i])
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .anchor(0.5f, 1.0f));
            if (marker != null) boundsBuilder.include(buildingPositions[i]);
        }

        if (hasValidMarker || buildingPositions.length > 0) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120));
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(39.5073, -84.7461), 12));
        }

        reloadMapMarkers();
        loadBusinessSitesFromFirestore();

        if (savedZoomLevel == 0f && mMap != null) {
            savedZoomLevel = mMap.getCameraPosition().zoom;
            savedCenterLatLng = mMap.getCameraPosition().target;
        }

    }

    private void loadBusinessSitesFromFirestore() {
        if (mMap == null) {
            Log.e("BUSINESS_FIRESTORE", "❌ Map is not ready, aborting business site loading.");
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("Business Site_csv")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.i("BUSINESS_FIRESTORE", "✅ Total business documents: " + queryDocumentSnapshots.size());
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        try {
                            String name = doc.getString("Business Name");
                            String type = doc.getString("Business Type");
                            Double lat = doc.getDouble("Latitude");
                            Double lon = doc.getDouble("Longitude");

                            if (name == null || type == null || lat == null || lon == null) {
                                Log.w("BUSINESS_FIRESTORE", "⚠️ Skipping invalid document: " + doc.getId());
                                continue;
                            }

                            LatLng position = new LatLng(lat, lon);
                            String emoji = getEmojiForType(type);
                            Bitmap icon = createEmojiMarkerIcon(emoji);

                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(name)
                                    .icon(BitmapDescriptorFactory.fromBitmap(icon)));
                            if (marker != null) marker.setTag(doc.getId());

                            LatLng textLabelPos = new LatLng(lat - 0.0001, lon);
                            mMap.addMarker(new MarkerOptions()
                                    .position(textLabelPos)
                                    .icon(BitmapDescriptorFactory.fromBitmap(createTextLabelBitmap(name)))
                                    .anchor(0.5f, 0f));

                            Log.d("BUSINESS_FIRESTORE", "✅ Added business: " + name);

                        } catch (Exception e) {
                            Log.e("BUSINESS_FIRESTORE", "❌ Error drawing marker", e);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("BUSINESS_FIRESTORE", "❌ Failed to load business sites", e);
                });

        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof String) {
                Intent intent = new Intent(Map.this, BusinessDetail.class);
                intent.putExtra("business_id", (String) tag);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    // Emoji mapping for business types
    private String getEmojiForType(String type) {
        switch (type.toLowerCase()) {
            case "jewelry":
                return "💎";
            case "retail":
            case "retail store":
                return "🏪";
            case "restaurant":
                return "🍽️";
            case "museum":
                return "🏛️";
            case "bookstore":
                return "📚";
            case "cafe":
            case "bakery":
                return "☕";
            case "grocery":
            case "grocery store":
                return "🛒";
            case "hotel":
                return "🏨";
            case "bar":
                return "🍺";
            case "art gallery":
                return "🖼️";
            case "winery":
                return "🍷";
            case "venue":
                return "🎭";
            case "supermarket":
                return "🛍️";
            case "seafood market":
                return "🦞";
            case "delicatessen":
                return "🥪";
            default:
                return "📍";
        }
    }

    // Create marker icon with emoji + circular background
    private Bitmap createEmojiMarkerIcon(String emoji) {
        int size = 120;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#1565C0")); // Deep blue
        bgPaint.setAntiAlias(true);
        canvas.drawCircle(size / 2f, size / 2f, size / 2.2f, bgPaint);

        Paint textPaint = new Paint();
        textPaint.setTextSize(60);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setAntiAlias(true);

        Rect bounds = new Rect();
        textPaint.getTextBounds(emoji, 0, emoji.length(), bounds);
        canvas.drawText(emoji, size / 2f, size / 2f + bounds.height() / 2f, textPaint);

        return bitmap;
    }

    private Bitmap createTextLabelBitmap(String text) {
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(36);
        textPaint.setColor(Color.BLACK);
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);

        int padding = 20;
        int width = bounds.width() + padding * 2;
        int height = bounds.height() + padding * 2;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.argb(200, 255, 255, 255)); // 半透明
        canvas.drawRoundRect(new RectF(0, 0, width, height), 20, 20, bgPaint);

        float x = width / 2f;
        float y = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(text, x, y, textPaint);

        return bitmap;
    }

    @Override
    protected void onResume() {
        super.onResume();
        new FirebaseMerge(this).fetchApprovedHousesFromFirebase();
        list = databaseHelper.getAllHousesIncludeUnchecked();
        reloadMapMarkers();

        if (mMap != null && savedCenterLatLng != null && savedZoomLevel != 0f) {
            Log.d("MAP_RESUME", "✅ Restoring camera to previous position");
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(savedCenterLatLng, savedZoomLevel));
        } else {
            Log.w("MAP_RESUME", "⚠️ Camera position not recorded yet");
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem menuItem = menu.findItem(R.id.action_col);
        MenuItem menuItem1 = menu.findItem(R.id.action_apply);
        MenuItem menuItem2 = menu.findItem(R.id.action_manage);
        MenuItem menuItem3 = menu.findItem(R.id.action_my);
        if(role.equals("landlord")){
            menuItem.setVisible(false);
            menuItem1.setVisible(true);
            menuItem2.setVisible(true);
            menuItem3.setVisible(true);
        }else if(role.equals("tenant")){
            menuItem.setVisible(true);
            menuItem1.setVisible(false);
            menuItem2.setVisible(false);
            menuItem3.setVisible(true);
        }else if(role.equals("household")){
            menuItem.setVisible(true);
            menuItem1.setVisible(false);
            menuItem2.setVisible(true);
            menuItem3.setVisible(true);
        }
        else{
            menuItem.setVisible(false);
            menuItem1.setVisible(false);
            menuItem2.setVisible(false);
            menuItem3.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_manage){
            //跳转到房源管理页面
            Intent intent = new Intent(Map.this, Manage.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.action_apply){
            //跳转到维修管理页面
            Intent intent = new Intent(Map.this, MaintenanceManage.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.action_col){
            //跳转到收藏夹页面
            Intent intent = new Intent(Map.this, Collect.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.action_out){
            //退出系统
            //清空存储信息
            SharedPreferences sp = getSharedPreferences("userinfo", 0);
            SharedPreferences.Editor editor = sp.edit();
            editor.clear();
            editor.commit();
            startActivity(new Intent(Map.this, Login.class));
        } else if (item.getItemId() == R.id.action_my) {
            //跳转到个人中心页面
            Intent intent = new Intent(Map.this, MyCenter.class);
            startActivity(intent);
        }
        return true;
    }

    private void showPopupWindow(View _view) {
        // 创建 PopupWindow 的内容视图
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.search_popup_layout, null);

        // 创建 PopupWindow
        popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        // 设置 PopupWindow 的背景
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        // 显示 PopupWindow
        popupWindow.showAsDropDown(_view, 0, 0);
        //设置点击外部消失
        popupWindow.setOutsideTouchable(true);
        et_title = popupView.findViewById(R.id.et_title);
        et_minPrice = popupView.findViewById(R.id.et_minPrice);
        et_maxPrice = popupView.findViewById(R.id.et_maxPrice);
        et_minArea = popupView.findViewById(R.id.et_minArea);
        et_maxArea = popupView.findViewById(R.id.et_maxArea);
        sp_bedrooms = popupView.findViewById(R.id.sp_bedrooms);
        sp_bedrooms.setPrompt("Choose bedrooms");
        sp_bedrooms.setOnItemSelectedListener(new MySelectedListener());
        ArrayAdapter<String> starAdapter = new ArrayAdapter<String>(this,R.layout.item_select,arr);
        starAdapter.setDropDownViewResource(R.layout.item_dropdown);
        sp_bedrooms.setAdapter(starAdapter);
        sp_bathrooms = popupView.findViewById(R.id.sp_bathrooms);
        sp_bathrooms.setPrompt("Choose bedrooms");
        sp_bathrooms.setOnItemSelectedListener(new MySelectedListener1());
        ArrayAdapter<String> starAdapter1 = new ArrayAdapter<String>(this,R.layout.item_select,arr1);
        starAdapter1.setDropDownViewResource(R.layout.item_dropdown);
        sp_bathrooms.setAdapter(starAdapter1);
        Button queryBtn = popupView.findViewById(R.id.queryBtn);

        Button resetBtn = popupView.findViewById(R.id.resetBtn);
        resetBtn.setOnClickListener(v -> {
            isSearchMode = false;
            searchResultList.clear();
            popupWindow.dismiss();
            reloadMapMarkers();
        });


        queryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toQuery();
            }
        });
    }

    class MySelectedListener implements AdapterView.OnItemSelectedListener{
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            sel_bed=arr[i];
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }

    class MySelectedListener1 implements AdapterView.OnItemSelectedListener{
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            sel_bath=arr1[i];
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }

    private void toQuery() {
        String title = et_title.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a place name in Title", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ 使用 Google Places API 查询该地名，并跳转地图
        new Thread(() -> {
            try {
                String apiKey = "AIzaSyAtUw9mwOj2mY6vADIwtyLR5QDRdlWe0RU";
                String encoded = java.net.URLEncoder.encode(title, "UTF-8");
                String urlStr = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=" + encoded + "&key=" + apiKey;

                Log.d("PLACES_SEARCH", "🔍 Searching for: " + title);
                Log.d("PLACES_SEARCH", "➡️ URL: " + urlStr);

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                JSONArray results = json.getJSONArray("results");

                if (results.length() > 0) {
                    JSONObject loc = results.getJSONObject(0).getJSONObject("geometry").getJSONObject("location");
                    double lat = loc.getDouble("lat");
                    double lng = loc.getDouble("lng");
                    LatLng placeLatLng = new LatLng(lat, lng);

                    Log.d("PLACES_SEARCH", "✅ Found: " + lat + "," + lng);

                    runOnUiThread(() -> {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(placeLatLng, 16));
                        mMap.addMarker(new MarkerOptions().position(placeLatLng).title("📍 " + title));
                        Toast.makeText(Map.this, "✅ Found place: " + title, Toast.LENGTH_SHORT).show();
                        popupWindow.dismiss();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(Map.this, "❌ Place not found", Toast.LENGTH_SHORT).show());
                    Log.e("PLACES_SEARCH", "❌ No results for " + title);
                }

            } catch (Exception e) {
                Log.e("PLACES_SEARCH", "❌ Error during place search", e);
                runOnUiThread(() -> Toast.makeText(Map.this, "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }


    private void reloadMapMarkers() {
        if (mMap == null) return;

        mMap.clear();
        reloadOSMMarkers();

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasValidMarker = false;

        List<House> allHouses = list;

        for (House h : allHouses) {
            try {
                double lat = Double.parseDouble(h.getLat());
                double lng = Double.parseDouble(h.getLng());
                LatLng latLng = new LatLng(lat, lng);

                boolean isMatch = false;
                if (isSearchMode) {
                    for (House match : searchResultList) {
                        if (match.getId() == h.getId()) {
                            isMatch = true;
                            break;
                        }
                    }
                }

                SharedPreferences sp = getSharedPreferences("userinfo", 0);
                String currentUid = sp.getString("uid", "");

                float hue;
                if ("rented".equalsIgnoreCase(h.getStatus())) {
                    if (currentUid.equals(h.getTenantUid())) {
                        hue = BitmapDescriptorFactory.HUE_GREEN;
                    } else {
                        hue = BitmapDescriptorFactory.HUE_VIOLET;
                    }
                } else if (isMatch) {
                    hue = BitmapDescriptorFactory.HUE_YELLOW;
                } else {
                    hue = BitmapDescriptorFactory.HUE_RED;
                }

                Marker houseMarker = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .icon(BitmapDescriptorFactory.defaultMarker(hue))
                        .anchor(0.5f, 1.0f));
                if (houseMarker != null) {
                    houseMarker.setTag(h);
                    boundsBuilder.include(latLng);
                    hasValidMarker = true;
                }

                LatLng labelLatLng = new LatLng(lat + 0.00025, lng);
                mMap.addMarker(new MarkerOptions()
                        .position(labelLatLng)
                        .icon(BitmapDescriptorFactory.fromBitmap(createLabelBitmap(h.getTitle())))
                        .anchor(0.5f, 1.0f)
                        .zIndex(999));

                LatLng analyzeLatLng = new LatLng(lat + 0.0005, lng);
                Marker analyzeMarker = mMap.addMarker(new MarkerOptions()
                        .position(analyzeLatLng)
                        .icon(BitmapDescriptorFactory.fromBitmap(createAnalyzeLabelBitmap()))
                        .anchor(0.5f, 1.0f)
                        .zIndex(1000));
                if (analyzeMarker != null) {
                    analyzeMarker.setTag("analyze_" + h.getLat() + "_" + h.getLng());
                }


                if (analyzeMarker != null) {
                    analyzeMarker.setTag("analyze_" + h.getLat() + "_" + h.getLng());
                }

            } catch (Exception e) {
                Log.e("RELOAD_MARKER", "❌ Failed to reload marker", e);
            }
        }

        if (hasValidMarker) {
            if (savedZoomLevel == 0f || savedCenterLatLng == null) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120));
            } else {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(savedCenterLatLng, savedZoomLevel));
            }
        }

        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag == null) return false;

            if (tag instanceof House) {
                House h = (House) tag;
                Intent intent = new Intent(Map.this, Detail.class);
                intent.putExtra("id", h.getId() + "");
                startActivity(intent);
                return true;
            } else if (tag instanceof String && tag.toString().startsWith("analyze_")) {
                try {
                    String[] parts = tag.toString().split("_");
                    double lat = Double.parseDouble(parts[1]);
                    double lng = Double.parseDouble(parts[2]);
                    LatLng origin = new LatLng(lat, lng);
                    Toast.makeText(this, "🧠 Analyzing nearby POIs...", Toast.LENGTH_SHORT).show();

                    String[] keywords = {"restaurant", "grocery", "airport", "bank", "hospital", "school"};
                    int[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.CYAN, Color.YELLOW};

                    for (int i = 0; i < keywords.length; i++) {
                        final int idx = i;
                        new android.os.Handler().postDelayed(() -> {
                            findNearbyPlace(keywords[idx], origin, placeLatLng -> {
                                if (placeLatLng != null) {
                                    drawRoute(origin, placeLatLng, colors[idx]);
                                }
                            });
                        }, i * 1500);  // 每1.5秒请求一次，避免 API 冲突
                    }

                } catch (Exception e) {
                    Log.e("ANALYZE_MARKER", "❌ Failed to parse analyze tag", e);
                }
                return true;
            }

            return false;
        });
    }

    private Bitmap createAnalyzeLabelBitmap() {
        String text = "🧠 Analyze";

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(44);
        textPaint.setColor(Color.BLACK);
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);

        int padding = 24;
        int width = bounds.width() + padding * 2;
        int height = bounds.height() + padding * 2;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.argb(220, 255, 255, 255)); // 220 透明度
        canvas.drawRoundRect(new RectF(0, 0, width, height), 20, 20, bgPaint);

        canvas.drawText(text, width / 2f, height / 2f + bounds.height() / 2f - 8, textPaint);
        return bitmap;
    }



    private Bitmap createLabelBitmap(String text) {
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(34);
        textPaint.setColor(Color.BLACK);
        textPaint.setFakeBoldText(true);

        Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);

        int padding = 12;
        int width = bounds.width() + padding * 2;
        int height = bounds.height() + padding * 2;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.argb(150, 255, 255, 255));
        canvas.drawRoundRect(new RectF(0, 0, width, height), 20, 20, bgPaint);

        float x = padding;
        float y = padding - bounds.top;
        canvas.drawText(text, x, y, textPaint);

        return bitmap;
    }

    private void searchZipAndMoveCamera(String zip) {
        new Thread(() -> {
            try {
                String apiKey = "AIzaSyAtUw9mwOj2mY6vADIwtyLR5QDRdlWe0RU"; // ✅ 使用你的 Google Maps API Key
                String urlStr = "https://maps.googleapis.com/maps/api/geocode/json?address=" + zip + "&key=" + apiKey;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    result.append(line);
                }
                conn.disconnect();

                JSONObject json = new JSONObject(result.toString());
                JSONArray results = json.getJSONArray("results");
                if (results.length() > 0) {
                    JSONObject location = results.getJSONObject(0)
                            .getJSONObject("geometry")
                            .getJSONObject("location");
                    double lat = location.getDouble("lat");
                    double lng = location.getDouble("lng");

                    runOnUiThread(() -> {
                        LatLng latLng = new LatLng(lat, lng);
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(Map.this, "Location not found for ZIP", Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(Map.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /**
     * After clicking a house marker, this draws paths to nearby important places like Walmart and schools.
     * Each destination shows a time label and a marker showing its type.
     */
    private void showRouteToNearbyLandmarks(LatLng houseLocation) {

        if (analyzeRouteMarkers != null) {
            for (Marker m : analyzeRouteMarkers) m.remove();
            analyzeRouteMarkers.clear();
        }
        if (analyzePolylines != null) {
            for (Polyline p : analyzePolylines) p.remove();
            analyzePolylines.clear();
        }

        findNearbyPlace("Walmart", houseLocation, placeLatLng -> {
            if (placeLatLng != null) {
                drawRoute(houseLocation, placeLatLng, Color.BLUE);
            }
        });

        findNearbyPlace("Donald Bren School of Information and Computer Sciences", houseLocation, placeLatLng -> {
            if (placeLatLng != null) {
                drawRoute(houseLocation, placeLatLng, Color.GREEN);
            }
        });
    }


    private void findNearbyPlace(String keyword, LatLng origin, OnPlaceFoundListener callback) {
        new Thread(() -> {
            try {
                String apiKey = "AIzaSyAtUw9mwOj2mY6vADIwtyLR5QDRdlWe0RU";
                String urlStr = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" +
                        origin.latitude + "," + origin.longitude + "&radius=5000&keyword=" +
                        java.net.URLEncoder.encode(keyword, "UTF-8") + "&key=" + apiKey;

                Log.d("PLACES_API", "Requesting: " + urlStr);  // ✅ 打印请求地址

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                Log.d("PLACES_API", "Response: " + json.toString());  // ✅ 打印原始响应

                JSONArray results = json.getJSONArray("results");
                if (results.length() > 0) {
                    JSONObject loc = results.getJSONObject(0).getJSONObject("geometry").getJSONObject("location");
                    LatLng latLng = new LatLng(loc.getDouble("lat"), loc.getDouble("lng"));
                    Log.d("PLACES_API", "Found place for " + keyword + " at: " + latLng);  // ✅ 找到地址
                    runOnUiThread(() -> callback.onFound(latLng));
                } else {
                    Log.e("PLACES_API", "No results found for: " + keyword);  // ✅ 没找到
                }
            } catch (Exception e) {
                Log.e("PLACES_API", "❌ Error while fetching place for keyword: " + keyword, e);
            }
        }).start();
    }


    /**
     * Given a start and end point, this draws one walking route using Google Directions API.
     * It also drops two extra markers: one with duration label, and one for destination type (e.g. Grocery).
     */
    private void drawRoute(LatLng origin, LatLng destination, int color) {
        new Thread(() -> {
            try {
                String apiKey = "AIzaSyAtUw9mwOj2mY6vADIwtyLR5QDRdlWe0RU";
                String urlStr = "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                        origin.latitude + "," + origin.longitude +
                        "&destination=" + destination.latitude + "," + destination.longitude +
                        "&mode=walking&key=" + apiKey;

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                JSONArray routes = json.getJSONArray("routes");
                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    String polyline = route.getJSONObject("overview_polyline").getString("points");
                    List<LatLng> path = decodePoly(polyline);

                    JSONObject leg = route.getJSONArray("legs").getJSONObject(0);
                    String duration = leg.getJSONObject("duration").getString("text");
                    String endAddress = leg.optString("end_address", "Destination");

                    LatLng endPoint = path.get(path.size() - 1);
                    LatLng midPoint = path.get(path.size() / 2);

                    String typeLabel = "📍POI";
                    String addrLower = endAddress.toLowerCase();
                    if (addrLower.contains("hospital")) typeLabel = "🏥Hospital";
                    else if (addrLower.contains("bank")) typeLabel = "🏦Bank";
                    else if (addrLower.contains("grocery")) typeLabel = "🛒Grocery";
                    else if (addrLower.contains("school") || addrLower.contains("university")) typeLabel = "🎓School";
                    else if (addrLower.contains("airport")) typeLabel = "✈️Airport";
                    else if (addrLower.contains("restaurant")) typeLabel = "🍽Restaurant";
                    else if (addrLower.contains("pharmacy")) typeLabel = "💊Pharmacy";

                    final String labelText = typeLabel;

                    runOnUiThread(() -> {
                        com.google.android.gms.maps.model.Polyline polylineObj = mMap.addPolyline(new PolylineOptions()
                                .addAll(path)
                                .width(10)
                                .color(color));
                        analyzePolylines.add(polylineObj);

                        Bitmap timeBitmap = createRouteLabelBitmap(duration, false);
                        Marker timeMarker = mMap.addMarker(new MarkerOptions()
                                .position(midPoint)
                                .icon(BitmapDescriptorFactory.fromBitmap(timeBitmap))
                                .anchor(0.5f, 1f)
                                .zIndex(999));
                        analyzeRouteMarkers.add(timeMarker);

                        Bitmap destBitmap = createColoredLabel(labelText, color);
                        Marker typeMarker = mMap.addMarker(new MarkerOptions()
                                .position(endPoint)
                                .icon(BitmapDescriptorFactory.fromBitmap(destBitmap))
                                .anchor(0.5f, 1f)
                                .zIndex(999));
                        analyzeRouteMarkers.add(typeMarker);
                    });

                } else {
                    Log.e("DIRECTIONS_API", "❌ No route found");
                }

            } catch (Exception e) {
                Log.e("DIRECTIONS_API", "❌ Error drawing route", e);
            }
        }).start();
    }


    private Bitmap createColoredLabel(String text, int color) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(34);
        paint.setColor(Color.WHITE);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(color);

        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        int padding = 14;
        int width = bounds.width() + padding * 2;
        int height = bounds.height() + padding * 2;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawRoundRect(new RectF(0, 0, width, height), 20, 20, bgPaint);
        canvas.drawText(text, width / 2f, height / 2f + bounds.height() / 2f - 6, paint);
        return bitmap;
    }


    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length(), lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            poly.add(new LatLng((lat / 1E5), (lng / 1E5)));
        }

        return poly;
    }

    private interface OnPlaceFoundListener {
        void onFound(LatLng placeLatLng);
    }

    private static class OSMNode {
        public double lat;
        public double lon;
        public String wheelchair = "";
        public String tactilePaving = "";
        public String incline = "";
        public String kerb = "";
        public String surface = "";
        public String crossing = "";
    }

    private List<OSMNode> fetchNearbyAccessibilityFeatures(List<LatLng> path) {
        List<OSMNode> nearby = new ArrayList<>();
        FirebaseFirestore.getInstance().collection("Manhatton_Business_Site")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Log.d("ACCESS_FETCH", "✅ Loaded POIs from Firebase: " + snapshot.size());
                    for (LatLng point : path) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            double lat = doc.getDouble("lat");
                            double lon = doc.getDouble("lon");

                            float[] result = new float[1];
                            android.location.Location.distanceBetween(
                                    point.latitude, point.longitude,
                                    lat, lon, result
                            );
                            float distance = result[0];
                            if (distance <= 50) {  // 50 米以内视为相关点
                                OSMNode node = new OSMNode();
                                node.lat = lat;
                                node.lon = lon;
                                node.wheelchair = doc.getString("wheelchair");
                                node.kerb = doc.getString("kerb");
                                node.incline = doc.getString("incline");
                                node.tactilePaving = doc.getString("tactile_paving");
                                node.crossing = doc.getString("crossing");
                                node.surface = doc.getString("surface");
                                nearby.add(node);

                                Log.d("ACCESS_MATCH", "🟦 Matched POI near path: "
                                        + String.format("(%.5f,%.5f) wheelchair=%s incline=%s tactile=%s",
                                        lat, lon, node.wheelchair, node.incline, node.tactilePaving));
                            }
                        }
                    }
                    Log.d("ACCESS_DONE", "🧮 Found " + nearby.size() + " accessibility POIs near path.");
                })
                .addOnFailureListener(e -> {
                    Log.e("ACCESS_FETCH", "❌ Failed to load POIs", e);
                });
        return nearby;
    }

    private int scorePathByAccessibility(List<LatLng> path, String userType) {
        AtomicInteger totalScore = new AtomicInteger(0);

        FirebaseFirestore.getInstance().collection("Manhatton_Business_Site")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Log.d("SCORE_PATH", "✅ Loaded POIs for scoring");

                    for (LatLng point : path) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            double lat = doc.getDouble("lat");
                            double lon = doc.getDouble("lon");

                            float[] result = new float[1];
                            android.location.Location.distanceBetween(
                                    point.latitude, point.longitude, lat, lon, result);
                            if (result[0] <= 50) {
                                String wheelchair = doc.getString("wheelchair");
                                String tactile = doc.getString("tactile_paving");
                                String inclineStr = doc.getString("incline");
                                String kerb = doc.getString("kerb");

                                if ("wheelchair".equals(userType)) {
                                    if ("yes".equalsIgnoreCase(wheelchair)) totalScore.addAndGet(5);
                                    if ("lowered".equalsIgnoreCase(kerb)) totalScore.addAndGet(2);
                                    try {
                                        if (inclineStr != null && inclineStr.contains("%")) {
                                            double incline = Double.parseDouble(inclineStr.replace("%", "").trim());
                                            if (incline <= 5) totalScore.addAndGet(3);
                                            else if (incline <= 8) totalScore.addAndGet(1);
                                        }
                                    } catch (Exception ignored) {}
                                } else if ("blind".equals(userType)) {
                                    if ("yes".equalsIgnoreCase(tactile)) totalScore.addAndGet(5);
                                    if ("traffic_signals".equalsIgnoreCase(doc.getString("crossing"))) totalScore.addAndGet(2);
                                }
                            }
                        }
                    }

                    Log.d("SCORE_DONE", "🟦 Final score = " + totalScore.get() + " for userType=" + userType);
                })
                .addOnFailureListener(e -> {
                    Log.e("SCORE_PATH", "❌ Error loading POIs", e);
                });

        return totalScore.get();
    }

    /**
     * Builds special routes for either blind or wheelchair users.
     * Each route is scored based on nearby business POIs from Firebase,
     * and color-coded to show best vs. less accessible paths.
     */
    private void drawAccessibleRoutes(LatLng origin, LatLng destination, String userType) {
        String apiKey = "AIzaSyAtUw9mwOj2mY6vADIwtyLR5QDRdlWe0RU";
        String urlStr = "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&mode=walking&alternatives=true&key=" + apiKey;

        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                JSONArray routes = json.getJSONArray("routes");

                List<List<LatLng>> paths = new ArrayList<>();
                List<String> durations = new ArrayList<>();

                for (int i = 0; i < routes.length(); i++) {
                    JSONObject route = routes.getJSONObject(i);
                    String polyline = route.getJSONObject("overview_polyline").getString("points");
                    paths.add(decodePoly(polyline));

                    JSONObject leg = route.getJSONArray("legs").getJSONObject(0);
                    durations.add(leg.getJSONObject("duration").getString("text"));
                }

                runOnUiThread(() -> {
                    FirebaseFirestore.getInstance().collection("Business Site_csv")
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                int bestIndex = -1;
                                int bestScore = -1;

                                for (int i = 0; i < paths.size(); i++) {
                                    final List<LatLng> path = paths.get(i);
                                    final String duration = durations.get(i);

                                    int totalScore = 0;
                                    for (LatLng point : path) {
                                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                            Double lat = doc.getDouble("Latitude");
                                            Double lon = doc.getDouble("Longitude");
                                            if (lat != null && lon != null) {
                                                LatLng businessLoc = new LatLng(lat, lon);
                                                float[] result = new float[1];
                                                android.location.Location.distanceBetween(
                                                        point.latitude, point.longitude,
                                                        businessLoc.latitude, businessLoc.longitude,
                                                        result
                                                );
                                                if (result[0] < 30) {

                                                    int score = calculateAccessibilityScore(doc, userType);
                                                    totalScore += score;
                                                }
                                            }
                                        }
                                    }

                                    boolean isAccessible = totalScore > 0;
                                    boolean isBest = false;
                                    if (totalScore > bestScore) {
                                        bestScore = totalScore;
                                        bestIndex = i;
                                    }

                                    int finalScore = totalScore;
                                    boolean finalAccessible = isAccessible;
                                    int color = isAccessible ? Color.parseColor("#1565C0") : Color.GRAY;

                                    PolylineOptions options = new PolylineOptions()
                                            .addAll(path)
                                            .width(14)
                                            .color(color)
                                            .zIndex(isAccessible ? 99 : 0);
                                    mMap.addPolyline(options);

                                    LatLng midPoint = path.get(path.size() / 2);
                                    boolean isThisBest = (i == bestIndex);
                                    Bitmap label = createAccessibleRouteLabel(duration, finalScore, finalAccessible, isThisBest, userType);
                                    mMap.addMarker(new MarkerOptions()
                                            .position(midPoint)
                                            .icon(BitmapDescriptorFactory.fromBitmap(label))
                                            .anchor(0.5f, 1.0f)
                                            .zIndex(200));
                                }
                            });
                });

            } catch (Exception e) {
                Log.e("ACCESS_ROUTE", "❌ Failed to load directions", e);
            }
        }).start();
    }


    /**
     * Applies a scoring algorithm to each path based on accessibility fields like tactile paving and slope.
     * Then it draws them in different blue shades and labels them with accessibility score + estimated duration.
     */
    private void scoreAndDrawPaths(List<List<LatLng>> paths, String userType) {
        FirebaseFirestore.getInstance().collection("Manhatton_Business_Site").get()
                .addOnSuccessListener(snapshot -> {
                    List<Integer> scores = new ArrayList<>();
                    List<String> durations = new ArrayList<>();

                    for (List<LatLng> path : paths) {
                        int score = 0;
                        for (LatLng point : path) {
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                double lat = doc.getDouble("lat");
                                double lon = doc.getDouble("lon");
                                float[] result = new float[1];
                                android.location.Location.distanceBetween(point.latitude, point.longitude, lat, lon, result);
                                if (result[0] <= 50) {
                                    String wheelchair = doc.getString("wheelchair");
                                    String tactile = doc.getString("tactile_paving");
                                    String inclineStr = doc.getString("incline");
                                    String kerb = doc.getString("kerb");

                                    if ("wheelchair".equals(userType)) {
                                        if ("yes".equalsIgnoreCase(wheelchair)) score += 5;
                                        if ("lowered".equalsIgnoreCase(kerb)) score += 2;
                                        try {
                                            if (inclineStr != null && inclineStr.contains("%")) {
                                                double incline = Double.parseDouble(inclineStr.replace("%", "").trim());
                                                if (incline <= 5) score += 3;
                                                else if (incline <= 8) score += 1;
                                            }
                                        } catch (Exception ignored) {}
                                    } else if ("blind".equals(userType)) {
                                        if ("yes".equalsIgnoreCase(tactile)) score += 5;
                                        if ("traffic_signals".equalsIgnoreCase(doc.getString("crossing"))) score += 2;
                                    }
                                }
                            }
                        }
                        scores.add(score);
                        durations.add((path.size() / 60) + "min");
                    }

                    List<Integer> indices = new ArrayList<>();
                    for (int i = 0; i < paths.size(); i++) indices.add(i);
                    indices.sort((a, b) -> scores.get(b) - scores.get(a));

                    int[] blues = {Color.rgb(0, 0, 128), Color.rgb(65, 105, 225), Color.rgb(135, 206, 250)};
                    for (int rank = 0; rank < Math.min(3, indices.size()); rank++) {
                        int idx = indices.get(rank);
                        List<LatLng> path = paths.get(idx);
                        int score = scores.get(idx);
                        String duration = durations.get(idx);

                        PolylineOptions polylineOptions = new PolylineOptions()
                                .addAll(path)
                                .width(12)
                                .color(blues[rank]);
                        mMap.addPolyline(polylineOptions);

                        LatLng mid = path.get(path.size() / 2);
                        String emoji = "wheelchair".equals(userType) ? "🧑🏼‍🦽‍➡️" : "🧑‍🦯";

                        Bitmap bitmap = createAccessiblePathLabel(emoji, duration, score);
                        mMap.addMarker(new MarkerOptions()
                                .position(mid)
                                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                                .anchor(0.5f, 0f)  // 保证文字在图标正下方
                                .zIndex(999));
                    }
                });
    }

    private Bitmap createAccessiblePathLabel(String emoji, String duration, int score) {
        String text = emoji + duration + " / " + score;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(34);
        paint.setColor(Color.WHITE);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.rgb(13, 71, 161));

        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        int padding = 20;
        int width = bounds.width() + padding * 2;
        int height = bounds.height() + padding * 2;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawRoundRect(new RectF(0, 0, width, height), 20, 20, bgPaint);
        canvas.drawText(text, width / 2f, height / 2f + bounds.height() / 2f - 6, paint);

        return bitmap;
    }

    private DocumentSnapshot findNearestBusiness(LatLng mid, Iterable<DocumentSnapshot> sites) {
        DocumentSnapshot nearest = null;
        double minDist = Double.MAX_VALUE;

        for (DocumentSnapshot doc : sites) {
            try {
                double lat = doc.getDouble("lat");
                double lon = doc.getDouble("lon");
                double dist = Math.pow(lat - mid.latitude, 2) + Math.pow(lon - mid.longitude, 2);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = doc;
                }
            } catch (Exception ignored) {}
        }
        return nearest;
    }

}