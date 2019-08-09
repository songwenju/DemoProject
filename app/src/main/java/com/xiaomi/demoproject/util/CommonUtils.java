
package com.xiaomi.demoproject.util;

import android.os.Build;
import android.util.ArraySet;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

/** Util class for common use in TV app and inputs. */
@SuppressWarnings("AndroidApiChecker") // TODO(b/32513850) remove when error prone is updated
public final class CommonUtils {
    private static final String TAG = "CommonUtils";
    private static final ThreadLocal<SimpleDateFormat> ISO_8601 =
            new ThreadLocal() {
                private final SimpleDateFormat value =
                        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);

                @Override
                protected SimpleDateFormat initialValue() {
                    return value;
                }
            };
    // Hardcoded list for known bundled inputs not written by OEM/SOCs.
    // Bundled (system) inputs not in the list will get the high priority
    // so they and their channels come first in the UI.
    private static final Set<String> BUNDLED_PACKAGE_SET = new ArraySet<>();

    static {
        BUNDLED_PACKAGE_SET.add("com.android.tv");
    }

    private static Boolean sRunningInTest;

    private CommonUtils() {}


    /**
     * Checks if this application is running in tests.
     *
     * <p>{@link android.app.ActivityManager#isRunningInTestHarness} doesn't return {@code true} for
     * the usual devices even the application is running in tests. We need to figure it out by
     * checking whether the class in tv-tests-common module can be loaded or not.
     */
    public static synchronized boolean isRunningInTest() {
        if (sRunningInTest == null) {
            try {
                Class.forName("com.android.tv.testing.utils.Utils");
                Log.i(
                        TAG,
                        "Assumed to be running in a test because"
                                + " com.android.tv.testing.utils.Utils is found");
                sRunningInTest = true;
            } catch (ClassNotFoundException e) {
                sRunningInTest = false;
            }
        }
        return sRunningInTest;
    }

    /** Checks whether a given package is in our bundled package set. */
    public static boolean isInBundledPackageSet(String packageName) {
        return BUNDLED_PACKAGE_SET.contains(packageName);
    }

    /** Checks whether a given input is a bundled input. */
    public static boolean isBundledInput(String inputId) {
        for (String prefix : BUNDLED_PACKAGE_SET) {
            if (inputId.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }


    /** Converts time in milliseconds to a ISO 8061 string. */
    public static String toIsoDateTimeString(long timeMillis) {
        return ISO_8601.get().format(new Date(timeMillis));
    }

    /** Deletes a file or a directory. */
    public static void deleteDirOrFile(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteDirOrFile(child);
            }
        }
        fileOrDirectory.delete();
    }

    public static boolean isRoboTest() {
        return "robolectric".equals(Build.FINGERPRINT);
    }
}
