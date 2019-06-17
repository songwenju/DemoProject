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
 * limitations under the License
 */

package com.xiaomi.demoproject;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.Map;
import java.util.Set;

/**
 * sharedPreferences的管理类
 */
public class SharedPreferencesUtils {
    private static SharedPreferences mSp;

    /**
     * 获得sharePreferences
     *
     * @return
     */
    public static SharedPreferences getInstance(Context context) {
        if (mSp == null) {
            mSp = context.getSharedPreferences("config", Context.MODE_PRIVATE);
        }
        return mSp;
    }

    /**
     * 使用sharedPreferences存入String类型的数据
     *
     * @param key   key
     * @param value value
     */
    public static void putString(Context context, String key, String value) {
        mSp = getInstance(context);
        if (!TextUtils.isEmpty(key)) {
            mSp.edit().putString(key, value).apply();
        }
    }


    /**
     * 使用sharedPreferences存入String类型的数据,并设置默认值
     *
     * @param key key
     * @return 获得的String类型的数据
     */
    public static String getString(Context context, String key) {
        mSp = getInstance(context);
        String result = null;
        if (!TextUtils.isEmpty(key)) {
            result = mSp.getString(key, null);
        }
        return result;
    }

    /**
     * 使用sharedPreferences存入String类型的数据,并设置默认值
     *
     * @param key          key
     * @param defaultValue defaultValue
     * @return 获得的String类型的数据
     */
    public static String getString(Context context, String key, String defaultValue) {
        mSp = getInstance(context);
        String result = defaultValue;
        if (!TextUtils.isEmpty(key)) {
            result = mSp.getString(key, defaultValue);
        }
        return result;
    }

    /**
     * 使用sharedPreferences存入boolean类型的数据
     *
     * @param key   key
     * @param value value
     */
    public static void putBoolean(Context context, String key, boolean value) {
        mSp = getInstance(context);
        if (!TextUtils.isEmpty(key)) {
            mSp.edit().putBoolean(key, value).apply();
        }
    }

    /**
     * 使用sharedPreferences存入boolean类型的数据,并设置默认值
     *
     * @param key          key
     * @param defaultValue defaultValue
     * @return 获得的boolean类型的数据
     */
    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        mSp = getInstance(context);
        boolean result = defaultValue;
        if (!TextUtils.isEmpty(key)) {
            result = mSp.getBoolean(key, defaultValue);
        }
        return result;
    }

    /**
     * 使用sharedPreferences存入int类型的数据
     *
     * @param key   key
     * @param value value
     */
    public static void putInt(Context context, String key, int value) {
        mSp = getInstance(context);
        if (!TextUtils.isEmpty(key)) {
            mSp.edit().putInt(key, value).apply();
        }
    }

    /**
     * 使用sharedPreferences获得的int类型的数据,并设置默认值
     *
     * @param key          key
     * @param defaultValue defaultValue
     * @return 获得的int类型的数据
     */
    public static long getLong(Context context, String key, long defaultValue) {
        mSp = getInstance(context);
        long result = defaultValue;
        if (!TextUtils.isEmpty(key)) {
            result = mSp.getLong(key, defaultValue);
        }
        return result;
    }

    /**
     * 使用sharedPreferences存入int类型的数据
     *
     * @param key   key
     * @param value value
     */
    public static void putLong(Context context, String key, long value) {
        mSp = getInstance(context);
        if (!TextUtils.isEmpty(key)) {
            mSp.edit().putLong(key, value).apply();
        }
    }

    /**
     * 使用sharedPreferences获得的int类型的数据,并设置默认值
     *
     * @param key          key
     * @param defaultValue defaultValue
     * @return 获得的int类型的数据
     */
    public static int getInt(Context context, String key, int defaultValue) {
        mSp = getInstance(context);
        int result = defaultValue;
        if (!TextUtils.isEmpty(key)) {
            result = mSp.getInt(key, defaultValue);
        }
        return result;
    }

    /**
     * 使用sharedPreferences存入set类型的数据
     *
     * @param context 上下文
     * @param key     key
     * @param value   value
     */
    public static void putStringSet(Context context, String key, Set<String> value) {
        mSp = getInstance(context);
        if (!TextUtils.isEmpty(key)) {
            mSp.edit().putStringSet(key, value).apply();
        }
    }

    /**
     * 使用sharedPreferences获得的set数据
     *
     * @param context context
     * @param key     key
     * @return
     */
    public static Set<String> getStringSet(Context context, String key) {
        mSp = getInstance(context);
        Set<String> set = null;
        if (!TextUtils.isEmpty(key)) {
            set = mSp.getStringSet(key, null);
        }
        return set;
    }

    /**
     * 使用sharedPreferences获得的set数据
     *
     * @param context      context
     * @param key          key
     * @param defaultValue defaultValue
     * @return
     */
    public static Set<String> getStringSet(Context context, String key, Set<String> defaultValue) {
        mSp = getInstance(context);
        Set<String> set = defaultValue;
        if (!TextUtils.isEmpty(key)) {
            set = mSp.getStringSet(key, defaultValue);
        }
        return set;
    }

    /**
     * 查询某个key是否已经存在
     *
     * @param key
     * @return
     */
    public static boolean contains(Context context, String key) {
        mSp = getInstance(context);
        return mSp.contains(key);
    }

    /**
     * 移除某个key值已经对应的值
     *
     * @param key
     */
    public static void remove(Context context, String key) {
        mSp = getInstance(context);

        mSp.edit().remove(key).apply();
    }

    /**
     * 清除所有数据
     */
    public static void clear(Context context) {
        mSp = getInstance(context);

        mSp.edit().clear().apply();
    }

    /**
     * 返回所有的键值对
     *
     * @return
     */
    public static Map<String, ?> getAll(Context context) {
        mSp = getInstance(context);
        return mSp.getAll();
    }
}
