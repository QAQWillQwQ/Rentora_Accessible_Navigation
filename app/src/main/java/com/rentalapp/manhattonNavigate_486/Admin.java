// 🟣 Admin.java
package com.rentalapp.manhattonNavigate_486;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rentalapp.R;
import com.rentalapp.adapter.MainAdapter;
import com.rentalapp.auth.Login;
import com.rentalapp.base.DatabaseHelper;
import com.rentalapp.base.FirebaseHouseManagerActivity;
import com.rentalapp.bean.House;
import com.rentalapp.house.Detail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Admin extends AppCompatActivity {

    private ListView listView;
    private MainAdapter adapter;
    private DatabaseHelper databaseHelper;
    private List<House> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin);
        databaseHelper = new DatabaseHelper(this);

        Button btnSyncFirebaseDelete = findViewById(R.id.btn_syncFirebaseDelete);
        btnSyncFirebaseDelete.setOnClickListener(v -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DatabaseHelper dbHelper = new DatabaseHelper(this);

            db.collection("houses").get().addOnSuccessListener(snapshot -> {
                List<Integer> firebaseIds = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshot) {
                    Long houseId = doc.getLong("id");
                    if (houseId != null) {
                        firebaseIds.add(houseId.intValue());
                    }
                }

                List<House> localHouses = dbHelper.getAllHousesIncludeUnchecked();
                int deleted = 0;
                for (House house : localHouses) {
                    if (!firebaseIds.contains(house.getId())) {
                        dbHelper.deleteHouse(house.getId());
                        Log.d("SYNC_DELETE", "🧹 Removed local-only house ID: " + house.getId());
                        deleted++;
                    }
                }

                Toast.makeText(this, "✅ Local cleanup done. Removed: " + deleted, Toast.LENGTH_LONG).show();
            }).addOnFailureListener(e -> {
                Log.e("SYNC_DELETE", "❌ Failed to fetch Firebase houses", e);
                Toast.makeText(this, "❌ Firebase fetch failed!", Toast.LENGTH_SHORT).show();
            });
        });

        Button outBtn = findViewById(R.id.outBtn);
        outBtn.setOnClickListener(view -> {
            SharedPreferences sp = getSharedPreferences("userinfo", 0);
            SharedPreferences.Editor editor = sp.edit();
            editor.clear();
            editor.apply();
            startActivity(new Intent(Admin.this, Login.class));
        });

        listView = findViewById(R.id.listView);
        adapter = new MainAdapter(this, list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            Intent intent = new Intent(Admin.this, Detail.class);
            intent.putExtra("id", list.get(i).getId() + "");
            startActivity(intent);
        });

        Button clearLocalBtn = findViewById(R.id.clearLocalBtn);
        clearLocalBtn.setOnClickListener(v -> {
            boolean ok = databaseHelper.deleteAllHouses();
            if (ok) {
                list.clear();
                adapter.notifyDataSetChanged();
                Toast.makeText(Admin.this, "Local house data cleared.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(Admin.this, "Failed to clear local data.", Toast.LENGTH_SHORT).show();
            }
        });

        Button storedHousesBtn = findViewById(R.id.btn_stored_houses);
        storedHousesBtn.setOnClickListener(v -> {
            Log.d("Admin", "📂 Opening Firebase House Manager");
            Intent intent = new Intent(Admin.this, FirebaseHouseManagerActivity.class);
            startActivity(intent);
        });

        Button btnImportBusinessCsv = findViewById(R.id.btn_import_business_csv);
        btnImportBusinessCsv.setOnClickListener(v -> {
            Log.d("CSV_IMPORT", "📥 Launching file picker for business site CSV...");
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("text/csv");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Select Business Site CSV"), 9001);
        });

        // ✅ 动态申请权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 10001);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
    }

    private void refreshData() {
        list.clear();
        List<House> templist = databaseHelper.getHouseForCheck();
        list.addAll(templist);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 9001 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                Log.d("CSV_IMPORT", "📄 Selected CSV Uri: " + uri.toString());
                try {
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    final String[] line = new String[1];
                    final int[] lineNumber = {0};

                    FirebaseFirestore firestore = FirebaseFirestore.getInstance();

                    List<HashMap<String, Object>> parsedData = new ArrayList<>();

                    firestore.collection("Business Site_csv").get().addOnSuccessListener(snapshot -> {
                        Set<String> existingNames = new HashSet<>();
                        for (QueryDocumentSnapshot doc : snapshot) {
                            String name = doc.getString("Business Name");
                            if (name != null) {
                                existingNames.add(name.trim().toLowerCase());
                            }
                        }

                        try {
                            while ((line[0] = reader.readLine()) != null) {
                                lineNumber[0]++;
                                if (lineNumber[0] == 1) continue;

                                String[] parts = line[0].split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                                if (parts.length != 9) {
                                    Log.e("CSV_IMPORT", "❌ Invalid format at line " + lineNumber[0] + ": " + line[0]);
                                    continue;
                                }

                                String name = clean(parts[0]);
                                if (name.isEmpty()) {
                                    Log.w("CSV_IMPORT", "⚠️ Empty name, skipping.");
                                    continue;
                                }

                                if (existingNames.contains(name.trim().toLowerCase())) {
                                    Log.w("CSV_IMPORT", "⚠️ Duplicate (business name): " + name);
                                    continue;
                                }

                                HashMap<String, Object> dataMap = new HashMap<>();
                                dataMap.put("Business Name", name);
                                dataMap.put("Business Type", clean(parts[1]));
                                dataMap.put("Address", clean(parts[2]));

                                try {
                                    dataMap.put("Latitude", Double.parseDouble(clean(parts[3])));
                                    dataMap.put("Longitude", Double.parseDouble(clean(parts[4])));
                                } catch (NumberFormatException e) {
                                    Log.e("CSV_IMPORT", "❌ Invalid lat/lng at line " + lineNumber[0] + ": " + line[0], e);
                                    continue;
                                }

                                dataMap.put("Tactile Paving", clean(parts[5]));
                                dataMap.put("Steep Slope", clean(parts[6]));
                                dataMap.put("Wheelchair", clean(parts[7]));
                                dataMap.put("Elevator", clean(parts[8]));

                                parsedData.add(dataMap);
                            }
                        } catch (Exception e) {
                            Log.e("CSV_IMPORT", "❌ Error parsing CSV", e);
                            return;
                        }

                        if (parsedData.isEmpty()) {
                            Toast.makeText(Admin.this, "❌ No valid business data to import.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        Log.d("CSV_IMPORT", "🚀 Start uploading " + parsedData.size() + " items...");
                        int total = parsedData.size();
                        int[] uploaded = {0};

                        for (HashMap<String, Object> map : parsedData) {
                            firestore.collection("Business Site_csv")
                                    .add(map)
                                    .addOnSuccessListener(doc -> {
                                        Log.d("CSV_IMPORT", "✅ Added: " + map.get("Business Name"));
                                        synchronized (uploaded) {
                                            uploaded[0]++;
                                            if (uploaded[0] == total) {
                                                runOnUiThread(() -> {
                                                    Toast.makeText(Admin.this, "✅ CSV import completed (" + total + " added)", Toast.LENGTH_SHORT).show();
                                                    Intent intent = new Intent(Admin.this, Map.class);
                                                    intent.putExtra("refreshBusinessSites", true);
                                                    startActivity(intent);
                                                });
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e("CSV_IMPORT", "❌ Failed to upload: " + map.get("Business Name"), e));
                        }

                    }).addOnFailureListener(e -> {
                        Log.e("CSV_IMPORT", "❌ Failed to fetch existing business names", e);
                        Toast.makeText(this, "❌ Firestore fetch failed", Toast.LENGTH_SHORT).show();
                    });

                } catch (Exception e) {
                    Log.e("CSV_IMPORT", "❌ Failed to read CSV", e);
                    Toast.makeText(this, "❌ Error reading CSV file", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }



    private String clean(String input) {
        if (input == null) return "";
        return input
                .replaceAll("^\\s*\"+", "")       // leading quotes + space
                .replaceAll("\"+\\s*$", "")       // trailing quotes + space
                .replaceAll("\\s+", " ")          // collapse multiple spaces
                .trim();                          // final trim
    }



}
