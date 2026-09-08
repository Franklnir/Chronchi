package com.irsyadlabs.espbridge.collector

import com.irsyadlabs.espbridge.core.model.AppSource
import com.irsyadlabs.espbridge.core.model.DisplayEvent
import com.irsyadlabs.espbridge.core.model.DisplayCategory
import com.irsyadlabs.espbridge.core.model.OrderStatus
import com.irsyadlabs.espbridge.core.model.PaymentDirection
import com.irsyadlabs.espbridge.core.model.SourceCategory
import java.util.Locale

object NotificationNormalizer {
    private const val PRIMARY_LIMIT = 28
    private const val SECONDARY_LIMIT = 32
    private const val MESSAGE_PREVIEW_LIMIT = 40
    private const val RAW_LIMIT = 180

    private val messageSourceIds = setOf("whatsapp", "whatsapp_business", "telegram", "messenger")
    private val marketplaceSourceIds = setOf("shopee", "tokopedia", "lazada")
    private val promoKeywords = listOf(
        "promo", "promosi", "diskon", "voucher", "flash sale", "gratis ongkir",
        "penawaran", "cashback hingga", "buruan", "belanja sekarang"
    )
    private val incomingKeywords = listOf(
        "menerima", "diterima", "transfer masuk", "uang masuk", "dana masuk",
        "saldo bertambah", "berhasil masuk", "received", "credited", "kredit masuk"
    )
    private val outgoingKeywords = listOf(
        "transfer berhasil", "pembayaran berhasil", "membayar", "dibayar", "terkirim",
        "sent", "debit", "dana keluar", "purchase", "pembayaran"
    )
    private val transactionKeywords = incomingKeywords + outgoingKeywords + listOf("transfer", "transaksi", "top up")
    private val financialKeywords = listOf(
        "rp", "idr", "uang", "dana", "saldo", "transfer", "transaksi",
        "pembayaran", "top up", "refund", "debit", "kredit"
    )
    private val amountRegex = Regex(
        """(?i)\b(?:rp\.?|idr)\s*([0-9]+(?:[.\s][0-9]{3})*(?:,[0-9]{1,2})?)"""
    )
    private val otpRegex = Regex("""(?<!\d)(\d{4,8})(?!\d)""")
    private val incomingPartyRegex = Regex(
        """(?i)\b(?:dari|from)\s+([\p{L}\p{N}][\p{L}\p{N} .'-]{0,40}?)(?=\s+(?:sebesar|senilai|melalui|pada|ke rekening)\b|[,.•]|$)"""
    )
    private val outgoingPartyRegex = Regex(
        """(?i)\b(?:kepada|ke|to)\s+([\p{L}\p{N}][\p{L}\p{N} .'-]{0,40}?)(?=\s+(?:sebesar|senilai|melalui|pada|dengan)\b|[,.•]|$)"""
    )

    fun normalize(
        source: AppSource,
        title: String,
        text: String,
        subText: String = "",
        timestamp: Long = System.currentTimeMillis()
    ): DisplayEvent? {
        val safeTitle = clean(title)
        val safeText = clean(text)
        val safeSubText = clean(subText)
        val combined = listOf(safeTitle, safeText, safeSubText)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" • ")

        return when {
            source.id in messageSourceIds -> normalizeMessage(source, safeTitle, safeText, timestamp)
            source.id == "gmail" -> normalizeEmail(source, safeTitle, safeText, timestamp)
            source.category == SourceCategory.PAYMENTS || source.category == SourceCategory.BANKING ->
                normalizeTransaction(source, combined, timestamp)
            source.id in marketplaceSourceIds -> normalizeOrder(source, safeTitle, safeText, combined, timestamp)
            source.id == "linkedin" || source.id == "jobstreet" ->
                normalizeProfessional(source, safeTitle, safeText, timestamp)
            else -> normalizeFallback(source, safeTitle, safeText, timestamp)
        }
    }

    fun rawPreview(title: String, text: String, subText: String = ""): String =
        listOf(title, text, subText)
            .map(::clean)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("\n")
            .take(RAW_LIMIT)

    private fun normalizeMessage(
        source: AppSource,
        title: String,
        text: String,
        timestamp: Long
    ): DisplayEvent {
        val sender = title
            .replace(Regex("""\s*\(\d+\s+(?:pesan|messages?)\)\s*$""", RegexOption.IGNORE_CASE), "")
            .takeUnless { isGenericTitle(it, source) }
            ?.let { shorten(it, PRIMARY_LIMIT) }
            ?: "Pesan Baru"
        val preview = text
            .takeIf { it.isNotBlank() && !it.equals(sender, ignoreCase = true) }
            ?.let { shorten(it, MESSAGE_PREVIEW_LIMIT) }
        return DisplayEvent(
            sourceApp = displayName(source),
            category = DisplayCategory.MESSAGE,
            primaryText = sender,
            secondaryText = preview,
            timestamp = timestamp
        )
    }

    private fun normalizeEmail(source: AppSource, title: String, text: String, timestamp: Long): DisplayEvent {
        val sender = title.takeUnless { isGenericTitle(it, source) }
            ?.let { shorten(it, PRIMARY_LIMIT) }
            ?: "Email Baru"
        val subject = normalizeOtp(text).ifBlank { shorten(text, SECONDARY_LIMIT) }.takeIf { it.isNotBlank() }
        return DisplayEvent(
            sourceApp = displayName(source),
            category = DisplayCategory.PROFESSIONAL,
            primaryText = sender,
            secondaryText = subject,
            timestamp = timestamp
        )
    }

    private fun normalizeTransaction(source: AppSource, combined: String, timestamp: Long): DisplayEvent? {
        val lower = combined.lowercase(Locale.ROOT)
        if (promoKeywords.any(lower::contains)) return null

        val direction = when {
            incomingKeywords.any(lower::contains) -> PaymentDirection.INCOMING
            outgoingKeywords.any(lower::contains) -> PaymentDirection.OUTGOING
            else -> PaymentDirection.UNKNOWN
        }
        val amount = amountRegex.find(combined)?.groupValues?.getOrNull(1)
            ?.replace(" ", "")
            ?.let { "Rp$it" }
        if (transactionKeywords.none(lower::contains) || (amount == null && financialKeywords.none(lower::contains))) {
            return null
        }

        val primary = when {
            amount != null -> "${direction.symbol} $amount".trim()
            direction == PaymentDirection.INCOMING -> "Dana Masuk"
            direction == PaymentDirection.OUTGOING -> "Transaksi Keluar"
            else -> "Transaksi"
        }
        val party = extractParty(combined, direction)?.let {
            when (direction) {
                PaymentDirection.INCOMING -> "Dari $it"
                PaymentDirection.OUTGOING -> "Ke $it"
                PaymentDirection.UNKNOWN -> it
            }
        } ?: if (direction == PaymentDirection.OUTGOING && lower.contains("pembayaran")) {
            "Pembayaran"
        } else {
            null
        }

        return DisplayEvent(
            sourceApp = displayName(source),
            category = DisplayCategory.PAYMENT,
            primaryText = shorten(primary, PRIMARY_LIMIT),
            secondaryText = party,
            timestamp = timestamp,
            paymentDirection = direction
        )
    }

    private fun normalizeOrder(
        source: AppSource,
        title: String,
        text: String,
        combined: String,
        timestamp: Long
    ): DisplayEvent? {
        val lower = combined.lowercase(Locale.ROOT)
        if (promoKeywords.any(lower::contains)) return null

        val status = when {
            containsAny(lower, "dibatalkan", "cancelled", "canceled") -> OrderStatus.CANCELLED
            containsAny(lower, "sedang diantar", "dibawa kurir", "out for delivery") ||
                (lower.contains("kurir") && lower.contains("menuju")) -> OrderStatus.OUT_FOR_DELIVERY
            containsAny(lower, "telah diterima", "pesanan tiba", "paket tiba", "delivered") -> OrderStatus.DELIVERED
            containsAny(lower, "dalam perjalanan", "in transit", "menuju hub", "sedang menuju") -> OrderStatus.IN_TRANSIT
            containsAny(lower, "dikirim", "shipped", "diserahkan ke jasa kirim") -> OrderStatus.SHIPPED
            containsAny(lower, "dikemas", "packed", "diproses penjual") -> OrderStatus.PACKED
            containsAny(lower, "dikonfirmasi", "order confirmed", "pesanan dibuat") -> OrderStatus.CONFIRMED
            else -> OrderStatus.UNKNOWN
        }
        val hasOrderSignal = containsAny(lower, "paket", "pesanan", "order", "kurir")
        if (status == OrderStatus.UNKNOWN && !hasOrderSignal) return null

        val fallback = sequenceOf(text, title)
            .firstOrNull { it.isNotBlank() && !isGenericTitle(it, source) }
            ?.let { shorten(it, SECONDARY_LIMIT) }
            ?: OrderStatus.UNKNOWN.label
        return DisplayEvent(
            sourceApp = displayName(source),
            category = DisplayCategory.ORDER,
            primaryText = if (status == OrderStatus.UNKNOWN) fallback else status.label,
            timestamp = timestamp,
            orderStatus = status
        )
    }

    private fun normalizeProfessional(
        source: AppSource,
        title: String,
        text: String,
        timestamp: Long
    ): DisplayEvent {
        val primary = title.takeUnless { isGenericTitle(it, source) }
            ?.let { shorten(it, PRIMARY_LIMIT) }
            ?: shorten(text, PRIMARY_LIMIT).ifBlank { "Notifikasi Baru" }
        val secondary = text
            .takeIf { it.isNotBlank() && !it.equals(primary, ignoreCase = true) }
            ?.let { shorten(it, SECONDARY_LIMIT) }
        return DisplayEvent(
            sourceApp = displayName(source),
            category = DisplayCategory.PROFESSIONAL,
            primaryText = primary,
            secondaryText = secondary,
            timestamp = timestamp
        )
    }

    private fun normalizeFallback(
        source: AppSource,
        title: String,
        text: String,
        timestamp: Long
    ): DisplayEvent {
        val primary = title.takeUnless { isGenericTitle(it, source) }
            ?.let { shorten(it, PRIMARY_LIMIT) }
            ?: shorten(text, PRIMARY_LIMIT).ifBlank { "Notifikasi Baru" }
        val secondary = text
            .takeIf { it.isNotBlank() && !it.equals(primary, ignoreCase = true) }
            ?.let { shorten(it, MESSAGE_PREVIEW_LIMIT) }
        return DisplayEvent(
            sourceApp = displayName(source),
            category = when (source.category) {
                SourceCategory.PROFESSIONAL -> DisplayCategory.PROFESSIONAL
                SourceCategory.SYSTEM -> DisplayCategory.SYSTEM
                else -> DisplayCategory.MESSAGE
            },
            primaryText = primary,
            secondaryText = secondary,
            timestamp = timestamp
        )
    }

    private fun normalizeOtp(value: String): String {
        val lower = value.lowercase(Locale.ROOT)
        if (!containsAny(lower, "otp", "kode", "code", "verification", "verifikasi")) return ""
        val code = otpRegex.find(value)?.groupValues?.getOrNull(1) ?: return ""
        return "Kode OTP $code"
    }

    private fun extractParty(value: String, direction: PaymentDirection): String? {
        val regex = when (direction) {
            PaymentDirection.INCOMING -> incomingPartyRegex
            PaymentDirection.OUTGOING -> outgoingPartyRegex
            PaymentDirection.UNKNOWN -> return null
        }
        val party = regex.find(value)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (party.isBlank() || party.startsWith("rekening", ignoreCase = true)) return null
        return shorten(party, PRIMARY_LIMIT)
    }

    private fun displayName(source: AppSource): String = when (source.id) {
        "gopay" -> "GoPay"
        "shopeepay" -> "ShopeePay"
        "livin" -> "Livin'"
        "octo" -> "OCTO"
        else -> source.label
    }

    private fun isGenericTitle(value: String, source: AppSource): Boolean {
        val lower = value.lowercase(Locale.ROOT).trim()
        return lower.isBlank() ||
            lower == source.label.lowercase(Locale.ROOT) ||
            lower == displayName(source).lowercase(Locale.ROOT) ||
            containsAny(lower, "new notification", "notifikasi baru", "pesan baru")
    }

    internal fun sanitizeText(value: String, maxLength: Int = Int.MAX_VALUE): String {
        val cleaned = value
            .replace(Regex("""[\u0000-\u001F\u007F]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (cleaned.length <= maxLength) return cleaned
        if (maxLength <= 1) return cleaned.take(maxLength)
        return cleaned.take(maxLength - 1).trimEnd() + "…"
    }

    private fun clean(value: String): String = sanitizeText(value)

    private fun shorten(value: String, maxLength: Int): String = sanitizeText(value, maxLength)

    private fun containsAny(source: String, vararg needles: String): Boolean = needles.any(source::contains)
}
