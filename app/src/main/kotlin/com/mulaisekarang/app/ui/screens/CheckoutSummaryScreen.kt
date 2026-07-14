package com.mulaisekarang.app.ui.screens

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mulaisekarang.app.ui.components.ErrorState
import com.mulaisekarang.app.ui.components.IdrCurrencyFormat
import com.mulaisekarang.app.ui.components.LoadingState
import com.mulaisekarang.app.viewmodel.CheckoutEvent
import com.mulaisekarang.app.viewmodel.CourseDetailUiState
import com.mulaisekarang.app.viewmodel.CourseDetailViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutSummaryScreen(
    courseId: Int,
    viewModel: CourseDetailViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(courseId) {
        viewModel.load(courseId)
    }

    LaunchedEffect(Unit) {
        viewModel.checkoutEvent.collect { event ->
            when (event) {
                is CheckoutEvent.Loading -> isProcessing = true
                is CheckoutEvent.OpenInvoice -> {
                    isProcessing = false
                    CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(event.url))
                }
                is CheckoutEvent.EnrolledFree -> {
                    isProcessing = false
                    onBack()
                }
                is CheckoutEvent.Error -> {
                    isProcessing = false
                    coroutineScope.launch { snackbarHostState.showSnackbar(event.message) }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(20.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = course.coverImageUrl,
                            contentDescription = course.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(16.dp)),
                        )
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Text(course.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            course.mentor?.let {
                                Text(
                                    "oleh ${it.displayName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Rincian Pembayaran", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    "HARGA KURSUS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(IdrCurrencyFormat.format(course.currentPrice), fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TOTAL", fontWeight = FontWeight.Bold)
                                Text(
                                    IdrCurrencyFormat.format(course.currentPrice),
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    Text(
                        "Pembayaran diproses secara aman melalui Xendit (Transfer Bank, E-Wallet, Kartu Kredit).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )

                    Button(
                        onClick = { viewModel.checkout() },
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Bayar Sekarang", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
