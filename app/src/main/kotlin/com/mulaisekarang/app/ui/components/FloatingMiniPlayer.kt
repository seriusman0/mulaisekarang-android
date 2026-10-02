
package com.mulaisekarang.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.mulaisekarang.app.util.GlobalVideoManager

@Composable
fun FloatingMiniPlayer(modifier: Modifier = Modifier) {
    if (!GlobalVideoManager.isMiniPlayerVisible.value || GlobalVideoManager.exoPlayer == null) return

    Surface(
        modifier = modifier
            .padding(16.dp)
            .width(200.dp)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp)),
        shadowElevation = 8.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx -> 
                    PlayerView(ctx).apply { 
                        useController = false 
                    } 
                },
                update = { view -> 
                    view.player = GlobalVideoManager.exoPlayer 
                }
            )

            // Close button
            IconButton(
                onClick = { GlobalVideoManager.close() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Tutup Video",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
