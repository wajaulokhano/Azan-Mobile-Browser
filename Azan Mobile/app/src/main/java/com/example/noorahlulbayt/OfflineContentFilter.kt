package com.example.noorahlulbayt

import android.webkit.WebView
import com.example.noorahlulbayt.utils.AppLogger

object OfflineContentFilter {
    
    private val suspiciousUrlPatterns = listOf(
        "porn", "adult", "xxx", "sex", "nude", "naked", "bikini", "lingerie", "underwear",
        "swimwear", "swimsuit", "bra", "panties", "intimate", "erotic", "sensual", "sexy",
        "hot", "babes", "girls", "models", "dating", "hookup", "cam", "webcam", "strip",
        "escort", "massage", "onlyfans", "chaturbate", "xhamster", "pornhub", "redtube"
    )
    
    private val blockedDomains = listOf(
        "pornhub.com", "xvideos.com", "xnxx.com", "redtube.com", "youporn.com",
        "tube8.com", "spankbang.com", "xhamster.com", "beeg.com", "tnaflix.com",
        "chaturbate.com", "onlyfans.com", "cam4.com", "livejasmin.com",
        "bikini.com", "swimwear.com", "lingerie.com", "victoriassecret.com"
    )
    
    private val suspiciousKeywords = listOf(
        "nude", "naked", "adult", "explicit", "porn", "sex", "xxx", "erotic",
        "bikini", "lingerie", "underwear", "swimwear", "swimsuit", "bra", "panties",
        "intimate", "sensual", "sexy", "hot", "babes", "girls", "models",
        "dating", "hookup", "cam", "webcam", "strip", "escort", "massage",
        "breast", "boobs", "ass", "butt", "thong", "cleavage", "revealing"
    )
    
    fun shouldBlockUrl(url: String): FilterResult {
        val urlLower = url.lowercase()
        
        // Check URL patterns
        for (pattern in suspiciousUrlPatterns) {
            if (urlLower.contains(pattern)) {
                AppLogger.logFilteringResult(url, true, "URL blocked for pattern: $pattern")
                return FilterResult(
                    blocked = true,
                    reason = "Inappropriate URL pattern detected: $pattern",
                    method = "url_pattern_filter",
                    confidence = 0.9f
                )
            }
        }
        
        // Check blocked domains
        for (domain in blockedDomains) {
            if (urlLower.contains(domain)) {
                AppLogger.logFilteringResult(url, true, "URL blocked for domain: $domain")
                return FilterResult(
                    blocked = true,
                    reason = "Blocked domain detected: $domain",
                    method = "domain_filter",
                    confidence = 0.95f
                )
            }
        }
        
        AppLogger.logFilteringResult(url, false, "URL passed initial filtering")
        return FilterResult(
            blocked = false,
            reason = "URL appears safe",
            method = "url_filter",
            confidence = 0.1f
        )
    }
    
    fun analyzePageContent(webView: WebView, url: String, onResult: (FilterResult) -> Unit) {
        AppLogger.d("OfflineFilter", "Starting content analysis for: $url")
        
        val jsCode = """
            (function() {
                try {
                    var text = (document.body ? document.body.innerText : '').toLowerCase();
                    var title = (document.title || '').toLowerCase();
                    var metaDescription = '';
                    
                    // Get meta description
                    var metaTags = document.getElementsByTagName('meta');
                    for (var i = 0; i < metaTags.length; i++) {
                        if (metaTags[i].name === 'description') {
                            metaDescription = (metaTags[i].content || '').toLowerCase();
                            break;
                        }
                    }
                    
                    // Combine all text content
                    var allText = text + ' ' + title + ' ' + metaDescription;
                    
                    // Keywords to check
                    var keywords = [
                        'nude', 'naked', 'adult', 'explicit', 'porn', 'sex', 'xxx', 'erotic',
                        'bikini', 'lingerie', 'underwear', 'swimwear', 'swimsuit', 'bra', 'panties',
                        'intimate', 'sensual', 'sexy', 'hot', 'babes', 'girls', 'models',
                        'dating', 'hookup', 'cam', 'webcam', 'strip', 'escort', 'massage',
                        'breast', 'boobs', 'ass', 'butt', 'thong', 'cleavage', 'revealing'
                    ];
                    
                    var foundKeywords = [];
                    var keywordCount = 0;
                    
                    // Check for keywords
                    for (var i = 0; i < keywords.length; i++) {
                        var keyword = keywords[i];
                        if (allText.indexOf(keyword) !== -1) {
                            foundKeywords.push(keyword);
                            keywordCount++;
                        }
                    }
                    
                    // Check images for suspicious attributes
                    var images = document.getElementsByTagName('img');
                    var suspiciousImages = 0;
                    var totalImages = images.length;
                    
                    for (var i = 0; i < images.length; i++) {
                        var img = images[i];
                        var src = (img.src || '').toLowerCase();
                        var alt = (img.alt || '').toLowerCase();
                        var className = (img.className || '').toLowerCase();
                        
                        for (var j = 0; j < keywords.length; j++) {
                            if (src.indexOf(keywords[j]) !== -1 || 
                                alt.indexOf(keywords[j]) !== -1 || 
                                className.indexOf(keywords[j]) !== -1) {
                                suspiciousImages++;
                                break;
                            }
                        }
                    }
                    
                    // Decision logic
                    var shouldBlock = false;
                    var reason = '';
                    var confidence = 0;
                    
                    if (keywordCount >= 3 || suspiciousImages >= 2) {
                        shouldBlock = true;
                        reason = 'Multiple suspicious indicators found - Keywords: ' + foundKeywords.join(', ') + 
                                ' (count: ' + keywordCount + '), Suspicious images: ' + suspiciousImages;
                        confidence = Math.min(0.95, (keywordCount + suspiciousImages) * 0.15);
                    } else if (keywordCount >= 1 || suspiciousImages >= 1) {
                        shouldBlock = true;
                        reason = 'Suspicious content detected - Keywords: ' + foundKeywords.join(', ') + 
                                ', Suspicious images: ' + suspiciousImages;
                        confidence = 0.7;
                    }
                    
                    return JSON.stringify({
                        blocked: shouldBlock,
                        reason: shouldBlock ? reason : 'Content appears safe',
                        method: 'enhanced_offline_analysis',
                        confidence: confidence,
                        keywordCount: keywordCount,
                        suspiciousImages: suspiciousImages,
                        totalImages: totalImages,
                        foundKeywords: foundKeywords
                    });
                    
                } catch (e) {
                    return JSON.stringify({
                        blocked: false,
                        reason: 'Analysis error: ' + e.message,
                        method: 'enhanced_offline_analysis',
                        confidence: 0.0,
                        error: true
                    });
                }
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(jsCode) { result ->
            try {
                AppLogger.d("OfflineFilter", "Content analysis result: $result")
                
                val blocked = result.contains("\"blocked\":true")
                val filterResult = FilterResult(
                    blocked = blocked,
                    reason = if (blocked) "Content blocked by offline analysis" else "Content appears safe",
                    method = "enhanced_offline_analysis",
                    confidence = if (blocked) 0.8f else 0.2f,
                    details = result
                )
                
                if (blocked) {
                    AppLogger.logFilteringResult(url, true, "Content blocked by offline analysis: $result")
                } else {
                    AppLogger.logFilteringResult(url, false, "Content passed offline analysis: $result")
                }
                
                onResult(filterResult)
                
            } catch (e: Exception) {
                AppLogger.e("OfflineFilter", "Error processing analysis result", e)
                onResult(FilterResult(
                    blocked = false,
                    reason = "Analysis failed: ${e.message}",
                    method = "enhanced_offline_analysis",
                    confidence = 0.0f
                ))
            }
        }
    }
    
    fun createBlockedPageHtml(reason: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Content Blocked</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        background-color: #000000;
                        color: #ffffff;
                        text-align: center;
                        padding: 50px 20px;
                        margin: 0;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                    }
                    .icon {
                        font-size: 80px;
                        margin-bottom: 20px;
                    }
                    .title {
                        color: #006400;
                        font-size: 28px;
                        font-weight: bold;
                        margin-bottom: 20px;
                    }
                    .message {
                        font-size: 18px;
                        line-height: 1.6;
                        margin-bottom: 30px;
                    }
                    .reason {
                        background-color: #1a1a1a;
                        padding: 15px;
                        border-radius: 8px;
                        margin-bottom: 30px;
                        font-size: 14px;
                        color: #cccccc;
                    }
                    .info {
                        font-size: 14px;
                        color: #888888;
                        margin-top: 30px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="icon">🛡️</div>
                    <div class="title">Content Blocked</div>
                    <div class="message">
                        This content has been blocked to maintain Islamic values and family-safe browsing.
                    </div>
                    <div class="reason">
                        <strong>Reason:</strong> $reason
                    </div>
                    <div class="info">
                        Azan Mobile Browser - Islamic Family-Safe Browsing<br>
                        Content filtering is active to protect you and your family.
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}

data class FilterResult(
    val blocked: Boolean,
    val reason: String,
    val method: String,
    val confidence: Float,
    val details: String = ""
)