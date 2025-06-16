package funs.gamez.model;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import funs.gamez.minos.R;
import funs.page.MazeActivity;
import funs.page.MazeFragment;

public class AuthActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private TextView tvMessage;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_CURRENT_USER = "currentUser";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // 先初始化 sharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 再清除之前的登录状态
        clearCurrentUser();

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        tvMessage = findViewById(R.id.tv_message);
        Button btnLogin = findViewById(R.id.btn_login);
        Button btnRegister = findViewById(R.id.btn_register);

        btnLogin.setOnClickListener(v -> loginUser());
        btnRegister.setOnClickListener(v -> registerUser());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次返回登录界面时清除登录状态
        if (sharedPreferences != null) {
            clearCurrentUser();
        }
    }

    private void clearCurrentUser() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_CURRENT_USER);
        editor.apply();
    }

    private void loginUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            tvMessage.setText("请输入用户名和密码");
            return;
        }

        String storedPassword = sharedPreferences.getString(username, null);
        if (storedPassword != null && storedPassword.equals(password)) {
            tvMessage.setText("登录成功！");
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(this, MazeActivity.class);
                intent.putExtra("username", username);
                startActivity(intent);
                finish(); // 关闭 AuthActivity
            }, 1000);
        } else {
            tvMessage.setText("用户名或密码错误");
        }
    }

    private void registerUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            tvMessage.setText("请输入用户名和密码");
            return;
        }

        if (sharedPreferences.contains(username)) {
            tvMessage.setText("用户名已存在");
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(username, password); // 保存用户凭证
        editor.apply();

        tvMessage.setText("注册成功！");
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(this, MazeActivity.class);
            intent.putExtra("username", username); // 添加用户名传递
            startActivity(intent);
            finish();
        }, 1000);
    }

    private void showMazeFragment(String username) {
        // 创建Bundle传递用户名
        Bundle bundle = new Bundle();
        bundle.putString("username", username);

        // 创建Fragment实例
        MazeFragment mazeFragment = new MazeFragment();
        mazeFragment.setArguments(bundle);

        // 显示Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mazeFragment)
                .commit();

        // 显示Fragment容器
        findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
    }
}
