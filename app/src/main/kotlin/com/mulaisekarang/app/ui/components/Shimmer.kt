package com.mulaisekarang.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * A slow left-to-right sheen brush for skeleton placeholders — stands in for
 * real content while it loads so the layout never collapses to a blank
 * screen or a single centered spinner.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val highlightColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )
    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim - 500f, 0f),
        end = Offset(translateAnim, 0f),
    )
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(rememberShimmerBrush()),
    ) {}
}

/** Mirrors [com.mulaisekarang.app.ui.components.CourseCard]'s layout so the
 * placeholder occupies exactly the space the real card will. */
@Composable
fun CourseCardSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ShimmerBox(modifier = Modifier.size(80.dp), shape = RoundedCornerShape(16.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.3f).height(10.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.85f).height(16.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f).height(16.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp))
        }
    }
}

@Composable
fun CourseListSkeleton(modifier: Modifier = Modifier, itemCount: Int = 6) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(itemCount) { CourseCardSkeleton() }
    }
}

/** Mirrors the hero + price-card + lesson-row shape of [com.mulaisekarang.app.ui.screens.CourseDetailScreen]. */
@Composable
fun CourseDetailSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(220.dp), shape = RoundedCornerShape(0.dp))
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.8f).height(24.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.4f).height(16.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(96.dp), shape = RoundedCornerShape(16.dp))
            repeat(3) {
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp))
            }
        }
    }
}

/** Mirrors [com.mulaisekarang.app.ui.screens.LessonPlayerScreen]'s video + title + body shape. */
@Composable
fun LessonPlayerSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(220.dp), shape = RoundedCornerShape(0.dp))
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(26.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.3f).height(14.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(14.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.9f).height(14.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp))
        }
    }
}
