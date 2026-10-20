package com.kifpool.app

import android.content.Intent
import android.os.Bundle
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.kifpool.app.ui.screens.SplashScreen
import com.kifpool.app.ui.theme.KifPoolTheme
import com.kifpool.app.worker.ReminderWorker

/**
 * FragmentActivity (نه ComponentActivity) چون BiometricPrompt فقط با FragmentActivity کار می‌کند.
 *
 * - launchMode=singleTask در Manifest: کلیک روی اعلان، اکتیویتی را دوباره نمی‌سازد
 *   و از مسیر onNewIntent می‌آید (رفع کرش نسخه ۱٫۵٫۱).
 * - lifecycle observer: رفتن به پس‌زمینه، دوباره قفل می‌کند (وقتی قفل فعال باشد).
 * - اسپلش برند (نسخهٔ ۱٫۷): فقط در شروع سرد، با Crossfade نرم به AppRoot.
 */
class MainActivity : FragmentActivity() {

    private lateinit var isLocked: MutableState<Boolean>
    private lateinit var openReminders: MutableState<Boolean>
    private lateinit var openRecurring: MutableState<Boolean>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // حریم خصوصی مالی: جلوگیری از اسکرین‌شات و نمایش محتوا در thumbnails حافظهٔ اخیر
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        val vm = ViewModelProvider(this)[WalletViewModel::class.java]

        isLocked = mutableStateOf(vm.lockEnabled.value)
        openReminders = mutableStateOf(false)
        openRecurring = mutableStateOf(false)

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

        // زمان‌بندی چک روزانه یادآورها/تکرارشوها + جبران موارد از‌دست‌رفته
        ReminderWorker.scheduleDaily(this)
        ReminderWorker.checkNow(this)

        val composeView = findViewById<ComposeView>(R.id.compose_view)
        composeView.setContent {
            val dark by vm.darkMode.collectAsState()

            // اسپلش فقط در شروع سرد نمایش داده می‌شود (rememberSaveable تا با
            // چرخش صفحه/بازسازی اکتیویتی دوباره پخش نشود)
            var showSplash by rememberSaveable { mutableStateOf(true) }
            val versionName = remember {
                runCatching {
                    packageManager.getPackageInfo(packageName, 0).versionName
                }.getOrNull().orEmpty()
            }

            // در اسپلش (همیشه تیره) آیکون‌های نوار وضعیت روشن می‌مانند
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightStatusBars = !dark && !showSplash
            }

            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                KifPoolTheme(dark = dark) {
                    Crossfade(
                        targetState = showSplash,
                        animationSpec = tween(450),
                        label = "splash-to-app"
                    ) { showing ->
                        if (showing) {
                            SplashScreen(
                                onFinished = { showSplash = false },
                                versionName = versionName
                            )
                        } else {
                            AppRoot(
                                vm = vm,
                                locked = isLocked.value,
                                onUnlock = { isLocked.value = false },
                                openReminders = openReminders.value,
                                onRemindersOpened = { openReminders.value = false },
                                openRecurring = openRecurring.value,
                                onRecurringOpened = { openRecurring.value = false }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /**
     * کلیک روی اعلان => باز شدن مستقیم صفحهٔ مربوطه (بدون ساخت دوباره اکتیویتی).
     * یادآورها => صفحه یادآورها؛ سررسید تکرارشو => صفحه تکرارشوها.
     */
    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(ReminderScheduler.EXTRA_OPEN_REMINDERS, false) == true) {
            openReminders.value = true
        }
        if (intent?.getBooleanExtra(ReminderScheduler.EXTRA_OPEN_RECURRING, false) == true) {
            openRecurring.value = true
        }
    }
}
