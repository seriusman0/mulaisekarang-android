package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.mulaisekarang.app.data.model.Category
import com.mulaisekarang.app.ui.components.CourseCard
import com.mulaisekarang.app.ui.components.EmptyState
import com.mulaisekarang.app.ui.components.ErrorState
import com.mulaisekarang.app.ui.components.IdrCurrencyFormat
import com.mulaisekarang.app.viewmodel.MarketplaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    viewModel: MarketplaceViewModel,
    onCourseClick: (Int) -> Unit,
) {
    val filterState by viewModel.filterState.collectAsState()
    val lazyPagingItems = viewModel.pagedCourses.collectAsLazyPagingItems()
    val refreshState = lazyPagingItems.loadState.refresh

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text(
                "Marketplace Kursus",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            )

            OutlinedTextField(
                value = filterState.search,
                onValueChange = { viewModel.onSearchChange(it) },
                placeholder = { Text("Cari kursus atau keahlian baru...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    CategoryChip(
                        label = "Semua",
                        selected = filterState.selectedCategorySlug == null,
                        onClick = { viewModel.onCategorySelect(null) },
                    )
                }
                items(filterState.categories, key = { it.slug }) { category: Category ->
                    CategoryChip(
                        label = category.name,
                        selected = filterState.selectedCategorySlug == category.slug,
                        onClick = { viewModel.onCategorySelect(category.slug) },
                    )
                }
            }

            val sectionTitle = if (filterState.search.isNotBlank() || filterState.selectedCategorySlug != null) {
                "Hasil Pencarian"
            } else {
                "Semua Kursus"
            }
            Text(
                "$sectionTitle (${lazyPagingItems.itemCount})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            PullToRefreshBox(
                isRefreshing = refreshState is LoadState.Loading,
                onRefresh = { lazyPagingItems.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    refreshState is LoadState.Error -> ErrorState(
                        message = refreshState.error.message ?: "Terjadi kesalahan.",
                        onRetry = { lazyPagingItems.retry() },
                    )

                    lazyPagingItems.itemCount == 0 && refreshState !is LoadState.Loading -> EmptyState(
                        message = "Tidak ada course yang ditemukan. Coba kata kunci atau kategori lain.",
                    )

                    else -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            count = lazyPagingItems.itemCount,
                            key = lazyPagingItems.itemKey { it.id },
                        ) { index ->
                            val course = lazyPagingItems[index] ?: return@items
                            CourseCard(
                                title = course.title,
                                coverImageUrl = course.coverImageUrl,
                                mentorName = course.mentor?.displayName,
                                categoryLabel = course.category?.name,
                                rating = course.reviewsAvgRating,
                                onClick = { onCourseClick(course.id) },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        if (course.currentPrice <= 0.0) "Gratis" else IdrCurrencyFormat.format(course.currentPrice),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Icon(
                                        Icons.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.padding(2.dp),
                                    )
                                }
                            }
                        }

                        if (lazyPagingItems.loadState.append is LoadState.Loading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}
