package com.mulaisekarang.app.ui.screens

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mulaisekarang.app.data.model.Lesson
import com.mulaisekarang.app.data.model.Mentor
import com.mulaisekarang.app.data.model.Topic
import com.mulaisekarang.app.ui.components.ErrorState
import com.mulaisekarang.app.ui.components.HtmlText
import com.mulaisekarang.app.ui.components.IdrCurrencyFormat
import com.mulaisekarang.app.ui.components.LoadingState
import com.mulaisekarang.app.ui.components.accentHtml
import com.mulaisekarang.app.viewmodel.CheckoutEvent
import com.mulaisekarang.app.viewmodel.CourseDetailUiState
import com.mulaisekarang.app.viewmodel.CourseDetailViewModel
import kotlinx.coroutines.launch

private val EnrolledGreen = Color(0xFF16A34A)
private val EnrolledGreenBg = Color(0xFFDCFCE7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseId: Int,
    viewModel: CourseDetailViewModel,
    onBack: () -> Unit,
    onChatWithMentor: (username: String) -> Unit,
    onLessonClick: (lessonId: Int) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isCheckingOut by remember { mutableStateOf(false) }

    LaunchedEffect(courseId) {
        viewModel.load(courseId)
    }

    LaunchedEffect(Unit) {
        viewModel.checkoutEvent.collect { event ->
            when (event) {
                is CheckoutEvent.Loading -> isCheckingOut = true
                is CheckoutEvent.OpenInvoice -> {
                    isCheckingOut = false
                    CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(event.url))
                }
                is CheckoutEvent.EnrolledFree -> {
                    isCheckingOut = false
                    coroutineScope.launch { snackbarHostState.showSnackbar("Berhasil terdaftar!") }
                }
                is CheckoutEvent.Error -> {
                    isCheckingOut = false
                    coroutineScope.launch { snackbarHostState.showSnackbar(event.message) }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Course") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (val state = uiState) {
            is CourseDetailUiState.Loading -> LoadingState(modifier = Modifier.padding(padding))

            is CourseDetailUiState.Error -> ErrorState(
                message = state.message,
                modifier = Modifier.padding(padding),
                onRetry = { viewModel.load(courseId) },
            )

            is CourseDetailUiState.Loaded -> {
                val course = state.course
                val mentorUsername = course.mentor?.username

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    item {
                        AsyncImage(
                            model = course.coverImageUrl,
                            contentDescription = course.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                        )
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                            Text(
                                course.title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            course.mentor?.let {
                                Text(
                                    "oleh ${it.displayName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    if (course.currentPrice <= 0.0) "Gratis" else IdrCurrencyFormat.format(course.currentPrice),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                if (course.isEnrolled) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .padding(top = 10.dp)
                                            .background(EnrolledGreenBg, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = EnrolledGreen,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .padding(end = 6.dp),
                                        )
                                        Text(
                                            "Anda sudah terdaftar",
                                            color = EnrolledGreen,
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }
                                }
                                if (course.isEnrolled && mentorUsername != null) {
                                    Button(
                                        onClick = { onChatWithMentor(mentorUsername) },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp),
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 8.dp),
                                        )
                                        Text("Chat dengan Mentor")
                                    }
                                }
                                if (!course.isEnrolled) {
                                    Button(
                                        onClick = { viewModel.checkout() },
                                        enabled = !isCheckingOut,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp),
                                    ) {
                                        Text(
                                            if (course.currentPrice <= 0.0) "Daftar Gratis" else "Beli Sekarang",
                                        )
                                    }
                                }
                            }
                        }
                    }

                    course.mentor?.let { mentor ->
                        item {
                            MentorCard(
                                mentor = mentor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 24.dp),
                            )
                        }
                    }

                    course.description?.let {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 24.dp),
                            ) {
                                HtmlText(
                                    html = accentHtml(it, MaterialTheme.colorScheme.primary),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                )
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                "${course.lessonsCount} pelajaran · ${course.enrollmentsCount} siswa",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "Materi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            )
                        }
                    }

                    items(course.topics, key = { it.id }) { topic -> TopicSection(topic, onLessonClick) }
                }
            }
        }
    }
}

@Composable
private fun MentorCard(mentor: Mentor, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            AsyncImage(
                model = mentor.profilePhotoUrl,
                contentDescription = mentor.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    "Bertemu Mentor",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        mentor.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (mentor.isVerifiedInstructor) {
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = "Terverifikasi",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(start = 4.dp),
                        )
                    }
                }
                mentor.jobTitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicSection(topic: Topic, onLessonClick: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(true) }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
        ) {
            Text(
                topic.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Sembunyikan" else "Tampilkan",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            topic.lessons.forEach { lesson ->
                Column(
                    modifier = Modifier
                        .clickable(enabled = lesson.isAccessible) { onLessonClick(lesson.id) }
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = when {
                                !lesson.isAccessible -> Icons.Filled.Lock
                                lesson.isCompleted -> Icons.Filled.CheckCircle
                                else -> Icons.Filled.PlayArrow
                            },
                            contentDescription = null,
                            tint = if (lesson.isCompleted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(
                            lesson.title,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            lesson.formattedDuration(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (lesson.isCompleted) {
                            Text(
                                "Selesai",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Lesson.formattedDuration(): String = when {
    durationHours > 0 -> "${durationHours * 60 + durationMinutes} Menit"
    durationMinutes > 0 -> "$durationMinutes Menit"
    else -> "$durationSeconds Detik"
}
