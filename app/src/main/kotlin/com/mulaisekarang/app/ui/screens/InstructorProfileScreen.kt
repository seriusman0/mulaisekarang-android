package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mulaisekarang.app.ui.components.CourseCard
import com.mulaisekarang.app.ui.components.ErrorState
import com.mulaisekarang.app.ui.components.IdrCurrencyFormat
import com.mulaisekarang.app.ui.components.LoadingState
import com.mulaisekarang.app.viewmodel.InstructorProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructorProfileScreen(
    viewModel: InstructorProfileViewModel,
    onBack: () -> Unit,
    onCourseClick: (Int) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Profil Instruktur") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.error != null -> ErrorState(
                message = uiState.error ?: "Terjadi kesalahan.",
                modifier = Modifier.padding(padding),
                onRetry = { viewModel.load() },
            )

            uiState.instructor == null -> LoadingState(modifier = Modifier.padding(padding))

            else -> {
                val instructor = uiState.instructor!!
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(32.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (instructor.profilePhotoUrl != null) {
                                    AsyncImage(
                                        model = instructor.profilePhotoUrl,
                                        contentDescription = instructor.displayName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clip(CircleShape),
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Filled.Person,
                                            contentDescription = instructor.displayName,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(40.dp),
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                                    Text(
                                        instructor.displayName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    if (instructor.isVerifiedInstructor) {
                                        Icon(
                                            Icons.Filled.Verified,
                                            contentDescription = "Terverifikasi",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .padding(start = 4.dp),
                                        )
                                    }
                                }
                                instructor.jobTitle?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 20.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(vertical = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                ) {
                                    StatItem(
                                        icon = Icons.Filled.Star,
                                        value = instructor.rating?.toString() ?: "-",
                                        label = "Rating",
                                    )
                                    StatItem(
                                        icon = Icons.Filled.Person,
                                        value = (instructor.studentCount ?: 0).toString(),
                                        label = "Siswa",
                                    )
                                    StatItem(
                                        icon = null,
                                        value = instructor.courseCount.toString(),
                                        label = "Kursus",
                                    )
                                }

                                if (!instructor.bio.isNullOrBlank()) {
                                    Text(
                                        "Tentang Saya",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 20.dp),
                                    )
                                    Text(
                                        instructor.bio,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 8.dp),
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            "Kursus yang Diajar (${uiState.courses.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    items(uiState.courses, key = { it.id }) { course ->
                        CourseCard(
                            title = course.title,
                            coverImageUrl = course.coverImageUrl,
                            mentorName = null,
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
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector?, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Icon(
                    it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 4.dp),
                )
            }
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
