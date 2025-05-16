package com.rentalapp.base;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rentalapp.bean.House;
import com.rentalapp.bean.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseMerge {

    private final FirebaseFirestore firestore;
    private final DatabaseHelper db;
    private final Context context;

    public FirebaseMerge(Context context) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.db = new DatabaseHelper(context);
    }

    public void fetchApprovedHousesFromFirebase() {
        firestore.collection("houses")
                .whereEqualTo("status", "agree")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (var doc : querySnapshot) {
                        House house = doc.toObject(House.class);
                        // 避免重复（title+address 唯一约束）
                        boolean alreadyExists = false;
                        List<House> local = db.getAllHousesIncludeUnchecked();
                        for (House h : local) {
                            if (h.getTitle().equals(house.getTitle())
                                    && h.getAddress().equals(house.getAddress())) {
                                alreadyExists = true;
                                break;
                            }
                        }

                        if (!alreadyExists) {
                            boolean inserted = db.addHouse(house);
                            Log.d("FETCH", "✅ Synced from cloud: " + house.getTitle() + ", inserted=" + inserted);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("FETCH", "❌ Failed to fetch houses: " + e.getMessage()));
    }


    // ✅ 上传本地所有用户（避免重复）
    public void mergeUsersIfNotExists() {
        List<User> localUsers = db.getAllUsers(); // ✅ 你需要确保 DatabaseHelper 中有这个方法
        for (User user : localUsers) {
            firestore.collection("users").document(user.getEmail()).get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            Map<String, Object> map = new HashMap<>();
                            map.put("email", user.getEmail());
                            map.put("password", user.getPassword());
                            map.put("role", user.getRole());
                            map.put("truename", user.getTruename());
                            map.put("phone", user.getPhone());

                            firestore.collection("users").document(user.getEmail())
                                    .set(map)
                                    .addOnSuccessListener(unused -> Log.d("MERGE", "✅ User synced: " + user.getEmail()))
                                    .addOnFailureListener(e -> Log.e("MERGE", "❌ Failed to sync user: " + user.getEmail(), e));
                        }
                    });
        }
    }

    // ✅ 上传本地所有房源（避免重复，使用 title + address 作为唯一键）
    public void mergeHousesIfNotExists() {
        List<House> localHouses = db.getAllHousesIncludeUnchecked(); // ✅ 你需要添加这个方法支持获取全部房源（无论状态）

        for (House house : localHouses) {
            String docId = house.getTitle() + "_" + house.getAddress(); // 可自定义唯一键
            firestore.collection("houses").document(docId).get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            Map<String, Object> map = new HashMap<>();
                            map.put("uid", house.getUid());
                            map.put("title", house.getTitle());
                            map.put("price", house.getPrice());
                            map.put("area", house.getArea());
                            map.put("address", house.getAddress());
                            map.put("powerrate", house.getPowerrate());
                            map.put("imgpath", house.getImgpath());
                            map.put("housetype", house.getHousetype());
                            map.put("pdfpath", house.getPdfpath());
                            map.put("remark", house.getRemark());
                            map.put("uname", house.getUname());
                            map.put("uphone", house.getUphone());
                            map.put("umail", house.getUmail());
                            map.put("lng", house.getLng());
                            map.put("lat", house.getLat());
                            map.put("status", house.getStatus());

                            firestore.collection("houses").document(docId)
                                    .set(map)
                                    .addOnSuccessListener(unused -> Log.d("MERGE", "✅ House synced: " + docId))
                                    .addOnFailureListener(e -> Log.e("MERGE", "❌ Failed to sync house: " + docId, e));
                        }
                    });
        }
    }

    // 🟣 FirebaseMerge.java - 插入位置：文件尾部，添加新方法
    public static void deleteLocalHousesNotInFirebase(Context context) {
        DatabaseHelper localDb = new DatabaseHelper(context);

        FirebaseFirestore.getInstance().collection("houses")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Integer> firebaseIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Long id = doc.getLong("localId");
                        if (id != null) firebaseIds.add(id.intValue());
                    }

                    List<House> localHouses = localDb.getAllHousesIncludeUnchecked();

                    int deleted = 0;
                    for (House h : localHouses) {
                        if (!firebaseIds.contains(h.getId())) {
                            localDb.deleteHouse(h.getId());
                            Log.w("MERGE_DELETE", "🗑 Deleted local house not in Firebase: " + h.getTitle());
                            deleted++;
                        }
                    }

                    Log.i("MERGE_DELETE", "✅ Total local deletions: " + deleted);
                })
                .addOnFailureListener(e -> Log.e("MERGE_DELETE", "❌ Failed to fetch Firebase houses", e));
    }


    // ✅ 删除 Firebase 中的房源（使用 title+address 组合键）
    public void deleteHouseFromFirebase(House house) {
        String docId = house.getTitle() + "_" + house.getAddress();
        firestore.collection("houses").document(docId)
                .delete()
                .addOnSuccessListener(unused -> Log.d("DELETE", "✅ Deleted from Firebase: " + docId))
                .addOnFailureListener(e -> Log.e("DELETE", "❌ Failed to delete from Firebase: " + docId, e));
    }
}
