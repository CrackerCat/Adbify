package com.adbify.terminal;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.adbify.MainActivity;
import com.adbify.R;
import com.adbify.utils.AndroidUtilities;
import com.adbify.utils.Utilities;

import java.util.Objects;

public class TerminalViewClient extends TerminalViewClientBase {
    final MainActivity activity;

    final TerminalSessionActivityClient terminalSessionActivityClient;

    private boolean terminalCursorBlinkerStateAlreadySet;

    public TerminalViewClient(
            MainActivity activity, TerminalSessionActivityClient terminalSessionActivityClient) {
        this.activity = activity;
        this.terminalSessionActivityClient = terminalSessionActivityClient;
    }

    public MainActivity getActivity() {
        return activity;
    }

    public void onCreate() {
        activity.getTerminalView().setTextSize(TerminalSettingsHelper.getFontSize(activity));
        activity.getTerminalView()
                .setKeepScreenOn(TerminalSettingsHelper.shouldKeepScreenOn(activity));
    }

    public void onStart() {
        activity.getTerminalView().setIsTerminalViewKeyLoggingEnabled(false);
    }

    public void onResume() {
        terminalCursorBlinkerStateAlreadySet = false;
        if (activity.getTerminalView().mEmulator != null) {
            setTerminalCursorBlinkerState(true);
            terminalCursorBlinkerStateAlreadySet = true;
        }
    }

    public void onStop() {
        setTerminalCursorBlinkerState(false);
    }

    @Override
    public void onEmulatorSet() {
        if (!terminalCursorBlinkerStateAlreadySet) {
            setTerminalCursorBlinkerState(true);
            terminalCursorBlinkerStateAlreadySet = true;
        }
    }

    @Override
    public float onScale(float scale) {
        if (scale < 0.9f || scale > 1.1f) {
            boolean increase = scale > 1.f;
            changeFontSize(increase);
            return 1.0f;
        }
        return scale;
    }

    @Override
    public void onSingleTapUp(MotionEvent e) {
        TerminalEmulator term = Objects.requireNonNull(activity.getCurrentSession()).getEmulator();
        if (!term.isMouseTrackingActive() && !e.isFromSource(InputDevice.SOURCE_MOUSE)) {
            AndroidUtilities.showSoftKeyboard(activity.getTerminalView());
        }
    }

    @Override
    public boolean shouldBackButtonBeMappedToEscape() {
        return false;
    }

    @Override
    public boolean shouldEnforceCharBasedInput() {
        return true;
    }

    @Override
    public boolean shouldUseCtrlSpaceWorkaround() {
        return false;
    }

    @Override
    public boolean isTerminalViewSelected() {
        return true;
    }

    @Override
    public void copyModeChanged(boolean copyMode) {
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e, TerminalSession currentSession) {
        if (keyCode == KeyEvent.KEYCODE_ENTER && !currentSession.isRunning()) {
            activity.finishActivityIfNotFinishing();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_BACK && activity.getTerminalView().mEmulator == null) {
            activity.finishActivityIfNotFinishing();
            return true;
        }
        return false;
    }

    @Override
    public boolean readControlKey() {
        return false;
    }

    @Override
    public boolean readAltKey() {
        return false;
    }

    @Override
    public boolean readShiftKey() {
        return false;
    }

    @Override
    public boolean readFnKey() {
        return false;
    }

    @Override
    public boolean onLongPress(MotionEvent event) {
        return false;
    }

    @Override
    public boolean onCodePoint(int codePoint, boolean ctrlDown, TerminalSession session) {
        return false;
    }

    public void changeFontSize(boolean increase) {
        TerminalSettingsHelper.changeFontSize(activity, increase);
        activity.getTerminalView().setTextSize(TerminalSettingsHelper.getFontSize(activity));
    }

    public void setTerminalCursorBlinkerState(boolean start) {
        if (start) {
            if (activity.getTerminalView().setTerminalCursorBlinkerRate(0))
                activity.getTerminalView().setTerminalCursorBlinkerState(true, true);
        } else {
            activity.getTerminalView().setTerminalCursorBlinkerState(false, true);
        }
    }

    public void shareSessionTranscript() {
        TerminalSession session = activity.getCurrentSession();
        if (session == null) return;
        String transcriptText = Utilities.getTerminalSessionTranscriptText(session, false, true);
        if (transcriptText == null) return;
        transcriptText =
                Utilities.getTruncatedCommandOutput(
                                transcriptText,
                                Utilities.TRANSACTION_SIZE_LIMIT_IN_BYTES,
                                false,
                                true,
                                false)
                        .trim();

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, transcriptText);
        sendIntent.setType("text/plain");
        Intent shareIntent = Intent.createChooser(sendIntent, getActivity().getString(R.string.title_share_transcript_with));
        activity.startActivity(shareIntent);
    }
}
