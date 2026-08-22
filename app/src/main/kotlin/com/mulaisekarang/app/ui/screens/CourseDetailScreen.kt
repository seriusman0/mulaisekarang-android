package com.mulaisekarang.app.ui.screens

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mulaisekarang.app.data.model.Lesson
import com.mulaisekarang.app.data.model.Mentor
import com.mulaisekarang.app.data.model.Review
import com.mulaisekarang.app.data.model.Topic
import com.mulaisekarang.app.ui.components.ErrorState
import com.mulaisekarang.app.ui.components.HtmlText
import com.mulaisekarang.app.ui.components.IdrCurrencyFormat
import com.mulaisekarang.app.ui.components.LoadingState
import com.mulaisekarang.app.ui.components.accentHtml
import com.mulaisekarang.app.viewmodel.CheckoutEvent
import com.mulaisekarang.app.viewmodel.CourseDetailUiState
import com.mulaisekarang.app.viewmodel.CourseDetailViewModel
import com.mulaisekarang.app.viewmodel.MyCourseReviewState
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
    onInstructorClick: (username: String) -> Unit,
    onQuizClick: (quizId: Int) -> Unit,
    onAssignmentClick: (assignmentId: Int) -> Unit,
    onBuyNow: (courseId: Int) -> Unit,
    onOpenCart: () -> Unit,
    onChatPaywall: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val reviewState by viewModel.reviewState.collectAsState()
    val isSyncing = (uiState as? CourseDetailUiState.Loaded)?.isSyncing == true
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
                is CheckoutEvent.Message -> {
                    coroutineScope.launch { snackbarHostState.showSnackbar(event.text) }
                }
                is CheckoutEvent.ChatPaywall -> {
                    isCheckingOut = false
                    onChatPaywall()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Course") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 16.dp),
                            strokeWidth = 2.dp,
                        )
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
                    contentPadding = PaddingValues(bottom = 100.dp),
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
                                if (!course.isEnrolled && course.type != "mini_course") {
                                    Button(
                                        onClick = {
                                            if (course.currentPrice <= 0.0) viewModel.checkout() else onBuyNow(courseId)
                                        },
                                        enabled = !isCheckingOut,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp)
                                            .testTag("buy_now_button"),
                                    ) {
                                        Text(
                                            if (course.currentPrice <= 0.0) "Daftar Gratis" else "Beli Sekarang",
                                        )
                                    }

                                    // Free courses enrol instantly, so a cart
                                    // entry for them would be a dead end.
                                    if (course.currentPrice > 0.0) {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.addToCart()
                                                onOpenCart()
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.onSurface,
                                            ),
                                            border = BorderStroke(1.5.dp, Color(0xFF7F8C8D)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                                .testTag("add_to_cart_button"),
                                        ) {
                                            Text("Tambah ke Keranjang")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    course.mentor?.let { mentor ->
                        item {
                            MentorCard(
                                mentor = mentor,
                                onClick = mentor.username?.let { username -> { onInstructorClick(username) } },
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

                    if (course.type == "mini_course" && course.batches.isNotEmpty()) {
                        item {
                            Text(
                                "Pilihan Jadwal / Batch",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        items(course.batches, key = { it.id }) { batch ->
                            BatchCard(
                                batch = batch,
                                isEnrolled = course.isEnrolled && batch.zoomLink != null, // Approximation, PRD says zoom_link is non-null only if approved in this specific batch
                                onCheckout = { viewModel.checkout(batch.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            )
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

                    items(course.topics, key = { it.id }) { topic ->
                        TopicSection(topic, onLessonClick, onQuizClick, onAssignmentClick)
                    }

                    // Only enrolled students may review, matching the API gate.
                    if (course.isEnrolled) {
                        item {
                            WriteReviewSection(
                                state = reviewState,
                                onRatingChange = viewModel::setReviewRating,
                                onBodyChange = viewModel::setReviewBody,
                                onSubmit = viewModel::submitReview,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                            )
                        }
                    }

                    if (course.reviews.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
                                ) {
                                    Text(
                                        "Ulasan",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    course.reviewsAvgRating?.let { avg ->
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .size(16.dp),
                                        )
                                        Text(
                                            String.format("%.1f", avg),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(start = 2.dp),
                                        )
                                        Text(
                                            " (${course.reviews.size} ulasan)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        items(course.reviews, key = { it.id }) { review ->
                            ReviewCard(review, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Rating + comment form. The API treats a repeat submission as a replacement
 * (201 on create, 200 on update), so there is one form, not separate
 * create/edit modes.
 */
@Composable
private fun WriteReviewSection(
    state: MyCourseReviewState,
    onRatingChange: (Int) -> Unit,
    onBodyChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("write_review_section"),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                if (state.submitted) "Ulasan Anda" else "Beri Ulasan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Row(modifier = Modifier.padding(top = 8.dp)) {
                (1..5).forEach { star ->
                    IconButton(
                        onClick = { onRatingChange(star) },
                        modifier = Modifier.testTag("review_star_$star"),
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Beri $star bintang",
                            tint = if (star <= state.rating) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.body,
                onValueChange = onBodyChange,
                label = { Text("Komentar (opsional)") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("review_body_input"),
            )

            Button(
                onClick = onSubmit,
                enabled = state.rating in 1..5 && !state.isSubmitting,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .testTag("review_submit_button"),
            ) {
                Text(if (state.isSubmitting) "Mengirim…" else "Kirim Ulasan")
            }
        }
    }
}

@Composable
private fun ReviewCard(review: Review, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    review.user?.displayName ?: "Pengguna",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Row {
                    repeat(5) { index ->
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (index < review.rating) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
            if (!review.body.isNullOrBlank()) {
                Text(
                    review.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun MentorCard(mentor: Mentor, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick ?: {},
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
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
            if (onClick != null) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Lihat profil instruktur",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TopicSection(
    topic: Topic,
    onLessonClick: (Int) -> Unit,
    onQuizClick: (Int) -> Unit,
    onAssignmentClick: (Int) -> Unit,
) {
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
            topic.quizzes.forEach { quiz ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onQuizClick(quiz.id) }
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                ) {
                    Icon(
                        Icons.Filled.Quiz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(quiz.title, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "KUIS · ${quiz.questionsCount} pertanyaan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    quiz.myBestScore?.let {
                        Text(
                            "$it",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            topic.assignments.forEach { assignment ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onAssignmentClick(assignment.id) }
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                ) {
                    Icon(
                        Icons.Filled.Assignment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(assignment.title, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "TUGAS · ${assignment.totalPoints} poin",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (assignment.mySubmissionStatus != null) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Sudah dikumpulkan",
                            tint = MaterialTheme.colorScheme.primary,
                        )
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

@Composable
private fun BatchCard(
    batch: com.mulaisekarang.app.data.model.CourseBatch,
    isEnrolled: Boolean,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(batch.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("Mulai: ${batch.startTime}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            Text("Sisa kursi: ${batch.availableSeats} / ${batch.maxSeats}", style = MaterialTheme.typography.bodySmall)
            
            if (isEnrolled && batch.zoomLink != null) {
                Button(
                    onClick = { /* Open Zoom Link logic if needed */ },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Text("Join Zoom")
                }
            } else if (!isEnrolled && !batch.isSoldOut) {
                Button(
                    onClick = onCheckout,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Text("Beli Batch Ini")
                }
            } else if (batch.isSoldOut) {
                Text("Penuh", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
