package com.adbify

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.adbify.app.AppBarActivity
import com.adbify.databinding.ActivityMainBinding
import com.adbify.databinding.DialogAboutBinding
import com.adbify.terminal.TerminalService
import com.adbify.terminal.TerminalService.LocalBinder
import com.adbify.terminal.TerminalSession
import com.adbify.terminal.TerminalSessionActivityClient
import com.adbify.terminal.TerminalSettingsHelper.setKeepScreenOn
import com.adbify.terminal.TerminalSettingsHelper.shouldKeepScreenOn
import com.adbify.terminal.TerminalViewClient
import com.adbify.terminal.view.TerminalView
import com.adbify.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.core.util.ContextUtils
import rikka.html.text.toHtml
import java.io.File

class MainActivity : AppBarActivity(), ServiceConnection {
    private lateinit var binding: ActivityMainBinding

    var terminalService: TerminalService? = null
        private set
    private var terminalSessionClient: TerminalSessionActivityClient? = null
    private var terminalViewClient: TerminalViewClient? = null
    var isVisible = false
        private set
    private var isOnResumeAfterOnCreate = false
    private var isActivityRecreated = false
    private var isInvalidState = false

    private var choiceFile =
        registerForActivityResult(GetContentContract()) { uri: Uri? ->
            handleFileUri(this, uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        isOnResumeAfterOnCreate = true
        if (savedInstanceState != null)
            isActivityRecreated = savedInstanceState.getBoolean(ARG_ACTIVITY_RECREATED, false)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setTerminalViewAndClients()

        try {
            val serviceIntent = Intent(this, TerminalService::class.java)
            startService(serviceIntent)
            if (!bindService(
                    serviceIntent,
                    this,
                    0
                )
            ) throw RuntimeException("bindService() failed")
        } catch (e: Exception) {
            AndroidUtilities.toastLong(this, getString(R.string.terminal_service_start_error))
            isInvalidState = true
            return
        }
    }

    private fun setTerminalViewAndClients() {
        terminalSessionClient = TerminalSessionActivityClient(this)
        terminalViewClient = TerminalViewClient(this, terminalSessionClient)
        binding.terminalView.setTerminalViewClient(terminalViewClient)
        binding.terminalView.post {
            binding.terminalView.setTypeface(Typeface.MONOSPACE)
        }
        terminalViewClient?.onCreate()
        terminalSessionClient?.onCreate()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(ARG_ACTIVITY_RECREATED, true)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        terminalService = (service as LocalBinder).service
        if (terminalService != null) {
            terminalService!!.setTerminalSessionClient(terminalSessionClient)
            terminalSessionClient?.currentTerminalSession =
                terminalSessionClient?.currentTerminalSession
        } else isInvalidState = true
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        finishActivityIfNotFinishing()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val keepScreenOn = menu.findItem(R.id.action_keep_screen_on)
        keepScreenOn.isChecked = shouldKeepScreenOn(this@MainActivity)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val session = currentSession
        when (item.itemId) {
            R.id.action_attachment -> {
                attachNewFile()
                return true
            }

            R.id.action_share_transcript -> {
                terminalViewClient?.shareSessionTranscript()
                return true
            }

            R.id.action_kill -> {
                showKillSessionDialog(session)
                return true
            }

            R.id.action_keep_screen_on -> {
                toggleKeepScreenOn()
                item.isChecked = shouldKeepScreenOn(this@MainActivity)
                return true
            }

            R.id.action_about -> {
                showAboutDialog()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        if (isInvalidState) return
        isVisible = true
        terminalSessionClient?.onStart()
        terminalViewClient?.onStart()
    }

    override fun onResume() {
        super.onResume()
        if (isInvalidState) return
        terminalSessionClient?.onResume()
        terminalViewClient?.onResume()
        isOnResumeAfterOnCreate = false
    }

    override fun onStop() {
        super.onStop()
        if (isInvalidState) return
        isVisible = false
        terminalSessionClient?.onStop()
        terminalViewClient?.onStop()
    }

    override fun onDestroy() {
        if (isInvalidState) return
        if (terminalService != null) {
            terminalService!!.unsetTerminalSessionClient()
            terminalService = null
        }
        try {
            unbindService(this)
        } catch (e: Exception) {
            // ignore.
        }
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        moveTaskToBack(true)
        super.onBackPressed()
    }

    val terminalView: TerminalView
        get() = binding.terminalView

    val currentSession: TerminalSession?
        get() = binding.terminalView.currentSession

    private fun showAboutDialog() {
        val binding = DialogAboutBinding.inflate(layoutInflater, null, false)
        binding.desInfo.movementMethod = LinkMovementMethod.getInstance()
        binding.desInfo.text = getString(
            R.string.about_view_source_code,
            "<b><a href=\"https://github.com/RohitVerma882/Adbify\">GitHub</a></b>"
        ).toHtml()
        binding.icon.setImageBitmap(
            AppIconCache.getOrLoadBitmap(
                this,
                applicationInfo,
                android.os.Process.myUid() / 100000,
                resources.getDimensionPixelOffset(R.dimen.default_app_icon_size)
            )
        )
        binding.versionName.text = getString(R.string.app_version)
        MaterialAlertDialogBuilder(this)
            .setView(binding.root)
            .show()
    }

    fun finishActivityIfNotFinishing() {
        if (this@MainActivity.isFinishing) {
            return
        }
        finish()
    }

    private fun toggleKeepScreenOn() {
        if (binding.terminalView.keepScreenOn) {
            binding.terminalView.keepScreenOn = false
            setKeepScreenOn(this, false)
        } else {
            binding.terminalView.keepScreenOn = true
            setKeepScreenOn(this, true)
        }
    }

    private fun showKillSessionDialog(session: TerminalSession?) {
        if (session == null) return
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.title_confirm_kill_process)
            .setPositiveButton(
                R.string.yes
            ) { p1: DialogInterface, _: Int ->
                p1.dismiss()
                session.finishIfRunning()
            }
            .setNegativeButton(R.string.no) { p1: DialogInterface, _: Int ->
                p1.dismiss()
            }
            .show()
    }

    private fun appendLineToTerminal(line: String?) {
        val session = currentSession ?: return
        if (!line.isNullOrBlank()) {
            session.emulator.paste(Utilities.quote(line))
        }
    }

    private fun attachNewFile() {
        if (Build.VERSION.SDK_INT >= 33
            || (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            )
                    == PackageManager.PERMISSION_GRANTED)
        ) {
            choiceFile.launch("*/*")
            AdbifyApp.adShowEnabled = false
        } else {
            try {
                AndroidUtilities.toastLong(this, getString(R.string.ask_for_permission))
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_READ_FILE_CODE
                )
            } catch (e: Exception) {
                choiceFile.launch("*/*")
                AdbifyApp.adShowEnabled = false
            }
        }
    }

    private fun handleFileUri(context: Context, uri: Uri?) {
        AdbifyApp.adShowEnabled = true
        if (uri == null) {
            AndroidUtilities.toastLong(context, getString(R.string.file_attach_failed))
            return
        }
        val path = FileUtils.getPath(this@MainActivity, uri)
        if (path != null && File(path).canRead()) {
            val finalPath = path.removePrefix("file:")
            appendLineToTerminal(finalPath)
        } else {
            showLoading()
            lifecycleScope.launch(Dispatchers.IO) {
                if (adbCacheDir.exists()) adbCacheDir.deleteRecursively()
                if (!adbCacheDir.exists()) adbCacheDir.mkdirs()
                var output = FileUtils.copyUriToPath(context, uri, adbCacheDir.absolutePath)
                if (output.isNullOrBlank()) output = ""
                withContext(Dispatchers.Main) {
                    hideLoading()
                    appendLineToTerminal(output)
                }
            }
        }
    }

    private fun showLoading() {
        binding.linearProgressIndicator.visibility = View.VISIBLE
        terminalView.isEnabled = false
    }

    private fun hideLoading() {
        binding.linearProgressIndicator.visibility = View.GONE
        terminalView.isEnabled = true
    }

    private val adbCacheDir: File
        get() {
            return ContextUtils.getExternalCacheFile(this, "adb_files_cache")
        }

    companion object {
        private const val ARG_ACTIVITY_RECREATED = "activity_recreated"
        private const val PERMISSION_READ_FILE_CODE = 880

        fun newInstance(context: Context): Intent {
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            return intent
        }
    }
}