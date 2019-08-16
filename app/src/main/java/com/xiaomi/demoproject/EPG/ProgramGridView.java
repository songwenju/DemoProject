/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaomi.demoproject.EPG;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import com.xiaomi.demoproject.LogUtil;

/**
 * A {@link VerticalGridView} for the program table view.
 */
public class ProgramGridView extends VerticalGridView {
    private static final String TAG = "ProgramGrid";
    public ProgramGridView(Context context) {
        super(context);
    }

    public ProgramGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProgramGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int y = this.getChildAt(0).getHeight();
        View focusView = this.getFocusedChild();
        View childView = null;
        LogUtil.i(TAG, "ProgramGrid.dispatchKeyEvent.focusView:" + focusView);
        if (focusView != null) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    LogUtil.i(TAG, "ProgramGrid.dispatchKeyEvent");
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        return true;
                    } else {
                        if (focusView instanceof ViewGroup) {
                            //child的child
                            childView = ((ViewGroup) focusView).getFocusedChild();
                            LogUtil.d(TAG, "upChildView:" + childView);
                        }
                        if (childView != null) {
                            View upView = FocusFinder.getInstance().findNextFocus(this, childView, View.FOCUS_UP);
                            LogUtil.d(TAG, "upView:" + upView);
                            if (upView == null) {
                                this.scrollBy(0, -y);
                                return true;
                            } else {
                                return super.dispatchKeyEvent(event);
                            }
                        }
                    }
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        return true;
                    } else {
                        if (focusView instanceof ViewGroup) {
                            childView = ((ViewGroup) focusView).getFocusedChild();
                            LogUtil.d(TAG, "downChildView:" + childView);
                        }
                        if (childView != null) {
                            View downView = FocusFinder.getInstance().findNextFocus(this, childView, View.FOCUS_DOWN);
                            LogUtil.d(TAG, "downView:" + downView);
                            if (downView == null) {
                                this.scrollBy(0, y);
                                return true;
                            } else {
                                return super.dispatchKeyEvent(event);
                            }
                        }
                    }
            }
        }
        return super.dispatchKeyEvent(event);
    }



    private void setViewFocus(int direct) {
        //响应五向键，在Scroll时去获得下一个焦点
        View focusView = this.getFocusedChild();
        View childView = null;

        if (focusView != null) {
            if (focusView instanceof ViewGroup) {
                childView = ((ViewGroup) focusView).getFocusedChild();
                LogUtil.i(TAG, "setViewFocus.scrollChildView:" + childView);
            }
            if (childView != null) {
                if (direct == KeyEvent.KEYCODE_DPAD_DOWN) {
                    View downView = FocusFinder.getInstance().findNextFocus(this, childView, View.FOCUS_DOWN);
                    if (downView != null) {
                        LogUtil.i(this, "setViewFocus.downView:" + downView);
                        downView.requestFocusFromTouch();
                    }
                } else if (direct == KeyEvent.KEYCODE_DPAD_UP) {
                    View upView = FocusFinder.getInstance().findNextFocus(this, childView, View.FOCUS_UP);
                    if (upView != null) {
                        upView.requestFocusFromTouch();
                    }
                }
            }
        }
    }
}
