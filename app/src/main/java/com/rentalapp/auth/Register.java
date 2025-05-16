package com.rentalapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.rentalapp.R;
import com.rentalapp.bean.User;
import com.rentalapp.base.DatabaseHelper;

//注册页面
public class Register extends AppCompatActivity {
    private EditText et_email;
    private EditText et_password;
    private EditText et_enterpwd;
    RadioButton rb_landlord,rb_tenant;
    String role="";
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
        databaseHelper = new DatabaseHelper(this);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        }
        et_email = findViewById(R.id.et_email);
        et_email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 在文本改变前调用
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 在文本改变时调用，适合进行实时检查
                if (!isValidEmail(s.toString())) {
                    et_email.setError("Invalid email address");
                } else {
                    et_email.setError(null); // 清除错误提示
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 在文本改变后调用
            }
        });
        et_password = findViewById(R.id.et_password);
        et_enterpwd = findViewById(R.id.et_enterpwd);
        rb_landlord = findViewById(R.id.rb_landlord);
        rb_tenant = findViewById(R.id.rb_tenant);
        Button regBtn = findViewById(R.id.regBtn);
        regBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toReg();
            }
        });
    }

    private boolean isValidEmail(CharSequence email) {
        if (email == null) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
        }
    }

    private void toReg() {
        String email = et_email.getText().toString();
        String password = et_password.getText().toString();
        String enterpwd = et_enterpwd.getText().toString();
        if(rb_landlord.isChecked() == true){
            role = "landlord";
        }else{
            role = "tenant";
        }
        if(email.equals("")||password.equals("")||enterpwd.equals("")||role.equals("")){
            Toast.makeText(Register.this, "All information must be entered.", Toast.LENGTH_SHORT).show();
        }else if(password.equals(enterpwd)){
            User user = databaseHelper.getUserInfo(email);
            if(user!=null){
                Toast.makeText(this,"The user already exists.",Toast.LENGTH_SHORT).show();
            } else {
                //存入数据库
                boolean flag = databaseHelper.addUser(email,password,role);
                if(flag){

                    // ✅ 新增：同步注册用户到 Firebase Firestore
                    new FirestoreUser(this)
                            .registerAndSync(email, password, role);

                    Toast.makeText(this,"Registration successful, please log in.",Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Register.this, Login.class));
                }else{
                    Toast.makeText(this,"login has failed.",Toast.LENGTH_SHORT).show();
                }
            }
        }else{
            Toast.makeText(this,"The two password inputs are inconsistent.",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }



}