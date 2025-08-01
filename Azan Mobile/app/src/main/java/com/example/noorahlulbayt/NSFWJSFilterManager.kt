package com.example.noorahlulbayt

import android.content.Context
import android.webkit.WebView
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.example.noorahlulbayt.utils.AppLogger
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap

class NSFWJSFilterManager(private val context: Context) {
    
    private var isModelLoaded = false
    private var hiddenWebView: WebView? = null
    private val imageClassificationCache = ConcurrentHashMap<String, ClassificationResult>()
    private val trustedDomains = mutableSetOf<String>()
    
    // User preference settings
    var filteringMode = FilteringMode.MODERATE
    var allowBlobImages = false
    var allowBase64Images = false
    
    enum class FilteringMode {
        STRICT,    // Block all Blob/Base64, aggressive filtering
        MODERATE,  // Allow on whitelisted sites, standard filtering  
        CUSTOM     // User-defined rules
    }
    
    data class ClassificationResult(
        val isBlocked: Boolean,
        val className: String,
        val confidence: Float,
        val reason: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    suspend fun initialize() = withContext(Dispatchers.Main) {
        AppLogger.i("NSFWJSFilter", "Initializing NSFWJS Filter Manager")
        
        try {
            // Create hidden WebView for model loading
            hiddenWebView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
            }
            
            loadNSFWJSModel()
            loadTrustedDomains()
            
            AppLogger.i("NSFWJSFilter", "NSFWJS Filter Manager initialized successfully")
        } catch (e: Exception) {
            AppLogger.e("NSFWJSFilter", "Failed to initialize NSFWJS Filter Manager", e)
        }
    }
    
    private suspend fun loadNSFWJSModel() = withContext(Dispatchers.Main) {
        AppLogger.d("NSFWJSFilter", "Loading NSFWJS model in hidden WebView")
        
        try {
            // Read bundled NSFWJS script
            val nsfwjsScript = context.assets.open("nsfwjs/nsfwjs.min.js").bufferedReader().use { it.readText() }
            
            // Load model in hidden WebView
            hiddenWebView?.evaluateJavascript("""
                (function() {
                    try {
                        // Inject NSFWJS
                        $nsfwjsScript
                        
                        // Load model
                        nsfwjs.load().then(model => {
                            window.nsfwModel = model;
                            window.modelReady = true;
                            console.log('NSFWJS model loaded successfully');
                            return JSON.stringify({success: true, message: 'Model loaded'});
                        }).catch(err => {
                            console.error('NSFWJS model loading failed:', err);
                            window.modelReady = false;
                            return JSON.stringify({success: false, message: err.toString()});
                        });
                        
                        return JSON.stringify({success: true, message: 'Loading initiated'});
                    } catch (e) {
                        console.error('NSFWJS injection failed:', e);
                        return JSON.stringify({success: false, message: e.toString()});
                    }
                })();
            """.trimIndent()) { result ->
                AppLogger.d("NSFWJSFilter", "Model loading result: $result")
                if (result.contains("\"success\":true")) {
                    // Wait a bit for model to fully load
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(3000) // Give model time to load
                        isModelLoaded = true
                        AppLogger.i("NSFWJSFilter", "NSFWJS model ready for use")
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("NSFWJSFilter", "Failed to load NSFWJS model", e)
        }
    }
    
    private fun loadTrustedDomains() {
        // Load trusted domains for moderate mode
        trustedDomains.addAll(listOf(
            "wikipedia.org", "bbc.com", "cnn.com", "reuters.com",
            "islamqa.info", "islamweb.net", "islamhouse.com",
            "quran.com", "sunnah.com", "islamicfinder.org"
        ))
        AppLogger.d("NSFWJSFilter", "Loaded ${trustedDomains.size} trusted domains")
    }
    
    fun shouldInterceptRequest(request: WebResourceRequest?): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null
        
        try {
            // 1. Block Blob/Base64 by default (as suggested)
            if (shouldBlockBlobOrBase64(url)) {
                AppLogger.logFilteringResult(url, true, "Blocked blob/base64 image - Mode: $filteringMode")
                return createBlockedImageResponse("Blob/Base64 images blocked for safety")
            }
            
            // 2. Check if it's an image URL that needs NSFWJS processing
            if (isImageUrl(url)) {
                return handleImageRequest(url)
            }
            
            // 3. Check for video content with keyword filtering
            if (isVideoUrl(url)) {
                return handleVideoRequest(url)
            }
            
        } catch (e: Exception) {
            AppLogger.e("NSFWJSFilter", "Error in request interception for $url", e)
        }
        
        return null // Allow request to proceed normally
    }
    
    private fun shouldBlockBlobOrBase64(url: String): Boolean {
        val isBlobOrBase64 = url.startsWith("blob:") || url.startsWith("data:")
        
        return when (filteringMode) {
            FilteringMode.STRICT -> isBlobOrBase64 // Block all blob/base64
            FilteringMode.MODERATE -> {
                if (!isBlobOrBase64) return false
                // Allow blob/base64 on trusted domains
                val domain = extractDomain(url)
                !trustedDomains.contains(domain)
            }
            FilteringMode.CUSTOM -> {
                // User-defined rules
                if (allowBlobImages && url.startsWith("blob:")) return false
                if (allowBase64Images && url.startsWith("data:")) return false
                isBlobOrBase64
            }
        }
    }
    
    private fun isImageUrl(url: String): Boolean {
        val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp")
        val urlLower = url.lowercase()
        return imageExtensions.any { urlLower.contains(it) } || 
               url.contains("image/") ||
               urlLower.contains("img") ||
               urlLower.contains("photo") ||
               urlLower.contains("picture")
    }
    
    private fun isVideoUrl(url: String): Boolean {
        val videoExtensions = listOf(".mp4", ".avi", ".mov", ".wmv", ".flv", ".webm")
        val urlLower = url.lowercase()
        return videoExtensions.any { urlLower.contains(it) } || 
               url.contains("video/")
    }
    
    private fun handleImageRequest(url: String): WebResourceResponse? {
        AppLogger.d("NSFWJSFilter", "Processing image request: $url")
        
        // Check cache first
        val cachedResult = imageClassificationCache[url]
        if (cachedResult != null && !isCacheExpired(cachedResult)) {
            if (cachedResult.isBlocked) {
                AppLogger.logFilteringResult(url, true, "Cached result: ${cachedResult.reason}")
                return createBlockedImageResponse(cachedResult.reason)
            }
            return null // Allow cached safe image
        }
        
        // Quick keyword check first
        val keywordResult = checkImageUrlKeywords(url)
        if (keywordResult.isBlocked) {
            imageClassificationCache[url] = keywordResult
            AppLogger.logFilteringResult(url, true, "Keyword filtering: ${keywordResult.reason}")
            return createBlockedImageResponse(keywordResult.reason)
        }
        
        // If NSFWJS is ready, classify the image
        if (isModelLoaded) {
            // Note: In real implementation, we'd need to download and classify the image
            // For now, we'll use a placeholder that schedules classification
            scheduleImageClassification(url)
        }
        
        return null // Allow image to load (will be filtered post-load if needed)
    }
    
    private fun handleVideoRequest(url: String): WebResourceResponse? {
        AppLogger.d("NSFWJSFilter", "Processing video request: $url")
        
        // Use keyword filtering for videos (NSFWJS doesn't handle video well)
        val keywordResult = checkVideoUrlKeywords(url)
        if (keywordResult.isBlocked) {
            AppLogger.logFilteringResult(url, true, "Video blocked: ${keywordResult.reason}")
            return createBlockedVideoResponse(keywordResult.reason)
        }
        
        return null // Allow video
    }
    
    private fun checkImageUrlKeywords(url: String): ClassificationResult {
        val suspiciousPatterns = listOf(
            "nude", "naked", "porn", "sex", "xxx", "adult", "explicit",
            "bikini", "lingerie", "underwear", "swimwear", "swimsuit",
            "sexy", "hot", "babes", "girls", "models", "erotic",
            "topless", "revealing", "provocative", "intimate"
        )
        
        val urlLower = url.lowercase()
        val foundPatterns = suspiciousPatterns.filter { urlLower.contains(it) }
        
        return if (foundPatterns.isNotEmpty()) {
            ClassificationResult(
                isBlocked = true,
                className = "suspicious_url",
                confidence = 0.8f,
                reason = "Suspicious URL patterns: ${foundPatterns.joinToString(", ")}"
            )
        } else {
            ClassificationResult(
                isBlocked = false,
                className = "safe_url",
                confidence = 0.2f,
                reason = "URL appears safe"
            )
        }
    }
    
    private fun checkVideoUrlKeywords(url: String): ClassificationResult {
        val videoSuspiciousPatterns = listOf(
            "nude", "naked", "porn", "sex", "xxx", "adult", "explicit",
            "wet", "bikini", "lingerie", "strip", "dance", "twerk",
            "sexy", "hot", "babes", "cam", "webcam", "live"
        )
        
        val urlLower = url.lowercase()
        val foundPatterns = videoSuspiciousPatterns.filter { urlLower.contains(it) }
        
        return if (foundPatterns.isNotEmpty()) {
            ClassificationResult(
                isBlocked = true,
                className = "suspicious_video",
                confidence = 0.9f,
                reason = "Suspicious video patterns: ${foundPatterns.joinToString(", ")}"
            )
        } else {
            ClassificationResult(
                isBlocked = false,
                className = "safe_video",
                confidence = 0.1f,
                reason = "Video appears safe"
            )
        }
    }
    
    private fun scheduleImageClassification(url: String) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val result = classifyImageWithNSFWJS(url)
                imageClassificationCache[url] = result
                
                if (result.isBlocked) {
                    // Image was classified as inappropriate after loading
                    // We'll handle this in post-load filtering
                    AppLogger.logFilteringResult(url, true, "NSFWJS classification: ${result.reason}")
                }
            } catch (e: Exception) {
                AppLogger.e("NSFWJSFilter", "Failed to classify image: $url", e)
            }
        }
    }
    
    private suspend fun classifyImageWithNSFWJS(url: String): ClassificationResult = withContext(Dispatchers.Main) {
        if (!isModelLoaded || hiddenWebView == null) {
            return@withContext ClassificationResult(
                isBlocked = false,
                className = "model_not_ready",
                confidence = 0.0f,
                reason = "NSFWJS model not ready"
            )
        }
        
        // Create a promise-based classification
        val deferred = CompletableDeferred<ClassificationResult>()
        
        hiddenWebView?.evaluateJavascript("""
            (function() {
                if (!window.modelReady || !window.nsfwModel) {
                    return JSON.stringify({
                        blocked: false,
                        className: 'model_not_ready',
                        confidence: 0.0,
                        reason: 'Model not ready'
                    });
                }
                
                // Create image element for classification
                const img = new Image();
                img.crossOrigin = 'anonymous';
                
                img.onload = function() {
                    window.nsfwModel.classify(img).then(predictions => {
                        const topPrediction = predictions[0];
                        const isBlocked = ['Porn', 'Sexy'].includes(topPrediction.className) && 
                                         topPrediction.probability > 0.6;
                        
                        const result = {
                            blocked: isBlocked,
                            className: topPrediction.className,
                            confidence: topPrediction.probability,
                            reason: isBlocked ? 
                                'NSFWJS detected ' + topPrediction.className + ' (' + (topPrediction.probability * 100).toFixed(1) + '%)' :
                                'Content appears safe (' + topPrediction.className + ')'
                        };
                        
                        // Store result for retrieval
                        window.lastClassificationResult = JSON.stringify(result);
                    }).catch(err => {
                        window.lastClassificationResult = JSON.stringify({
                            blocked: false,
                            className: 'classification_error',
                            confidence: 0.0,
                            reason: 'Classification failed: ' + err.toString()
                        });
                    });
                };
                
                img.onerror = function() {
                    window.lastClassificationResult = JSON.stringify({
                        blocked: false,
                        className: 'image_load_error',
                        confidence: 0.0,
                        reason: 'Failed to load image for classification'
                    });
                };
                
                img.src = '$url';
                return 'classification_started';
            })();
        """.trimIndent()) { result ->
            // Poll for result
            CoroutineScope(Dispatchers.Main).launch {
                var attempts = 0
                while (attempts < 10) { // Max 5 seconds wait
                    delay(500)
                    attempts++
                    
                    hiddenWebView?.evaluateJavascript("window.lastClassificationResult || 'pending'") { classificationResult ->
                        if (classificationResult != "pending" && classificationResult != "null") {
                            try {
                                // Parse the result
                                val blocked = classificationResult.contains("\"blocked\":true")
                                val className = extractJsonValue(classificationResult, "className")
                                val confidence = extractJsonValue(classificationResult, "confidence").toFloatOrNull() ?: 0.0f
                                val reason = extractJsonValue(classificationResult, "reason")
                                
                                deferred.complete(ClassificationResult(
                                    isBlocked = blocked,
                                    className = className,
                                    confidence = confidence,
                                    reason = reason
                                ))
                            } catch (e: Exception) {
                                deferred.complete(ClassificationResult(
                                    isBlocked = false,
                                    className = "parse_error",
                                    confidence = 0.0f,
                                    reason = "Failed to parse classification result"
                                ))
                            }
                        }
                    }
                }
                
                // Timeout fallback
                if (!deferred.isCompleted) {
                    deferred.complete(ClassificationResult(
                        isBlocked = false,
                        className = "timeout",
                        confidence = 0.0f,
                        reason = "Classification timeout"
                    ))
                }
            }
        }
        
        return@withContext deferred.await()
    }
    
    fun performPostLoadFiltering(webView: WebView, url: String) {
        if (!isModelLoaded) {
            AppLogger.w("NSFWJSFilter", "NSFWJS model not ready for post-load filtering")
            return
        }
        
        AppLogger.d("NSFWJSFilter", "Performing post-load image filtering for: $url")
        
        webView.evaluateJavascript("""
            (function() {
                if (!window.nsfwModel || !window.modelReady) {
                    return 'Model not ready';
                }
                
                const images = document.querySelectorAll('img:not([data-nsfwjs-processed])');
                let processedCount = 0;
                
                images.forEach(async (img, index) => {
                    try {
                        // Mark as processing
                        img.setAttribute('data-nsfwjs-processed', 'true');
                        
                        // Skip if image is too small (likely icons)
                        if (img.naturalWidth < 100 || img.naturalHeight < 100) {
                            return;
                        }
                        
                        // Classify image
                        const predictions = await window.nsfwModel.classify(img);
                        const topPrediction = predictions[0];
                        
                        // Block if inappropriate
                        if (['Porn', 'Sexy'].includes(topPrediction.className) && 
                            topPrediction.probability > 0.6) {
                            
                            // Create blocked content placeholder
                            const blockedDiv = document.createElement('div');
                            blockedDiv.style.cssText = `
                                background: #000000;
                                color: #006400;
                                padding: 20px;
                                text-align: center;
                                border: 2px solid #006400;
                                border-radius: 8px;
                                font-family: Arial, sans-serif;
                                width: ${'$'}{img.offsetWidth || 200}px;
                                height: ${'$'}{img.offsetHeight || 150}px;
                                display: flex;
                                flex-direction: column;
                                justify-content: center;
                                align-items: center;
                            `;
                            
                            blockedDiv.innerHTML = `
                                <div style="font-size: 32px; margin-bottom: 10px;">🛡️</div>
                                <div style="font-weight: bold; margin-bottom: 5px;">Image Blocked</div>
                                <div style="font-size: 12px; opacity: 0.8;">
                                    ${'$'}{topPrediction.className} detected (${'$'}{(topPrediction.probability * 100).toFixed(1)}%)
                                </div>
                                <div style="font-size: 10px; margin-top: 10px; opacity: 0.6;">
                                    Islamic family-safe filtering
                                </div>
                            `;
                            
                            // Replace image with blocked content
                            img.parentNode.replaceChild(blockedDiv, img);
                            
                            console.log('NSFWJS blocked image:', topPrediction.className, topPrediction.probability);
                            processedCount++;
                        }
                        
                    } catch (error) {
                        console.error('NSFWJS classification error:', error);
                    }
                });
                
                return 'Processed ' + images.length + ' images, blocked ' + processedCount;
            })();
        """.trimIndent()) { result ->
            AppLogger.d("NSFWJSFilter", "Post-load filtering result: $result")
        }
    }
    
    private fun createBlockedImageResponse(reason: String): WebResourceResponse {
        val blockedImageSvg = """
            <svg width="200" height="150" xmlns="http://www.w3.org/2000/svg">
                <rect width="200" height="150" fill="#000000" stroke="#006400" stroke-width="2"/>
                <text x="100" y="60" text-anchor="middle" fill="#006400" font-family="Arial" font-size="24">🛡️</text>
                <text x="100" y="85" text-anchor="middle" fill="#006400" font-family="Arial" font-size="12" font-weight="bold">Image Blocked</text>
                <text x="100" y="105" text-anchor="middle" fill="#006400" font-family="Arial" font-size="10">$reason</text>
                <text x="100" y="125" text-anchor="middle" fill="#006400" font-family="Arial" font-size="8" opacity="0.7">Islamic family-safe filtering</text>
            </svg>
        """.trimIndent()
        
        return WebResourceResponse(
            "image/svg+xml",
            "UTF-8",
            ByteArrayInputStream(blockedImageSvg.toByteArray())
        )
    }
    
    private fun createBlockedVideoResponse(reason: String): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream("".toByteArray())
        )
    }
    
    private fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
            uri.host ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun extractJsonValue(json: String, key: String): String {
        return try {
            val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"|\"$key\"\\s*:\\s*([^,}]*)".toRegex()
            val match = pattern.find(json)
            match?.groupValues?.get(1) ?: match?.groupValues?.get(2) ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun isCacheExpired(result: ClassificationResult): Boolean {
        val cacheExpiryTime = 24 * 60 * 60 * 1000 // 24 hours
        return System.currentTimeMillis() - result.timestamp > cacheExpiryTime
    }
    
    fun addTrustedDomain(domain: String) {
        trustedDomains.add(domain)
        AppLogger.i("NSFWJSFilter", "Added trusted domain: $domain")
    }
    
    fun removeTrustedDomain(domain: String) {
        trustedDomains.remove(domain)
        AppLogger.i("NSFWJSFilter", "Removed trusted domain: $domain")
    }
    
    fun clearCache() {
        imageClassificationCache.clear()
        AppLogger.i("NSFWJSFilter", "Classification cache cleared")
    }
    
    fun getCacheStats(): String {
        val totalEntries = imageClassificationCache.size
        val blockedEntries = imageClassificationCache.values.count { it.isBlocked }
        return "Cache: $totalEntries entries, $blockedEntries blocked"
    }
    
    fun cleanup() {
        hiddenWebView?.destroy()
        hiddenWebView = null
        imageClassificationCache.clear()
        AppLogger.i("NSFWJSFilter", "NSFWJS Filter Manager cleaned up")
    }
}