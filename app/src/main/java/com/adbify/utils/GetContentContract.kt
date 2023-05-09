package com.adbify.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.CallSuper

class GetContentContract : ActivityResultContract<String, Uri?>() {

    @CallSuper
    override fun createIntent(context: Context, input: String): Intent {
        val chooseFile = Intent()
        chooseFile.action = Intent.ACTION_GET_CONTENT
        chooseFile.addCategory(Intent.CATEGORY_OPENABLE)
        chooseFile.type = input
        return chooseFile
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent?.data
    }
}