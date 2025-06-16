package funs.gamez.controller;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import funs.common.tools.CLogger;
import funs.gamez.minos.R;
import funs.gamez.model.Cell;
import funs.gamez.model.Direction;
import funs.gamez.model.Game;
import funs.gamez.model.GameState;
import funs.gamez.view.Coords;
import funs.gamez.view.GameView;
import funs.gamez.view.render.GameRenderer;

// 游戏控制器
public class GameController {

    private static final String TAG = "GameController";

    private Context context;
    private final Game game;
    private final GameView gameView;

    private final Vibrator vibrator;

    private boolean isDragging = false;
    private Coords screenDragStartPos;
    private Coords playerDragStartPos;
    private Coords lastPlayerDragPos;
    private int dragPointerId;

    private final MediaPlayer finishedPlayer;

    // 新增：无视墙壁模式相关变量
    private boolean wallIgnoreMode = false;
    private long wallIgnoreEndTime = 0;
    private final Handler handler = new Handler();
    private final Runnable endWallIgnoreRunnable = this::deactivateWallIgnoreMode;

    public GameController(Context context, Game game, GameView gameView) {
        this.context = context;
        this.game = game;
        this.gameView = gameView;

        this.vibrator = (Vibrator) context
                .getSystemService(Context.VIBRATOR_SERVICE);
        finishedPlayer = MediaPlayer.create(context, R.raw.win);
    }

    /* --- 操控"小球"拖放 -------------------------------------- */

    public boolean handleDrag(View view, MotionEvent ev) {

        if (game.getState() == GameState.PLAYING) {
            final int action = ev.getAction();

            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    int pointerIndex = ev.getActionIndex(); // 修正：使用 getActionIndex() 而不是直接使用 action
                    int pointerId = ev.getPointerId(pointerIndex);
                    float x = ev.getX(pointerIndex);
                    float y = ev.getY(pointerIndex);
                    if (playerContainsPoint(x, y)) {
                        isDragging = true;
                        dragPointerId = pointerId; // 使用正确的指针ID
                        screenDragStartPos = new Coords(x, y);
                        playerDragStartPos = createBracketedMazeCoords(
                                game.getPlayer().getX(),
                                game.getPlayer().getY()
                        );
                        lastPlayerDragPos = playerDragStartPos;
                        CLogger.i(TAG,
                                String.format(
                                        "Start dragging at screenDragStart=%s, playerDragStart=%s",
                                        screenDragStartPos, playerDragStartPos));
                        performInitDragFeedback();
                    }
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (isDragging) {
                        int pointerIndex = ev.findPointerIndex(dragPointerId);
                        if (pointerIndex == -1) {
                            // 如果找不到指针，可能是事件被取消
                            isDragging = false;
                            break;
                        }

                        float x = ev.getX(pointerIndex);
                        float y = ev.getY(pointerIndex);
                        Coords playerDelta = Coords.toMazeCoords(game.getMetrics(),
                                x - screenDragStartPos.getX(),
                                y - screenDragStartPos.getY());

                        Coords playerDragTargetPos = createBracketedMazeCoords(
                                playerDragStartPos.getX() + playerDelta.getX(),
                                playerDragStartPos.getY() + playerDelta.getY());

                        CLogger.i(TAG,
                                String.format(
                                        "Dragging to x=%s, y=%s, playerDragPos=%s, playerDelta=%s",
                                        x, y, playerDragTargetPos, playerDelta));

                        // 新增：无视墙壁模式下的特殊处理
                        if (wallIgnoreMode) {
                            // 在无视墙壁模式下，直接移动到目标位置
                            game.getPlayer().setX(playerDragTargetPos.getX());
                            game.getPlayer().setY(playerDragTargetPos.getY());
                            lastPlayerDragPos = playerDragTargetPos;
                        } else {
                            // 正常模式下的路径查找和碰撞检测
                            List<Cell> route = findRoute(lastPlayerDragPos, playerDragTargetPos);
                            Coords newPlayerDragPos = lastPlayerDragPos;
                            if (route.size() > 0) {
                                Cell dest = route.get(route.size() - 1);
                                newPlayerDragPos = createBracketedMazeCoords(
                                        game.getMaze().getCol(dest),
                                        game.getMaze().getRow(dest));
                            }
                            Cell playerCell = game.getMaze().getCell(
                                    newPlayerDragPos.getCol(),
                                    newPlayerDragPos.getRow());
                            Set<Direction> availableDirections = game.getMaze()
                                    .getConnectedNeighbours(playerCell).keySet();
                            newPlayerDragPos = createConstrainedMazeCoords(
                                    newPlayerDragPos, playerDragTargetPos,
                                    availableDirections);
                            game.getPlayer().setX(newPlayerDragPos.getX());
                            game.getPlayer().setY(newPlayerDragPos.getY());
                            lastPlayerDragPos = newPlayerDragPos;
                        }

                        // 检查是否到达终点
                        if (hasPlayerReachedDestination()) {
                            game.getPlayer().moveTo(game.getMetrics().getDestinationPosition());
                            setGameState(GameState.FINISHED);
                        }

                        gameView.invalidate();
                        game.addPathPoint(new Coords(
                                game.getPlayer().getX(),
                                game.getPlayer().getY()
                        ));
                    }
                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    game.clearPath();
                    isDragging = false;
                    break;
                }
                case MotionEvent.ACTION_POINTER_UP: {
                    final int pointerIndex = ev.getActionIndex();
                    final int pointerId = ev.getPointerId(pointerIndex);

                    if (pointerId == dragPointerId) {
                        isDragging = false;
                    }
                    break;
                }
            }

        }

        return true;
    }

    private boolean hasPlayerReachedDestination() {
        return game.getMetrics().cellContains(game.getMetrics().getDestinationPosition(),
                game.getPlayer().getX(), game.getPlayer().getY());
    }

    private List<Cell> findRoute(Coords start, Coords dest) {
        Cell currentCell = game.getMaze().getCell(start.getCol(),
                start.getRow());
        Cell destCell = game.getMaze().getCell(dest.getCol(), dest.getRow());
        List<Cell> route = new ArrayList<>();
        int dCols = dest.getCol() - start.getCol();
        int dRows = dest.getRow() - start.getRow();
        CLogger.i(TAG, String.format("findRoute: dCols=%s, dRows=%s", dCols, dRows));

        List<Direction> queryDirections = getQueryDirections(dCols, dRows);
        Map<Direction, Cell> candidates = game.getMaze()
                .getConnectedNeighbours(currentCell, queryDirections);

        while (currentCell != destCell && candidates.size() > 0) {
            currentCell = null;
            for (Direction queryDirection : queryDirections) {
                CLogger.i(TAG, String.format("  queryDirection=%s", queryDirection));
                if (candidates.containsKey(queryDirection)) {
                    currentCell = candidates.get(queryDirection);
                    route.add(currentCell);
                    dCols = dest.getCol() - game.getMaze().getCol(currentCell);
                    dRows = dest.getRow() - game.getMaze().getRow(currentCell);
                    queryDirections = getQueryDirections(dCols, dRows);
                    candidates = game.getMaze().getConnectedNeighbours(
                            currentCell, queryDirections);
                    break;
                }
            }
        }
        return route;
    }

    private List<Direction> getQueryDirections(int dCols, int dRows) {
        List<Direction> queryDirections = new ArrayList<>();
        if (Math.abs(dCols) > Math.abs(dRows)) {
            queryDirections.add(dCols > 0 ? Direction.EAST : Direction.WEST);
            if (dRows > 0) {
                queryDirections.add(Direction.SOUTH);
            } else if (dRows < 0) {
                queryDirections.add(Direction.NORTH);
            }
        } else if (dRows != 0) {
            queryDirections.add(dRows > 0 ? Direction.SOUTH : Direction.NORTH);
            if (dCols > 0) {
                queryDirections.add(Direction.EAST);
            } else if (dCols < 0) {
                queryDirections.add(Direction.WEST);
            }
        }
        return queryDirections;
    }

    /* --- 坐标工具函数 -------------------------------------------- */

    private boolean playerContainsPoint(float x, float y) {
        Coords mazeCoords = Coords.toMazeCoords(game.getMetrics(), x, y);
        float dx = game.getPlayer().getX() - mazeCoords.getX() + 0.5f;
        float dy = game.getPlayer().getY() - mazeCoords.getY() + 0.5f;
        return Math.sqrt(dx * dx + dy * dy) <= GameRenderer.REL_PLAYER_SIZE;
    }

    private Coords createBracketedMazeCoords(float col, float row) {
        if (col < 0) {
            col = 0f;
        } else if (col > game.getMaze().getCols() - 1) {
            col = game.getMaze().getCols() - 1;
        }
        if (row < 0) {
            row = 0f;
        } else if (row > game.getMaze().getRows() - 1) {
            row = game.getMaze().getRows() - 1;
        }
        return new Coords(col, row);
    }

    private Coords createConstrainedMazeCoords(Coords cellCoords,
                                               Coords pullCoords, Set<Direction> availableDirections) {
        float pullCol = pullCoords.getX() - cellCoords.getCol();
        float pullRow = pullCoords.getY() - cellCoords.getRow();
        float dCol = pullCol;
        float dRow = pullRow;
        if (!availableDirections.contains(pullCol > 0 ? Direction.EAST
                : Direction.WEST)) {
            dCol = 0;
        }
        if (!availableDirections.contains(pullRow > 0 ? Direction.SOUTH
                : Direction.NORTH)) {
            dRow = 0;
        }
        if (Math.abs(dCol) > Math.abs(dRow)) {
            dRow = 0;
        } else {
            dCol = 0;
        }
        return createBracketedMazeCoords(cellCoords.getCol() + dCol,
                cellCoords.getRow() + dRow);
    }

    /* --- 游戏状态处理 ------------------------------------- */

    public void setGameState(GameState state) {
        CLogger.d(TAG, "设置游戏状态：" + state);
        GameState oldState = game.getState();
        game.setState(state); // 先更新状态
        gameView.invalidate();//  强制重绘

        // 根据旧状态和新状态触发回调
        switch (oldState) {
            case READY:
                if (state == GameState.PLAYING) {
                    performGameStartedFeedback();
                }
                break;
            case PLAYING:
                if (state == GameState.FINISHED) {
                    performGameFinishedFeedback();
                }
                break;
        }

    }

    /* --- 处理反馈 --------------------------------------- */

    private void performInitDragFeedback() {
        vibrator.vibrate(100);
    }

    private void performWallHitFeedback() {
        vibrator.vibrate(20);
    }

    private void performGameStartedFeedback() {
        CLogger.d(TAG, "触发游戏开始反馈");
        //playAudio(startingPlayer);
        vibrator.vibrate(200);

        if (mListener != null) {
            mListener.onGameStarted();
        }
    }

    public void notifyGameFinished(boolean isSuccess) {
        if (mListener != null) {
            mListener.onGameFinished(isSuccess);
        }
    }

    private void performGameFinishedFeedback() {
        playAudio(finishedPlayer);
        vibrator.vibrate(200);
        notifyGameFinished(true); // 玩家成功到达终点
    }

    private void playAudio(MediaPlayer player) {
//        if (player.isPlaying()) {
//            player.stop();
//            player.seekTo(0);
//        }
        player.start();
    }

    public void release() {
        if (finishedPlayer != null) {
            finishedPlayer.release();
        }
        // 移除所有回调
        handler.removeCallbacks(endWallIgnoreRunnable);
    }

    /* --- 新增：无视墙壁模式功能 --------------------------------- */

    public void activateWallIgnoreMode() {
        wallIgnoreMode = true;
        wallIgnoreEndTime = System.currentTimeMillis() + 300;

        // 提供反馈
        vibrator.vibrate(100);

        // 设置定时器自动关闭
        handler.removeCallbacks(endWallIgnoreRunnable);
        handler.postDelayed(endWallIgnoreRunnable, 300);

        // 通知视图更新
        gameView.invalidate();
    }

    public void deactivateWallIgnoreMode() {
        wallIgnoreMode = false;
        // 通知视图更新
        gameView.invalidate();
    }

    public boolean isWallIgnoreModeActive() {
        return wallIgnoreMode;
    }

    public long getRemainingWallIgnoreTime() {
        return Math.max(0, wallIgnoreEndTime - System.currentTimeMillis());
    }

    /* --- 监听器接口 ------------------------------------- */

    private OnGameStateChangedListener mListener;
    public interface OnGameStateChangedListener {
        void onGameStarted();
        void onGameFinished(boolean isSuccess); // 新增参数区分成功/失败
    }

    public void setOnGameStateChangedListener(OnGameStateChangedListener mListener) {
        this.mListener = mListener;
    }
}