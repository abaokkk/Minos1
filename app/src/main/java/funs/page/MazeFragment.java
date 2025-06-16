package funs.page;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer; // 确保导入
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.appcompat.app.AlertDialog;

import java.util.concurrent.atomic.AtomicInteger;

import funs.common.tools.CLogger;
import funs.gamez.controller.GameController;
import funs.gamez.minos.R;
import funs.gamez.model.Game;
import funs.gamez.model.GameState;
import funs.gamez.model.LeaderboardActivity;
import funs.gamez.model.SettingsFormData;
import funs.gamez.model.factory.GameFactory;
import funs.gamez.model.factory.GameMetrics;
import funs.gamez.view.DisplayUtils;
import funs.gamez.view.GameView;
import funs.gamez.view.SettingsWindow;

public class MazeFragment extends Fragment {
    private static final String TAG = "MazeFragment";
    private static final String STATE_KEY_GAME = "game";
    private static final String STATE_KEY_GAME_SETTINGS = "gameSettings";
    private static final String STATE_KEY_SCREEN_ORIENTATION = "screenOrientation";

    private static final String SCORE_FORMAT = " \uD83C\uDFC6  %d ";
    private int score = 0;
    private int scoreStep = 10;

    private FrameLayout gameContainer = null;
    private GameView gameView = null;
    private SettingsWindow settingsWindow = null;

    private GameMetrics gameMetrics = null;

    private GameController gameController = null;

    private Animation destroyMazeAnimation = null;
    private Animation createMazeAnimation = null;

    /* --- 实例状态 --- */
    private Game game;
    private SettingsFormData gameSettings;
    private int screenOrientation;

    private TextView scoreTv;

    private static final String STATE_KEY_REMAINING_TIME = "remainingTime";
    private CountDownTimer gameTimer;
    private long remainingTimeMillis = 15000;
    private TextView timeRemainingTv;
    private String currentUsername; // 存储当前用户名
    private static final String PREFS_SCORES = "GameScores";

    // 添加指数增长相关的常量
    private static final long BASE_TIME = 15000; // 基础时间（15秒）
    private static final float TIME_EXPONENT_BASE = 2.0f; // 指数基数
    private static final float TIME_EXPONENT_FACTOR = 3.0f; // 指数因子

    // 计算当前复杂度下的初始时间
    private long calculateInitialTime() {
        float sizeFactor = gameSettings.getMazeSize();
        // 指数公式: time = BASE_TIME * (base ^ (factor * size))
        return (long) (BASE_TIME * Math.pow(TIME_EXPONENT_BASE, TIME_EXPONENT_FACTOR * sizeFactor));
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 从 Activity 获取传递过来的用户名
        if (getArguments() != null) {
            currentUsername = getArguments().getString("username");
        }

        // App 首次启动
        if (savedInstanceState == null) {

            // 确定并锁定当前屏幕方向
            screenOrientation = DisplayUtils.getCurrentScreenOrientation(getContext());
            requireActivity().setRequestedOrientation(screenOrientation);

            gameSettings = new SettingsFormData();
        } else {
            onRestoreInstanceState(savedInstanceState);
        }
        CLogger.i(TAG, "onCreate");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        CLogger.i(TAG, "onSaveInstanceState");
        outState.putParcelable(STATE_KEY_GAME, game);
        outState.putParcelable(STATE_KEY_GAME_SETTINGS, gameSettings);
        outState.putInt(STATE_KEY_SCREEN_ORIENTATION, screenOrientation);
        outState.putLong(STATE_KEY_REMAINING_TIME, remainingTimeMillis); // 合并保存时间
        super.onSaveInstanceState(outState);
    }

    private void onRestoreInstanceState(Bundle savedState) {
//        super.onRestoreInstanceState(savedState);
        if (savedState == null) {
            return;
        }
        CLogger.i(TAG, "onRestoreInstanceState");

        screenOrientation = savedState.getInt(STATE_KEY_SCREEN_ORIENTATION);
        if (screenOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            // 确定并锁定当前屏幕方向
            screenOrientation = DisplayUtils.getCurrentScreenOrientation(getContext());
            requireActivity().setRequestedOrientation(screenOrientation);
        }

        gameSettings = savedState.getParcelable(STATE_KEY_GAME_SETTINGS);

        game = savedState.getParcelable(STATE_KEY_GAME);
        if (game != null) {
            Runnable task = () -> {
                CLogger.i(TAG, "Creating game metrics & view...");
                // 重新创建 game metrics...
                gameMetrics = new GameMetrics(gameContainer, gameSettings);
                // ...设定游戏...
                game.setMetrics(gameMetrics);
                // ...并创建游戏视图
                createGameView();
            };
            runOnUiThread(task);
        }
        remainingTimeMillis = savedState.getLong(STATE_KEY_REMAINING_TIME, 60000);
        CLogger.d(TAG, "恢复时间剩余：" + remainingTimeMillis + "ms");
        if (game != null && game.getState() == GameState.PLAYING) {
            CLogger.d(TAG, "恢复进行中的游戏倒计时");
            startTimer(remainingTimeMillis); // 恢复倒计时
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 创建你的游戏视图
        View view = inflater.inflate(R.layout.fragment_maze, container, false);
        // 初始化游戏逻辑...
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        gameContainer = view.findViewById(R.id.gameContainer);
        initViews(view);
        CLogger.i(TAG, "onViewCreated");
        // 如果这是应用程序的首次启动或没有游戏被保留...
        if (game == null) {
            // ...请求创建新游戏
            createGame();
        }
    }

    private void initViews(View view) {
        View settingsBtn = view.findViewById(R.id.settingBtn);
        View nextBtn = view.findViewById(R.id.nextBtn);
        scoreTv = view.findViewById(R.id.scoreTv);
        AtomicInteger count = new AtomicInteger(1);
        scoreTv.setOnClickListener(v -> {
            // 检查是否可以激活无视墙壁模式
            if (!gameController.isWallIgnoreModeActive() && score >= 20 && count.get() >=1) {
                count.getAndDecrement();
                // 消耗积分
                score -= 20;
                scoreTv.setText(String.format(SCORE_FORMAT, score));

                // 激活无视墙壁状态
                gameController.activateWallIgnoreMode();

                // 视觉反馈
                scoreTv.setBackgroundColor(Color.YELLOW);
                scoreTv.setTextColor(Color.RED);

                // 设置恢复原始样式的定时器
                new Handler().postDelayed(() -> {
                    scoreTv.setBackgroundColor(Color.TRANSPARENT);
                    scoreTv.setTextColor(Color.BLACK);
                }, 300);
            }
        });

        settingsBtn.setOnClickListener(v -> showSettingsWindow());
        nextBtn.setOnClickListener(v -> {
            if (!onNextGame()) goNextGame();
        });

        //scoreTv.setText(String.format(SCORE_FORMAT, score));
        timeRemainingTv = view.findViewById(R.id.timeRemainingTv);
        updateTimeDisplay(remainingTimeMillis);

        Button btnLeaderboard = view.findViewById(R.id.btn_leaderboard);
        btnLeaderboard.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LeaderboardActivity.class);
            startActivity(intent);
        });

    }

    private void startTimer(long durationMillis) {
        CLogger.d(TAG, "启动倒计时，剩余时间：" + durationMillis + "ms");
        cancelTimer();
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> { //  强制在 UI 线程
            gameTimer = new CountDownTimer(durationMillis, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    CLogger.d(TAG, "onTick: " + millisUntilFinished);
                    remainingTimeMillis = millisUntilFinished;
                    updateTimeDisplay(millisUntilFinished);
                }

                @Override
                public void onFinish() {
                    remainingTimeMillis = 0;
                    updateTimeDisplay(0);
                    handleTimeUp();
                }
            }.start();
            CLogger.d(TAG, "倒计时对象已创建: " + gameTimer.hashCode());
        });
    }

    private void cancelTimer() {
        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer = null;
        }
    }

    private void updateTimeDisplay(long millis) {
        long seconds = millis / 1000;
        String timeText = String.format("Time: %d", seconds);
        timeRemainingTv.setText(timeText);
        timeRemainingTv.invalidate(); //  强制重绘
        CLogger.d(TAG, "更新时间显示：" + timeText);
    }

    private void handleTimeUp() {
        if (gameController != null) {
            gameController.notifyGameFinished(false); // 时间耗尽触发失败
            showGameOverDialog(false);
        }
    }

    // 钩子函数
    protected boolean onNextGame() {
        return false;
    }

    protected void goNextGame() {
        if (game != null) {
            game.clearPath(); // 清除旧轨迹
        }
        remainingTimeMillis = 15000; // 重置时间
        if (isVisible()) createGame();
    }

    @Override
    public void onStop() {
        super.onStop();
        CLogger.i(TAG, "onStop");
        cancelTimer();
        // 关闭设置弹窗...
        if (settingsWindow != null) {
            settingsWindow.dismiss();
            settingsWindow = null;
        }
    }


    // 游戏难度设置
    private void showSettingsWindow() {
        CLogger.i(TAG, "showSettingsWindow");
        // 如果尚未可见...
        if (settingsWindow == null) {
            // 暂停游戏并保留旧的游戏状态
            final GameState oldGameState = game.getState();
            gameController.setGameState(GameState.PAUSED);
            // 创建新的设置弹窗
            View content = getLayoutInflater().inflate(R.layout.settings_window, null);
            settingsWindow = new SettingsWindow(content, gameSettings);
            // 确定按钮
            Button okButton = content.findViewById(R.id.okButton);
            okButton.setOnClickListener(v -> {
                // 确保确实存在一个设置窗口实例
                if (settingsWindow != null) {
                    // 关闭设置窗口
                    settingsWindow.dismiss();
                    settingsWindow = null;
//                        updateMenu();
                    // 恢复游戏
                    gameView.setInBackground(false);
                    if (game.getState() == GameState.READY) {
                        gameController.setGameState(GameState.PLAYING);
                    } else {
                        gameController.setGameState(oldGameState);
                    }
                }
            });
            // 游戏视图
            gameView.setInBackground(true);
            // 显示设置弹窗
            settingsWindow.showAtLocation(getView(), Gravity.BOTTOM, 0, 0);

            settingsWindow.setOnGameSettingsChangedListener(settings -> {
                // 更新预览时间
                updateTimePreview(settings.getMazeSize());
            });
            // 初始预览时间
            updateTimePreview(gameSettings.getMazeSize());
        }
    }
    private void updateTimePreview(float sizeFactor) {
        long newTime = calculateInitialTime();
        String previewText = "新游戏时间: " + (newTime / 1000) + "秒";

        if (settingsWindow != null) {
            settingsWindow.updateTimePreview(previewText);
        }
    }
    /* --- 游戏操控 ------------------------------------------- */

    private void createGame() {
        CLogger.i(TAG, "createGame");

        // 放弃旧游戏（如果有的话）
        if (gameController != null) {
            gameController.setGameState(GameState.PAUSED);
        }
        game = null;

        if (settingsWindow != null) {
            settingsWindow.setMazeSizeProgress(gameSettings.getMazeSize());
        }

        remainingTimeMillis = calculateInitialTime();
        CLogger.d(TAG, "新游戏初始时间: " + remainingTimeMillis + "ms (复杂度: " + gameSettings.getMazeSize() + ")");

        // 当前屏幕方向
        int currentScreenOrientation = DisplayUtils
                .getCurrentScreenOrientation(getContext());

        // 如果设置窗口尚未可见...
        if (settingsWindow == null) {
            // 锁定当前屏幕方向
            requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
            // 确定并锁定当前屏幕方向
            screenOrientation = DisplayUtils.getCurrentScreenOrientation(getContext());
            requireActivity().setRequestedOrientation(screenOrientation);
        }

        // 如果屏幕方向没有改变...
        if (screenOrientation == currentScreenOrientation) {
            Runnable task = () -> {
                if (getContext() == null || getActivity() == null) {
                    CLogger.d(TAG, "无法创建游戏：Context 或 Activity 为 null");
                    return;
                }
                CLogger.i(TAG, "Creating game...");
                // 确定游戏参数指标
                gameMetrics = new GameMetrics(gameContainer, gameSettings);
                // 创建游戏
                game = GameFactory.createGame(gameMetrics);
                // 创建游戏示图
                createGameView();

                gameController = new GameController(getContext(), game, gameView);
                gameView.setGameController(gameController);
                //gameController.setGameState(GameState.PLAYING);

                // todo
                gameController.setOnGameStateChangedListener(new GameController.OnGameStateChangedListener() {
                    @Override
                    public void onGameStarted() {
                        startTimer(remainingTimeMillis);
                    }

                    @Override
                    public void onGameFinished(boolean isSuccess) {
                        cancelTimer();
                        if (isSuccess) {
                            score += Math.max(scoreStep, 10);
                            scoreTv.setText(String.format(SCORE_FORMAT, score));
                            showSuccessDialog(); //  显示成功通关对话框
                        } else {
                            showGameOverDialog(false);
                        }
                    }
                });
                if (settingsWindow == null) {
                    gameController.setGameState(GameState.PLAYING); // 触发状态变更
                }
            };
            runOnUiThread(task);

        } else {
            // 游戏将在 Activity重新创建期间创建
        }

    }

    private void createGameView() {
        CLogger.i(TAG, "createGameView");
        // 如果有旧的游戏视图
        if (gameView != null) {
            final ViewGroup gc = gameContainer;
            final View gv = gameView;
            if (createMazeAnimation != null && !createMazeAnimation.hasEnded()) {
                CLogger.d(TAG, "stopping createMazeAnimation");
                createMazeAnimation.cancel();
            }
            destroyMazeAnimation = AnimationUtils.loadAnimation(getContext(),
                    R.anim.destroy_maze);
            CLogger.d(TAG, "starting destroyMazeAnimation");
            destroyMazeAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    Runnable task = () -> {
                        CLogger.d(TAG, "destroyMazeAnimation finished, removing old game view");
                        gc.removeView(gv);
                    };
                    runOnUiThread(task);
                }
            });
            gameView.startAnimation(destroyMazeAnimation);
        }
        // 创建新的游戏视图
        gameView = new GameView(getContext(), game);
        // gameView.setVisibility(View.GONE);
        gameContainer.addView(gameView, 0);

        CLogger.d(TAG, "starting createMazeAnimation");
        createMazeAnimation = AnimationUtils.loadAnimation(getContext(),
                R.anim.create_maze);
        gameView.startAnimation(createMazeAnimation);
    }

    /* --- 游戏设置处理 --- */
    private void onGameSettingsChanged() {
        CLogger.i(TAG, "onGameSettingsChanged");
        // 仅在游戏的参数指标发生变化时才创建新游戏
        GameMetrics newGameMetrics = new GameMetrics(gameContainer,
                gameSettings);
        if (!newGameMetrics.equals(gameMetrics)) {
            updateScoreStep();
            createGame();
        }
    }

    protected void runOnUiThread(Runnable task) {
        if (null != gameContainer) gameContainer.post(task);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelTimer();
        if (gameController != null) {
            gameController.release();
        }
    }

    private boolean isDialogShowing = false;

    private void showGameOverDialog(boolean isSuccess) {
        if (isDialogShowing) return;
        isDialogShowing = true;
        int penalty =  scoreStep;
        // 游戏失败时的特殊处理
        if (!isSuccess) {
            // 减去双倍积分（不低于0）

            score = Math.max(0, score - penalty);
            // 难度降低一级
            decreaseMazeSize();
            // 更新分数显示
            scoreTv.setText(String.format(SCORE_FORMAT, score));
        }

        saveScore(score);

        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(isSuccess ? "恭喜通关！" : "挑战失败");
            String message = isSuccess
                    ? "你成功找到了出口！"
                    : String.format("扣除%d积分\n当前积分：%d\n难度已降低一级", penalty, score);
            builder.setMessage(message);

            // 退出游戏按钮
            builder.setNegativeButton("退出游戏", (dialog, which) -> {
                if (getActivity() != null) {
                    getActivity().finish();
                }
            });

            // 新游戏按钮
            builder.setPositiveButton("新游戏", (dialog, which) -> {
                //score = 0;
                remainingTimeMillis = calculateInitialTime();
                scoreTv.setText(String.format(SCORE_FORMAT, score));
                createGame();
            });

            builder.setCancelable(false);

            //  正确获取 dialog 并设置监听器
            AlertDialog dialog = builder.create();
            dialog.setOnDismissListener(d -> isDialogShowing = false);
            dialog.show();
        });
    }
    private void showSuccessDialog() {
        if (getActivity() == null) return;
        saveScore(score);

        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("恭喜通关！");
            builder.setMessage("你成功找到了出口！\n当前分数：" + score);

            // 退出游戏按钮
            builder.setNegativeButton("退出游戏", (dialog, which) -> {
                if (getActivity() != null) {
                    getActivity().finish();
                }
            });

            // 新游戏按钮
            builder.setNeutralButton("新游戏", (dialog, which) -> {
                score = 0; // 重置分数
                remainingTimeMillis = 15000; // 重置时间
                scoreTv.setText(String.format(SCORE_FORMAT, 0));
                createGame(); // 重启游戏
            });

            // 继续游戏按钮（保留分数）
            builder.setPositiveButton("继续游戏", (dialog, which) -> {
                // 保留当前分数，只重置时间
                increaseMazeSize();
                remainingTimeMillis = calculateInitialTime();
                createGame(); // 创建新关卡
            });

            builder.setCancelable(false);
            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    private void increaseMazeSize() {
        float currentSize = gameSettings.getMazeSize();
        // 每次增加 5%，最大不超过 100%
        float newSize = Math.min(1.0f, currentSize + 0.05f);
        gameSettings.setMazeSize(newSize);

        // 更新计分步长
        updateScoreStep();

        // 更新 SeekBar 显示
        if (settingsWindow != null) {
            settingsWindow.setMazeSizeProgress(newSize);
        }

        CLogger.i(TAG, "迷宫尺寸增加至: " + newSize);
    }
    private void decreaseMazeSize() {
        float currentSize = gameSettings.getMazeSize();
        // 每次降低5%，最小不低于10%
        float newSize = Math.max(0.1f, currentSize - 0.05f);
        gameSettings.setMazeSize(newSize);

        // 更新计分步长
        updateScoreStep();

        // 更新设置窗口显示
        if (settingsWindow != null) {
            settingsWindow.setMazeSizeProgress(newSize);
        }

        CLogger.i(TAG, "迷宫尺寸降低至: " + newSize);
    }
    private void updateScoreStep() {
        int standard = (int) (100 * gameSettings.getMazeSize());
        scoreStep = standard > 19 ? 10 * (standard / 10) : 10;
        CLogger.i(TAG, "更新计分步长: " + scoreStep);
    }
    // 在游戏结束时保存分数
    private void saveScore(int score) {
        Context context = getContext();
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_SCORES, Context.MODE_PRIVATE);
        int highScore = prefs.getInt(currentUsername, 0);

        // 只保存最高分
        if (score > highScore) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(currentUsername, score);
            editor.apply();
        }
    }

}



