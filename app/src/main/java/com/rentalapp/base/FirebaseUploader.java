package com.rentalapp.base;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

public class FirebaseUploader {

    private static final String TAG = "FirebaseUploader";

    private static final FirebaseStorage storage = FirebaseStorage.getInstance();

    /**
     * Uploads a local file (image or PDF) to Firebase Storage.
     *
     * @param localPath full path on local disk (e.g. /data/data/.../images/img_123.jpg)
     * @param remoteFolder remote folder in Firebase Storage (e.g. "images" or "pdfs")
     * @param callback callback to return download URL
     */
    public static void uploadFile(@NonNull String localPath,
                                  @NonNull String remoteFolder,
                                  @NonNull UploadCallback callback) {

        File file = new File(localPath);
        if (!file.exists()) {
            Log.e(TAG, "File not found: " + localPath);
            callback.onFailure("File not found: " + localPath);
            return;
        }

        Uri fileUri = Uri.fromFile(file);
        String fileName = file.getName();
        StorageReference storageRef = storage.getReference()
                .child(remoteFolder + "/" + fileName);

        UploadTask uploadTask = storageRef.putFile(fileUri);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            // 🔁 get download URL
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                Log.d(TAG, "Upload success: " + downloadUrl);
                callback.onSuccess(downloadUrl);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Get URL failed: " + e.getMessage());
                callback.onFailure("Get URL failed: " + e.getMessage());
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Upload failed: " + e.getMessage());
            callback.onFailure("Upload failed: " + e.getMessage());
        });
    }

    /**
     * Callback interface for upload results
     */
    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(String errorMessage);
    }
}
