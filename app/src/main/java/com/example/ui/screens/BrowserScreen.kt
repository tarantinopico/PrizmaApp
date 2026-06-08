package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color.parseColor
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodels.BrowserViewModel
import kotlin.math.roundToInt

import android.os.Build
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
// Add the imports and permission logic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    profileId: Long,
    viewModel: BrowserViewModel,
    onNavigateToTabs: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDownloads: () -> Unit
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    
    LaunchedEffect(profileId) {
        viewModel.initProfile(profileId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val currentProfile by viewModel.currentProfile.collectAsStateWithLifecycle()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()

    var urlInput by remember { mutableStateOf("") }
    var isInputFocused by remember { mutableStateOf(false) }
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // Sync input with current tab URL when not focused
    LaunchedEffect(currentTab, isInputFocused) {
        if (!isInputFocused) {
            urlInput = currentTab?.url ?: ""
        }
    }

    // Scroll Connection to hide bottom bar
    var bottomBarOffset by remember { mutableFloatStateOf(0f) }
    val bottomBarHeight = 80.dp
    val bottomBarHeightPx = with(LocalDensity.current) { bottomBarHeight.roundToPx().toFloat() }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                bottomBarOffset = (bottomBarOffset + delta).coerceIn(-bottomBarHeightPx, 0f)
                return Offset.Zero
            }
        }
    }

    // WebView state cache
    val webViewStates = remember { mutableMapOf<Long, Bundle>() }
    var activeWebView by remember { mutableStateOf<WebView?>(null) }
    
    val profileColor = try {
        Color(parseColor(currentProfile?.accentColorHex ?: "#7C5CFF"))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP BAR
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile indicator
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(profileColor)
                        )
                        Spacer(Modifier.width(12.dp))

                        // Address Bar
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .focusRequester(focusRequester)
                                .onFocusChanged { 
                                    isInputFocused = it.isFocused 
                                    if (it.isFocused && urlInput.startsWith("http")) {
                                        // highlight all or keep it simple
                                    }
                                },
                            placeholder = { Text("Hledat nebo zadat webovou adresu", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            shape = CircleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedBorderColor = profileColor,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Search, "Hledat", modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = {
                                if (isInputFocused && urlInput.isNotEmpty()) {
                                    IconButton(onClick = { urlInput = "" }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Clear, "Vymazat", modifier = Modifier.size(16.dp))
                                    }
                                } else if (!isInputFocused && isLoading) {
                                    IconButton(onClick = { activeWebView?.stopLoading() }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, "Zastavit", modifier = Modifier.size(16.dp))
                                    }
                                } else if (!isInputFocused) {
                                    IconButton(onClick = { activeWebView?.reload() }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Refresh, "Obnovit", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    focusManager.clearFocus()
                                    val parsedUrl = viewModel.parseUrl(urlInput)
                                    if (currentTab != null) {
                                        activeWebView?.loadUrl(parsedUrl)
                                    } else {
                                        viewModel.addTab(parsedUrl, "Načítání...")
                                    }
                                }
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(Modifier.width(4.dp))

                        // Menu Button
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, "Menu")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Nová karta") },
                                    onClick = { 
                                        viewModel.addTab("https://www.google.com", "Nová karta")
                                        menuExpanded = false 
                                    },
                                    leadingIcon = { Icon(Icons.Default.Add, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (bookmarks.any { it.url == currentTab?.url }) "Odebrat záložku" else "Přidat záložku") },
                                    onClick = {
                                        currentTab?.let { tab ->
                                            val existing = bookmarks.find { it.url == tab.url }
                                            if (existing == null) {
                                                viewModel.addBookmark(tab.url, tab.title)
                                            }
                                        }
                                        menuExpanded = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Star, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sdílet") },
                                    onClick = {
                                        menuExpanded = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Share, null) }
                                )
                            }
                        }
                    }
                    if (isLoading) {
                        LinearProgressIndicator(
                            progress = { pageProgress },
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = profileColor,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                    } else {
                        Spacer(Modifier.height(2.dp))
                    }
                }
            }

            // MAIN CONTENT (WebView or Suggestions)
            Box(Modifier.weight(1f)) {
                if (currentTab != null) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)) {
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.setSupportZoom(true)
                                    settings.builtInZoomControls = true
                                    settings.displayZoomControls = false
                                    isNestedScrollingEnabled = true
                                    
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                            pageProgress = newProgress / 100f
                                        }
                                    }
                                    
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            isLoading = true
                                            if (!isInputFocused) urlInput = url ?: ""
                                        }
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            isLoading = false
                                            url?.let {
                                                viewModel.updateCurrentTabUrl(it, view?.title ?: "Web Page")
                                            }
                                        }
                                    }
                                    
                                    setDownloadListener { defaultUrl, userAgent, contentDisposition, mimetype, contentLength ->
                                        viewModel.startDownload(defaultUrl, userAgent, contentDisposition, mimetype, contentLength)
                                    }
                                    
                                    activeWebView = this
                                }
                            },
                            update = { webView ->
                                val targetTabId = currentTab?.id ?: return@AndroidView
                                
                                if (webView.tag != targetTabId) {
                                    // Save state of previous tab
                                    val previousTabId = webView.tag as? Long
                                    if (previousTabId != null) {
                                        val bundle = Bundle()
                                        webView.saveState(bundle)
                                        webViewStates[previousTabId] = bundle
                                    }
                                    
                                    // Load new tab state
                                    webView.tag = targetTabId
                                    val savedState = webViewStates[targetTabId]
                                    if (savedState != null) {
                                        webView.restoreState(savedState)
                                    } else {
                                        webView.loadUrl(currentTab!!.url)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Suggestions Overlay
                if (isInputFocused) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val queryPattern = urlInput.trim().lowercase()
                        val filteredHistory = remember(queryPattern, history) {
                            if (queryPattern.isEmpty()) history.take(15)
                            else history.filter { it.title.lowercase().contains(queryPattern) || it.url.lowercase().contains(queryPattern) }.take(15)
                        }
                        
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(filteredHistory) { entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            focusManager.clearFocus()
                                            if (currentTab != null) {
                                                activeWebView?.loadUrl(entry.url)
                                            } else {
                                                viewModel.addTab(entry.url, entry.title)
                                            }
                                        }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(entry.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(entry.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // BOTTOM BAR
        BottomAppBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset { IntOffset(0, -bottomBarOffset.roundToInt()) },
            containerColor = MaterialTheme.colorScheme.surface,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            IconButton(onClick = { activeWebView?.goBack() }) {
                Icon(Icons.Default.ArrowBack, "Zpět")
            }
            IconButton(onClick = { activeWebView?.goForward() }) {
                Icon(Icons.Default.ArrowForward, "Vpřed")
            }
            Spacer(Modifier.weight(1f))
            
            // Tabs button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { 
                        activeWebView?.let { wv ->
                            try {
                                if (wv.width > 0 && wv.height > 0) {
                                    val bmp = Bitmap.createBitmap(wv.width, wv.height, Bitmap.Config.ARGB_8888)
                                    val canvas = android.graphics.Canvas(bmp)
                                    wv.draw(canvas)
                                    currentTab?.let { tab ->
                                        viewModel.tabThumbnails[tab.id] = bmp
                                    }
                                }
                            } catch(e: Exception) {}
                        }
                        onNavigateToTabs() 
                    }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckBoxOutlineBlank, "Karty", modifier = Modifier.fillMaxSize())
                Text(
                    text = "${tabs.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(Modifier.weight(1f))
            val activeDownloads by remember {
                derivedStateOf { viewModel.downloads.value.count { it.status == "probíhá" } }
            }
            
            IconButton(onClick = onNavigateToDownloads) {
                BadgedBox(
                    badge = {
                        if (activeDownloads > 0) {
                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                Text("$activeDownloads")
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Download, "Stahování")
                }
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Menu, "Možnosti")
            }
        }
    }

    BackHandler(enabled = isInputFocused || activeWebView?.canGoBack() == true) {
        if (isInputFocused) {
            focusManager.clearFocus()
        } else if (activeWebView?.canGoBack() == true) {
            activeWebView?.goBack()
        }
    }
}
