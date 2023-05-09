package com.adbify.utils;

import com.adbify.terminal.TerminalBuffer;
import com.adbify.terminal.TerminalEmulator;
import com.adbify.terminal.TerminalSession;

import java.util.Locale;

public class Utilities {
    public static final int TRANSACTION_SIZE_LIMIT_IN_BYTES = 100 * 1024; // 100KB
    protected static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String quote(String s) {
        return ("'" + s.replace("'", "'\\''") + "'");
    }

    public static String getTruncatedCommandOutput(
            String text, int maxLength, boolean fromEnd, boolean onNewline, boolean addPrefix) {
        if (text == null) return null;
        String prefix = "(truncated) ";
        if (addPrefix) maxLength = maxLength - prefix.length();
        if (maxLength < 0 || text.length() < maxLength) return text;
        if (fromEnd) {
            text = text.substring(0, maxLength);
        } else {
            int cutOffIndex = text.length() - maxLength;
            if (onNewline) {
                int nextNewlineIndex = text.indexOf('\n', cutOffIndex);
                if (nextNewlineIndex != -1 && nextNewlineIndex != text.length() - 1) {
                    cutOffIndex = nextNewlineIndex + 1;
                }
            }
            text = text.substring(cutOffIndex);
        }
        if (addPrefix) text = prefix + text;
        return text;
    }

    public static String getTerminalSessionTranscriptText(
            TerminalSession terminalSession, boolean linesJoined, boolean trim) {
        if (terminalSession == null) return null;
        TerminalEmulator terminalEmulator = terminalSession.getEmulator();
        if (terminalEmulator == null) return null;
        TerminalBuffer terminalBuffer = terminalEmulator.getScreen();
        if (terminalBuffer == null) return null;
        String transcriptText;
        if (linesJoined) transcriptText = terminalBuffer.getTranscriptTextWithFullLinesJoined();
        else transcriptText = terminalBuffer.getTranscriptTextWithoutJoinedLines();
        if (trim) transcriptText = transcriptText.trim();
        return transcriptText;
    }

    public static String capitalize(String string) {
        return string.substring(0, 1).toUpperCase(Locale.US) + string.substring(1);
    }

    public static void replaceSubStringsInStringArrayItems(
            String[] array, String find, String replace) {
        if (array == null || array.length == 0) return;
        for (int i = 0; i < array.length; i++) {
            array[i] = array[i].replace(find, replace);
        }
    }

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexToBytes(String hex) {
        if (hex == null) {
            return null;
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] =
                    (byte)
                            ((Character.digit(hex.charAt(i), 16) << 4)
                                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
