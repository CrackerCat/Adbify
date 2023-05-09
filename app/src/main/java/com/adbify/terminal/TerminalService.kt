package com.adbify.terminal

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.system.Os
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.adbify.MainActivity
import com.adbify.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import java.io.File

class TerminalService : Service() {
    private val binder: IBinder = LocalBinder()

    private var terminalSession: TerminalSession? = null
    private var terminalSessionActivityClient: TerminalSessionActivityClient? = null

    private var scope: CoroutineScope = MainScope()

    override fun onCreate() {
        super.onCreate()
        runStartForeground()
    }

    override fun onDestroy() {
        runStopForeground()
        super.onDestroy()
    }

    override fun onBind(p1: Intent): IBinder {
        return binder
    }

    override fun onUnbind(p1: Intent): Boolean {
        if (terminalSessionActivityClient != null) unsetTerminalSessionClient()
        return false
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        runStartForeground()
        val action: String? = intent.action
        if (ACTION_STOP_SERVICE == action) {
            actionStopService()
        }
        return START_NOT_STICKY
    }

    private fun runStartForeground() {
        setupNotificationChannel()
        try {
            startForeground(NOTIFICATION_CHANNEL_ID, buildNotification())
        } catch (e: Throwable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is ForegroundServiceStartNotAllowedException
            ) {
                Log.e(TAG, "start service in Foreground failed: ${e.message}")
            }
        }
    }

    private fun runStopForeground() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Throwable) {

        }
    }

    private fun requestStopService() {
        runStopForeground()
        stopSelf()
    }

    fun actionStopService() {
        terminalSession?.finishIfRunning()
        requestStopService()
    }

    private fun setupNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(
            "" + NOTIFICATION_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_LOW
        )
            .setName(getString(R.string.notification_terminal_session))
            .setDescription(getString(R.string.notification_terminal_session_des))
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, TerminalService::class.java).setAction(ACTION_STOP_SERVICE)
        val contentIntent = MainActivity.newInstance(this)
        return NotificationCompat.Builder(this, "" + NOTIFICATION_CHANNEL_ID)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setContentTitle(getString(R.string.notification_terminal_session))
            .setShowWhen(false)
            .setSilent(true)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_adbify)
            .setContentText(getString(R.string.notification_running))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.ic_close,
                getString(R.string.notification_action_stop),
                PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)
            )
            .build()
    }

    @Synchronized
    fun getTerminalSession(): TerminalSession? {
        return terminalSession
    }

    @Synchronized
    fun getTerminalSessionClient(): TerminalSessionClientBase? {
        return terminalSessionActivityClient
    }

    @Synchronized
    fun setTerminalSessionClient(
        terminalSessionActivityClient: TerminalSessionActivityClient?,
    ) {
        this.terminalSessionActivityClient = terminalSessionActivityClient
        terminalSession?.updateTerminalSessionClient(terminalSessionActivityClient)
    }

    @Synchronized
    fun unsetTerminalSessionClient() {
        terminalSession?.updateTerminalSessionClient(terminalSessionActivityClient)
        terminalSessionActivityClient = null
    }

    @Synchronized
    fun getOrCreateTerminalSession(): TerminalSession {
        if (terminalSession == null) {
            val envs = arrayOfNulls<String>(5)
            envs[0] = "TERM=screen"
            envs[1] = "HOME=$adbHomeDir"
            envs[2] = "TMPDIR=${cacheDir.absolutePath}"
            envs[3] = "ADB=$ADB"
            envs[4] = "PATH=${Os.getenv("PATH")}:${ADB.substring(0, ADB.lastIndexOf("/"))}"
            terminalSession = TerminalSession(
                "/system/bin/sh",
                "/", arrayOf<String>(),
                envs,
                TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
                terminalSessionActivityClient
            )
        }
        return terminalSession as TerminalSession
    }

    private val adbHomeDir: String
        get() {
            val homeDir = File(filesDir, "home")
            if (!homeDir.exists()) homeDir.mkdirs()
            return homeDir.absolutePath
        }

    private val ADB: String
        get() {
            val binDir = File(filesDir, "bin")
            binDir.deleteRecursively()
            if (!binDir.exists()) binDir.mkdirs()
            Os.symlink(
                "${applicationInfo.nativeLibraryDir}/libadb.so",
                "${binDir.absolutePath}/adb"
            )
            return binDir.resolve("adb").absolutePath
        }

    inner class LocalBinder : Binder() {
        val service = this@TerminalService
    }

    companion object {
        private const val TAG = "TerminalService"
        private const val NOTIFICATION_CHANNEL_ID = 882
        private const val ACTION_STOP_SERVICE = "com.adbify.STOP_SERVICE"
    }
}