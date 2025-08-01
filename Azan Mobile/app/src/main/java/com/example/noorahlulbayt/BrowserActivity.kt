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
        
        browserViewModel = BrowserViewModel(application)
        
        // Register Azan block receiver
        azanBlockReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.example.noorahlulbayt.BLOCK_BROWSER") {
                    isAzanBlocked = true
                    browserViewModel.blockAllTabs()
                    showAzanBlockMessage()
                }
            }
        }
        registerReceiver(azanBlockReceiver, IntentFilter("com.example.noorahlulbayt.BLOCK_BROWSER"))

        setContent {
            BrowserScreen(
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

@OptIn(ExperimentalPagerApi::class)
@Composable
fun BrowserScreen(
    browserViewModel: BrowserViewModel = viewModel(),
    isAzanBlocked: Boolean = false
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Top Bar
            TopBar(
                onNewTab = { browserViewModel.addTab() },
                onSettings = { /* TODO: Navigate to settings */ },
                onSetDefault = {
                    val intent = Intent(android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS)
                    context.startActivity(intent)
                },
                onFavorites = { /* TODO: Show favorites */ },
                onDownloads = { /* TODO: Show downloads */ },
                onHistory = { /* TODO: Show history */ }
            )

            // Tab Bar
            val tabs by browserViewModel.tabs.collectAsState()
            TabBar(
                tabs = tabs,
                currentTabIndex = pagerState.currentPage,
                onTabClick = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                onTabClose = { index ->
                    browserViewModel.closeTab(index)
                }
            )

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
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(tabs.size) { index ->
            val tab = tabs[index]
            TabItem(
                tab = tab,
                isSelected = index == currentTabIndex,
                onClick = { onTabClick(index) },
                onClose = { onTabClose(index) }
            )
        }
    }
}

@Composable
fun TabItem(
    tab: BrowserTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFF006400) else Color(0xFF333333)
    
    Row(
        modifier = Modifier
            .background(backgroundColor)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tab.title.ifEmpty { "New Tab" },
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onClose) {
            Text("×", color = Color.White, fontSize = 16.sp)
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
                                isLoading = true
                                showFilteringAnimation = true
                                
                                // Start filtering
                                view?.post {
                                    performFiltering(view, url ?: "")
                                }
                            }
                            
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                showFilteringAnimation = false
                                onUrlChanged(url ?: "")
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
    // Keyword filtering
    webView.evaluateJavascript("""
        (function() {
            var text = document.body ? document.body.innerText : '';
            var keywords = ['nude', 'adult', 'explicit', 'porn', 'sex', 'xxx'];
            var regex = /\\b(nudity|pornography|erotic)\\b/i;
            
            for (var i = 0; i < keywords.length; i++) {
                if (text.toLowerCase().indexOf(keywords[i]) !== -1) {
                    return true;
                }
            }
            
            if (regex.test(text)) {
                return true;
            }
            
            return false;
        })();
    """.trimIndent()) { result ->
        if (result == "true") {
            webView.loadUrl("about:blank")
            // TODO: Add to blocklist
        }
    }
    
    // NSFWJS filtering (simplified for demo)
    // In real implementation, capture screenshot and analyze with NSFWJS
    webView.postDelayed({
        // Simulate NSFWJS analysis
        if (url.contains("porn") || url.contains("adult")) {
            webView.loadUrl("about:blank")
        }
    }, 500)
} 