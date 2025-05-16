package com.rentalapp.base;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rentalapp.R;
import com.rentalapp.bean.House;

import java.util.List;

public class FirebaseHouseManagerActivity extends AppCompatActivity {

    private static final String TAG = "FirebaseHouseMgr";
    private LinearLayout houseListContainer;
    private FirebaseFirestore db;
    private DatabaseHelper localDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_house_manager);

        houseListContainer = findViewById(R.id.firebaseHouseListContainer);
        db = FirebaseFirestore.getInstance();
        localDbHelper = new DatabaseHelper(this);

        // ✅ 添加返回按钮逻辑
        Button backBtn = findViewById(R.id.btnBackToAdmin);
        backBtn.setOnClickListener(v -> {
            Log.d(TAG, "⬅ Back to Admin");
            finish();
        });

        loadFirebaseApprovedHouses();
    }

    private void loadFirebaseApprovedHouses() {
        db.collection("houses")
                .whereEqualTo("status", "agree")
                .get()
                .addOnSuccessListener(query -> {
                    houseListContainer.removeAllViews();
                    for (QueryDocumentSnapshot doc : query) {
                        String docId = doc.getId();
                        int localId = doc.getLong("localId") != null ? doc.getLong("localId").intValue() : -1;
                        String title = doc.getString("title");
                        String address = doc.getString("address");

                        TextView titleView = new TextView(this);
                        titleView.setText("🏠 " + title + "\n📍" + address);
                        titleView.setPadding(20, 20, 20, 10);

                        Button deleteBtn = new Button(this);
                        deleteBtn.setText("Delete from Local & Firebase");
                        deleteBtn.setBackgroundColor(getResources().getColor(R.color.red));
                        deleteBtn.setTextColor(getResources().getColor(android.R.color.white));
                        deleteBtn.setTag(new HouseRef(docId, localId));

                        deleteBtn.setOnClickListener(v -> {
                            HouseRef ref = (HouseRef) v.getTag();
                            deleteHouseEverywhere(ref);
                        });

                        houseListContainer.addView(titleView);
                        houseListContainer.addView(deleteBtn);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Failed to load houses", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "🔥 Firebase load failed", e);
                });
    }

    private void deleteHouseEverywhere(HouseRef ref) {
        // 🔥 删除 Firebase
        db.collection("houses").document(ref.firebaseId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "✅ Firebase house deleted: " + ref.firebaseId);

                    // 🧹 删除本地数据库
                    if (ref.localId != -1) {
                        boolean success = localDbHelper.deleteHouseAndRelated(ref.localId);
                        Log.d(TAG, "🗑 Local delete success=" + success + " for id=" + ref.localId);
                    }

                    Toast.makeText(this, "✅ Deleted from Firebase & Local", Toast.LENGTH_SHORT).show();
                    loadFirebaseApprovedHouses(); // ✅ 刷新列表
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firebase delete failed", e);
                    Toast.makeText(this, "❌ Firebase deletion failed", Toast.LENGTH_SHORT).show();
                });
    }

    // ✅ 内部类用于传递删除信息
    private static class HouseRef {
        String firebaseId;
        int localId;

        public HouseRef(String firebaseId, int localId) {
            this.firebaseId = firebaseId;
            this.localId = localId;
        }
    }
}
