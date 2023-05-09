package com.adbify.terminal;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adbify.MainActivity;

public class TerminalSessionActivityClient extends TerminalSessionClientBase {
    private final MainActivity activity;

    public TerminalSessionActivityClient(MainActivity activity) {
        this.activity = activity;
    }

    public void onCreate() {
    }

    public void onStart() {
        if (activity.getTerminalService() != null) {
            setCurrentTerminalSession(getCurrentTerminalSession());
        }
    }

    public void onResume() {
    }

    public void onStop() {
    }

    @Override
    public void onTextChanged(@NonNull TerminalSession changedSession) {
        if (!activity.isVisible()) return;
        if (activity.getTerminalView() != null) activity.getTerminalView().onScreenUpdated();
    }

    @Override
    public void onTitleChanged(@NonNull TerminalSession updatedSession) {
    }

    @Override
    public void onSessionFinished(@NonNull TerminalSession finishedSession) {
        TerminalService service = activity.getTerminalService();
        if (service != null) {
            service.actionStopService();
        }
        activity.finishActivityIfNotFinishing();
    }

    @Override
    public void onCopyTextToClipboard(@NonNull TerminalSession session, String text) {
        if (!activity.isVisible()) return;
        ClipboardManager clipboard =
                (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(
                new ClipData(null, new String[]{"text/plain"}, new ClipData.Item(text)));
    }

    @Override
    public void onPasteTextFromClipboard(@Nullable TerminalSession session) {
        if (!activity.isVisible()) return;
        ClipboardManager clipboard =
                (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = clipboard.getPrimaryClip();
        if (clipData != null) {
            CharSequence paste = clipData.getItemAt(0).coerceToText(activity);
            if (!TextUtils.isEmpty(paste))
                activity.getTerminalView().mEmulator.paste(paste.toString());
        }
    }

    @Override
    public void onBell(@NonNull TerminalSession session) {
    }

    @Override
    public void onColorsChanged(@NonNull TerminalSession changedSession) {
    }

    @Override
    public void onTerminalCursorStateChange(boolean enabled) {
        if (enabled && !activity.isVisible()) {
            return;
        }
        activity.getTerminalView().setTerminalCursorBlinkerState(enabled, false);
    }

    @Override
    public void setTerminalShellPid(@NonNull TerminalSession terminalSession, int pid) {
    }

    @Override
    public Integer getTerminalCursorStyle() {
        return TerminalEmulator.DEFAULT_TERMINAL_CURSOR_STYLE;
    }

    public TerminalSession getCurrentTerminalSession() {
        TerminalService service = activity.getTerminalService();
        if (service == null) return null;
        return service.getOrCreateTerminalSession();
    }

    public void setCurrentTerminalSession(TerminalSession session) {
        if (session == null) return;
        TerminalSession current = activity.getTerminalView().getCurrentSession();
        if (current != null && current.mHandle.equals(session.mHandle)) {
            return;
        }
        activity.getTerminalView().attachSession(session);
        activity.getTerminalView().onScreenUpdated();
    }
}
