package com.example.noorahlulbayt

import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Base64

class NSFWJSAnalyzer(private val context: Context) {
    
    private var isInitialized = false
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if NSFWJS files exist
            val jsFile = context.assets.open("nsfwjs/nsfwjs.min.js")
            jsFile.close()
            isInitialized = true
            true
        } catch (e: Exception) {
            // NSFWJS files not found, use basic implementation
            isInitialized = false
            false
        }
    }
    
    suspend fun analyzeImage(webView: WebView): NSFWResult = withContext(Dispatchers.Main) {
        if (!isInitialized) {
            return@withContext NSFWResult(isSafe = true, confidence = 0.0f, reason = "NSFWJS not available")
        }
        
        try {
            // Capture screenshot
            val bitmap = captureWebViewScreenshot(webView)
            if (bitmap == null) {
                return@withContext NSFWResult(isSafe = true, confidence = 0.0f, reason = "Screenshot failed")
            }
            
            // Convert to base64
            val base64Image = bitmapToBase64(bitmap)
            
            // Analyze with NSFWJS (simplified implementation)
            // In a real implementation, this would use TensorFlow Lite or similar
            val result = analyzeWithNSFWJS(webView, base64Image)
            
            result
        } catch (e: Exception) {
            NSFWResult(isSafe = true, confidence = 0.0f, reason = "Analysis failed: ${e.message}")
        }
    }
    
    private suspend fun captureWebViewScreenshot(webView: WebView): Bitmap? = withContext(Dispatchers.Main) {
        try {
            val bitmap = Bitmap.createBitmap(
                webView.width,
                webView.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            webView.draw(canvas)
            
            // Resize to 224x224 for NSFWJS
            Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.getEncoder().encodeToString(byteArray)
    }
    
    private suspend fun analyzeWithNSFWJS(webView: WebView, base64Image: String): NSFWResult = withContext(Dispatchers.Main) {
        // This is a simplified implementation
        // In a real app, you would load the NSFWJS model and run inference
        
        val jsCode = """
            (function() {
                try {
                    // This would normally load and run NSFWJS
                    // For now, we'll do basic URL-based detection
                    var url = window.location.href.toLowerCase();
                    var suspiciousKeywords = ['porn', 'adult', 'xxx', 'nude', 'sex'];
                    
                    for (var i = 0; i < suspiciousKeywords.length; i++) {
                        if (url.indexOf(suspiciousKeywords[i]) !== -1) {
                            return JSON.stringify({
                                isSafe: false,
                                confidence: 0.8,
                                reason: 'Suspicious URL detected'
                            });
                        }
                    }
                    
                    // Check for suspicious images
                    var images = document.getElementsByTagName('img');
                    var suspiciousImageCount = 0;
                    
                    for (var i = 0; i < images.length; i++) {
                        var img = images[i];
                        var src = img.src.toLowerCase();
                        var alt = (img.alt || '').toLowerCase();
                        
                        for (var j = 0; j < suspiciousKeywords.length; j++) {
                            if (src.indexOf(suspiciousKeywords[j]) !== -1 || 
                                alt.indexOf(suspiciousKeywords[j]) !== -1) {
                                suspiciousImageCount++;
                                break;
                            }
                        }
                    }
                    
                    if (suspiciousImageCount > 0) {
                        return JSON.stringify({
                            isSafe: false,
                            confidence: Math.min(0.9, suspiciousImageCount * 0.3),
                            reason: 'Suspicious images detected'
                        });
                    }
                    
                    return JSON.stringify({
                        isSafe: true,
                        confidence: 0.1,
                        reason: 'No suspicious content detected'
                    });
                    
                } catch (e) {
                    return JSON.stringify({
                        isSafe: true,
                        confidence: 0.0,
                        reason: 'Analysis error: ' + e.message
                    });
                }
            })();
        """.trimIndent()
        
        var result = NSFWResult(isSafe = true, confidence = 0.0f, reason = "Default safe")
        
        webView.evaluateJavascript(jsCode) { jsonResult ->
            try {
                // Parse the JSON result
                // This is simplified - in a real app you'd use a proper JSON parser
                if (jsonResult.contains("\"isSafe\":false")) {
                    result = NSFWResult(isSafe = false, confidence = 0.8f, reason = "Content flagged by analysis")
                }
            } catch (e: Exception) {
                // Keep default safe result
            }
        }
        
        // Wait a bit for the JavaScript to execute
        kotlinx.coroutines.delay(100)
        result
    }
}

data class NSFWResult(
    val isSafe: Boolean,
    val confidence: Float,
    val reason: String
)