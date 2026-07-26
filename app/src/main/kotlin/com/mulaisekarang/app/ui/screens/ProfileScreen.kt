package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mulaisekarang.app.viewmodel.AuthUiState
import com.mulaisekarang.app.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onLoggedOut: () -> Unit,
    onNavigateToMyCourses: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenInstructorPortal: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToMyReviews: () -> Unit = {},
    onNavigateToQuizHistory: () -> Unit = {},
    onNavigateToSubmissionHistory: () -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    onNavigateToChatSubscription: () -> Unit = {},
) {
    val uiState by authViewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.LoggedOut) onLoggedOut()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        val user = (uiState as? AuthUiState.LoggedIn)?.user

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            // Header Row: title "Profil" on the left, edit icon button on the right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Profil",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111) // Onyx-text
                )
                IconButton(onClick = onEditProfile) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit Profil",
                        tint = Color(0xFF111111)
                    )
                }
            }

            if (user != null) {
                // Profile Header Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
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
                        // Profile Avatar (120x120px)
                        if (user.profilePhotoUrl != null) {
                            AsyncImage(
                                model = user.profilePhotoUrl,
                                contentDescription = user.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = user.displayName,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp),
                                )
                            }
                        }

                        // Name
                        Text(
                            user.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111111), // Onyx-text
                            modifier = Modifier.padding(top = 16.dp),
                        )
                        // Email
                        Text(
                            user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666), // Charcoal-text
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        // Role Badge
                        Text(
                            user.role.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                        // Bio
                        Text(
                            if (!user.bio.isNullOrBlank()) user.bio else "Suka belajar hal baru setiap hari.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666), // Charcoal-text
                            modifier = Modifier.padding(top = 16.dp),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Edit Profile Button (contained inside the card)
                        Button(
                            onClick = onEditProfile,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                "Edit Profil",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Activity Title
                Text(
                    "AKTIVITAS SAYA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF666666), // Charcoal-text
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp, start = 4.dp),
                )

                // Instructor portal entry — only for tutor_instructor accounts.
                if (user.role == "tutor_instructor") {
                    Surface(
                        onClick = onOpenInstructorPortal,
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("instructor_portal_entry"),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                                Text(
                                    "Portal Instruktur",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111111),
                                )
                                Text(
                                    "Kelola kelas, penilaian, & pendapatan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF666666),
                                )
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }

                // Menu list: "Kursus Saya"
                Surface(
                    onClick = onNavigateToMyCourses,
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                        ) {
                            Text(
                                "Kursus Saya",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111111) // Onyx-text
                            )
                            Text(
                                "Lanjutkan pembelajaran Anda",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF666666), // Charcoal-text
                            )
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }

                // Student menu. Every row maps to one /api/v1 student feature.
                ProfileMenuRow(
                    icon = Icons.Filled.Receipt,
                    title = "Riwayat Transaksi",
                    subtitle = "Lihat semua pembayaran Anda",
                    onClick = onNavigateToTransactions,
                    testTag = "menu_transactions",
                )
                ProfileMenuRow(
                    icon = Icons.Filled.ShoppingCart,
                    title = "Keranjang",
                    subtitle = "Beli beberapa kelas sekaligus",
                    onClick = onNavigateToCart,
                    testTag = "menu_cart",
                )
                ProfileMenuRow(
                    icon = Icons.Filled.Notifications,
                    title = "Notifikasi",
                    subtitle = "Pengumuman, nilai tugas, status pembayaran",
                    onClick = onNavigateToNotifications,
                    testTag = "menu_notifications",
                )
                ProfileMenuRow(
                    icon = Icons.Filled.Star,
                    title = "Ulasan Saya",
                    subtitle = "Ulasan yang pernah Anda tulis",
                    onClick = onNavigateToMyReviews,
                    testTag = "menu_my_reviews",
                )
                ProfileMenuRow(
                    icon = Icons.Filled.Quiz,
                    title = "Riwayat Kuis",
                    subtitle = "Nilai dan pembahasan percobaan kuis",
                    onClick = onNavigateToQuizHistory,
                    testTag = "menu_quiz_history",
                )
                ProfileMenuRow(
                    icon = Icons.Filled.Assignment,
                    title = "Riwayat Tugas",
                    subtitle = "Tugas yang Anda kumpulkan",
                    onClick = onNavigateToSubmissionHistory,
                    testTag = "menu_submission_history",
                )
                ProfileMenuRow(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    title = "Langganan Chat",
                    subtitle = "Status add-on chat mentor",
                    onClick = onNavigateToChatSubscription,
                    testTag = "menu_chat_subscription",
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            OutlinedButton(
                onClick = { authViewModel.logout() },
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFD32F2F)), // Error crimson border
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text("  Keluar", fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable
private fun ProfileMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111), // Onyx-text
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF666666), // Charcoal-text
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}
