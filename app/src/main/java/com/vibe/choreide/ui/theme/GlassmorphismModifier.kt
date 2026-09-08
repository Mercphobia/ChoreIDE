package com.vibe.choreide.ui.theme

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

fun Modifier.glassPanel(cornerRadius: Int = 16): Modifier = composed {
    val shape = RoundedCornerShape(cornerRadius.dp)
    this
        .clip(shape)
        .background(GlassBackground)
        .border(1.dp, GlassBorder, shape)
}
