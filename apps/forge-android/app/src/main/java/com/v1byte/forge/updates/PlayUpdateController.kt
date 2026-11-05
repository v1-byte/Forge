package com.v1byte.forge.updates

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The UI-facing state of a flexible Google Play update. */
data class PlayUpdateState(
    val updateMessage: String? = null,
    val updateReady: Boolean = false,
    val status: PlayUpdateStatus = PlayUpdateStatus.IDLE,
)

enum class PlayUpdateStatus {
    IDLE,
    CHECKING,
    NO_UPDATE,
    AVAILABLE,
    AWAITING_USER_CONFIRMATION,
    DOWNLOADING,
    DOWNLOADED,
    INSTALLING,
    COMPLETED,
    CANCELLED,
    ERROR,
}

/**
 * Coordinates a user-confirmed flexible update through Google Play.
 * The host should call [checkForUpdate] at startup and [onResume] from its
 * Activity's onResume callback. Forward the Activity Result callback to
 * [onUpdateFlowResult] so cancellation and flow errors can be reflected in state.
 */
class PlayUpdateController(
    private val activity: ComponentActivity,
    private val launcher: ActivityResultLauncher<IntentSenderRequest>,
) {
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private val mutableState = MutableStateFlow(PlayUpdateState())

    /** Observe with Compose, for example `state.collectAsState()`. */
    val state: StateFlow<PlayUpdateState> = mutableState.asStateFlow()

    private var isClosed = false
    private var isChecking = false
    private var autoPromptAttempted = false

    private val installStateListener = InstallStateUpdatedListener { installState ->
        if (isClosed) return@InstallStateUpdatedListener

        when (installState.installStatus()) {
            InstallStatus.PENDING -> setState(
                PlayUpdateStatus.AWAITING_USER_CONFIRMATION,
                "Menunggu konfirmasi Google Play untuk memulai pembaruan.",
            )
            InstallStatus.DOWNLOADING -> setState(
                PlayUpdateStatus.DOWNLOADING,
                "Pembaruan sedang diunduh dari Google Play.",
            )
            InstallStatus.DOWNLOADED -> setDownloaded()
            InstallStatus.INSTALLING -> setState(
                PlayUpdateStatus.INSTALLING,
                "Google Play sedang memasang pembaruan.",
            )
            InstallStatus.INSTALLED -> {
                mutableState.value = PlayUpdateState(
                    updateMessage = "Pembaruan berhasil dipasang.",
                    status = PlayUpdateStatus.COMPLETED,
                )
            }
            InstallStatus.CANCELED -> setState(
                PlayUpdateStatus.CANCELLED,
                "Pembaruan dibatalkan. Anda dapat memeriksa lagi nanti.",
            )
            InstallStatus.FAILED -> setError("Unduhan pembaruan gagal. Silakan coba lagi nanti.")
            else -> Unit
        }
    }

    init {
        appUpdateManager.registerListener(installStateListener)
    }

    /** Check Play for an update; at most one automatic flow is launched per controller lifetime. */
    fun checkForUpdate(autoStart: Boolean = true) {
        if (isClosed || isChecking) return

        isChecking = true
        setState(PlayUpdateStatus.CHECKING, "Memeriksa pembaruan di Google Play.")
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                isChecking = false
                if (isClosed) return@addOnSuccessListener

                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    setDownloaded()
                    return@addOnSuccessListener
                }

                if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) {
                    when (info.installStatus()) {
                        InstallStatus.DOWNLOADING, InstallStatus.PENDING -> setState(
                            PlayUpdateStatus.DOWNLOADING,
                            "Pembaruan sedang diproses oleh Google Play.",
                        )
                        InstallStatus.INSTALLING -> setState(
                            PlayUpdateStatus.INSTALLING,
                            "Google Play sedang memasang pembaruan.",
                        )
                        else -> mutableState.value = PlayUpdateState(
                            updateMessage = "Tidak ada pembaruan yang tersedia.",
                            status = PlayUpdateStatus.NO_UPDATE,
                        )
                    }
                    return@addOnSuccessListener
                }

                if (!info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    setState(
                        PlayUpdateStatus.AVAILABLE,
                        "Pembaruan tersedia, tetapi Google Play tidak mengizinkan alur fleksibel saat ini.",
                    )
                    return@addOnSuccessListener
                }

                if (!autoStart || autoPromptAttempted) {
                    setState(
                        PlayUpdateStatus.AVAILABLE,
                        if (!autoStart) {
                            "Pembaruan tersedia. Jalankan pemeriksaan dengan autoStart=true untuk membuka konfirmasi Google Play."
                        } else {
                            "Pembaruan tersedia. Prompt otomatis sudah digunakan pada Activity ini; tidak ada prompt ulang."
                        },
                    )
                    return@addOnSuccessListener
                }

                // Set before launching so resume/repeated checks cannot spam the Play confirmation UI.
                autoPromptAttempted = true
                setState(
                    PlayUpdateStatus.AWAITING_USER_CONFIRMATION,
                    "Pembaruan tersedia. Konfirmasi Google Play diperlukan untuk memulai unduhan.",
                )
                try {
                    val launched = appUpdateManager.startUpdateFlowForResult(
                        info,
                        launcher,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                    )
                    if (!launched) {
                        setError("Google Play tidak dapat memulai alur pembaruan.")
                    }
                } catch (error: Exception) {
                    setError(error.message?.let { "Gagal memulai pembaruan: $it" }
                        ?: "Gagal memulai alur pembaruan Google Play.")
                }
            }
            .addOnFailureListener { error ->
                isChecking = false
                if (!isClosed) {
                    setError(error.message?.let { "Gagal memeriksa pembaruan: $it" }
                        ?: "Gagal memeriksa pembaruan Google Play.")
                }
            }
    }

    /** Re-check on Activity resume and restore the downloaded state if needed. */
    fun onResume() {
        checkForUpdate(autoStart = true)
    }

    /**
     * Forward the result from the host's Activity Result callback.
     * A canceled flow is recorded without immediately prompting the user again.
     */
    fun onUpdateFlowResult(resultCode: Int) {
        if (isClosed) return

        when (resultCode) {
            Activity.RESULT_OK -> setState(
                PlayUpdateStatus.DOWNLOADING,
                "Konfirmasi diterima. Google Play sedang mengunduh pembaruan.",
            )
            Activity.RESULT_CANCELED -> setState(
                PlayUpdateStatus.CANCELLED,
                "Anda membatalkan pembaruan. Tidak ada pemasangan yang dilakukan.",
            )
            else -> setError("Alur pembaruan Google Play gagal (kode $resultCode).")
        }
    }

    /** Complete the flexible update only in response to an explicit user action. */
    fun completeUpdate() {
        if (isClosed || !mutableState.value.updateReady) return

        setState(PlayUpdateStatus.INSTALLING, "Meminta Google Play memasang pembaruan.")
        appUpdateManager.completeUpdate()
            .addOnSuccessListener {
                if (!isClosed) {
                    mutableState.value = PlayUpdateState(
                        updateMessage = "Google Play telah memulai pemasangan pembaruan.",
                        status = PlayUpdateStatus.COMPLETED,
                    )
                }
            }
            .addOnFailureListener { error ->
                if (!isClosed) {
                    // Keep updateReady true so the user can explicitly retry the action.
                    mutableState.value = mutableState.value.copy(
                        updateMessage = error.message?.let { "Gagal memasang pembaruan: $it" }
                            ?: "Google Play gagal memasang pembaruan. Anda dapat mencoba lagi.",
                        updateReady = true,
                        status = PlayUpdateStatus.ERROR,
                    )
                }
            }
    }

    /** Unregister the Play listener when the owning Activity is finished. */
    fun close() {
        if (isClosed) return
        isClosed = true
        appUpdateManager.unregisterListener(installStateListener)
    }

    private fun setDownloaded() {
        mutableState.value = PlayUpdateState(
            updateMessage = "Pembaruan telah diunduh. Pilih pasang untuk meminta Google Play melanjutkan.",
            updateReady = true,
            status = PlayUpdateStatus.DOWNLOADED,
        )
    }

    private fun setState(status: PlayUpdateStatus, message: String) {
        mutableState.value = PlayUpdateState(
            updateMessage = message,
            updateReady = status == PlayUpdateStatus.DOWNLOADED,
            status = status,
        )
    }

    private fun setError(message: String) {
        mutableState.value = mutableState.value.copy(
            updateMessage = message,
            status = PlayUpdateStatus.ERROR,
        )
    }
}

// Untuk pengujian, pasang aplikasi melalui Google Play agar Play dapat memberikan metadata update.
