package com.example.noorahlulbayt

import android.webkit.WebResourceResponse
import com.example.noorahlulbayt.utils.AppLogger
import java.io.ByteArrayInputStream

object AdBlocker {
    
    private val blockedDomains = setOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "googletagmanager.com",
        "google-analytics.com",
        "facebook.com/tr",
        "connect.facebook.net",
        "ads.yahoo.com",
        "advertising.com",
        "adsystem.com",
        "adsense.com",
        "amazon-adsystem.com",
        "outbrain.com",
        "taboola.com",
        "criteo.com",
        "adsafeprotected.com"
    )
    
    private val blockedPatterns = listOf(
        "/ads/",
        "/advertisement/",
        "/advertising/",
        "/adsense/",
        "/adnxs/",
        "/doubleclick/",
        "/googleads/",
        "/googlesyndication/",
        "/amazon-adsystem/",
        "ads.js",
        "analytics.js",
        "gtag.js",
        "fbevents.js"
    )
    
    fun shouldBlock(url: String): WebResourceResponse? {
        if (url.isEmpty()) return null
        
        try {
            val lowerUrl = url.lowercase()
            
            // Check blocked domains
            for (domain in blockedDomains) {
                if (lowerUrl.contains(domain)) {
                    AppLogger.d("AdBlocker", "Blocked domain: $domain in $url")
                    return createBlockedResponse()
                }
            }
            
            // Check blocked patterns
            for (pattern in blockedPatterns) {
                if (lowerUrl.contains(pattern)) {
                    AppLogger.d("AdBlocker", "Blocked pattern: $pattern in $url")
                    return createBlockedResponse()
                }
            }
            
            // Not blocked
            return null
            
        } catch (e: Exception) {
            AppLogger.e("AdBlocker", "Error checking URL for blocking: $url", e)
            return null
        }
    }
    
    private fun createBlockedResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "utf-8",
            ByteArrayInputStream("".toByteArray())
        )
    }
    
    fun getBlockedDomainsCount(): Int = blockedDomains.size
    
    fun getBlockedPatternsCount(): Int = blockedPatterns.size
}