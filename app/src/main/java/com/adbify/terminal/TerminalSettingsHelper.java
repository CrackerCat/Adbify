package com.adbify.terminal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.adbify.utils.AndroidUtilities;

@SuppressLint("ApplySharedPref")
public class TerminalSettingsHelper {
    private static final String KEY_KEEP_SCREEN_ON = "key_keep_screen_on";
    private static final String KEY_FONT_SIZE = "key_font_size";

    private static int DEFAULT_FONT_SIZE;
    private static int MIN_FONT_SIZE;
    private static int MAX_FONT_SIZE;

    // term
    public static void changeFontSize(Context context, boolean increase) {
        int fontSize = getFontSize(context);
        fontSize += (increase ? 1 : -1) * 2;
        fontSize = Math.max(MIN_FONT_SIZE, Math.min(fontSize, MAX_FONT_SIZE));
        setFontSize(context, fontSize);
    }

    public static int getFontSize(Context context) {
        return getPreferences(context).getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
    }

    public static void setFontSize(Context context, int value) {
        SharedPreferences.Editor edit = getPreferences(context).edit();
        edit.putInt(KEY_FONT_SIZE, value);
        edit.commit();
    }

    public static boolean shouldKeepScreenOn(Context context) {
        return getPreferences(context).getBoolean(KEY_KEEP_SCREEN_ON, true);
    }

    public static void setKeepScreenOn(Context context, boolean value) {
        SharedPreferences.Editor edit = getPreferences(context).edit();
        edit.putBoolean(KEY_KEEP_SCREEN_ON, value);
        edit.commit();
    }

    public static SharedPreferences getPreferences(@NonNull Context context) {
        DEFAULT_FONT_SIZE = (int) AndroidUtilities.dpToPx(context, 12);
        MIN_FONT_SIZE = (int) AndroidUtilities.dpToPx(context, 8);
        MAX_FONT_SIZE = (int) AndroidUtilities.dpToPx(context, 20);
        return context.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }
}