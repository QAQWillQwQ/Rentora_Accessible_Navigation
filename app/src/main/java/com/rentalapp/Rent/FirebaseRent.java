package com.rentalapp.Rent;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rentalapp.base.DatabaseHelper;
import com.rentalapp.bean.House;

import java.util.HashMap;
import java.util.Map;

public class FirebaseRent {

    private static final String TAG = "FirebaseRent";

    private final FirebaseFirestore firestore;
    private final DatabaseHelper db;

    public FirebaseRent(Context context) {
        this.firestore = FirebaseFirestore.getInstance();
        this.db = new DatabaseHelper(context);
    }

    public void fetchMyRentingHouses(String uid) {
        firestore.collection("rentings")
                .whereEqualTo("uid", Long.parseLong(uid)) // ✅ 用 Long 匹配 Long
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Renting renting = doc.toObject(Renting.class);
                        boolean alreadyExists = db.checkRentingExists(renting.getUid(), renting.getHid());

                        if (!alreadyExists) {
                            // ✅ 插入本地租赁记录
                            db.addRenting(renting);
                        }

                        // 检查本地是否已有该房源
                        House localHouse = db.getHouse(renting.getHid());
                        if (localHouse == null || localHouse.getId() == 0) {
                            // 如果没有该房源，则从 Firebase 拉取完整房源信息
                            firestore.collection("houses")
                                    .document(String.valueOf(renting.getHid()))
                                    .get()
                                    .addOnSuccessListener(houseDoc -> {
                                        if (houseDoc.exists()) {
                                            House house = houseDoc.toObject(House.class);
                                            if (house != null) {
                                                db.addHouse(house); // ✅ 插入本地房源表
                                                Log.d(TAG, "✅ House inserted from Firebase: hid=" + house.getId());
                                            }
                                        }
                                    });
                        }

                        // ✅ 更新房源状态为 rented（无论是否新插入）
                        db.updateHouseStatus(String.valueOf(renting.getHid()), "rented");

                        Log.d(TAG, "✅ Renting synced: hid=" + renting.getHid());
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to fetch renting: " + e.getMessage()));
    }

    public void recordRentingToCloud(Renting renting) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        Map<String, Object> data = new HashMap<>();
        data.put("uid", renting.getUid());
        data.put("hid", renting.getHid());
        data.put("signature", renting.getSignature());
        data.put("contract", renting.getContract());
        data.put("rentaltime", renting.getRentaltime());
        data.put("addtime", renting.getAddtime());
        data.put("status", renting.getStatus());

        // ✅ 写入 rentings 集合（文档ID可自定义，避免重复）
        String docId = renting.getUid() + "_" + renting.getHid();
        firestore.collection("rentings").document(docId)
                .set(data)
                .addOnSuccessListener(unused ->
                        Log.d("FirebaseRent", "✅ Renting uploaded: " + docId))
                .addOnFailureListener(e ->
                        Log.e("FirebaseRent", "❌ Failed to upload renting: " + e.getMessage()));

        // ✅ 更新 users 集合中该用户的 rented 字段
        firestore.collection("users").document(String.valueOf(renting.getUid()))
                .update("rented", FieldValue.arrayUnion(renting.getHid()))
                .addOnSuccessListener(unused ->
                        Log.d("FirebaseRent", "✅ User rented list updated"))
                .addOnFailureListener(e ->
                        Log.e("FirebaseRent", "❌ Failed to update user rented field"));
    }

    // 🟣 FirestoreHouse.java

    public void updateRentInfo(int houseId, Renting renting) {
        FirebaseFirestore.getInstance()
                .collection("houses")
                .whereEqualTo("localId", houseId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        doc.getReference().update(new HashMap<String, Object>() {{
                            put("rentStatus", renting.getStatus());
                            put("tenantUid", renting.getUid());
                            put("rentalTime", renting.getRentaltime());
                            put("signature", renting.getSignature());
                            put("contract", renting.getContract());
                            put("rentedAt", renting.getAddtime());
                        }});
                        Log.d("FirestoreHouse", "✅ Firebase house updated with rent info: " + houseId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHouse", "❌ Failed to update Firebase house rent info", e);
                });
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



}
