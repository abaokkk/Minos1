package funs.gamez.view.render;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.List;

import funs.common.tools.CLogger;
import funs.gamez.minos.R;
import funs.gamez.model.Cell;
import funs.gamez.model.Game;
import funs.gamez.model.GameState;
import funs.gamez.view.Coords;

// 游戏画面渲染器
public class GameRenderer {

    private static final String TAG = "GameRenderer";

    public static final float REL_DESTINATION_SIZE = 0.7f;
    public static final float REL_PLAYER_SIZE = 0.9f;

    private final Context context;
    private final Game game;

    private final Paint wallPaint;
    private final Paint destinationPaint;
    private final Paint overlayPaint;


    private static final int TRAIL_COLOR = Color.argb(128, 255, 255, 0); // 半透明黄色
    private static final float TRAIL_WIDTH = 5f;

    
    public GameRenderer(Context context, Game game) {
        this.context = context;
        this.game = game;
        
        wallPaint = new Paint();
        wallPaint.setStyle(Paint.Style.FILL);
        wallPaint.setColor(context.getResources().getColor(
                R.color.maze_wall));

        destinationPaint = new Paint();
        destinationPaint.setStyle(Paint.Style.STROKE);
        destinationPaint.setStrokeWidth(2 * game.getMetrics()
                .getWallThickness());
        destinationPaint.setColor(context.getResources().getColor(
                R.color.destination));
        
        overlayPaint = new Paint();
        overlayPaint.setStyle(Paint.Style.FILL);
        overlayPaint.setColor(context.getResources().getColor(R.color.overlay));
        
    }

    public void render(Canvas canvas) {
        CLogger.d(TAG, "render");

        // 渲染迷宫墙壁，砌墙
        renderWalls(canvas);
        // 渲染迷宫目的地
        renderDestination(canvas);
        // 渲染 受用户拖动的圆形球
        renderPlayer(canvas);

        // 创建画笔
        Paint trailPaint = new Paint();
        trailPaint.setColor(TRAIL_COLOR);
        trailPaint.setStrokeWidth(TRAIL_WIDTH);
        trailPaint.setStyle(Paint.Style.STROKE);

// 获取路径并逐段绘制
        List<Coords> path = game.getMovementPath();

        int wallThickness = game.getMetrics().getWallThickness();
        int offset = wallThickness + game.getMetrics().getCellSize() / 2;

        for (int i = 1; i < path.size(); i++) {
            // 设置透明度渐变效果
            float alpha = 0.2f + 0.8f * (i / (float) path.size());
            trailPaint.setAlpha((int)(alpha * 255));

            Coords prev = path.get(i - 1);
            Coords curr = path.get(i);

            // 计算实际屏幕坐标（考虑墙壁厚度偏移）
            float startX = offset + prev.getX() * game.getMetrics().getCellSize();
            float startY = offset + prev.getY() * game.getMetrics().getCellSize();
            float endX = offset + curr.getX() * game.getMetrics().getCellSize();
            float endY = offset + curr.getY() * game.getMetrics().getCellSize();

            canvas.drawLine(startX, startY, endX, endY, trailPaint);
        }

    }

    private void renderWalls(Canvas canvas) {
        int wallThickness = game.getMetrics().getWallThickness(); // 墙壁厚度
        int cellSize = game.getMetrics().getCellSize(); // 单元格尺寸

        // 渲染迷宫四周(最外围的)墙壁
        RenderUtils.drawWalls(canvas, 15, 0, 0, canvas.getWidth() - 1,
                canvas.getHeight() - 1, wallThickness, wallPaint);

        final int gRows = game.getMaze().getRows();
        for (int row = 0; row < gRows; row++) { // row < game.getMaze().getRows();

            int gCols = game.getMaze().getCols();
            for (int col = 0; col < gCols; col++) { // col < game.getMaze().getCols();
                int left = wallThickness + cellSize * col;
                int top = wallThickness + cellSize * row;
                Cell cell = game.getMaze().getCell(col, row);
                RenderUtils.drawWalls(canvas, cell.getWalls(), left, top,
                        cellSize, cellSize, wallThickness, wallPaint);
            }
        }
    }

    private void renderDestination(Canvas canvas) {
        int cx = Math.round(game.getMetrics().getWallThickness()
                + (float) game.getMetrics().getCellSize()
                * (game.getMetrics().getDestinationPosition().getCol() + 0.5f));
        int cy = Math.round(game.getMetrics().getWallThickness()
                + (float) game.getMetrics().getCellSize()
                * (game.getMetrics().getDestinationPosition().getRow() + 0.5f));
        int r = Math.round(REL_DESTINATION_SIZE
                * (game.getMetrics().getCellSize() / 2f - game.getMetrics()
                        .getWallThickness()));

        canvas.drawLine(cx - r, cy - r, cx + r, cy + r, destinationPaint);
        canvas.drawLine(cx - r, cy + r, cx + r, cy - r, destinationPaint);
        int r0 = r/2 ;//(int)( Math.sqrt(2) *r);
        canvas.drawCircle(cx, cy, r0,destinationPaint);
    }

    private void renderPlayer(Canvas canvas) {
        Paint playerPaint = new Paint();
        playerPaint.setStyle(Paint.Style.FILL);
        playerPaint.setColor(context.getResources().getColor(
                game.getState() == GameState.FINISHED ? R.color.player_finished
                        : R.color.player));

        int cx = Math.round(game.getMetrics().getWallThickness()
                + (float) game.getMetrics().getCellSize()
                * (game.getPlayer().getX() + 0.5f));
        int cy = Math.round(game.getMetrics().getWallThickness()
                + (float) game.getMetrics().getCellSize()
                * (game.getPlayer().getY() + 0.5f));
        int r = Math.round(REL_PLAYER_SIZE
                * (game.getMetrics().getCellSize() / 2f - game.getMetrics()
                        .getWallThickness()));



        canvas.drawCircle(cx, cy, r, playerPaint);
    }

    public void renderOverlay(Canvas canvas) {
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), overlayPaint);
    }

}
