package funs.gamez.model;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import funs.gamez.minos.R;

public class LeaderboardActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private List<LeaderboardItem> leaderboardItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        recyclerView = findViewById(funs.gamez.minos.R.id.rv_leaderboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadLeaderboardData();

        adapter = new LeaderboardAdapter(leaderboardItems);
        recyclerView.setAdapter(adapter);
    }

    private void loadLeaderboardData() {
        SharedPreferences prefs = getSharedPreferences("GameScores", MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getValue() instanceof Integer) {
                leaderboardItems.add(new LeaderboardItem(entry.getKey(), (Integer) entry.getValue()));
            }
        }

        // 按分数降序排序
        Collections.sort(leaderboardItems, (item1, item2) -> item2.getScore() - item1.getScore());

        // 限制显示前20名
        if (leaderboardItems.size() > 20) {
            leaderboardItems = leaderboardItems.subList(0, 20);
        }
    }

    static class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
        private List<LeaderboardItem> items;

        public LeaderboardAdapter(List<LeaderboardItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_leaderboard, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LeaderboardItem item = items.get(position);
            holder.tvRank.setText(String.valueOf(position + 1));
            holder.tvUsername.setText(item.getUsername());
            holder.tvScore.setText(String.valueOf(item.getScore()));

            /*// 高亮显示当前用户
            String currentUser = AuthActivity.getCurrentUser(holder.itemView.getContext());
            if (item.getUsername().equals(currentUser)) {
                holder.itemView.setBackgroundResource(R.drawable.highlight_bg);
            } else {
                holder.itemView.setBackgroundResource(R.drawable.rounded_bg);
            }*/
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvRank, tvUsername, tvScore;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvRank = itemView.findViewById(R.id.tv_rank);
                tvUsername = itemView.findViewById(R.id.tv_username);
                tvScore = itemView.findViewById(R.id.tv_score);
            }
        }
    }

    static class LeaderboardItem {
        private String username;
        private int score;

        public LeaderboardItem(String username, int score) {
            this.username = username;
            this.score = score;
        }

        public String getUsername() {
            return username;
        }

        public int getScore() {
            return score;
        }
    }
}