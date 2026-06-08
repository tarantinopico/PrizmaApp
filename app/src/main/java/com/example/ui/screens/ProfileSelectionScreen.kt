package com.example.ui.screens

import android.graphics.Color.parseColor
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ProfileEntity
import com.example.ui.viewmodels.ProfileViewModel
import com.example.utils.ProfileUtils
import java.util.Calendar
import com.example.ui.utils.HapticType
import com.example.ui.utils.rememberHapticHelper

val PROFILE_COLORS = listOf(
    "#7C5CFF", "#FF5C5C", "#5CFFB6", "#FFB65C", "#5CBAFF"
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileSelectionScreen(
    viewModel: ProfileViewModel,
    onProfileSelected: (Long) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val hapticHelper = rememberHapticHelper()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val activeProfileId by viewModel.activeProfileId.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(activeProfileId) {
        if (!viewModel.hasHandledInitialNavigation && activeProfileId != null) {
            viewModel.hasHandledInitialNavigation = true
            ProfileUtils.setWebViewProfileOrRestart(context, activeProfileId!!)
            onProfileSelected(activeProfileId!!)
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editDialogProfile by remember { mutableStateOf<ProfileEntity?>(null) }
    
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Dobré ráno,"
            in 12..17 -> "Dobré odpoledne,"
            else -> "Dobrý večer,"
        }
    }

    Scaffold(
        bottomBar = {
            BottomAppBar(
                containerColor = Color.Transparent,
                contentPadding = PaddingValues(16.dp)
            ) {
                Spacer(Modifier.weight(1f))
                FilledTonalButton(
                    onClick = onNavigateToSettings,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Nastavení", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Nastavení")
                }
                Spacer(Modifier.weight(1f))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(32.dp))
            Text(
                text = greeting,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Kdo se dívá?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(48.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(profiles) { profile ->
                    ProfileCard(
                        profile = profile,
                        onClick = {
                            hapticHelper.perform(HapticType.SWITCH_PROFILE)
                            viewModel.hasHandledInitialNavigation = true
                            viewModel.setActiveProfile(profile.id)
                            ProfileUtils.setWebViewProfileOrRestart(context, profile.id)
                            onProfileSelected(profile.id)
                        },
                        onLongClick = {
                            hapticHelper.perform(HapticType.LONG_PRESS)
                            editDialogProfile = profile
                        }
                    )
                }
                item {
                    AddProfileCard(onClick = { showAddDialog = true })
                }
            }
        }
    }

    if (showAddDialog) {
        ProfileDialog(
            title = "Nový profil",
            initialName = "",
            initialColor = PROFILE_COLORS[0],
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color ->
                viewModel.createProfile(name, color)
                showAddDialog = false
            }
        )
    }

    editDialogProfile?.let { profile ->
        ProfileDialog(
            title = "Upravit profil",
            initialName = profile.name,
            initialColor = profile.accentColorHex ?: PROFILE_COLORS[0],
            showDelete = true,
            onDismiss = { editDialogProfile = null },
            onDelete = {
                viewModel.deleteProfile(profile.id)
                try {
                    val dir = java.io.File(context.applicationInfo.dataDir, "app_webview_profile_${profile.id}")
                    if (dir.exists()) dir.deleteRecursively()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (activeProfileId == profile.id) viewModel.setActiveProfile(null)
                editDialogProfile = null
            },
            onConfirm = { name, color ->
                viewModel.updateProfile(profile.copy(name = name, accentColorHex = color))
                editDialogProfile = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileCard(profile: ProfileEntity, onClick: () -> Unit, onLongClick: () -> Unit) {
    val color = try {
        Color(parseColor(profile.accentColorHex ?: PROFILE_COLORS[0]))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.fillMaxSize().background(color.copy(alpha = 0.1f)))
            Text(
                text = profile.name.take(1).uppercase(),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = profile.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun AddProfileCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Přidat profil",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Přidat profil",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ProfileDialog(
    title: String,
    initialName: String,
    initialColor: String,
    showDelete: Boolean = false,
    onDismiss: () -> Unit,
    onDelete: () -> Unit = {},
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Jméno") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
                Text("Barva profilu", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PROFILE_COLORS.forEach { hex ->
                        val color = try { Color(parseColor(hex)) } catch (e: Exception) { Color.Gray }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = 2.dp,
                                    color = if (selectedColor == hex) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) onConfirm(name, selectedColor)
            }) { Text("Uložit") }
        },
        dismissButton = {
            if (showDelete) {
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Smazat")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Zrušit") }
            }
        }
    )
}
