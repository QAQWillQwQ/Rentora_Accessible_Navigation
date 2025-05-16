package com.rentalapp.auth;
import android.content.Context;
import android.util.Log;

import com.rentalapp.base.FirebaseSync;
import com.rentalapp.bean.User;
import com.rentalapp.base.DatabaseHelper;

/**
 * Handles user registration and cloud sync to Firebase Firestore.
 */
public class FirestoreUser {

    private static final String TAG = "FirestoreUser";

    private final DatabaseHelper databaseHelper;

    public FirestoreUser(Context context) {
        this.databaseHelper = new DatabaseHelper(context);
    }

    /**
     * Register user locally and sync to Firestore.
     *
     * @param email    user email
     * @param password user password
     * @param role     user role
     * @return true if local registration successful
     */
    public boolean registerAndSync(String email, String password, String role) {
        boolean inserted = databaseHelper.addUser(email, password, role);
        if (!inserted) {
            Log.e(TAG, "Local SQLite user registration failed.");
            return false;
        }

        // fetch full user info to sync
        User user = databaseHelper.getUserInfo(email);
        if (user != null) {
            FirebaseSync.syncUser(user);
            return true;
        } else {
            Log.e(TAG, "User not found after insert.");
            return false;
        }
    }
}
