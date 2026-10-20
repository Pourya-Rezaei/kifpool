package com.kifpool.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kifpool.app.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// ---------------------------------------------------------------------------
// رنگ‌های برند اسپلش — آینهٔ پالت تیرهٔ Theme.kt (اسپلش همیشه تیره است تا با
// windowBackground منیفست (#0B1020) یکپارچه باشد؛ مستقل از تم انتخابی کاربر)
// ---------------------------------------------------------------------------
private val SplashNavy = Color(0xFF0B1020)
private val SplashNavyLight = Color(0xFF141C3A)
private val SplashTeal = Color(0xFF2DD4BF)
private val SplashTealDeep = Color(0xFF0D9488)
private val SplashIndigo = Color(0xFF818CF8)
private val SplashText = Color(0xFFE7EBF5)
private val SplashTextDim = Color(0xFF9CA7C4)

/** کل زمان نمایش اسپلش پیش از ورود به برنامه (میلی‌ثانیه) */
private const val SPLASH_DURATION_MS = 2_300L

/** گرادیان پس‌زمینه — یک‌بار ساخته می‌شود و همه‌جا استفاده می‌شود (DRY) */
private val SplashBackgroundBrush = Brush.verticalGradient(
    colors = listOf(SplashNavy, SplashNavyLight, SplashNavy)
)

/** اندازهٔ کانتینر لوگو */
private val LOGO_BOX: Dp = 132.dp

/**
 * اسپلش اسکرین برند «کیف پول من».
 *
 * ساختار بصری:
 *  ۱) پس‌زمینهٔ گرادیان سرمه‌ای + ذرات شناور ملایم
 *  ۲) لوگوی کیف (Canvas سفارشی، هم‌هندس با آیکون لانچر) درون کانتینر فیروزه‌ای
 *     با ورود فنری + هالهٔ نبض‌دار و حلقهٔ موج‌دار
 *  ۳) نام برنامه و شعار با ورود پلکانی
 *  ۴) سه نقطهٔ بارگذاری و شمارهٔ نسخه در پایین
 *
 * پس از SPLASH_DURATION_MS روی [onFinished] فراخوانی می‌شود؛ جابه‌جایی نرم
 * با Crossfade در MainActivity انجام می‌شود.
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    versionName: String = ""
) {
    // ---- انیمیشن‌های ورود ----------------------------------------------------
    val logoScale = remember { Animatable(0.55f) }
    val logoAlpha = remember { Animatable(0f) }
    val titleProgress = remember { Animatable(0f) }
    val taglineProgress = remember { Animatable(0f) }
    val footerProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        runSplashTimeline(logoScale, logoAlpha, titleProgress, taglineProgress, footerProgress)
    }

    // پایان اسپلش
    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = SplashBackgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        FloatingParticles()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // ---- لوگوی کیف ----------------------------------------------------
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.graphicsLayer {
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                    alpha = logoAlpha.value
                }
            ) {
                PulsingGlow()
                WalletLogo()
            }

            Spacer(Modifier.height(34.dp))

            // ---- نام برنامه ---------------------------------------------------
            Text(
                text = stringResource(R.string.app_name),
                color = SplashText,
                fontSize = 34.sp,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = titleProgress.value
                    translationY = (1f - titleProgress.value) * 60f
                }
            )

            Spacer(Modifier.height(10.dp))

            // ---- شعار ---------------------------------------------------------
            Text(
                text = stringResource(R.string.splash_tagline),
                color = SplashTextDim,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = taglineProgress.value
                    translationY = (1f - taglineProgress.value) * 40f
                }
            )
        }

        // ---- نوار پایین: نقطه‌های بارگذاری + نسخه ------------------------------
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
                .alpha(footerProgress.value)
        ) {
            LoadingDots()
            if (versionName.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "v$versionName",
                    color = SplashTextDim,
                    fontSize = 11.sp,
                    modifier = Modifier.alpha(0.55f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// خط زمانی ورود — همهٔ انیمیشن‌ها هم‌زمان آغاز و با تأخیر پلکانی باز می‌شوند
// ---------------------------------------------------------------------------

private suspend fun runSplashTimeline(
    logoScale: Animatable<Float, *>,
    logoAlpha: Animatable<Float, *>,
    title: Animatable<Float, *>,
    tagline: Animatable<Float, *>,
    footer: Animatable<Float, *>
) {
    coroutineScope {
        // ورود فنری لوگو
        launch {
            logoAlpha.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        // نام برنامه بعد از نشستن لوگو
        launch {
            delay(420)
            title.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
        }
        // شعار کمی بعد
        launch {
            delay(780)
            tagline.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        }
        // نوار پایین در آخر
        launch {
            delay(1_050)
            footer.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
        }
    }
}

// ---------------------------------------------------------------------------
// لوگوی کیف — بازترسیم Canvas آیکون لانچر (هندسهٔ یکسان، برش واقعی جیب)
// ---------------------------------------------------------------------------

@Composable
private fun WalletLogo() {
    val cornerPx = with(androidx.compose.ui.platform.LocalDensity.current) { 34.dp.toPx() }

    Box(contentAlignment = Alignment.Center) {
        // کانتینر گرادیان فیروزه‌ای + لمس نور و حاشیهٔ ظریف
        Canvas(modifier = Modifier.size(LOGO_BOX)) {
            val r = CornerRadius(cornerPx, cornerPx)
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(SplashTeal, SplashTealDeep),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                ),
                cornerRadius = r
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.45f
                ),
                cornerRadius = r
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.14f),
                cornerRadius = r,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // گلیف سفید کیف با برش جیب و خط لولا (لایهٔ آفسکرین تا BlendMode.Clear
        // فقط همین لایه را برش دهد و به گرادیان زیرین نرسد)
        Canvas(
            modifier = Modifier
                .size(LOGO_BOX)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            val w = size.width * 0.46f
            val h = w * 0.72f
            val left = (size.width - w) / 2f
            val top = (size.height - h) / 2f
            val cr = w * 0.16f
            val white = Color.White

            // بدنهٔ کیف
            drawRoundRect(
                color = white,
                topLeft = Offset(left, top),
                size = Size(w, h),
                cornerRadius = CornerRadius(cr, cr)
            )
            // جیب (برش از لبهٔ راست — مانند آیکون لانچر)
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(left + w * 0.72f, top + h * 0.34f),
                size = Size(w * 0.30f + 2f, h * 0.32f),
                cornerRadius = CornerRadius(h * 0.16f, h * 0.16f),
                blendMode = BlendMode.Clear
            )
            // خط لولا (برش نازک در بالای بدنه)
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(left + w * 0.10f, top + h * 0.20f),
                size = Size(w * 0.80f, h * 0.055f),
                cornerRadius = CornerRadius(h * 0.03f, h * 0.03f),
                blendMode = BlendMode.Clear
            )
            // دکمهٔ جیب
            drawCircle(
                color = white,
                radius = w * 0.048f,
                center = Offset(left + w * 0.845f, top + h * 0.50f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// هالهٔ نبض‌دار + حلقهٔ موج‌دار پشت لوگو
// ---------------------------------------------------------------------------

@Composable
private fun PulsingGlow() {
    val glowSize = LOGO_BOX * 2.1f
    val pulse = rememberInfiniteTransition(label = "glow")
    val g by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    val ripple = rememberInfiniteTransition(label = "ripple")
    val r by ripple.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_400, easing = LinearEasing)
        ),
        label = "ripplePhase"
    )

    Canvas(modifier = Modifier.size(glowSize)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val base = min(size.width, size.height) / 2f

        // هالهٔ نرم
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SplashTeal.copy(alpha = 0.30f - 0.10f * g),
                    SplashTeal.copy(alpha = 0.10f - 0.05f * g),
                    Color.Transparent
                ),
                center = center,
                radius = base * (0.86f + 0.14f * g)
            ),
            radius = base,
            center = center
        )
        // حلقهٔ موج‌دار منبسط‌شونده
        drawCircle(
            color = SplashTeal.copy(alpha = 0.30f * (1f - r)),
            radius = base * (0.52f + 0.48f * r),
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

// ---------------------------------------------------------------------------
// ذرات شناور — نقطه‌های کم‌رنگ بالارونده (قطعی با Seed ثابت)
// ---------------------------------------------------------------------------

private data class Particle(
    val xFrac: Float,
    val speed: Float,
    val phase: Float,
    val radiusDp: Float,
    val teal: Boolean
)

@Composable
private fun FloatingParticles() {
    val particles = remember {
        Random(20260207).let { rnd ->
            List(10) {
                Particle(
                    xFrac = rnd.nextFloat() * 0.9f + 0.05f,
                    speed = rnd.nextFloat() * 0.55f + 0.45f,
                    phase = rnd.nextFloat(),
                    radiusDp = rnd.nextFloat() * 1.7f + 1.1f,
                    teal = rnd.nextBoolean()
                )
            }
        }
    }
    val drift = rememberInfiniteTransition(label = "drift")
    val p by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9_000, easing = LinearEasing)
        ),
        label = "driftPhase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { part ->
            val cycle = (p * part.speed + part.phase) % 1f
            val alpha = 0.20f * sin(cycle * Math.PI).toFloat()
            if (alpha <= 0.005f) return@forEach
            drawCircle(
                color = if (part.teal) SplashTeal else SplashIndigo,
                radius = part.radiusDp.dp.toPx(),
                center = Offset(part.xFrac * size.width, (1f - cycle) * size.height),
                alpha = alpha
            )
        }
    }
}

// ---------------------------------------------------------------------------
// سه نقطهٔ بارگذاری
// ---------------------------------------------------------------------------

@Composable
private fun LoadingDots() {
    val transition = rememberInfiniteTransition(label = "dots")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_050, easing = LinearEasing)
        ),
        label = "dotsPhase"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val local = ((phase * 3f - i + 3f) % 3f) / 3f
            val lift = sin(local * Math.PI).toFloat()
            Box(
                Modifier
                    .offset(y = (-6).dp * lift)
                    .alpha(0.35f + 0.65f * lift)
                    .padding(horizontal = 5.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(SplashTeal, CircleShape)
            )
        }
    }
}
