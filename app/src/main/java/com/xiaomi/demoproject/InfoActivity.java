package com.xiaomi.demoproject;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class InfoActivity extends Activity {
    private static final String TAG = "InfoActivity";
    private LinearLayout mTopLayout;
    private LinearLayout mDetailLayout;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nav_banner_layout);
        mTopLayout = (LinearLayout) findViewById(R.id.banner_info_layout);
        mDetailLayout = (LinearLayout) findViewById(R.id.banner_detail_layout);
        TextView resolutionText = (TextView) findViewById(R.id.banner_video_format_eu);
        resolutionText.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                Log.i(TAG, "onSystemUiVisibilityChange: ");
            }
        });
        setResolution(resolutionText, "20:10");
        setSourceType("Satellite");
        showWindow();


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    showWindow();
                } else {
                    Toast.makeText(this, "ACTION_MANAGE_OVERLAY_PERMISSION权限已被拒绝", Toast.LENGTH_SHORT).show();
                    ;
                }
            }

        }
    }

    public void showWindow() {

        WindowManager mWmManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
        wmParams.type = android.view.WindowManager.LayoutParams.TYPE_PHONE; // 设置window type.
        wmParams.format = PixelFormat.TRANSPARENT; // 设置图片格式，效果为背景透明
        wmParams.flags = android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        wmParams.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL; // 调整悬浮窗口至右侧中间
        wmParams.x = 0;// 以屏幕左上角为原点，设置x、y初始值
        wmParams.y = 0;
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;// 设置悬浮窗口长宽数据
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;// 系统提示window
        TextView expandView = new TextView(mContext);
        expandView.setClickable(false);
        expandView.setFocusable(false);
        expandView.setTextSize(18);
        expandView.setSingleLine(true);
        expandView.setElevation(16);
        expandView.setBackgroundResource(R.color.key_back);
        mWmManager.addView(expandView, wmParams);


    }

    private void setSourceType(String type) {
        ImageView sourceView = (ImageView) findViewById(R.id.banner_simple_receiver_type);
        int resId = 0;
        switch (type) {
            case "Antenna":
                resId = R.drawable.type_antenna;
                break;
            case "Cable":
                resId = R.drawable.type_cable;
                break;

            case "Satellite":
                resId = R.drawable.type_satellite;
                break;

        }
        sourceView.setImageResource(resId);
    }

    private void setResolution(TextView textView, String resolution) {
        String[] strings = resolution.split(" ");
        String text = strings[0];
        textView.setText(text);
        if (strings.length == 2) {
            String type = strings[1];
            setResolutionType(textView, type);
        }
    }


    private void setResolutionType(TextView resolutionText, String type) {

        Drawable drawable = null;
        switch (type) {
            case "SD":
                drawable = getResources().getDrawable(R.drawable.type_sd);
                drawable.setBounds(-10, 0, 17, 8);
                break;
            case "FHD":
                drawable = getResources().getDrawable(R.drawable.type_fhd);
                drawable.setBounds(-10, 0, 17, 8);
                break;
            case "HD":
                drawable = getResources().getDrawable(R.drawable.type_hd);
                drawable.setBounds(-10, 0, 17, 8);
                break;

            case "UHD":
                drawable = getResources().getDrawable(R.drawable.type_uhd);
                drawable.setBounds(-10, 0, 17, 8);
                break;

        }
        resolutionText.setCompoundDrawables(null, null, drawable, null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                LogUtil.i(this, "InfoActivity.onKeyDown.KEYCODE_MENU");
                showDetailLayout();
                mDetailLayout.setVisibility(View.VISIBLE);
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showDetailLayout() {
        AnimatorSet animSet = new AnimatorSet();
        ObjectAnimator moveIn = ObjectAnimator.ofFloat(mTopLayout, "translationY", 214f, 0);
        animSet.play(moveIn);
        animSet.setDuration(600);
        animSet.start();
    }
}
