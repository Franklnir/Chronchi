package com.irsyadlabs.espbridge.ui.components

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.irsyadlabs.espbridge.R
import com.irsyadlabs.espbridge.core.model.AppSource
import com.irsyadlabs.espbridge.ui.theme.BrandBlue
import com.irsyadlabs.espbridge.ui.theme.BrandRed
import com.irsyadlabs.espbridge.ui.theme.BrandYellow
import com.irsyadlabs.espbridge.ui.theme.InkBlack
import com.irsyadlabs.espbridge.ui.theme.SoftBlue
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppIconCache {
    private val bitmaps = ConcurrentHashMap<Int, ImageBitmap>()

    fun snapshot(): Map<Int, ImageBitmap> = bitmaps.toMap()

    suspend fun preload(resources: Resources, sourceIds: Collection<String>): Map<Int, ImageBitmap> =
        withContext(Dispatchers.IO) {
            sourceIds.mapNotNull(::bundledAppIcon).toSet().forEach { resourceId ->
                if (!bitmaps.containsKey(resourceId)) {
                    BitmapFactory.decodeResource(resources, resourceId)
                        ?.asImageBitmap()
                        ?.let { bitmaps.putIfAbsent(resourceId, it) }
                }
            }
            snapshot()
        }
}

@Composable
fun SourceIcon(
    source: AppSource,
    bitmap: ImageBitmap? = null,
    modifier: Modifier = Modifier
) {
    if (source.systemSource) {
        val icon = when (source.id) {
            "navigation" -> Icons.Rounded.Navigation
            "weather" -> Icons.Rounded.WbSunny
            "location" -> Icons.Rounded.LocationOn
            "phone_status" -> Icons.Rounded.PhoneAndroid
            "network_status" -> Icons.Rounded.Cloud
            else -> Icons.Rounded.Bluetooth
        }
        Box(modifier.size(44.dp).background(SoftBlue, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = source.label, tint = BrandBlue)
        }
        return
    }

    val bundledIcon = bundledAppIcon(source.id)
    if (bundledIcon != null && bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = source.label,
            modifier = modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
        )
        return
    }

    val bg = when ((source.id.hashCode() and 0x7fffffff) % 3) {
        0 -> BrandYellow
        1 -> BrandRed
        else -> BrandBlue
    }
    Box(
        modifier = modifier.size(44.dp).background(bg, RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            source.label.take(1).uppercase(),
            color = if (bg == BrandYellow) InkBlack else Color.White,
            fontWeight = FontWeight.Black
        )
    }
}

@DrawableRes
internal fun bundledAppIcon(sourceId: String): Int? = when (sourceId) {
    "whatsapp" -> R.drawable.ic_app_whatsapp
    "whatsapp_business" -> R.drawable.ic_app_whatsapp_business
    "telegram" -> R.drawable.ic_app_telegram
    "instagram" -> R.drawable.ic_app_instagram
    "facebook" -> R.drawable.ic_app_facebook
    "messenger" -> R.drawable.ic_app_messenger
    "tiktok" -> R.drawable.ic_app_tiktok
    "x" -> R.drawable.ic_app_x
    "threads" -> R.drawable.ic_app_threads
    "gmail" -> R.drawable.ic_app_gmail
    "linkedin" -> R.drawable.ic_app_linkedin
    "jobstreet" -> R.drawable.ic_app_jobstreet
    "dana" -> R.drawable.ic_app_dana
    "ovo" -> R.drawable.ic_app_ovo
    "gopay", "gojek" -> R.drawable.ic_app_gojek
    "shopeepay", "shopee" -> R.drawable.ic_app_shopee
    "mybca" -> R.drawable.ic_app_mybca
    "bca_mobile" -> R.drawable.ic_app_bca_mobile
    "brimo" -> R.drawable.ic_app_brimo
    "livin" -> R.drawable.ic_app_livin
    "wondr" -> R.drawable.ic_app_wondr
    "octo" -> R.drawable.ic_app_octo
    "seabank" -> R.drawable.ic_app_seabank
    "jago" -> R.drawable.ic_app_jago
    "superbank" -> R.drawable.ic_app_superbank
    "shopee_partner" -> R.drawable.ic_app_shopee_partner
    "tokopedia" -> R.drawable.ic_app_tokopedia
    "lazada" -> R.drawable.ic_app_lazada
    "grab" -> R.drawable.ic_app_grab
    else -> null
}
