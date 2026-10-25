package com.example.starbox.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class AppEntry(val packageName: String, val label: String, val isMessaging: Boolean)

/** Labels and icons of launchable apps, for the app picker and for display. */
class InstalledApps(context: Context) {
    private val packageManager = context.packageManager
    private val ownPackage = context.packageName
    private val labels = ConcurrentHashMap<String, String>()

    /** Launchable apps, messaging and social apps first, then alphabetical. */
    suspend fun launchable(): List<AppEntry> = withContext(Dispatchers.IO) {
        launcherActivities()
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != ownPackage }
            .map { AppEntry(it, label(it), it in MESSAGING_APPS) }
            .sortedWith(compareBy<AppEntry> { !it.isMessaging }.thenBy { it.label.lowercase() })
    }

    fun label(packageName: String): String = labels.getOrPut(packageName) {
        try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    fun icon(packageName: String): Drawable? = try {
        packageManager.getApplicationIcon(packageName)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    private fun launcherActivities(): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
    }

    companion object {
        /** Shown at the top of the app picker when installed. */
        val MESSAGING_APPS = setOf(
            "com.tencent.mm", // WeChat
            "com.tencent.mobileqq", // QQ
            "com.tencent.tim", // TIM
            "com.tencent.wework", // WeCom
            "com.alibaba.android.rimet", // DingTalk
            "com.ss.android.lark", // Feishu
            "com.larksuite.suite", // Lark
            "com.sina.weibo", // Weibo
            "com.xingin.xhs", // Xiaohongshu
            "com.twitter.android", // X
            "org.telegram.messenger", // Telegram
            "com.whatsapp", // WhatsApp
            "com.Slack", // Slack
            "com.discord", // Discord
            "jp.naver.line.android", // LINE
            "com.facebook.orca", // Messenger
            "com.instagram.android", // Instagram
            "com.google.android.gm", // Gmail
            "com.google.android.apps.messaging", // Google Messages
            "com.android.mms", // AOSP / OEM SMS
        )
    }
}
