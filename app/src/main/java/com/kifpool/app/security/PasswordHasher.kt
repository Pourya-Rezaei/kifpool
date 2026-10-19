package com.kifpool.app.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * هَش‌کنندهٔ رمز — انتزاع قابل تعویض (تست‌پذیری + توسعه‌پذیری).
 *
 * پیاده‌سازی پیش‌فرض: PBKDF2WithHmacSHA256 با salt تصادفی برای هر راز.
 * (SHA-256 خالص برای PIN چندرقمی ناامن است؛ PBKDF2 هزینهٔ محاسبهٔ هر حدس را
 * هزاران برابر می‌کند.)
 *
 * فرمت ذخیره‌شده:
 *   pbkdf2-sha256$<iterations>$<saltBase64Url>$<hashBase64Url>
 *
 * سازگاری با نسخه‌های قدیمی (مهاجرت شفاف):
 *   هش‌های ۶۴ کاراکتری هگز (SHA-256 بدون salt از نسخه‌های ≤ ۱٫۶٫۰) هنگام
 *   verify پذیرفته می‌شوند و [needsUpgrade] true برمی‌گرداند تا پس از اولین
 *   ورود موفق، دوباره با PBKDF2 ذخیره شوند.
 */
interface PasswordHasher {

    /** یک راز را هش می‌کند و رشتهٔ ذخیره‌شونده (شامل salt و پارامترها) را می‌دهد. */
    fun hash(secret: String): String

    /** راز را با هشِ ذخیره‌شده مقایسهٔ زمان‌ثابت می‌کند. */
    fun verify(secret: String, stored: String): Boolean

    /** آیا هش ذخیره‌شده با الگوریتم/پارامتر فعلی ساخته نشده و باید ارتقا یابد؟ */
    fun needsUpgrade(stored: String): Boolean
}

/**
 * PBKDF2 با پارامترهای متعارف موبایل:
 * ۱۲۰٬۰۰۰ تکرار / خروجی ۲۵۶ بیت / salt تصادفی ۱۶ بایتی (SecureRandom).
 *
 * کاملاً JVM (javax.crypto + java.util.Base64) — بدون وابستگی اندروید،
 * مستقیماً در تست‌های واحد قابل استفاده است. (java.util.Base64 از API 26.)
 */
object Pbkdf2Hasher : PasswordHasher {

    const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val PREFIX = "pbkdf2-sha256"
    private const val LEGACY_HEX_LENGTH = 64

    private val random = SecureRandom()

    override fun hash(secret: String): String {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val hash = pbkdf2(secret, salt, ITERATIONS)
        return "$PREFIX$$ITERATIONS$${b64(salt)}$${b64(hash)}"
    }

    override fun verify(secret: String, stored: String): Boolean = try {
        if (stored.startsWith("$PREFIX$")) {
            val parts = stored.split('$')
            if (parts.size != 4) {
                false
            } else {
                val iterations = parts[1].toIntOrNull()?.takeIf { it in 1..2_000_000 }
                    ?: return false
                val salt = unb64(parts[2])
                val expected = unb64(parts[3])
                val actual = pbkdf2(secret, salt, iterations)
                // مقایسهٔ زمان‌ثابت — مقاوم به حملهٔ timing
                MessageDigest.isEqual(expected, actual)
            }
        } else {
            verifyLegacySha256(secret, stored)
        }
    } catch (e: Exception) {
        false
    }

    override fun needsUpgrade(stored: String): Boolean =
        !stored.startsWith("$PREFIX$$ITERATIONS$")

    // ---- سازگاری با نسخه‌های قدیمی (مهاجرت) ---------------------------------

    /** هش‌های قدیمی نسخه‌های ≤ ۱٫۶٫۰ (SHA-256 hex بدون salt) — فقط برای مهاجرت. */
    private fun verifyLegacySha256(secret: String, stored: String): Boolean {
        if (stored.length != LEGACY_HEX_LENGTH || !stored.all { it.isDigit() || it in 'a'..'f' }) {
            return false
        }
        val actual = sha256Hex(secret)
        return MessageDigest.isEqual(
            actual.toByteArray(Charsets.US_ASCII),
            stored.toByteArray(Charsets.US_ASCII)
        )
    }

    private fun sha256Hex(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    // ---- helpers ------------------------------------------------------------

    private fun pbkdf2(secret: String, salt: ByteArray, iterations: Int): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(
                PBEKeySpec(secret.toCharArray(), salt, iterations, KEY_BITS)
            ).encoded

    private fun b64(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun unb64(text: String): ByteArray =
        Base64.getUrlDecoder().decode(text)
}
