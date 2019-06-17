package com.xiaomi.demoproject;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;

public class DialogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dailog1);
        float pxDimension = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, 385,
                getResources().getDisplayMetrics());

        LogUtil.i(this,"DialogActivity.onCreate:"+pxDimension);

    }
}
