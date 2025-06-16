
package funs.page;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import funs.gamez.minos.R;
import funs.gamez.model.AuthActivity;

//  You need to use a Theme.AppCompat theme (or descendant) with this activity.
public class MazeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maze);

        // 获取从 AuthActivity 传递过来的用户名
        String username = getIntent().getStringExtra("username");

        // 创建 Bundle 传递用户名给 Fragment
        Bundle bundle = new Bundle();
        bundle.putString("username", username);

        // 创建并设置 Fragment
        MazeFragment mazeFragment = new MazeFragment();
        mazeFragment.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mazeFragment)
                .commit();
    }
}