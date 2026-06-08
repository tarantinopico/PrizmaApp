package com.example.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.webkit.WebView
import kotlin.system.exitProcess

object ProfileUtils {
    var webViewSuffixSetForProfile: Long? = null

    fun setWebViewProfileOrRestart(context: Context, profileId: Long) {
        if (webViewSuffixSetForProfile == profileId) return
        
        if (webViewSuffixSetForProfile != null) {
            // Already set for different profile. Restart process.
            restartApp(context)
            return
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                WebView.setDataDirectorySuffix("profile_$profileId")
                webViewSuffixSetForProfile = profileId
            } catch (e: Exception) {
                e.printStackTrace()
                restartApp(context)
            }
        } else {
            webViewSuffixSetForProfile = profileId
        }
    }

    fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val mainIntent = Intent.makeRestartActivityTask(intent?.component)
        context.startActivity(mainIntent)
        exitProcess(0)
    }
}
