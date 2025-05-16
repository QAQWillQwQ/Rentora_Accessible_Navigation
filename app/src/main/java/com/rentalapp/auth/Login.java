package com.rentalapp.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.rentalapp.manhattonNavigate_486.Admin;
import com.rentalapp.base.FirebaseMerge;
import com.rentalapp.manhattonNavigate_486.Map;
import com.rentalapp.R;
import com.rentalapp.bean.User;
import com.rentalapp.base.DatabaseHelper;


//登录页面
public class Login extends AppCompatActivity {

    private EditText et_email;
    private EditText et_password;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // ✅ Firebase 连接测试
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Toast.makeText(Login.this, "☁️ Firebase connected successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Login.this, "❌ Firebase connection failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

        databaseHelper = new DatabaseHelper(this);
        et_email = findViewById(R.id.et_email);
        et_password = findViewById(R.id.et_password);
        Button loginBtn = findViewById(R.id.loginBtn);
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toLogin();
            }
        });
        Button inBtn = findViewById(R.id.inBtn);
        inBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Login.this, Map.class));
            }
        });
        TextView regBtn = findViewById(R.id.regBtn);
        regBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Login.this, Register.class));
            }
        });

        // ✅ 尝试合并本地数据（用户、房源）进 Firebase（跳过重复）
        new FirebaseMerge(this)
                .mergeUsersIfNotExists();
        new FirebaseMerge(this)
                .mergeHousesIfNotExists();
    }

    private void toLogin() {
        String email = et_email.getText().toString();
        String password = et_password.getText().toString();
        if (email.equals("") || password.equals("")) {
            Toast.makeText(this, "input email and password", Toast.LENGTH_LONG).show();
        } else {

            User user = databaseHelper.getUserInfo(email);

            if (user == null) {
                // 🔁 用户在本地不存在，尝试从 Firebase Firestore 拉取
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(email)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                // 构造 User 对象并插入本地数据库
                                User firebaseUser = new User();
                                firebaseUser.setEmail(email);
                                firebaseUser.setPassword(documentSnapshot.getString("password"));
                                firebaseUser.setRole(documentSnapshot.getString("role"));
                                firebaseUser.setTruename(documentSnapshot.getString("truename"));
                                firebaseUser.setPhone(documentSnapshot.getString("phone"));

                                boolean inserted = databaseHelper.addUser(
                                        firebaseUser.getEmail(),
                                        firebaseUser.getPassword(),
                                        firebaseUser.getRole()
                                );
                                if (inserted) {
                                    // 再次获取刚插入用户
                                    User syncedUser = databaseHelper.getUserInfo(email);
                                    if (password.equals(syncedUser.getPassword())) {
                                        loginSuccess(syncedUser);
                                    } else {
                                        Toast.makeText(Login.this, "Password incorrect (from Firebase).", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(Login.this, "Failed to sync Firebase user.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(Login.this, "User not found in Firebase.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(Login.this, "Firebase error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else if (password.equals(user.getPassword())) {
                loginSuccess(user);
            } else {
                Toast.makeText(this, "password error.", Toast.LENGTH_SHORT).show();
            }

            /*
            User user = databaseHelper.getUserInfo(email);
            if (user == null) {
                Toast.makeText(this, "The user does not exist.", Toast.LENGTH_SHORT).show();

            } else if (password.equals(user.getPassword())) {
                // 保存登录信息
                SharedPreferences sp = this.getSharedPreferences("userinfo", MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("uid", user.getId() + "");
                editor.putString("email", email);
                editor.putString("role",user.getRole());
                editor.commit();
                // 登录成功
                Toast.makeText(this, "Login Success", Toast.LENGTH_LONG).show();
                // 跳转首页
                if(user.getRole().equals("admin")) {
                    startActivity(new Intent(Login.this, MainActivity.class));
                }else{
                    startActivity(new Intent(Login.this, MapActivity.class));
                }
            } else {
                Toast.makeText(this, "password error.", Toast.LENGTH_SHORT).show();
            }

             */
        }

    }

    private void loginSuccess(User user) {
        SharedPreferences sp = this.getSharedPreferences("userinfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("uid", String.valueOf(user.getId()));
        editor.putString("email", user.getEmail());
        editor.putString("role", user.getRole());
        editor.apply(); // ✅ 更安全，异步

        Toast.makeText(this, "Login Success", Toast.LENGTH_LONG).show();

        if ("admin".equals(user.getRole())) {
            startActivity(new Intent(Login.this, Admin.class));
        } else {
            startActivity(new Intent(Login.this, Map.class));
        }
    }


}