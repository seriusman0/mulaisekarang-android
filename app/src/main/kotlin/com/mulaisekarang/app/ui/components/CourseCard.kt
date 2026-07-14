package com.mulaisekarang.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mulaisekarang.app.ui.theme.Accent
import java.text.NumberFormat
import java.util.Locale

val IdrCurrencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

/** List-style course card: thumbnail + category/rating row + title + mentor + [footer]. */
@Composable
fun CourseCard(
    title: String,
    coverImageUrl: String?,
    mentorName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    categoryLabel: String? = null,
    rating: Double? = null,
    footer: @Composable ColumnScope.() -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AsyncImage(
                model = coverImageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                if (categoryLabel != null || rating != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        categoryLabel?.let {
                            Text(
                                it.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                        rating?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = Accent, modifier = Modifier.size(12.dp))
                                Text(
                                    " $it",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Accent,
                                )
                            }
                        }
                    }
                }
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
                mentorName?.let {
                    Text(
                        "oleh $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                footer()
            }
        }
    }
}
