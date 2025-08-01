package com.example.noorahlulbayt

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.noorahlulbayt.utils.AppLogger

class FilteringSettingsManager(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "filtering_settings",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            AppLogger.e("FilteringSettings", "Failed to create encrypted preferences, using regular", e)
            context.getSharedPreferences("filtering_settings", Context.MODE_PRIVATE)
        }
    }
    
    // Filtering mode settings
    var filteringMode: NSFWJSFilterManager.FilteringMode
        get() = NSFWJSFilterManager.FilteringMode.valueOf(
            sharedPreferences.getString("filtering_mode", NSFWJSFilterManager.FilteringMode.MODERATE.name) 
                ?: NSFWJSFilterManager.FilteringMode.MODERATE.name
        )
        set(value) {
            sharedPreferences.edit().putString("filtering_mode", value.name).apply()
            AppLogger.i("FilteringSettings", "Filtering mode changed to: $value")
        }
    
    // Blob/Base64 image settings
    var allowBlobImages: Boolean
        get() = sharedPreferences.getBoolean("allow_blob_images", false)
        set(value) {
            sharedPreferences.edit().putBoolean("allow_blob_images", value).apply()
            AppLogger.i("FilteringSettings", "Allow blob images: $value")
        }
    
    var allowBase64Images: Boolean
        get() = sharedPreferences.getBoolean("allow_base64_images", false)
        set(value) {
            sharedPreferences.edit().putBoolean("allow_base64_images", value).apply()
            AppLogger.i("FilteringSettings", "Allow base64 images: $value")
        }
    
    // NSFWJS settings
    var nsfwjsEnabled: Boolean
        get() = sharedPreferences.getBoolean("nsfwjs_enabled", true)
        set(value) {
            sharedPreferences.edit().putBoolean("nsfwjs_enabled", value).apply()
            AppLogger.i("FilteringSettings", "NSFWJS enabled: $value")
        }
    
    var nsfwjsConfidenceThreshold: Float
        get() = sharedPreferences.getFloat("nsfwjs_confidence_threshold", 0.6f)
        set(value) {
            val clampedValue = value.coerceIn(0.1f, 0.9f)
            sharedPreferences.edit().putFloat("nsfwjs_confidence_threshold", clampedValue).apply()
            AppLogger.i("FilteringSettings", "NSFWJS confidence threshold: $clampedValue")
        }
    
    // Keyword filtering settings
    var keywordFilteringEnabled: Boolean
        get() = sharedPreferences.getBoolean("keyword_filtering_enabled", true)
        set(value) {
            sharedPreferences.edit().putBoolean("keyword_filtering_enabled", value).apply()
            AppLogger.i("FilteringSettings", "Keyword filtering enabled: $value")
        }
    
    var strictKeywordMode: Boolean
        get() = sharedPreferences.getBoolean("strict_keyword_mode", false)
        set(value) {
            sharedPreferences.edit().putBoolean("strict_keyword_mode", value).apply()
            AppLogger.i("FilteringSettings", "Strict keyword mode: $value")
        }
    
    // Trusted domains management
    fun getTrustedDomains(): Set<String> {
        val domainsString = sharedPreferences.getString("trusted_domains", "") ?: ""
        return if (domainsString.isEmpty()) {
            getDefaultTrustedDomains()
        } else {
            domainsString.split(",").toSet()
        }
    }
    
    fun addTrustedDomain(domain: String) {
        val currentDomains = getTrustedDomains().toMutableSet()
        currentDomains.add(domain.lowercase().trim())
        saveTrustedDomains(currentDomains)
        AppLogger.i("FilteringSettings", "Added trusted domain: $domain")
    }
    
    fun removeTrustedDomain(domain: String) {
        val currentDomains = getTrustedDomains().toMutableSet()
        currentDomains.remove(domain.lowercase().trim())
        saveTrustedDomains(currentDomains)
        AppLogger.i("FilteringSettings", "Removed trusted domain: $domain")
    }
    
    private fun saveTrustedDomains(domains: Set<String>) {
        val domainsString = domains.joinToString(",")
        sharedPreferences.edit().putString("trusted_domains", domainsString).apply()
    }
    
    private fun getDefaultTrustedDomains(): Set<String> {
        return setOf(
            // Islamic websites
            "islamqa.info", "islamweb.net", "islamhouse.com",
            "quran.com", "sunnah.com", "islamicfinder.org",
            "islamicity.org", "islamreligion.com", "aboutislam.net",
            
            // Educational websites
            "wikipedia.org", "britannica.com", "khanacademy.org",
            "coursera.org", "edx.org", "mit.edu", "harvard.edu",
            
            // News websites
            "bbc.com", "cnn.com", "reuters.com", "ap.org",
            "aljazeera.com", "npr.org", "pbs.org",
            
            // Technology websites
            "github.com", "stackoverflow.com", "mozilla.org",
            "w3schools.com", "developer.mozilla.org",
            
            // Government websites
            "gov.uk", "usa.gov", "canada.ca", "gov.au"
        )
    }
    
    // Custom keyword management
    fun getCustomBlockedKeywords(): Set<String> {
        val keywordsString = sharedPreferences.getString("custom_blocked_keywords", "") ?: ""
        return if (keywordsString.isEmpty()) {
            emptySet()
        } else {
            keywordsString.split(",").map { it.trim().lowercase() }.toSet()
        }
    }
    
    fun addCustomBlockedKeyword(keyword: String) {
        val currentKeywords = getCustomBlockedKeywords().toMutableSet()
        currentKeywords.add(keyword.lowercase().trim())
        saveCustomBlockedKeywords(currentKeywords)
        AppLogger.i("FilteringSettings", "Added custom blocked keyword: $keyword")
    }
    
    fun removeCustomBlockedKeyword(keyword: String) {
        val currentKeywords = getCustomBlockedKeywords().toMutableSet()
        currentKeywords.remove(keyword.lowercase().trim())
        saveCustomBlockedKeywords(currentKeywords)
        AppLogger.i("FilteringSettings", "Removed custom blocked keyword: $keyword")
    }
    
    private fun saveCustomBlockedKeywords(keywords: Set<String>) {
        val keywordsString = keywords.joinToString(",")
        sharedPreferences.edit().putString("custom_blocked_keywords", keywordsString).apply()
    }
    
    // Override settings for trusted sites
    var allowOverrideOnTrustedSites: Boolean
        get() = sharedPreferences.getBoolean("allow_override_trusted_sites", true)
        set(value) {
            sharedPreferences.edit().putBoolean("allow_override_trusted_sites", value).apply()
            AppLogger.i("FilteringSettings", "Allow override on trusted sites: $value")
        }
    
    var showOverrideButton: Boolean
        get() = sharedPreferences.getBoolean("show_override_button", true)
        set(value) {
            sharedPreferences.edit().putBoolean("show_override_button", value).apply()
            AppLogger.i("FilteringSettings", "Show override button: $value")
        }
    
    // Performance settings
    var enableImageCaching: Boolean
        get() = sharedPreferences.getBoolean("enable_image_caching", true)
        set(value) {
            sharedPreferences.edit().putBoolean("enable_image_caching", value).apply()
            AppLogger.i("FilteringSettings", "Enable image caching: $value")
        }
    
    var maxCacheSize: Int
        get() = sharedPreferences.getInt("max_cache_size", 1000)
        set(value) {
            val clampedValue = value.coerceIn(100, 5000)
            sharedPreferences.edit().putInt("max_cache_size", clampedValue).apply()
            AppLogger.i("FilteringSettings", "Max cache size: $clampedValue")
        }
    
    // Statistics and monitoring
    fun incrementBlockedImageCount() {
        val current = sharedPreferences.getInt("blocked_image_count", 0)
        sharedPreferences.edit().putInt("blocked_image_count", current + 1).apply()
    }
    
    fun incrementBlockedVideoCount() {
        val current = sharedPreferences.getInt("blocked_video_count", 0)
        sharedPreferences.edit().putInt("blocked_video_count", current + 1).apply()
    }
    
    fun getBlockedImageCount(): Int {
        return sharedPreferences.getInt("blocked_image_count", 0)
    }
    
    fun getBlockedVideoCount(): Int {
        return sharedPreferences.getInt("blocked_video_count", 0)
    }
    
    fun resetStatistics() {
        sharedPreferences.edit()
            .putInt("blocked_image_count", 0)
            .putInt("blocked_video_count", 0)
            .apply()
        AppLogger.i("FilteringSettings", "Statistics reset")
    }
    
    // Export/Import settings
    fun exportSettings(): String {
        val settings = mutableMapOf<String, Any>()
        
        settings["filtering_mode"] = filteringMode.name
        settings["allow_blob_images"] = allowBlobImages
        settings["allow_base64_images"] = allowBase64Images
        settings["nsfwjs_enabled"] = nsfwjsEnabled
        settings["nsfwjs_confidence_threshold"] = nsfwjsConfidenceThreshold
        settings["keyword_filtering_enabled"] = keywordFilteringEnabled
        settings["strict_keyword_mode"] = strictKeywordMode
        settings["trusted_domains"] = getTrustedDomains().joinToString(",")
        settings["custom_blocked_keywords"] = getCustomBlockedKeywords().joinToString(",")
        settings["allow_override_trusted_sites"] = allowOverrideOnTrustedSites
        settings["show_override_button"] = showOverrideButton
        settings["enable_image_caching"] = enableImageCaching
        settings["max_cache_size"] = maxCacheSize
        
        return settings.map { "${it.key}=${it.value}" }.joinToString("\n")
    }
    
    fun importSettings(settingsString: String): Boolean {
        return try {
            val editor = sharedPreferences.edit()
            
            settingsString.lines().forEach { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                    
                    when (key) {
                        "filtering_mode" -> editor.putString("filtering_mode", value)
                        "allow_blob_images" -> editor.putBoolean("allow_blob_images", value.toBoolean())
                        "allow_base64_images" -> editor.putBoolean("allow_base64_images", value.toBoolean())
                        "nsfwjs_enabled" -> editor.putBoolean("nsfwjs_enabled", value.toBoolean())
                        "nsfwjs_confidence_threshold" -> editor.putFloat("nsfwjs_confidence_threshold", value.toFloat())
                        "keyword_filtering_enabled" -> editor.putBoolean("keyword_filtering_enabled", value.toBoolean())
                        "strict_keyword_mode" -> editor.putBoolean("strict_keyword_mode", value.toBoolean())
                        "trusted_domains" -> editor.putString("trusted_domains", value)
                        "custom_blocked_keywords" -> editor.putString("custom_blocked_keywords", value)
                        "allow_override_trusted_sites" -> editor.putBoolean("allow_override_trusted_sites", value.toBoolean())
                        "show_override_button" -> editor.putBoolean("show_override_button", value.toBoolean())
                        "enable_image_caching" -> editor.putBoolean("enable_image_caching", value.toBoolean())
                        "max_cache_size" -> editor.putInt("max_cache_size", value.toInt())
                    }
                }
            }
            
            editor.apply()
            AppLogger.i("FilteringSettings", "Settings imported successfully")
            true
        } catch (e: Exception) {
            AppLogger.e("FilteringSettings", "Failed to import settings", e)
            false
        }
    }
    
    // Preset configurations
    fun applyStrictMode() {
        filteringMode = NSFWJSFilterManager.FilteringMode.STRICT
        allowBlobImages = false
        allowBase64Images = false
        nsfwjsEnabled = true
        nsfwjsConfidenceThreshold = 0.4f // Lower threshold = more strict
        keywordFilteringEnabled = true
        strictKeywordMode = true
        allowOverrideOnTrustedSites = false
        showOverrideButton = false
        AppLogger.i("FilteringSettings", "Applied strict mode configuration")
    }
    
    fun applyModerateMode() {
        filteringMode = NSFWJSFilterManager.FilteringMode.MODERATE
        allowBlobImages = false
        allowBase64Images = false
        nsfwjsEnabled = true
        nsfwjsConfidenceThreshold = 0.6f // Standard threshold
        keywordFilteringEnabled = true
        strictKeywordMode = false
        allowOverrideOnTrustedSites = true
        showOverrideButton = true
        AppLogger.i("FilteringSettings", "Applied moderate mode configuration")
    }
    
    fun applyCustomMode() {
        filteringMode = NSFWJSFilterManager.FilteringMode.CUSTOM
        // Keep current settings for other options
        AppLogger.i("FilteringSettings", "Applied custom mode configuration")
    }
    
    // Validation methods
    fun validateSettings(): List<String> {
        val issues = mutableListOf<String>()
        
        if (nsfwjsConfidenceThreshold < 0.1f || nsfwjsConfidenceThreshold > 0.9f) {
            issues.add("NSFWJS confidence threshold should be between 0.1 and 0.9")
        }
        
        if (maxCacheSize < 100 || maxCacheSize > 5000) {
            issues.add("Cache size should be between 100 and 5000")
        }
        
        val trustedDomains = getTrustedDomains()
        if (trustedDomains.isEmpty() && filteringMode == NSFWJSFilterManager.FilteringMode.MODERATE) {
            issues.add("Moderate mode requires at least one trusted domain")
        }
        
        return issues
    }
    
    fun getSettingsSummary(): String {
        return """
            Filtering Mode: $filteringMode
            NSFWJS: ${if (nsfwjsEnabled) "Enabled" else "Disabled"} (Threshold: $nsfwjsConfidenceThreshold)
            Keyword Filtering: ${if (keywordFilteringEnabled) "Enabled" else "Disabled"} ${if (strictKeywordMode) "(Strict)" else "(Normal)"}
            Blob Images: ${if (allowBlobImages) "Allowed" else "Blocked"}
            Base64 Images: ${if (allowBase64Images) "Allowed" else "Blocked"}
            Trusted Domains: ${getTrustedDomains().size}
            Custom Keywords: ${getCustomBlockedKeywords().size}
            Statistics: ${getBlockedImageCount()} images, ${getBlockedVideoCount()} videos blocked
        """.trimIndent()
    }
}