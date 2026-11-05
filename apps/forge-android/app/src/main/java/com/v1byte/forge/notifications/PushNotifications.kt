package com.v1byte.forge.notifications

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

/** Returns whether at least one Firebase app has been initialized in this process. */
fun isPushConfigured(context: Context): Boolean = try {
    FirebaseApp.getApps(context.applicationContext).isNotEmpty()
} catch (_: Exception) {
    false
}

/**
 * Enables Firebase Messaging auto-initialization when Firebase is available.
 * This does not request notification permission; Android 13+ permission must be
 * requested by an Activity.
 *
 * @return true when auto-initialization was enabled, or false when Firebase is
 * unavailable or enabling it fails.
 */
fun enablePushNotifications(context: Context): Boolean {
    val firebaseApp = try {
        if (!isPushConfigured(context)) return false
        FirebaseApp.getApps(context.applicationContext).firstOrNull() ?: return false
    } catch (_: Exception) {
        return false
    }

    return try {
        FirebaseMessaging.getInstance(firebaseApp).isAutoInitEnabled = true
        true
    } catch (_: Exception) {
        false
    }
}

/**
 * Retrieves an FCM registration token without logging or sending it anywhere.
 * A missing Firebase configuration or retrieval error is delivered through
 * [onResult] instead of being thrown to the caller.
 */
fun getRegistrationToken(context: Context, onResult: (Result<String>) -> Unit) {
    val firebaseApp = try {
        if (!isPushConfigured(context)) {
            onResult(Result.failure(IllegalStateException("Firebase belum dikonfigurasi.")))
            return
        }
        FirebaseApp.getApps(context.applicationContext).firstOrNull()
            ?: run {
                onResult(Result.failure(IllegalStateException("Firebase belum dikonfigurasi.")))
                return
            }
    } catch (error: Exception) {
        onResult(Result.failure(error))
        return
    }

    try {
        FirebaseMessaging.getInstance(firebaseApp).token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    if (!token.isNullOrBlank()) {
                        onResult(Result.success(token))
                    } else {
                        onResult(Result.failure(IllegalStateException("Token FCM tidak tersedia.")))
                    }
                } else {
                    onResult(
                        Result.failure(
                            task.exception ?: IllegalStateException("Gagal mengambil token FCM.")
                        )
                    )
                }
            }
    } catch (error: Exception) {
        onResult(Result.failure(error))
    }
}
