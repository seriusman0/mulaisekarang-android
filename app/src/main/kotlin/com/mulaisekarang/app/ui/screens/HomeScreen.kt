package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush as ComposeBrush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mulaisekarang.app.data.model.Category
import com.mulaisekarang.app.ui.components.CourseCard
import com.mulaisekarang.app.ui.components.IdrCurrencyFormat
import com.mulaisekarang.app.viewmodel.AuthUiState
import com.mulaisekarang.app.viewmodel.AuthViewModel
import com.mulaisekarang.app.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    authViewModel: AuthViewModel,
    onBrowseMarketplace: () -> Unit,
    onCourseClick: (Int) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val user = (authState as? AuthUiState.LoggedIn)?.user

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Text(
                    "Halo, ${user?.firstName ?: user?.displayName ?: ""} 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Semoga harimu menyenangkan!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.load() },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                ComposeBrush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                                ),
                            )
                            .padding(24.dp),
                    ) {
                        Column {
                            Text(
                                "TOTAL LEARNING HOURS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                            )
                            Text(
                                "${uiState.totalLearningHours} Jam",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(androidx.compose.ui.graphics.Color(0xFF1C1C1E))
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    "  MARKETPLACE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                            Text(
                                "Marketplace Kursus",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            Text(
                                "Lihat semua course berkualitas tinggi dari instruktur handal.",
                                style = MaterialTheme.typography.bodySmall,
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        androidx.compose.material3.Surface(
                            onClick = onBrowseMarketplace,
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Text(
                                "BELANJA",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            )
                        }
                    }
                }

                if (uiState.categories.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(uiState.categories.take(4), key = { it.id }) { category ->
                                CategoryQuickLink(category = category, onClick = onBrowseMarketplace)
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Kursus Unggulan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        androidx.compose.material3.TextButton(onClick = onBrowseMarketplace) {
                            Text("Lihat Semua", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(uiState.featuredCourses.take(3), key = { it.id }) { course ->
                    CourseCard(
                        title = course.title,
                        coverImageUrl = course.coverImageUrl,
                        mentorName = course.mentor?.displayName,
                        categoryLabel = course.category?.name,
                        rating = course.reviewsAvgRating,
                        onClick = { onCourseClick(course.id) },
                    ) {
                        Text(
                            if (course.currentPrice <= 0.0) "Gratis" else IdrCurrencyFormat.format(course.currentPrice),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun CategoryQuickLink(category: Category, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.material3.Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(iconForCategory(category), contentDescription = category.name, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            category.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

private fun iconForCategory(category: Category): ImageVector = when {
    category.slug.contains("design", ignoreCase = true) || category.name.contains("desain", ignoreCase = true) -> Icons.Filled.Palette
    category.slug.contains("program", ignoreCase = true) -> Icons.Filled.Code
    category.slug.contains("bisnis", ignoreCase = true) || category.slug.contains("business", ignoreCase = true) -> Icons.Filled.Storefront
    category.slug.contains("marketing", ignoreCase = true) || category.slug.contains("pemasaran", ignoreCase = true) -> Icons.Filled.Campaign
    else -> Icons.Filled.Brush
}
