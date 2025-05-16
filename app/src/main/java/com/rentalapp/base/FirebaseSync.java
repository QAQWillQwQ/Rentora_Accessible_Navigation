package com.rentalapp.base;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rentalapp.bean.House;
import com.rentalapp.bean.Maintenance;
import com.rentalapp.bean.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rentalapp.house.FirestoreHouse;

public class FirebaseSync {

    private static final String TAG = "FirebaseSync";

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // 🟣 FirebaseSync.java - 添加方法：同步维修记录
    public static void syncMaintenanceToCloud(@NonNull Maintenance m) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", m.getUid());
        data.put("lid", m.getLid());
        data.put("hid", m.getHid());
        data.put("content", m.getContent());
        data.put("applytime", m.getApplytime());
        data.put("status", m.getStatus());

        FirebaseFirestore.getInstance()
                .collection("maintenances")
                .add(data)
                .addOnSuccessListener(doc -> Log.d("FirebaseSync", "✅ Maintenance synced: " + m.getContent()))
                .addOnFailureListener(e -> Log.e("FirebaseSync", "❌ Maintenance sync failed", e));
    }

    // ⬆️ 上传用户信息
    public static void syncUser(@NonNull User user) {
        Map<String, Object> data = new HashMap<>();
        data.put("email", user.getEmail());
        data.put("password", user.getPassword());
        data.put("role", user.getRole());
        data.put("truename", user.getTruename() == null ? "" : user.getTruename());
        data.put("phone", user.getPhone() == null ? "" : user.getPhone());

        db.collection("users")
                .document(String.valueOf(user.getEmail()))
                .set(data)
                .addOnSuccessListener(unused -> Log.d(TAG, "User synced: " + user.getEmail()))
                .addOnFailureListener(e -> Log.e(TAG, "User sync failed: " + e.getMessage()));
    }

    // ⬆️ 上传房源信息
    public static void syncHouse(@NonNull House house) {
        Map<String, Object> data = new HashMap<>();
        data.put("localId", house.getId()); // ✅ 添加本地主键id，便于后续删除
        data.put("uid", house.getUid());
        data.put("title", house.getTitle());
        data.put("price", house.getPrice());
        data.put("area", house.getArea());
        data.put("address", house.getAddress());
        data.put("powerrate", house.getPowerrate());
        data.put("imgpath", house.getImgpath());
        data.put("housetype", house.getHousetype());
        data.put("pdfpath", house.getPdfpath());
        data.put("remark", house.getRemark());
        data.put("uname", house.getUname());
        data.put("uphone", house.getUphone());
        data.put("umail", house.getUmail());
        data.put("lng", house.getLng());
        data.put("lat", house.getLat());
        data.put("status", house.getStatus());

        db.collection("houses")
                .document(String.valueOf(house.getId())) // use local DB id for now
                .set(data)
                .addOnSuccessListener(unused -> Log.d(TAG, "House synced: " + house.getTitle()))
                .addOnFailureListener(e -> Log.e(TAG, "House sync failed: " + e.getMessage()));
    }

    public static void deleteHouse(int localId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("houses")
                .whereEqualTo("localId", localId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                })
                .addOnFailureListener(e -> Log.e("FirebaseSync", "Delete failed", e));
    }

    // ✅ 插入位置：FirebaseSync.java 文件尾部添加
    public static void syncAllLocalRentedHouses(Context context) {
        DatabaseHelper db = new DatabaseHelper(context);
        List<House> all = db.getAllHousesIncludeUnchecked();  // 包含所有本地房源
        FirestoreHouse service = new FirestoreHouse(context);

        int count = 0;
        for (House h : all) {
            if ("rented".equalsIgnoreCase(h.getStatus())) {
                service.updateRentalFields(h);
                Log.d("SYNC_RENTED", "✅ Synced rented house to Firebase: " + h.getTitle());
                count++;
            }
        }
        Log.i("SYNC_RENTED", "✅ Total rented houses synced: " + count);
    }

}
