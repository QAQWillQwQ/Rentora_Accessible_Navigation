package com.rentalapp.house;

// [🟣 FirestoreHouse.java]

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rentalapp.base.FirebaseSync;
import com.rentalapp.bean.House;
import com.rentalapp.base.DatabaseHelper;
import com.rentalapp.Rent.Renting;

import java.util.function.Consumer;

/**
 * Handles house upload and cloud sync to Firestore.
 */
public class FirestoreHouse {

    private static final String TAG = "FirestoreHouse";

    private final DatabaseHelper databaseHelper;

    public FirestoreHouse(Context context) {
        this.databaseHelper = new DatabaseHelper(context);
    }

    /**
     * Insert house into local DB and sync to Firebase Firestore.
     *
     * @param house the house object
     * @return true if local insert successful
     */
    public boolean addAndSync(House house) {
        boolean success = databaseHelper.addHouse(house);
        if (success) {
            // ✅ 获取“刚插入本地”的 house 对象（含真实 id）再上传
            House inserted = databaseHelper.getLastHouse(); // 👈 必须使用插入后的对象
            FirebaseSync.syncHouse(inserted);
            Log.d(TAG, "House synced after local insert: id=" + inserted.getId());
        } else {
            Log.e(TAG, "Local house insert failed");
        }
        return success;
    }

    public void deleteHouse(int houseId) {
        boolean deleted = databaseHelper.delHouse(houseId);
        if (deleted) {
            FirebaseSync.deleteHouse(houseId); // ✅ 同步删除 Firebase 中的数据
            Log.d(TAG, "House deleted locally and from Firebase: id=" + houseId);
        } else {
            Log.e(TAG, "Failed to delete house locally: id=" + houseId);
        }
    }

    public void updateHouseStatus(int houseId, String newStatus) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("houses")
                .whereEqualTo("localId", houseId)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        doc.getReference().update("status", newStatus);
                    }
                })
                .addOnFailureListener(e -> Log.e("FirestoreService", "Failed to update house status", e));
    }

    // 🟣 FirestoreHouse.java
    public void updateRentInfo(int houseId, Renting renting) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("houses")
                .whereEqualTo("localId", houseId)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        doc.getReference().update(
                                "status", "rented",
                                "tenantUid", String.valueOf(renting.getUid()),
                                "rentPeriod", renting.getRentaltime(),
                                "signature", renting.getSignature(),
                                "contract", renting.getContract(),
                                "addtime", renting.getAddtime()
                        );
                        Log.d(TAG, "✅ Firestore house updated with rent info: houseId=" + houseId);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to update rent info in Firestore", e));
    }

    // 🟣 FirestoreHouse.java（添加方法）
    public void updateRentalFields(House house) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("houses")
                .whereEqualTo("localId", house.getId()) // 或其它你定义的标识
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        doc.getReference().update(
                                "status", house.getStatus(),
                                "tenantUid", house.getTenantUid(),
                                "rentaltime", house.getRentaltime(),
                                "contract", house.getContract(),
                                "signature", house.getSignature()
                        );
                    }
                })
                .addOnFailureListener(e -> Log.e("FirestoreHouse", "❌ Failed to update rental fields", e));
    }

    public void fetchHouseById(int houseId, Consumer<House> callback) {
        FirebaseFirestore.getInstance().collection("houses")
                .whereEqualTo("localId", houseId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        House h = doc.toObject(House.class);
                        if (h != null) h.setId(houseId); // 保持一致性
                        callback.accept(h);
                    } else {
                        callback.accept(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHouse", "❌ fetchHouseById failed", e);
                    callback.accept(null);
                });
    }



}
