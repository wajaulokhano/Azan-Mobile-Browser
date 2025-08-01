package com.example.noorahlulbayt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.noorahlulbayt.screens.*
import com.example.noorahlulbayt.utils.AppLogger
import com.example.noorahlulbayt.utils.PermissionHelper
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BrowserActivity : ComponentActivity() {
    private lateinit var browserViewModel: BrowserViewModel
    private lateinit var azanBlockReceiver: BroadcastReceiver
    private var isAzanBlocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        AppLogger.i("BrowserActivity", "onCreate - Starting Azan Mobile Browser")
        
        // Request storage permissions for logging
        if (!PermissionHelper.hasStoragePermission(this)) {
            AppLogger.w("BrowserActivity", "Storage permission not granted, requesting...")
            PermissionHelper.requestStoragePermission(this)
        } else {
            AppLogger.i("BrowserActivity", "Storage permission already granted")
        }
        
        browserViewModel = BrowserViewModel(application)
        
        // Register Azan block receiver
        azanBlockReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.example.noorahlulbayt.BLOCK_BROWSER") {
                    AppLogger.logPrayerTimeEvent("Browser blocking started")
                    isAzanBlocked = true
                    browserViewModel.blockAllTabs()
                    showAzanBlockMessage()
                }
            }
        }
        registerReceiver(azanBlockReceiver, IntentFilter("com.example.noorahlulbayt.BLOCK_BROWSER"))
        AppLogger.i("BrowserActivity", "Azan block receiver registered")

        setContent {
            BrowserApp(
                browserViewModel = browserViewModel,
                isAzanBlocked = isAzanBlocked
            )
        }
    }

    private fun showAzanBlockMessage() {
        Toast.makeText(this, "Browser is not available during prayer time for 10 minutes", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(azanBlockReceiver)
    }
}

@Composable
fun BrowserApp(
    browserViewModel: BrowserViewModel = viewModel(),
    isAzanBlocked: Boolean = false
) {
    val navController = rememberNavController()
    var currentTabIndex by remember { mutableStateOf(0) }
    
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF006400), // Green
            background = Color.Black,
            surface = Color.Black,
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        NavHost(
            navController = navController,
            startDestination = "browser"
        ) {
            composable("browser") {
                BrowserScreen(
                    browserViewModel = browserViewModel,
                    isAzanBlocked = isAzanBlocked,
                    onNavigateToFavorites = { navController.navigate("favorites") },
                    onNavigateToDownloads = { navController.navigate("downloads") },
                    onNavigateToHistory = { navController.navigate("history") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onCurrentTabIndexChanged = { currentTabIndex = it }
                )
            }
            composable("favorites") {
                FavoritesScreen(
                    browserViewModel = browserViewModel,
                    onNavigateToUrl = { url ->
                        // Navigate back to browser and load URL in current tab
                        navController.popBackStack()
                        if (currentTabIndex >= 0 && currentTabIndex < browserViewModel.tabs.value.size) {
                            browserViewModel.navigateToUrl(currentTabIndex, url)
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("downloads") {
                DownloadsScreen(
                    browserViewModel = browserViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("history") {
                HistoryScreen(
                    browserViewModel = browserViewModel,
                    onNavigateToUrl = { url ->
                        // Navigate back to browser and load URL in current tab
                        navController.popBackStack()
                        if (currentTabIndex >= 0 && currentTabIndex < browserViewModel.tabs.value.size) {
                            browserViewModel.navigateToUrl(currentTabIndex, url)
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onViewLogs = { navController.navigate("logs") }
                )
            }
            composable("logs") {
                LogViewerScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun BrowserScreen(
    browserViewModel: BrowserViewModel = viewModel(),
    isAzanBlocked: Boolean = false,
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onCurrentTabIndexChanged: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()
    
    // Track current tab index and notify parent
    LaunchedEffect(pagerState.currentPage) {
        onCurrentTabIndexChanged(pagerState.currentPage)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top Bar
        TopBar(
            onNewTab = { browserViewModel.addTab() },
            onSettings = onNavigateToSettings,
            onSetDefault = {
                val intent = Intent(android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS)
                context.startActivity(intent)
            },
            onFavorites = onNavigateToFavorites,
            onDownloads = onNavigateToDownloads,
            onHistory = onNavigateToHistory
        )

        // Tab Bar
        val tabs by browserViewModel.tabs.collectAsState()
        if (tabs.isNotEmpty()) {
            TabBar(
                tabs = tabs,
                currentTabIndex = pagerState.currentPage,
                onTabClick = { index ->
                    AppLogger.d("TabBar", "Tab clicked: $index")
                    coroutineScope.launch {
                        try {
                            pagerState.animateScrollToPage(index)
                            AppLogger.d("TabBar", "Successfully switched to tab: $index")
                        } catch (e: Exception) {
                            AppLogger.e("TabBar", "Failed to switch to tab: $index", e)
                        }
                    }
                },
                onTabClose = { index ->
                    AppLogger.d("TabBar", "Close tab clicked: $index")
                    try {
                        browserViewModel.closeTab(index)
                        AppLogger.d("TabBar", "Successfully closed tab: $index")
                    } catch (e: Exception) {
                        AppLogger.e("TabBar", "Failed to close tab: $index", e)
                    }
                }
            )
        }

        // WebView Pager
        HorizontalPager(
            count = tabs.size,
            state = pagerState
        ) { page ->
            if (page < tabs.size) {
                val tab = tabs[page]
                WebViewTab(
                    tab = tab,
                    isAzanBlocked = isAzanBlocked,
                    onUrlChanged = { url -> browserViewModel.updateTabUrl(page, url) },
                    onTitleChanged = { title -> browserViewModel.updateTabTitle(page, title) }
                )
            }
        }
    }
}

@Composable
fun TopBar(
    onNewTab: () -> Unit,
    onSettings: () -> Unit,
    onSetDefault: () -> Unit,
    onFavorites: () -> Unit,
    onDownloads: () -> Unit,
    onHistory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF006400))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Azan Mobile Browser",
            color = Color.White,
            fontSize = 18.sp,
            fontFamily = FontFamily.Default
        )
        
        Row {
            IconButton(onClick = onFavorites) {
                Text("⭐", color = Color.White, fontSize = 20.sp)
            }
            IconButton(onClick = onDownloads) {
                Text("⬇", color = Color.White, fontSize = 20.sp)
            }
            IconButton(onClick = onHistory) {
                Text("📚", color = Color.White, fontSize = 20.sp)
            }
            IconButton(onClick = onNewTab) {
                Text("+", color = Color.White, fontSize = 20.sp)
            }
            IconButton(onClick = onSettings) {
                Text("⚙", color = Color.White, fontSize = 20.sp)
            }
            IconButton(onClick = onSetDefault) {
                Text("🌐", color = Color.White, fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun TabBar(
    tabs: List<BrowserTab>,
    currentTabIndex: Int,
    onTabClick: (Int) -> Unit,
    onTabClose: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .height(40.dp), // Fixed height for mobile
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(tabs.size) { index ->
            val tab = tabs[index]
            TabItem(
                tab = tab,
                isSelected = index == currentTabIndex,
                onClick = { 
                    AppLogger.logTabOperation("CLICKED", index, tabs.size, tab.url)
                    onTabClick(index) 
                },
                onClose = { 
                    AppLogger.logTabOperation("CLOSE_CLICKED", index, tabs.size, tab.url)
                    onTabClose(index) 
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabItem(
    tab: BrowserTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFF006400) else Color(0xFF333333)
    
    Card(
        modifier = Modifier
            .widthIn(min = 120.dp, max = 200.dp)
            .height(32.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = tab.title.ifEmpty { "New Tab" },
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            )
            
            // Close button
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "×",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun WebViewTab(
    tab: BrowserTab,
    isAzanBlocked: Boolean,
    onUrlChanged: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    browserViewModel: BrowserViewModel = viewModel()
) {
    var isLoading by remember { mutableStateOf(false) }
    var showFilteringAnimation by remember { mutableStateOf(false) }
    
    if (isAzanBlocked) {
        AzanBlockScreen()
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                        }
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                AppLogger.d("WebView", "Page started loading: $url")
                                isLoading = true
                                showFilteringAnimation = true
                                
                                // Start filtering
                                view?.post {
                                    performFiltering(view, url ?: "")
                                }
                            }
                            
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                AppLogger.d("WebView", "Page finished loading: $url")
                                isLoading = false
                                showFilteringAnimation = false
                                onUrlChanged(url ?: "")
                            }
                            
                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                AppLogger.logWebViewError(
                                    url = request?.url?.toString() ?: "unknown",
                                    error = error?.description?.toString() ?: "unknown error",
                                    errorCode = error?.errorCode ?: -1
                                )
                            }
                            
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                return AdBlocker.shouldBlock(request?.url?.toString() ?: "")
                            }
                        }
                        
                        webChromeClient = object : WebChromeClient() {
                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                onTitleChanged(title ?: "")
                                
                                // Add to history
                                val url = view?.url ?: ""
                                val pageTitle = title ?: ""
                                if (url.isNotEmpty() && pageTitle.isNotEmpty()) {
                                    browserViewModel.addHistoryEntry(url, pageTitle)
                                }
                            }
                        }

                        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                            // Start download
                            val filename = contentDisposition?.substringAfter("filename=")?.removeSurrounding("\"") ?: "download"
                            browserViewModel.startDownload(url, filename)
                        }

                        loadUrl(tab.url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF006400))
                }
            }
            
            // Filtering animation
            if (showFilteringAnimation) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF006400))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Checking for Profanity",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AzanBlockScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🕌",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Browser is not available during prayer time for 10 minutes",
                color = Color(0xFF006400),
                fontSize = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun performFiltering(webView: WebView, url: String) {
    AppLogger.d("ContentFilter", "Starting content filtering for URL: $url")
    
    // Keyword filtering
    webView.evaluateJavascript("""
        (function() {
            var text = document.body ? document.body.innerText : '';
            var keywords = ['nude', 'adult', 'explicit', 'porn', 'sex', 'xxx'];
            var regex = /\\b(nudity|pornography|erotic)\\b/i;
            
            for (var i = 0; i < keywords.length; i++) {
                if (text.toLowerCase().indexOf(keywords[i]) !== -1) {
                    return JSON.stringify({blocked: true, reason: 'keyword: ' + keywords[i]});
                }
            }
            
            if (regex.test(text)) {
                return JSON.stringify({blocked: true, reason: 'regex match'});
            }
            
            return JSON.stringify({blocked: false, reason: 'clean content'});
        })();
    """.trimIndent()) { result ->
        try {
            if (result.contains("\"blocked\":true")) {
                AppLogger.logFilteringResult(url, true, "Keyword filtering - $result")
                webView.loadUrl("about:blank")
            } else {
                AppLogger.logFilteringResult(url, false, "Keyword filtering passed")
            }
        } catch (e: Exception) {
            AppLogger.e("ContentFilter", "Error processing keyword filter result", e)
        }
    }
    
    // NSFWJS filtering (simplified for demo)
    // In real implementation, capture screenshot and analyze with NSFWJS
    webView.postDelayed({
        try {
            if (url.contains("porn") || url.contains("adult")) {
                AppLogger.logFilteringResult(url, true, "URL-based filtering - suspicious URL pattern")
                webView.loadUrl("about:blank")
            } else {
                AppLogger.logFilteringResult(url, false, "URL-based filtering passed")
            }
        } catch (e: Exception) {
            AppLogger.e("ContentFilter", "Error in URL-based filtering", e)
        }
    }, 500)
}