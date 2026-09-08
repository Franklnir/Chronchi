package com.irsyadlabs.espbridge.core.model

import androidx.compose.runtime.Immutable

enum class SourceCategory(val title: String) {
    SOCIAL("Messaging & Social"),
    PROFESSIONAL("Professional"),
    PAYMENTS("Payment"),
    BANKING("Banking"),
    SHOPPING("Marketplace"),
    SYSTEM("System Sources")
}

@Immutable
data class AppSource(
    val id: String,
    val label: String,
    val packageName: String?,
    val category: SourceCategory,
    val defaultEnabled: Boolean = false,
    val systemSource: Boolean = false
)

object SupportedSources {
    val all = listOf(
        AppSource("whatsapp", "WhatsApp", "com.whatsapp", SourceCategory.SOCIAL, true),
        AppSource("whatsapp_business", "WhatsApp Business", "com.whatsapp.w4b", SourceCategory.SOCIAL),
        AppSource("telegram", "Telegram", "org.telegram.messenger", SourceCategory.SOCIAL, true),
        AppSource("instagram", "Instagram", "com.instagram.android", SourceCategory.SOCIAL, true),
        AppSource("facebook", "Facebook", "com.facebook.katana", SourceCategory.SOCIAL),
        AppSource("messenger", "Messenger", "com.facebook.orca", SourceCategory.SOCIAL),
        AppSource("tiktok", "TikTok", "com.zhiliaoapp.musically", SourceCategory.SOCIAL),
        AppSource("x", "X", "com.twitter.android", SourceCategory.SOCIAL),
        AppSource("threads", "Threads", "com.instagram.barcelona", SourceCategory.SOCIAL),

        AppSource("gmail", "Gmail", "com.google.android.gm", SourceCategory.PROFESSIONAL, true),
        AppSource("linkedin", "LinkedIn", "com.linkedin.android", SourceCategory.PROFESSIONAL, true),
        AppSource("jobstreet", "JobStreet", "com.jobstreet.jobstreet", SourceCategory.PROFESSIONAL, true),

        AppSource("dana", "DANA", "id.dana", SourceCategory.PAYMENTS, true),
        AppSource("ovo", "OVO", "ovo.id", SourceCategory.PAYMENTS),
        AppSource("gopay", "GoPay / Gojek", "com.gojek.app", SourceCategory.PAYMENTS, true),
        AppSource("shopeepay", "ShopeePay", "com.shopee.id", SourceCategory.PAYMENTS, true),

        AppSource("mybca", "myBCA", "com.bca.mybca.omni.android", SourceCategory.BANKING),
        AppSource("bca_mobile", "BCA mobile", "com.bca", SourceCategory.BANKING),
        AppSource("brimo", "BRImo", "id.co.bri.brimo", SourceCategory.BANKING),
        AppSource("livin", "Livin' by Mandiri", "id.bmri.livin", SourceCategory.BANKING),
        AppSource("wondr", "wondr by BNI", "id.bni.wondr", SourceCategory.BANKING),
        AppSource("octo", "OCTO / CIMB Niaga", "id.co.cimbniaga.mobile.android", SourceCategory.BANKING),
        AppSource("seabank", "SeaBank", "id.co.bankbkemobile.digitalbank", SourceCategory.BANKING),
        AppSource("jago", "Bank Jago", "com.jago.digitalBanking", SourceCategory.BANKING),
        AppSource("superbank", "Superbank", "id.co.bankfama.android", SourceCategory.BANKING),

        AppSource("shopee", "Shopee", "com.shopee.id", SourceCategory.SHOPPING, true),
        AppSource("shopee_partner", "Shopee Partner", "com.shopeepay.merchant.id", SourceCategory.SHOPPING),
        AppSource("tokopedia", "Tokopedia", "com.tokopedia.tkpd", SourceCategory.SHOPPING),
        AppSource("lazada", "Lazada", "com.lazada.android", SourceCategory.SHOPPING),
        AppSource("gojek", "Gojek", "com.gojek.app", SourceCategory.SHOPPING),
        AppSource("grab", "Grab", "com.grabtaxi.passenger", SourceCategory.SHOPPING),

        AppSource("navigation", "Navigation", null, SourceCategory.SYSTEM, true, true),
        AppSource("weather", "Weather", null, SourceCategory.SYSTEM, true, true),
        AppSource("location", "Location", null, SourceCategory.SYSTEM, true, true),
        AppSource("phone_status", "Phone Status", null, SourceCategory.SYSTEM, true, true),
        AppSource("network_status", "Network Status", null, SourceCategory.SYSTEM, true, true)
    )

    fun byPackage(packageName: String): AppSource? = all.firstOrNull { it.packageName == packageName }
    fun allByPackage(packageName: String): List<AppSource> = all.filter { it.packageName == packageName }
}
