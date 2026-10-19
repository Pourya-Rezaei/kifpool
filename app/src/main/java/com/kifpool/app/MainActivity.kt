package com.kifpool.app

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.kifpool.app.logic.ReminderScheduler
import com.kifpool.app.ui.AppRoot
import com.kifpool.app.ui.theme.KifPoolTheme
import com.kifpool.app.worker.ReminderWorker

/**
 * FragmentActivity (نه ComponentActivity) چون BiometricPrompt فقط با FragmentActivity کار می‌کند.
 *
 * - launchMode=singleTask در Manifest: کلیک روی اعلان، اکتیویتی را دوباره نمی‌سازد
 *   و از مسیر onNewIntent می‌آید (رفع کرش نسخه ۱٫۵٫۱).
 * - lifecycle observer: رفتن به پس‌زمینه، دوباره قفل می‌کند (وقتی قفل فعال باشد).
 */
class MainActivity : FragmentActivity() {

    private lateinit var isLocked: MutableState<Boolean>
    private lateinit var openReminders: MutableState<Boolean>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        val vm = ViewModelProvider(this)[WalletViewModel::class.java]

        isLocked = mutableStateOf(vm.lockEnabled.value)
        openReminders = mutableStateOf(false)

        // رفتن به پس‌زمینه => قفل دوباره (فقط وقتی کاربر قفل را فعال کرده باشد)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                try {
                    if (vm.lockEnabled.value) isLocked.value = true
                } catch (e: Exception) {
                    // هرگز نباید متوقف‌شدن برنامه کرش ایجاد کند
                }
            }
        })

        handleIntent(intent)

        // زمان‌بندی چک روزانه یادآورها + جبران اعلان‌های از‌دست‌رفته
        ReminderWorker.scheduleDaily(this)
        ReminderWorker.checkNow(this)

        val composeView = findViewById<ComposeView>(R.id.compose_view)
        composeView.setContent {
            val dark by vm.darkMode.collectAsState()
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightStatusBars = !dark
            }
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                KifPoolTheme(dark = dark) {
                    AppRoot(
                        vm = vm,
                        locked = isLocked.value,
                        onUnlock = { isLocked.value = false },
                        openReminders = openReminders.value,
                        onRemindersOpened = { openReminders.value = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** کلیک روی اعلان یادآور => باز شدن مستقیم صفحه یادآورها (بدون ساخت دوباره اکتیویتی). */
    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(ReminderScheduler.EXTRA_OPEN_REMINDERS, false) == true) {
            openReminders.value = true
        }
    }
}
