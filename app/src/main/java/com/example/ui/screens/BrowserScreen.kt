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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.example.ui.utils.HapticType
import com.example.ui.utils.rememberHapticHelper
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    profileId: Long,
    viewModel: BrowserViewModel,
    onNavigateToTabs: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onSwitchProfile: (Long) -> Unit,
    onNavigateToProfileSelection: () -> Unit
) {
    val context = LocalContext.current
    val hapticHelper = rememberHapticHelper()
    var contextMenuHitResult by remember { mutableStateOf<WebView.HitTestResult?>(null) }
    var showGroupSelectorUrl by remember { mutableStateOf<String?>(null) }
    var showProfileSelectorUrl by remember { mutableStateOf<String?>(null) }
    var showQuickProfileSwitcher by remember { mutableStateOf(false) }
    
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val tabGroups by viewModel.tabGroups.collectAsStateWithLifecycle()
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    
    LaunchedEffect(profileId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val currentProfile by viewModel.currentProfile.collectAsStateWithLifecycle()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()

    val autoHidePanel by viewModel.settingsDataStore.autoHidePanel.collectAsStateWithLifecycle(initialValue = false)
    val panelStyle by viewModel.settingsDataStore.panelStyle.collectAsStateWithLifecycle(initialValue = "floating")
    var manuallyHidden by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    var urlInput by remember { mutableStateOf("") }
    var isInputFocused by remember { mutableStateOf(false) }
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    val scope = rememberCoroutineScope()
    var bookmarkToEdit by remember { mutableStateOf<com.example.data.local.entity.BookmarkEntity?>(null) }
    var bookmarkNewTitle by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Sync input with current tab URL when not focused
    LaunchedEffect(currentTab, isInputFocused) {
        if (!isInputFocused) {
            urlInput = if (currentTab?.url == BrowserViewModel.NEW_TAB_URL) "" else currentTab?.url ?: ""
        }
    }

    // Scroll Connection to hide bottom bar
    var bottomBarOffset by remember { mutableFloatStateOf(0f) }
    val bottomBarHeight = 84.dp
    val bottomBarHeightPx = with(LocalDensity.current) { bottomBarHeight.roundToPx().toFloat() }

    val nestedScrollConnection = remember(autoHidePanel, manuallyHidden) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!autoHidePanel || manuallyHidden) return Offset.Zero
                val delta = available.y
                bottomBarOffset = (bottomBarOffset + delta).coerceIn(-bottomBarHeightPx * 2f, 0f)
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(autoHidePanel, manuallyHidden) {
        if (!autoHidePanel && !manuallyHidden) {
             bottomBarOffset = 0f
        }
    }

    val targetOffset = if (manuallyHidden) -bottomBarHeightPx * 2f else bottomBarOffset 
    val animatedOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetOffset,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "panelOffset"
    )

    val webViewStates = viewModel.webViewStates
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
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(profileColor)
                                .clickable { showQuickProfileSwitcher = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentProfile?.name?.take(1)?.uppercase() ?: "",
                                color = MaterialTheme.colorScheme.surface,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Spacer(Modifier.width(8.dp))

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
                                    text = { Text(if (bookmarks.any { it.url == currentTab?.url }) "Odebrat z oblíbených" else "Přidat do oblíbených") },
                                    onClick = {
                                        currentTab?.let { tab ->
                                            if (tab.url != BrowserViewModel.NEW_TAB_URL) {
                                                hapticHelper.perform(HapticType.TAB_ACTION)
                                                viewModel.toggleBookmark(tab.url, tab.title)
                                                scope.launch {
                                                    val isBookmarked = bookmarks.any { it.url == tab.url }
                                                    val msg = if (isBookmarked) "Odebráno z oblíbených" else "Přidáno do oblíbených"
                                                    snackbarHostState.currentSnackbarData?.dismiss()
                                                    snackbarHostState.showSnackbar(msg)
                                                }
                                            }
                                        }
                                        menuExpanded = false
                                    },
                                    leadingIcon = { 
                                        Icon(if (bookmarks.any { it.url == currentTab?.url }) Icons.Default.Star else Icons.Outlined.Star, null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sdílet") },
                                    onClick = {
                                        menuExpanded = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Share, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (manuallyHidden) "Zobrazit panel" else "Skrýt panel") },
                                    onClick = {
                                        manuallyHidden = !manuallyHidden
                                        menuExpanded = false
                                    },
                                    leadingIcon = { 
                                        Icon(if (manuallyHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                                    }
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
            val paddingBottomDp = if (panelStyle == "full") {
                val offDp = with(LocalDensity.current) { (-animatedOffset).toDp() }
                // Calculate height of the full bar. Roughly 64.dp + navigationBars.
                val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val fullHeight = 64.dp + navBarHeight
                maxOf(0.dp, fullHeight - offDp)
            } else {
                0.dp
            }

            Box(Modifier.weight(1f).padding(bottom = paddingBottomDp)) {
                if (currentTab != null) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)) {
                        
                        if (currentTab?.url == BrowserViewModel.NEW_TAB_URL) {
                            NewTabPage(
                                profile = currentProfile,
                                profileColor = profileColor,
                                bookmarks = bookmarks,
                                history = history,
                                onUrlClick = { url ->
                                    if (currentTab != null) {
                                        activeWebView?.loadUrl(url)
                                        // Sometimes webview is not active yet, let's just update viewmodel url
                                        viewModel.updateCurrentTabUrl(url, "Načítání...")
                                    } else {
                                        viewModel.addTab(url, "Načítání...")
                                    }
                                    urlInput = url
                                },
                                onBookmarkLongCLick = { bookmark ->
                                    bookmarkToEdit = bookmark
                                    bookmarkNewTitle = bookmark.title
                                    hapticHelper.perform(HapticType.LONG_PRESS)
                                },
                                onSearchBarClick = {
                                    focusRequester.requestFocus()
                                }
                            )
                        }
                        
                        // Always keep WebView in hierarchy but invisible/gone if NewTabPage is showing,
                        // or just use Alpha, since removing it might destroy it. Actually doing a simple alpha or offset is safer for WebView.
                        // Wait, AndroidView can just be composed and visible/invisible or replaced. Re-creating WebView is expensive.
                        // But wait! If we do `if(url == ...) NewTabPage else AndroidView`, AndroidView is disposed!
                        // Instead, we just overlay NewTabPage and hide WebView if NEW_TAB_URL.
                        Box(modifier = Modifier.fillMaxSize().alpha(if (currentTab?.url == BrowserViewModel.NEW_TAB_URL) 0f else 1f)) {
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
                                            if (url == BrowserViewModel.NEW_TAB_URL || url == "about:blank") return
                                            isLoading = true
                                            if (!isInputFocused) urlInput = url ?: ""
                                        }
                                        override fun onReceivedError(
                                            view: WebView?,
                                            request: android.webkit.WebResourceRequest?,
                                            error: android.webkit.WebResourceError?
                                        ) {
                                            super.onReceivedError(view, request, error)
                                            val failingUrl = request?.url?.toString()
                                            if (failingUrl == BrowserViewModel.NEW_TAB_URL || failingUrl == "about:blank") return
                                            if (request?.isForMainFrame == true) {
                                                view?.loadDataWithBaseURL(null, "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>body{font-family:sans-serif;display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;margin:0;padding:24px;text-align:center;background:#1e1e1e;color:#ddd}h1{font-size:24px;margin-bottom:8px}p{font-size:14px;color:#aaa}</style></head><body><h1>Nepodařilo se načíst stránku</h1><p>Ověřte své připojení nebo zkuste adresu zadat znovu.</p></body></html>", "text/html", "UTF-8", null)
                                            }
                                        }
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            if (url == BrowserViewModel.NEW_TAB_URL || url == "about:blank") return
                                            isLoading = false
                                            url?.let {
                                                viewModel.updateCurrentTabUrl(it, view?.title ?: "Web Page")
                                            }
                                        }
                                    }
                                    
                                    setDownloadListener { defaultUrl, userAgent, contentDisposition, mimetype, contentLength ->
                                        viewModel.startDownload(defaultUrl, userAgent, contentDisposition, mimetype, contentLength)
                                    }
                                    
                                    setOnLongClickListener { v ->
                                        val wv = v as? WebView
                                        val result = wv?.hitTestResult
                                        if (result != null && result.type != WebView.HitTestResult.UNKNOWN_TYPE) {
                                            contextMenuHitResult = result
                                            hapticHelper.perform(HapticType.LONG_PRESS)
                                            true
                                        } else {
                                            false
                                        }
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
                                        if (currentTab!!.url != BrowserViewModel.NEW_TAB_URL && currentTab!!.url != "about:blank") {
                                            webView.loadUrl(currentTab!!.url)
                                        } else {
                                            webView.loadDataWithBaseURL(null, "", "text/html", "UTF-8", null)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        // Close Box for alpha wrapper
                        }
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

        // BOTTOM BAR CONTENT
        val bottomBarContent: @Composable () -> Unit = {
            val isFull = panelStyle == "full"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isFull) Modifier else Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp))
                    .then(if (isFull) Modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) else Modifier.clip(RoundedCornerShape(32.dp)))
                    .background(if (isFull) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
                    .then(if (isFull) Modifier.border(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) else Modifier.border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(32.dp)))
                    .windowInsetsPadding(WindowInsets.navigationBars),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isFull) 8.dp else 16.dp, vertical = if (isFull) 8.dp else 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { activeWebView?.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zpět")
                    }
                    IconButton(onClick = { activeWebView?.goForward() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Vpřed")
                    }
                    
                    // Tabs button
                    Box(
                        modifier = Modifier
                            .size(if (isFull) 40.dp else 44.dp)
                            .clip(RoundedCornerShape(12.dp))
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
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    val activeDownloads by remember(viewModel.downloads.value) {
                        derivedStateOf { viewModel.downloads.value.count { it.status == "probíhá" } }
                    }
                    
                    IconButton(onClick = onNavigateToDownloads) {
                        BadgedBox(
                            badge = {
                                if (activeDownloads > 0) {
                                    Badge(containerColor = profileColor) {
                                        Text("$activeDownloads", color = Color.White)
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
        }

        Box(
            modifier = Modifier.align(Alignment.BottomCenter)
                .offset { IntOffset(0, -animatedOffset.roundToInt()) }
        ) {
            bottomBarContent()
        }

        if (manuallyHidden || animatedOffset < -10f) {
             SmallFloatingActionButton(
                  onClick = { 
                      manuallyHidden = false
                      bottomBarOffset = 0f 
                  },
                  modifier = Modifier
                      .align(Alignment.BottomEnd)
                      .padding(16.dp)
                      .windowInsetsPadding(WindowInsets.navigationBars),
                  containerColor = MaterialTheme.colorScheme.primaryContainer,
                  contentColor = MaterialTheme.colorScheme.onPrimaryContainer
             ) {
                  Icon(Icons.Default.VerticalAlignTop, "Zobrazit panel")
             }
        }
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
        )
    }

    BackHandler(enabled = isInputFocused || activeWebView?.canGoBack() == true) {
        if (isInputFocused) {
            focusManager.clearFocus()
        } else if (activeWebView?.canGoBack() == true) {
            activeWebView?.goBack()
        }
    }

    if (contextMenuHitResult != null) {
        val hitResult = contextMenuHitResult!!
        val type = hitResult.type
        val extra = hitResult.extra
        
        val isLink = type == WebView.HitTestResult.SRC_ANCHOR_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
        val isImage = type == WebView.HitTestResult.IMAGE_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
        
        ModalBottomSheet(
            onDismissRequest = { contextMenuHitResult = null },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, top = 8.dp)
            ) {
                if (extra != null) {
                    Text(
                        text = extra,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                if (isLink) {
                    ListItem(
                        headlineContent = { Text("Otevřít na nové kartě") },
                        leadingContent = { Icon(Icons.Default.Add, null) },
                        modifier = Modifier.clickable {
                            if (extra != null) viewModel.addTab(extra, "Načítání...")
                            contextMenuHitResult = null
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    if (tabGroups.isNotEmpty()) {
                        ListItem(
                            headlineContent = { Text("Otevřít na nové kartě ve skupině…") },
                            leadingContent = { Icon(Icons.Default.Folder, null) },
                            modifier = Modifier.clickable {
                                if (extra != null) {
                                    showGroupSelectorUrl = extra
                                }
                                contextMenuHitResult = null
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                    if (profiles.isNotEmpty()) {
                        ListItem(
                            headlineContent = { Text("Otevřít v jiném profilu…") },
                            leadingContent = { Icon(Icons.Default.Person, null) },
                            modifier = Modifier.clickable {
                                if (extra != null) {
                                    showProfileSelectorUrl = extra
                                }
                                contextMenuHitResult = null
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                    ListItem(
                        headlineContent = { Text("Kopírovat odkaz") },
                        leadingContent = { Icon(Icons.Default.ContentCopy, null) },
                        modifier = Modifier.clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("URL", extra))
                            contextMenuHitResult = null
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Sdílet odkaz") },
                        leadingContent = { Icon(Icons.Default.Share, null) },
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                setType("text/plain")
                                putExtra(Intent.EXTRA_TEXT, extra)
                            }
                            context.startActivity(Intent.createChooser(intent, "Sdílet"))
                            contextMenuHitResult = null
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }

                if (isImage) {
                    if (isLink) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    ListItem(
                        headlineContent = { Text("Stáhnout obrázek") },
                        leadingContent = { Icon(Icons.Default.Download, null) },
                        modifier = Modifier.clickable {
                            if (extra != null) {
                                viewModel.startDownload(extra, null, null, null, 0)
                            }
                            contextMenuHitResult = null
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Otevřít obrázek na nové kartě") },
                        leadingContent = { Icon(Icons.Default.Image, null) },
                        modifier = Modifier.clickable {
                            if (extra != null) viewModel.addTab(extra, "Obrázek")
                            contextMenuHitResult = null
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Sdílet obrázek") },
                        leadingContent = { Icon(Icons.Default.Share, null) },
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                setType("text/plain")
                                putExtra(Intent.EXTRA_TEXT, extra)
                            }
                            context.startActivity(Intent.createChooser(intent, "Sdílet"))
                            contextMenuHitResult = null
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                
                if (!isLink && !isImage && type != WebView.HitTestResult.UNKNOWN_TYPE && extra != null) {
                    ListItem(
                        headlineContent = { Text("Kopírovat") },
                        leadingContent = { Icon(Icons.Default.ContentCopy, null) },
                        modifier = Modifier.clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Text", extra))
                            contextMenuHitResult = null
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Vyhledat na Googlu") },
                        leadingContent = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.clickable {
                            viewModel.addTab("https://www.google.com/search?q=${android.net.Uri.encode(extra)}", "Hledat: $extra")
                            contextMenuHitResult = null
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }

    if (showGroupSelectorUrl != null) {
        AlertDialog(
            onDismissRequest = { showGroupSelectorUrl = null },
            title = { Text("Otevřít ve skupině") },
            text = {
                LazyColumn {
                    items(tabGroups) { group ->
                        Text(
                            text = group.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addTabToGroup(showGroupSelectorUrl!!, "Odkaz", group.id)
                                    showGroupSelectorUrl = null
                                }
                                .padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGroupSelectorUrl = null }) {
                    Text("Zrušit")
                }
            }
        )
    }

    if (showProfileSelectorUrl != null) {
        AlertDialog(
            onDismissRequest = { showProfileSelectorUrl = null },
            title = { Text("Otevřít v jiném profilu") },
            text = {
                LazyColumn {
                    items(profiles) { p ->
                        Text(
                            text = p.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addTabToProfile(showProfileSelectorUrl!!, "Odkaz", p.id)
                                    showProfileSelectorUrl = null
                                }
                                .padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileSelectorUrl = null }) {
                    Text("Zrušit")
                }
            }
        )
    }

    if (showQuickProfileSwitcher) {
        ModalBottomSheet(
            onDismissRequest = { showQuickProfileSwitcher = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, top = 8.dp)
            ) {
                Text(
                    text = "Přepnout profil",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn {
                    items(profiles) { p ->
                        val isCurrent = p.id == profileId
                        val pColor = try { Color(parseColor(p.accentColorHex)) } catch(e:Exception) { MaterialTheme.colorScheme.primary }
                        ListItem(
                            headlineContent = { Text(p.name, style = if (isCurrent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge) },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(pColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = p.name.take(1).uppercase(),
                                        color = MaterialTheme.colorScheme.surface,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            },
                            trailingContent = {
                                if (isCurrent) {
                                    Icon(Icons.Default.Check, null, tint = pColor)
                                }
                            },
                            modifier = Modifier.clickable {
                                showQuickProfileSwitcher = false
                                if (!isCurrent) {
                                    hapticHelper.perform(HapticType.SWITCH_PROFILE)
                                    onSwitchProfile(p.id)
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ListItem(
                            headlineContent = { Text("Spravovat profily") },
                            leadingContent = { Icon(Icons.Default.Settings, null) },
                            modifier = Modifier.clickable {
                                showQuickProfileSwitcher = false
                                hapticHelper.perform(HapticType.SWITCH_PROFILE)
                                onNavigateToProfileSelection()
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }

    if (bookmarkToEdit != null) {
        AlertDialog(
            onDismissRequest = { bookmarkToEdit = null },
            title = { Text("Spravovat záložku") },
            text = {
                Column {
                    OutlinedTextField(
                        value = bookmarkNewTitle,
                        onValueChange = { bookmarkNewTitle = it },
                        label = { Text("Název") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateBookmarkTitle(bookmarkToEdit!!.id, bookmarkNewTitle)
                    bookmarkToEdit = null
                }) {
                    Text("Uložit")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.removeBookmark(bookmarkToEdit!!.id)
                    bookmarkToEdit = null
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Smazat")
                }
            }
        )
    }
}
