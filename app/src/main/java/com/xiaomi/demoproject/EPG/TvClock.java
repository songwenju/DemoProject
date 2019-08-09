package com.xiaomi.demoproject.EPG;

import android.content.Context;
import android.os.SystemClock;

public class TvClock {

    public TvClock(Context context) {

    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();

    }

    public long elapsedRealtime() {
        return SystemClock.elapsedRealtime();
    }

    public long uptimeMillis() {
        return SystemClock.uptimeMillis();
    }

    public void sleep(long ms) {
        SystemClock.sleep(ms);
    }
}
