package com.example.noorahlulbayt

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AdBlocker {
    private var adBlockingEnabled = false
    private val blockedDomains = mutableSetOf<String>()
    private val blockedPatterns = mutableListOf<Regex>()
    
    fun initialize(context: Context) {
        loadAdBlockingRules(context)
    }
    
    fun setAdBlockingEnabled(enabled: Boolean) {
        adBlockingEnabled = enabled
    }
    
    fun shouldBlock(url: String): WebResourceResponse? {
        if (!adBlockingEnabled || url.isEmpty()) {
            return null
        }
        
        try {
            val uri = java.net.URI(url)
            val domain = uri.host ?: return null
            
            // Check blocked domains
            if (blockedDomains.contains(domain)) {
                return createEmptyResponse()
            }
            
            // Check blocked patterns
            for (pattern in blockedPatterns) {
                if (pattern.containsMatchIn(url)) {
                    return createEmptyResponse()
                }
            }
            
            // Check for common ad indicators in URL
            val adKeywords = listOf(
                "ads", "ad", "advertisement", "banner", "tracker", "analytics",
                "doubleclick", "googleadservices", "googlesyndication",
                "facebook.com/tr", "facebook.com/audience", "facebook.com/events"
            )
            
            for (keyword in adKeywords) {
                if (url.contains(keyword, ignoreCase = true)) {
                    return createEmptyResponse()
                }
            }
            
        } catch (e: Exception) {
            // Invalid URL, allow it
        }
        
        return null
    }
    
    private fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse("text/plain", "UTF-8", null)
    }
    
    private fun loadAdBlockingRules(context: Context) {
        // Load EasyList rules
        loadFilterFile(context, "filters/easylist.txt") { line ->
            if (line.startsWith("||") && line.endsWith("^")) {
                val domain = line.substring(2, line.length - 1)
                blockedDomains.add(domain)
            } else if (line.startsWith("/") && line.endsWith("/")) {
                val pattern = line.substring(1, line.length - 1)
                try {
                    blockedPatterns.add(Regex(pattern, RegexOption.IGNORE_CASE))
                } catch (e: Exception) {
                    // Invalid regex, skip
                }
            }
        }
        
        // Load EasyPrivacy rules
        loadFilterFile(context, "filters/easyprivacy.txt") { line ->
            if (line.startsWith("||") && line.endsWith("^")) {
                val domain = line.substring(2, line.length - 1)
                blockedDomains.add(domain)
            }
        }
    }
    
    private fun loadFilterFile(context: Context, filename: String, processor: (String) -> Unit) {
        try {
            context.assets.open(filename).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.lineSequence().forEach { line ->
                        if (line.isNotEmpty() && !line.startsWith("!")) {
                            processor(line.trim())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // File not found or error reading, use basic rules
            loadBasicAdBlockingRules()
        }
    }
    
    private fun loadBasicAdBlockingRules() {
        // Basic ad blocking rules for common ad networks
        val basicAdDomains = listOf(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "facebook.com",
            "adnxs.com",
            "adsystem.com",
            "adtechus.com",
            "advertising.com",
            "amazon-adsystem.com",
            "criteo.com",
            "taboola.com",
            "outbrain.com"
        )
        
        blockedDomains.addAll(basicAdDomains)
        
        // Basic patterns
        val basicPatterns = listOf(
            ".*\\.ads\\..*",
            ".*ads\\..*",
            ".*adserver\\..*",
            ".*banner\\..*",
            ".*tracker\\..*",
            ".*analytics\\..*"
        )
        
        basicPatterns.forEach { pattern ->
            try {
                blockedPatterns.add(Regex(pattern, RegexOption.IGNORE_CASE))
            } catch (e: Exception) {
                // Invalid regex, skip
            }
        }
    }
    
    fun isAdBlockingEnabled(): Boolean = adBlockingEnabled
    
    fun getBlockedDomainsCount(): Int = blockedDomains.size
    
    fun getBlockedPatternsCount(): Int = blockedPatterns.size
} 