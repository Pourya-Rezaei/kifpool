package com.kifpool.app.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.withResumed
import com.kifpool.app.WalletViewModel
import kotlinx.coroutines.delay

/**
 * قفل برنامه: صفحه‌کلید PIN + ورود با اثر انگشت.
 *
 * رفع کرش ۱٫۵٫۱: نمایش BiometricPrompt فقط وقتی اکتیویتی در حالت RESUMED است
 * (withResumed) و کل فراخوانی داخل try/catch است تا هر مشکلی
 * به‌جای بستن برنامه، به صفحه‌کلید PIN برگردد.
 */
@Composable
fun LockScreen(
    vm: WalletViewModel,
    onUnlock: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val biometricEnabled by vm.biometricEnabled.collectAsState()

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var bioTried by remember { mutableStateOf(false) }

    val canBiometric = remember {
        try {
            BiometricManager.from(context)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            false
        }
    }

    fun submit() {
        if (pin.length < 4) return
        if (vm.checkPin(pin)) {
            onUnlock()
        } else {
            error = true
        }
    }

    fun showBiometricPrompt() {
        try {
            val activity = lifecycleOwner as androidx.fragment.app.FragmentActivity
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onUnlock()
                    }
                }
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("باز کردن کیف پول من")
                .setSubtitle("از اثر انگشت استفاده کن")
                .setNegativeButtonText("استفاده از PIN")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()
            prompt.authenticate(info)
        } catch (e: Exception) {
            // اگر هر مشکلی پیش آمد، کاربر با PIN وارد می‌شود
        }
    }

    // نمایش خودکار اثر انگشت فقط یک بار و فقط وقتی اکتیویتی RESUMED است (رفع کرش ۱٫۵٫۱)
    LaunchedEffect(biometricEnabled, canBiometric) {
        if (biometricEnabled && canBiometric && !bioTried) {
            bioTried = true
            lifecycleOwner.lifecycle.withResumed {
                showBiometricPrompt()
            }
        }
    }

    // رفتار خطا
    LaunchedEffect(error) {
        if (error) {
            delay(600)
            pin = ""
            error = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "کیف پول من قفل است",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                "رمز عبور را وارد کن",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(26.dp))

            // نقطه‌های PIN
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(6) { index ->
                    val filled = index < pin.length
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    error -> MaterialTheme.colorScheme.error
                                    filled -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                }
                            )
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // صفحه‌کلید
            val rows = listOf(
                listOf("۱", "۲", "۳"),
                listOf("۴", "۵", "۶"),
                listOf("۷", "۸", "۹"),
                listOf("اثر انگشت", "۰", "حذف")
            )
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    row.forEach { key ->
                        when (key) {
                            "اثر انگشت" -> {
                                val show = biometricEnabled && canBiometric
                                KeyPadButton(
                                    label = "",
                                    enabled = show,
                                    onClick = { showBiometricPrompt() },
                                    icon = if (show) Icons.Rounded.Fingerprint else null
                                )
                            }
                            "حذف" -> {
                                KeyPadButton(
                                    label = "",
                                    enabled = pin.isNotEmpty(),
                                    onClick = {
                                        if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                    },
                                    icon = Icons.Rounded.Backspace
                                )
                            }
                            else -> {
                                KeyPadButton(
                                    label = key,
                                    enabled = pin.length < 6,
                                    onClick = {
                                        pin += key
                                        if (pin.length == 6) submit()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                if (pin.length == 6) "بررسی..." else "PIN بین ۴ تا ۶ رقم است",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // دکمه تأیید برای PINهای ۴ یا ۵ رقمی
            if (pin.length in 4..5) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { submit() }
                ) {
                    Text(
                        "ورود",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyPadButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        shape = CircleShape,
        color = if (enabled) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled) { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            } else {
                Text(
                    label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
