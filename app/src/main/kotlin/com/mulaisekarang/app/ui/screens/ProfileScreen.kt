package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
    onEditProfile: () -> Unit,
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
            Text(
                "Profil",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
            )

            if (user != null) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (user.profilePhotoUrl != null) {
                                AsyncImage(
                                    model = user.profilePhotoUrl,
                                    contentDescription = user.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(104.dp)
                                        .clip(CircleShape),
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(104.dp)
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

                            Text(
                                user.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp),
                            )
                            Text(
                                user.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            Text(
                                user.role.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            )
                            if (!user.bio.isNullOrBlank()) {
                                Text(
                                    user.bio,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                            }
                        }

                        IconButton(
                            onClick = onEditProfile,
                            modifier = Modifier.align(Alignment.TopEnd),
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Profil")
                        }
                    }
                }

                Text(
                    "AKTIVITAS SAYA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp, start = 4.dp),
                )

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
                            Text("Kursus Saya", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Lanjutkan pembelajaran Anda",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

            OutlinedButton(
                onClick = { authViewModel.logout() },
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Keluar", fontWeight = FontWeight.Bold)
            }
        }
    }
}
