package funs.gamez.view;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import funs.common.tools.CLogger;
import funs.gamez.minos.R;
import funs.gamez.model.SettingsFormData;
import funs.gamez.view.DisplayUtils;

public class SettingsWindow extends PopupWindow implements OnSeekBarChangeListener {

    private final String TAG = this.getClass().getSimpleName();

    private final SettingsFormData gameSettings;

    private final SeekBar mazeSizeSeekBar;
    private TextView timePreviewTv;

    // 1. 首先声明接口
    public interface OnGameSettingsChangedListener {
        void onChanged(SettingsFormData settings);
    }

    private OnGameSettingsChangedListener mListener;

    public SettingsWindow(View content, SettingsFormData gameSettings) {
        super(content);
        CLogger.i(TAG, "ctor");

        this.gameSettings = gameSettings;

        Context context = content.getContext();
        WindowManager wm = (WindowManager) content.getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point displaySize = new Point();
        display.getSize(displaySize);

        content.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        setWidth(Math.min(displaySize.x, displaySize.y));
        setHeight(content.getMeasuredHeight());


        mazeSizeSeekBar = (SeekBar)content.findViewById(R.id.mazeSize);
        timePreviewTv = (TextView) content.findViewById(R.id.timePreviewTv);
        mazeSizeSeekBar.setOnSeekBarChangeListener(this);
        mazeSizeSeekBar.setProgress((int)(100 * gameSettings.getMazeSize()));
    }

    /* --- OnSeekBarChangeListener implementation --- */

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        CLogger.d(TAG, "onProgressChanged");
        if (fromUser) {
            onGameSettingsChanged();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        CLogger.d(TAG, "onStartTrackingTouch");
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        CLogger.d(TAG, "onStopTrackingTouch");
        onGameSettingsChanged();
    }

    /* --- Intent handling ----------------------------------------- */

    private void onGameSettingsChanged() {
        CLogger.i(TAG, "onGameSettingsChanged");

        gameSettings.setMazeSize((float)mazeSizeSeekBar.getProgress()/100f);

        if (mListener != null) {
            mListener.onChanged(gameSettings);
        }
    }

    public void setOnGameSettingsChangedListener(OnGameSettingsChangedListener listener) {
        this.mListener = listener;
    }

    public void setMazeSizeProgress(float size) {
        int progress = (int)(100 * size);
        // 临时移除监听防止触发回调
        mazeSizeSeekBar.setOnSeekBarChangeListener(null);
        mazeSizeSeekBar.setProgress(progress);
        mazeSizeSeekBar.setOnSeekBarChangeListener(this);
        CLogger.d(TAG, "设置迷宫尺寸进度: " + progress);
    }

    // 更新时间预览文本
    public void updateTimePreview(String previewText) {
        if (timePreviewTv != null) {
            timePreviewTv.setText(previewText);
            CLogger.d(TAG, "更新时间预览: " + previewText);
        }
    }
}