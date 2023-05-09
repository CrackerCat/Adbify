package com.adbify.utils;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class AndroidUtilities {
    private static WeakReference<Toast> sToast;

    public static void closeQuietly(final Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (final IOException ignored) { // NOPMD NOSONAR
            }
        }
    }

    public static boolean isServiceRunning(Context context, Class<?> clazz) {
        ActivityManager manager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (clazz.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void killServiceIfRunning(Context context, Class<?> clazz) {
        Intent intent = new Intent(context, clazz);
        if (isServiceRunning(context, clazz)) {
            context.stopService(intent);
        }
    }

    public static int getPid(Process p) {
        try {
            Field f = p.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            try {
                return f.getInt(p);
            } finally {
                f.setAccessible(false);
            }
        } catch (Throwable e) {
            return -1;
        }
    }

    public static float dpToPx(Context context, float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static float pxToDp(Context context, float px) {
        return px / context.getResources().getDisplayMetrics().density;
    }

    public static void setLayoutMarginsInDp(
            @NonNull View view, int left, int top, int right, int bottom) {
        Context context = view.getContext();
        setLayoutMarginsInPixels(
                view,
                (int) dpToPx(context, left),
                (int) dpToPx(context, top),
                (int) dpToPx(context, right),
                (int) dpToPx(context, bottom));
    }

    public static void setLayoutMarginsInPixels(
            @NonNull View view, int left, int top, int right, int bottom) {
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            params.setMargins(left, top, right, bottom);
            view.setLayoutParams(params);
        }
    }

    public static Drawable getDrawable(Context context, @DrawableRes int drawable) {
        return ResourcesCompat.getDrawable(
                Objects.requireNonNull(context).getResources(),
                drawable,
                Objects.requireNonNull(context).getTheme());
    }

    public static void showSoftKeyboard(View view) {
        if (view == null) return;
        try {
            InputMethodManager inputManager =
                    (InputMethodManager)
                            view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hideSoftKeyboard(View view) {
        if (view == null) return;
        try {
            InputMethodManager imm =
                    (InputMethodManager)
                            view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (!imm.isActive()) {
                return;
            }
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void toast(Context context, CharSequence msg) {
        toast(context, msg, Toast.LENGTH_SHORT);
    }

    public static void toastLong(Context context, CharSequence msg) {
        toast(context, msg, Toast.LENGTH_LONG);
    }

    public static void toast(@NonNull Context context, CharSequence msg, int duaration) {
        if (msg == null || duaration == -1) {
            return;
        }
        if (sToast != null && sToast.get() != null) {
            sToast.get().cancel();
        }
        Toast mToast =
                Toast.makeText(
                        context,
                        msg,
                        duaration);
        mToast.show();
        sToast = new WeakReference<>(mToast);
    }

    public static byte[] getStringBytes(String src) {
        try {
            return src.getBytes(StandardCharsets.UTF_8);
        } catch (Exception ignore) {

        }
        return new byte[0];
    }

    @SuppressLint("PrivateApi")
    public static String getSystemProperty(String key) {
        try {
            Class<?> props = Class.forName("android.os.SystemProperties");
            return (String) props.getMethod("get", String.class).invoke(null, key);
        } catch (Exception ignore) {

        }
        return null;
    }
}
