package funs.gamez.model.factory;

import android.util.DisplayMetrics;
import android.view.View;

import funs.common.tools.CLogger;
import funs.gamez.minos.R;
import funs.gamez.model.SettingsFormData;
import funs.gamez.view.Coords;
import funs.gamez.view.DisplayUtils;

// 各种参数指标
public class GameMetrics {

    private final String TAG = "GameMetrics";
    
    public static final float MIN_CELL_SIZE_INCH = 0.1f;       //
    public static final int MIN_SHORT_EDGE_CELLS_COUNT = 8;

    private static final float LOG2 = (float) Math.log(2);
    private static final float LOG2_MIN_SHORT_EDGE_CELLS_COUNT = (float) Math
            .log(MIN_SHORT_EDGE_CELLS_COUNT) / LOG2 ;

    private int wallThickness = 1;
    private int cols = 4;
    private int rows = 4;
    private int cellSize = 40;

    /* --- Constructors -------------------------------------------- */

    public GameMetrics(View gameContainer, SettingsFormData gameSettings) {
        wallThickness = (int) gameContainer.getResources().getDimension(
                R.dimen.wall_thickness);

        int containerWidth = gameContainer.getWidth();
        int containerHeight = gameContainer.getHeight();
        DisplayMetrics displayMetrics = DisplayUtils
                .getDisplayMetrics(gameContainer.getContext());

        // 计算可用空间（减去墙壁厚度）
        int usableWidth = containerWidth - 2 * wallThickness;
        int usableHeight = containerHeight - 2 * wallThickness;

        // 计算物理尺寸（英寸）
        float absWidth = (float) usableWidth / displayMetrics.xdpi;
        float absHeight = (float) usableHeight / displayMetrics.ydpi;
        float absShort = Math.min(absWidth, absHeight);
        float aspectRatio = Math.max(absWidth, absHeight) / absShort;

        // 计算最大格子数（基于物理限制）
        int maxCells = (int) (absShort / MIN_CELL_SIZE_INCH);
        int minCells = MIN_SHORT_EDGE_CELLS_COUNT;

        // 线性插值计算实际格子数
        float targetShortCells = minCells + gameSettings.getMazeSize() * (maxCells - minCells);

        // 计算长边格子数（保持比例）
        float targetLongCells = targetShortCells * aspectRatio;

        // 确定行列数（四舍五入）
        if (absWidth < absHeight) {
            cols = Math.round(targetShortCells);
            rows = Math.round(targetLongCells);
        } else {
            cols = Math.round(targetLongCells);
            rows = Math.round(targetShortCells);
        }

        // 确保最小尺寸限制
        cols = Math.max(minCells, cols);
        rows = Math.max(minCells, rows);

        // 计算格子尺寸（取可用空间的最小适配值）
        int horizCellSize = usableWidth / cols;
        int vertCellSize = usableHeight / rows;
        cellSize = Math.min(horizCellSize, vertCellSize);

    }

    public Coords getPlayerStartPosition() {
        return new Coords(getCols() - 1, 0);
    }

    public Coords getDestinationPosition() {
        return new Coords(0, getRows() - 1);
    }

    public boolean cellContains(Coords p, float x, float y) {
        return cellContains(p.getCol(), p.getRow(), x, y);
    }

    public boolean cellContains(int col, int row, float x, float y) {
        return (Math.abs(col - x) <= 0.5) && (Math.abs(row - y) <= 0.5);
    }

    /* --- Getters / Setters --------------------------------------- */

    public int getWallThickness() {
        return wallThickness;
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    public int getCellSize() {
        return cellSize;
    }

    /* --- Other methods ------------------------------------------- */

    public int getWidth() {
        return cols * cellSize + 2 * wallThickness;
    }

    public int getHeight() {
        return rows * cellSize + 2 * wallThickness;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof GameMetrics) {
            GameMetrics other = (GameMetrics) o;
            return this.cellSize == other.cellSize && this.cols == other.cols
                    && this.rows == other.rows;
        } else {
            return false;
        }
    }
}
