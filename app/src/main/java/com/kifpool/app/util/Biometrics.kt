package com.kifpool.app.util

import android.content.Context
import androidx.biometric.BiometricManager

/**
 * بررسی یک‌جا و بدون تکرار (DRY) قابلیت اثر انگشت.
 * قبلاً همین بلوک در LockScreen و SettingsScreen کپی شده بود.
 */
object Biometrics {

    /** آیا این دستگاه حسگر اثر انگشت کارآمد و ثبت‌شده دارد؟ */
    fun canUseBiometric(context: Context): Boolean = try {
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    } catch (e: Exception) {
        false
    }
}
