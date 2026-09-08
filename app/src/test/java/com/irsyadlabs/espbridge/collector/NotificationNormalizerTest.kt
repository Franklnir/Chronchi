package com.irsyadlabs.espbridge.collector

import com.irsyadlabs.espbridge.core.model.DisplayCategory
import com.irsyadlabs.espbridge.core.model.OrderStatus
import com.irsyadlabs.espbridge.core.model.PaymentDirection
import com.irsyadlabs.espbridge.core.model.SupportedSources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationNormalizerTest {
    @Test
    fun `WhatsApp keeps sender and short message preview`() {
        val event = normalize("whatsapp", "Budi", "Halo, nanti malam jadi ketemu jam 8 di rumah saya?")

        requireNotNull(event)
        assertEquals(DisplayCategory.MESSAGE, event.category)
        assertEquals("Budi", event.primaryText)
        assertTrue(requireNotNull(event.secondaryText).length <= 40)
        assertEquals("WhatsApp", event.espDisplayLines().first())
    }

    @Test
    fun `Gmail keeps sender and normalizes OTP subject`() {
        val event = normalize("gmail", "Google", "Your verification code is 381294")

        requireNotNull(event)
        assertEquals(DisplayCategory.PROFESSIONAL, event.category)
        assertEquals("Google", event.primaryText)
        assertEquals("Kode OTP 381294", event.secondaryText)
    }

    @Test
    fun `DANA extracts incoming amount and sender`() {
        val event = normalize("dana", "DANA", "Anda menerima uang Rp50.000 dari Budi")

        requireNotNull(event)
        assertEquals(DisplayCategory.PAYMENT, event.category)
        assertEquals(PaymentDirection.INCOMING, event.paymentDirection)
        assertEquals("+ Rp50.000", event.primaryText)
        assertEquals("Dari Budi", event.secondaryText)
    }

    @Test
    fun `bank transfer extracts incoming amount and sender`() {
        val event = normalize("brimo", "BRImo", "Transfer masuk Rp250.000 dari Andi")

        requireNotNull(event)
        assertEquals(DisplayCategory.PAYMENT, event.category)
        assertEquals(PaymentDirection.INCOMING, event.paymentDirection)
        assertEquals("+ Rp250.000", event.primaryText)
        assertEquals("Dari Andi", event.secondaryText)
    }

    @Test
    fun `Shopee normalizes courier status`() {
        val event = normalize("shopee", "Status Pesanan", "Paket Anda sedang dibawa kurir menuju alamat tujuan")

        requireNotNull(event)
        assertEquals(DisplayCategory.ORDER, event.category)
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, event.orderStatus)
        assertEquals("Sedang Diantar", event.primaryText)
    }

    @Test
    fun `shared Shopee package falls through payment parser to order parser`() {
        val matchingSources = SupportedSources.all.filter { it.packageName == "com.shopee.id" }
        val event = matchingSources.firstNotNullOfOrNull { source ->
            NotificationNormalizer.normalize(
                source = source,
                title = "Status Pesanan",
                text = "Paket Rp50.000 sedang dibawa kurir menuju alamat tujuan",
                timestamp = 1_000L
            )
        }

        requireNotNull(event)
        assertEquals("Shopee", event.sourceApp)
        assertEquals(DisplayCategory.ORDER, event.category)
    }

    @Test
    fun `delivered Shopee package is not mistaken for incoming payment`() {
        val matchingSources = SupportedSources.all.filter { it.packageName == "com.shopee.id" }
        val event = matchingSources.firstNotNullOfOrNull { source ->
            NotificationNormalizer.normalize(
                source = source,
                title = "Status Pesanan",
                text = "Paket Anda telah diterima",
                timestamp = 1_000L
            )
        }

        requireNotNull(event)
        assertEquals(DisplayCategory.ORDER, event.category)
        assertEquals(OrderStatus.DELIVERED, event.orderStatus)
    }

    @Test
    fun `payment promotion is filtered`() {
        val event = normalize("dana", "Promo spesial", "Dapatkan diskon dan cashback hingga Rp50.000")

        assertNull(event)
    }

    @Test
    fun `marketplace promotion is filtered`() {
        val event = normalize("shopee", "Flash Sale", "Promo diskon dan gratis ongkir hari ini")

        assertNull(event)
    }

    @Test
    fun `JobStreet keeps role and company short`() {
        val event = normalize("jobstreet", "Backend Developer", "PT ABC")

        requireNotNull(event)
        assertEquals(DisplayCategory.PROFESSIONAL, event.category)
        assertEquals("Backend Developer", event.primaryText)
        assertEquals("PT ABC", event.secondaryText)
    }

    @Test
    fun `supported social notification has safe short fallback`() {
        val event = normalize("instagram", "Instagram", "Seseorang menyukai postingan Anda yang sangat panjang sekali")

        requireNotNull(event)
        assertEquals(DisplayCategory.MESSAGE, event.category)
        assertTrue(event.primaryText.isNotBlank())
        assertTrue(event.primaryText.length <= 28)
    }

    @Test
    fun `sanitizer collapses whitespace preserves currency and truncates`() {
        val result = NotificationNormalizer.sanitizeText("  +  Rp75.000\n  dari   Andi  ", 18)

        assertEquals("+ Rp75.000 dari A…", result)
    }

    private fun normalize(sourceId: String, title: String, text: String) =
        NotificationNormalizer.normalize(
            source = SupportedSources.all.first { it.id == sourceId },
            title = title,
            text = text,
            timestamp = 1_000L
        )
}
