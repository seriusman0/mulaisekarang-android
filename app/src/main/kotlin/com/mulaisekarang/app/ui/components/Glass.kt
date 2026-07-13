package com.mulaisekarang.app.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.mulaisekarang.app.ui.theme.GlassBorderAlpha
import com.mulaisekarang.app.ui.theme.GlassSurfaceAlpha
import com.mulaisekarang.app.ui.theme.GlassSurfaceDark
import com.mulaisekarang.app.ui.theme.GlassSurfaceLight

/**
 * The frosted-glass background layer shared by every glassmorphism surface in the app
 * (bottom nav bar, profile cards, ...). Must be placed as a [Box] sibling *before* the
 * sharp foreground content — [Modifier.blur] blurs its entire subtree, so blurring a
 * parent of the foreground would blur the content too.
 */
@Composable
fun BoxScope.GlassBackground(shape: Shape, modifier: Modifier = Modifier) {
    val glassColor = if (isSystemInDarkTheme()) GlassSurfaceDark else GlassSurfaceLight
    val blurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(
        modifier = modifier
            .matchParentSize()
            .shadow(elevation = 12.dp, shape = shape, clip = false)
            .clip(shape)
            .then(if (blurSupported) Modifier.blur(20.dp) else Modifier)
            .background(glassColor.copy(alpha = GlassSurfaceAlpha))
            .border(1.dp, Color.White.copy(alpha = GlassBorderAlpha), shape),
    )
}

/** A ready-to-use frosted-glass card: [GlassBackground] plus a padded [Column] of content. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier) {
        GlassBackground(shape = shape)
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
