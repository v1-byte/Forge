package com.v1byte.forge.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.v1byte.forge.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ForgeMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Firebase may not be configured in this build. Never initialize it here.
        if (!isPushConfigured(this) || !hasNotificationPermission()) return

        val title = message.notification?.title.nonBlankOrNull()
            ?: message.data["title"].nonBlankOrNull()
            ?: message.data["notification_title"].nonBlankOrNull()
            ?: appName()
        val body = message.notification?.body.nonBlankOrNull()
            ?: message.data["body"].nonBlankOrNull()
            ?: message.data["notification_body"].nonBlankOrNull()
            ?: "Ada pembaruan untuk Anda."

        createNotificationChannel()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_forge_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)

        createLauncherPendingIntent()?.let(builder::setContentIntent)

        try {
            val notificationId = message.messageId?.hashCode()
                ?: (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
            NotificationManagerCompat.from(this).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission may have changed after the check; do not crash the service.
        }
    }

    private fun hasNotificationPermission(): Boolean {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            this,
            POST_NOTIFICATIONS_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pembaruan Forge",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Pembaruan dan informasi Forge."
            }
            manager.createNotificationChannel(channel)
        }
    }

    /** Only targets this app's launcher; no remote payload data is used as an Intent. */
    private fun createLauncherPendingIntent(): PendingIntent? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val requestCode = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
        return PendingIntent.getActivity(this, requestCode, launchIntent, flags)
    }

    private fun appName(): String = try {
        packageManager.getApplicationLabel(applicationInfo).toString()
    } catch (_: Exception) {
        "Forge"
    }

    private fun String?.nonBlankOrNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)

    private companion object {
        const val CHANNEL_ID = "forge_updates"
        const val POST_NOTIFICATIONS_PERMISSION = "android.permission.POST_NOTIFICATIONS"
    }
}
