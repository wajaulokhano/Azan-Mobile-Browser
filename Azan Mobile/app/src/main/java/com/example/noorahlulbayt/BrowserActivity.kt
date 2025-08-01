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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class BrowserActivity : ComponentActivity() {
    private lateinit var browserViewModel: BrowserViewModel
    private lateinit var azanBlockReceiver: BroadcastReceiver
    private var isAzanBlocked = false
    private lateinit var nsfwjsFilterManager: NSFWJSFilterManager
    private lateinit var filteringSettingsManager: FilteringSettingsManager

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
        
        // Initialize advanced filtering system
        initializeFiltering()
        
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

    private fun initializeFiltering() {
        AppLogger.i("BrowserActivity", "Initializing advanced filtering system")
        
        filteringSettingsManager = FilteringSettingsManager(this)
        nsfwjsFilterManager = NSFWJSFilterManager(this).apply {
            filteringMode = filteringSettingsManager.filteringMode
            allowBlobImages = filteringSettingsManager.allowBlobImages
            allowBase64Images = filteringSettingsManager.allowBase64Images
        }
        
        // Initialize NSFWJS in background
        CoroutineScope(Dispatchers.Main).launch {
            try {
                nsfwjsFilterManager.initialize()
                AppLogger.i("BrowserActivity", "NSFWJS filtering system initialized")
            } catch (e: Exception) {
                AppLogger.e("BrowserActivity", "Failed to initialize NSFWJS filtering", e)
            }
        }
    }

    private fun performFiltering(webView: WebView, url: String) {
        AppLogger.d("ContentFilter", "Starting advanced content filtering for URL: $url")
        
        // 1. Quick offline filtering first (for immediate blocking)
        val urlResult = OfflineContentFilter.shouldBlockUrl(url)
        if (urlResult.blocked) {
            val blockedHtml = OfflineContentFilter.createBlockedPageHtml(urlResult.reason)
            webView.loadDataWithBaseURL(null, blockedHtml, "text/html", "UTF-8", null)
            return
        }
        
        // 2. Enhanced post-load filtering with NSFWJS
        webView.postDelayed({
            // First do offline content analysis
            OfflineContentFilter.analyzePageContent(webView, url) { offlineResult ->
                if (offlineResult.blocked) {
                    AppLogger.i("ContentFilter", "Content blocked by offline analysis: ${offlineResult.reason}")
                    val blockedHtml = OfflineContentFilter.createBlockedPageHtml(offlineResult.reason)
                    webView.loadDataWithBaseURL(null, blockedHtml, "text/html", "UTF-8", null)
                } else {
                    // If offline analysis passes, run NSFWJS for visual content
                    if (filteringSettingsManager.nsfwjsEnabled) {
                        nsfwjsFilterManager.performPostLoadFiltering(webView, url)
                    }
                }
            }
        }, 1500) // Wait for page to load
    }

    private fun injectNSFWJS(webView: WebView, onComplete: () -> Unit) {
        AppLogger.d("NSFWJS", "Injecting NSFWJS library into WebView")
        
        try {
            // Read NSFWJS JavaScript from assets
            val context = webView.context
            val nsfwjsScript = context.assets.open("nsfwjs/nsfwjs.min.js").bufferedReader().use { it.readText() }
            
            // Inject the NSFWJS library
            webView.evaluateJavascript("""
                (function() {
                    try {
                        // Inject NSFWJS script
                        $nsfwjsScript
                        
                        // Verify NSFWJS is loaded
                        if (typeof nsfwjs !== 'undefined') {
                            console.log('NSFWJS loaded successfully');
                            return JSON.stringify({success: true, message: 'NSFWJS loaded'});
                        } else {
                            console.log('NSFWJS failed to load');
                            return JSON.stringify({success: false, message: 'NSFWJS not available'});
                        }
                    } catch (e) {
                        console.log('Error loading NSFWJS: ' + e.message);
                        return JSON.stringify({success: false, message: 'Error: ' + e.message});
                    }
                })();
            """.trimIndent()) { result ->
                try {
                    AppLogger.d("NSFWJS", "Injection result: $result")
                    if (result.contains("\"success\":true")) {
                        AppLogger.i("NSFWJS", "NSFWJS library loaded successfully")
                    } else {
                        AppLogger.w("NSFWJS", "NSFWJS library failed to load: $result")
                    }
                } catch (e: Exception) {
                    AppLogger.e("NSFWJS", "Error processing injection result", e)
                }
                
                // Continue with filtering regardless of NSFWJS status
                onComplete()
            }
            
        } catch (e: Exception) {
            AppLogger.e("NSFWJS", "Failed to read NSFWJS script from assets", e)
            // Continue with filtering even if NSFWJS fails to load
            onComplete()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(azanBlockReceiver)
        
        // Cleanup NSFWJS resources
        if (::nsfwjsFilterManager.isInitialized) {
            nsfwjsFilterManager.cleanup()
        }
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
                    onViewLogs = { navController.navigate("logs") },
                    onViewHistory = { navController.navigate("history") }
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
    
    // Store WebView references for navigation
    val webViewRefs = remember { mutableMapOf<Int, WebView>() }
    
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
            onFavorites = onNavigateToFavorites,
            onDownloads = onNavigateToDownloads
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
            
            // Navigation Controls Bar
            NavigationBar(
                currentTab = tabs.getOrNull(pagerState.currentPage),
                onBack = { 
                    val currentWebView = webViewRefs[pagerState.currentPage]
                    if (currentWebView?.canGoBack() == true) {
                        currentWebView.goBack()
                        AppLogger.d("Navigation", "WebView back navigation executed")
                    } else {
                        AppLogger.d("Navigation", "WebView cannot go back")
                    }
                },
                onRefresh = { 
                    val currentWebView = webViewRefs[pagerState.currentPage]
                    currentWebView?.reload()
                    AppLogger.d("Navigation", "WebView refresh executed")
                },
                onAddToFavorites = { 
                    val currentTab = tabs.getOrNull(pagerState.currentPage)
                    currentTab?.let { tab ->
                        browserViewModel.addFavorite(tab.url, tab.title)
                        AppLogger.d("Navigation", "Added to favorites: ${tab.url}")
                    }
                },
                onHistory = onNavigateToHistory
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
                    onTitleChanged = { title -> browserViewModel.updateTabTitle(page, title) },
                    onWebViewReady = { webView -> 
                        webViewRefs[page] = webView
                        AppLogger.d("WebView", "WebView reference stored for tab $page")
                    }
                )
            }
        }
    }
}

@Composable
fun TopBar(
    onNewTab: () -> Unit,
    onSettings: () -> Unit,
    onFavorites: () -> Unit,
    onDownloads: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF006400))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Azan Mobile Browser",
            color = Color.White,
            fontSize = 16.sp,
            fontFamily = FontFamily.Default,
            modifier = Modifier.weight(1f)
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onFavorites,
                modifier = Modifier.size(36.dp)
            ) {
                Text("⭐", color = Color.White, fontSize = 16.sp)
            }
            IconButton(
                onClick = onDownloads,
                modifier = Modifier.size(36.dp)
            ) {
                Text("⬇", color = Color.White, fontSize = 16.sp)
            }
            IconButton(
                onClick = onNewTab,
                modifier = Modifier.size(36.dp)
            ) {
                Text("+", color = Color.White, fontSize = 18.sp)
            }
            IconButton(
                onClick = onSettings,
                modifier = Modifier.size(36.dp)
            ) {
                Text("⚙", color = Color.White, fontSize = 16.sp)
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
    browserViewModel: BrowserViewModel = viewModel(),
    onWebViewReady: (WebView) -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(false) }
    var showFilteringAnimation by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    
    if (isAzanBlocked) {
        AzanBlockScreen()
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webView = this
                        onWebViewReady(this)
                        
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
                                    // Use offline filtering for now
                                    val urlResult = OfflineContentFilter.shouldBlockUrl(url ?: "")
                                    if (urlResult.blocked) {
                                        val blockedHtml = OfflineContentFilter.createBlockedPageHtml(urlResult.reason)
                                        view.loadDataWithBaseURL(null, blockedHtml, "text/html", "UTF-8", null)
                                    }
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
fun NavigationBar(
    currentTab: BrowserTab?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAddToFavorites: () -> Unit,
    onHistory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2A2A2A))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(40.dp)
        ) {
            Text("←", color = Color.White, fontSize = 20.sp)
        }
        
        // Refresh button
        IconButton(
            onClick = onRefresh,
            modifier = Modifier.size(40.dp)
        ) {
            Text("🔄", color = Color.White, fontSize = 16.sp)
        }
        
        // Add to favorites button
        IconButton(
            onClick = onAddToFavorites,
            modifier = Modifier.size(40.dp)
        ) {
            Text("⭐", color = Color.White, fontSize = 16.sp)
        }
        
        // History button
        IconButton(
            onClick = onHistory,
            modifier = Modifier.size(40.dp)
        ) {
            Text("📚", color = Color.White, fontSize = 16.sp)
        }
        
        // URL display (truncated)
        Text(
            text = currentTab?.url?.let { url ->
                if (url.length > 30) "${url.take(30)}..." else url
            } ?: "No URL",
            color = Color.Gray,
            fontSize = 10.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
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

