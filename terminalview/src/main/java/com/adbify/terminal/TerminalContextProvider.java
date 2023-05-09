package com.adbify.terminal;

import android.content.Context;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

public class TerminalContextProvider {
    private static WeakReference<Context> sContext;

    public static synchronized void init(@NonNull Context context) {
        sContext = new WeakReference<>(context);
    }

    public static Context getSContext() {
        if (sContext.get() == null) {
            throw new IllegalStateException("TerminalProvider  not initialized.");
        }
        return sContext.get();
    }
}
