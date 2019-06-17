package com.xiaomi.demoproject;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class ToastActivity extends AppCompatActivity {
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_toast);
        showToast();
        getFormatTime(1093876200000L);
        getFormatTime(1093878000000L);

        LogUtil.i(this,"ToastActivity.onCreate："+System.currentTimeMillis());
        String string = getTodayDateTime();
        LogUtil.i(this,"ToastActivity.onCreate:"+string);
    }

    public String getFormatTime(Long time) {

        if (time == null) {
            return "";
        }
//        time = time*1000;
        String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(time));   // 时间戳转换成时间

        LogUtil.i(this, "ToastActivity.getFormatTime.timeStr:" + timeStr);

        return timeStr;
    }

    public static String formatData(String dataFormat, long timeStamp) {
        if (timeStamp == 0) {
            return "";
        }
        timeStamp = timeStamp * 1000;
        String result = "";
        SimpleDateFormat format = new SimpleDateFormat(dataFormat);
        result = format.format(new Date(timeStamp));
        return result;
    }


        public static String getTodayDateTime() {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault());
            return format.format(new Date());
        }





    private void showToast() {
        Boolean isHDMI = false;
        Toast toast = new Toast(mContext);
        View view = LayoutInflater.from(mContext).inflate(R.layout.nav_simple_info_layout, null);
        View avLayout = view.findViewById(R.id.av_layout);
        View hdmiLayout = view.findViewById(R.id.hdmi_layout);
        View resolutionLayout = view.findViewById(R.id.resolution_layout);
        TextView hdmiName = (TextView) view.findViewById(R.id.hdmi_name);
        if (isHDMI) {
            avLayout.setVisibility(INVISIBLE);
            hdmiLayout.setVisibility(VISIBLE);
            resolutionLayout.setVisibility(INVISIBLE);
            hdmiName.setText("HDMI 2");
        } else {
            avLayout.setVisibility(VISIBLE);
            hdmiLayout.setVisibility(INVISIBLE);
            resolutionLayout.setVisibility(INVISIBLE);
        }

        toast.setView(view);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 50);
        toast.show();
    }
}
