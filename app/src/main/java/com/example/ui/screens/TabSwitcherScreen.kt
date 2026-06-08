package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.TabEntity
import com.example.data.local.entity.TabGroupEntity
import com.example.ui.viewmodels.BrowserViewModel

import androidx.compose.ui.graphics.asImageBitmap

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TabSwitcherScreen(
    profileId: Long,
    viewModel: BrowserViewModel,
    onNavigateBack: () -> Unit,
    onSwitchProfile: () -> Unit
) {
    LaunchedEffect(profileId) { viewModel.initProfile(profileId) }
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val tabGroups by viewModel.tabGroups.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val currentProfile by viewModel.currentProfile.collectAsStateWithLifecycle()

    var showGroupDialog by remember { mutableStateOf<TabEntity?>(null) }
    var showNewGroupDialog by remember { mutableStateOf<TabEntity?>(null) }

    val profileColor = try {
        Color(android.graphics.Color.parseColor(currentProfile?.accentColorHex ?: "#7C5CFF"))
    } catch(e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        currentProfile?.name ?: "Záložky", 
                        fontWeight = FontWeight.SemiBold,
                        color = profileColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.Close, "Zavřít") }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.addTab("https://www.google.com", "Nová karta")
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Add, "Nová karta")
                    }
                    IconButton(onClick = {
                        viewModel.closeAllTabs()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Delete, "Smazat vše")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = Color.Transparent, contentPadding = PaddingValues(16.dp)) {
                Spacer(Modifier.weight(1f))
                FilledTonalButton(onClick = onSwitchProfile) {
                    Text("Přepnout profil")
                }
                Spacer(Modifier.weight(1f))
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            tabGroups.forEach { group ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "group_${group.id}") {
                    GroupHeader(
                        group = group, 
                        onToggle = { viewModel.toggleGroupCollapse(group.id) },
                        onClose = { viewModel.closeGroup(group.id) }
                    )
                }
                
                if (!group.isCollapsed) {
                    val groupTabs = tabs.filter { it.groupId == group.id }
                    items(groupTabs, key = { it.id }) { tab ->
                        TabCard(
                            tab = tab,
                            viewModel = viewModel,
                            isSelected = tab.id == currentTab?.id,
                            profileColor = profileColor,
                            onClick = {
                                viewModel.selectTab(tab)
                                onNavigateBack()
                            },
                            onClose = { viewModel.closeTab(tab.id) },
                            onLongClick = { showGroupDialog = tab }
                        )
                    }
                }
            }

            val uncategorizedTabs = tabs.filter { it.groupId == null }
            if (uncategorizedTabs.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "uncategorized_header") {
                    Text(
                        "Bez skupiny",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                items(uncategorizedTabs, key = { it.id }) { tab ->
                    TabCard(
                        tab = tab,
                        viewModel = viewModel,
                        isSelected = tab.id == currentTab?.id,
                        profileColor = profileColor,
                        onClick = {
                            viewModel.selectTab(tab)
                            onNavigateBack()
                        },
                        onClose = { viewModel.closeTab(tab.id) },
                        onLongClick = { showGroupDialog = tab }
                    )
                }
            }
        }
    }

    if (showGroupDialog != null) {
        val tab = showGroupDialog!!
        AlertDialog(
            onDismissRequest = { showGroupDialog = null },
            title = { Text("Skupina karet") },
            text = {
                Column {
                    tabGroups.forEach { group ->
                        val groupColor = try { Color(android.graphics.Color.parseColor(group.colorHex)) } catch(e:Exception) { Color.Gray }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.assignTabToGroup(tab.id, group.id)
                                    showGroupDialog = null
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(groupColor))
                            Spacer(Modifier.width(16.dp))
                            Text(group.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    if (tab.groupId != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.assignTabToGroup(tab.id, null)
                                    showGroupDialog = null
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Close, null)
                            Spacer(Modifier.width(16.dp))
                            Text("Odebrat ze skupiny", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showGroupDialog = null
                                showNewGroupDialog = tab
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(16.dp))
                        Text("Vytvořit novou skupinu", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGroupDialog = null }) { Text("Zrušit") }
            }
        )
    }

    if (showNewGroupDialog != null) {
        val tab = showNewGroupDialog!!
        var groupName by remember { mutableStateOf("") }
        var selectedColor by remember { mutableStateOf(PROFILE_COLORS[0]) }

        AlertDialog(
            onDismissRequest = { showNewGroupDialog = null },
            title = { Text("Nová skupina") },
            text = {
                Column {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Název") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PROFILE_COLORS.forEach { hex ->
                            val color = try { Color(android.graphics.Color.parseColor(hex)) } catch(e:Exception){Color.Gray}
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(2.dp, if(selectedColor == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                                    .clickable { selectedColor = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (groupName.isNotBlank()) {
                        viewModel.createTabGroup(groupName, selectedColor, tab.id)
                        showNewGroupDialog = null
                    }
                }) { Text("Vytvořit") }
            },
            dismissButton = {
                TextButton(onClick = { showNewGroupDialog = null }) { Text("Zrušit") }
            }
        )
    }
}

@Composable
fun GroupHeader(group: TabGroupEntity, onToggle: () -> Unit, onClose: () -> Unit) {
    val groupColor = try { Color(android.graphics.Color.parseColor(group.colorHex)) } catch(e:Exception) { Color.Gray }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (group.isCollapsed) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = groupColor
        )
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(groupColor))
        Spacer(Modifier.width(8.dp))
        Text(
            text = group.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, "Zavřít skupinu")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabCard(
    tab: TabEntity,
    viewModel: BrowserViewModel,
    isSelected: Boolean,
    profileColor: Color,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clip(RoundedCornerShape(16.dp))
            .then(if (isSelected) Modifier.border(3.dp, profileColor, RoundedCornerShape(16.dp)) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tab.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, "Zavřít", modifier = Modifier.size(20.dp))
                }
            }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val thumbnail = viewModel.tabThumbnails[tab.id]
                if (thumbnail != null) {
                    androidx.compose.foundation.Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = tab.url.removePrefix("https://").removePrefix("http://"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
