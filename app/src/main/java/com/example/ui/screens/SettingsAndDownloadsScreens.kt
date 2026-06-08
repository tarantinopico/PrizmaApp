package com.example.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.PrizmaApp
import com.example.ui.viewmodels.BrowserViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DownloadsScreen(
    profileId: Long,
    viewModel: BrowserViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val downloadManager = remember { context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }
    LaunchedEffect(profileId) { viewModel.initProfile(profileId) }
    
    val downloads by viewModel.downloads.collectAsStateWithLifecycle(emptyList())

    // Periodic refresh of download sizes and status while active
    LaunchedEffect(downloads) {
        val activeDownloads = downloads.filter { it.status == "probíhá" }
        if (activeDownloads.isNotEmpty()) {
            var polling = true
            while (polling) {
                delay(1000)
                var hasActive = false
                activeDownloads.forEach { dl ->
                    if (dl.downloadManagerId != null) {
                        val q = DownloadManager.Query().setFilterById(dl.downloadManagerId)
                        val cursor = downloadManager.query(q)
                        if (cursor.moveToFirst()) {
                            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                            if (statusIndex != -1 && downloadedIndex != -1 && totalIndex != -1) {
                                val status = cursor.getInt(statusIndex)
                                val downloaded = cursor.getLong(downloadedIndex)
                                val total = cursor.getLong(totalIndex)

                                val newStatus = when (status) {
                                    DownloadManager.STATUS_SUCCESSFUL -> "dokončeno"
                                    DownloadManager.STATUS_FAILED -> "selhalo"
                                    else -> "probíhá"
                                }
                                
                                val newSize = if (total > 0) total else dl.fileSize

                                if (newStatus != dl.status || newSize != dl.fileSize) {
                                    viewModel.updateDownload(dl.copy(status = newStatus, fileSize = newSize))
                                }
                                
                                if (newStatus == "probíhá") {
                                    hasActive = true
                                }
                            }
                        }
                        cursor.close()
                    }
                }
                polling = hasActive
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stahování") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zpět") }
                }
            )
        }
    ) { padding ->
        if (downloads.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Žádná stahování",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Všechny vaše stažené soubory se zobrazí zde.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(downloads, key = { it.id }) { dl ->
                    var showDialog by remember { mutableStateOf(false) }

                    ListItem(
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                if (dl.status == "dokončeno") {
                                    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), dl.fileName)
                                    if (file.exists()) {
                                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, dl.mimeType.takeIf { it.isNotBlank() } ?: "*/*")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        try {
                                            context.startActivity(Intent.createChooser(intent, "Otevřít pomocí"))
                                        } catch (e: Exception) {}
                                    }
                                }
                            },
                            onLongClick = { showDialog = true }
                        ),
                        headlineContent = { Text(dl.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { 
                            val sizeText = if (dl.fileSize > 0) android.text.format.Formatter.formatShortFileSize(context, dl.fileSize) else "Neznámá velikost"
                            Text("${dl.status} • $sizeText")
                        },
                        leadingContent = {
                            Icon(Icons.Default.InsertDriveFile, contentDescription = null)
                        },
                        trailingContent = {
                            if (dl.status == "probíhá") {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    )
                    HorizontalDivider()

                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = { showDialog = false },
                            title = { Text("Smazat stahování") },
                            text = { Text("Chcete smazat záznam, nebo i samotný soubor?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), dl.fileName)
                                    if (file.exists()) file.delete()
                                    viewModel.deleteDownload(dl.id)
                                    showDialog = false
                                }) { Text("Smazat i soubor") }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    viewModel.deleteDownload(dl.id)
                                    showDialog = false
                                }) { Text("Jen záznam") }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profileId: Long,
    viewModel: BrowserViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as PrizmaApp
    val settingsDataStore = app.container.settingsDataStore
    val scope = rememberCoroutineScope()
    
    val hapticsEnabled by settingsDataStore.hapticsEnabled.collectAsState(initial = true)
    val hapticsIntensity by settingsDataStore.hapticsIntensity.collectAsState(initial = "střední")

    LaunchedEffect(profileId) { viewModel.initProfile(profileId) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Možnosti") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zpět") }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                Text("Obecné nastavení", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Tmavý režim", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = true, onCheckedChange = {})
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Úspora dat", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = false, onCheckedChange = {})
                }
            }
            
            item {
                Spacer(Modifier.height(24.dp))
                Text("Haptická odezva", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Povolit haptickou odezvu", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { settingsDataStore.setHapticsEnabled(enabled) }
                        }
                    )
                }
                
                if (hapticsEnabled) {
                    Spacer(Modifier.height(16.dp))
                    Text("Intenzita vibrací", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("jemná", "střední", "silná").forEach { intensity ->
                            FilterChip(
                                selected = hapticsIntensity == intensity,
                                onClick = { scope.launch { settingsDataStore.setHapticsIntensity(intensity) } },
                                label = { Text(intensity.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            }
        }
    }
}
