# Azan Mobile Browser - Crash Debugging Guide

## 🚨 App Crashing? Here's How to Debug It

If your Azan Mobile Browser is crashing on your mobile device, this guide will help you collect debug information to identify and fix the issue.

## 📱 Quick Steps to Get Crash Logs

### **Method 1: Using the Built-in Log Viewer (Recommended)**

1. **Open the Browser App** (if it doesn't crash immediately)
2. **Tap the ⚙ Settings icon** in the top bar
3. **Scroll down to "Debug & Support" section**
4. **Tap "View Debug Logs"**
5. **Look for RED lines** (ERROR) or MAGENTA lines (CRASH)
6. **Take screenshots** of the crash information

### **Method 2: Direct File Access**

1. **Open your device's File Manager**
2. **Navigate to**: `Documents/AzanBrowserLogs/`
3. **Look for files named**: `azan_browser_YYYY-MM-DD.log`
4. **Open the most recent log file** with a text editor
5. **Search for**: `CRASH REPORT` or `ERROR`

## 🔍 What to Look For in Logs

### **Crash Reports**
Look for sections that start with:
```
=== CRASH REPORT ===
Time: 2025-08-01 14:30:25.123
Thread: main
Exception: RuntimeException
Message: Unable to start activity
Stack Trace:
...
=== END CRASH REPORT ===
```

### **Error Messages**
Look for lines containing:
- `ERROR` - Critical errors that might cause crashes
- `WARN` - Warnings that might indicate problems
- `WebView Error` - Browser-specific issues
- `Permission` - Permission-related problems

### **Common Crash Patterns**

#### **Memory Issues**
```
ERROR [BrowserViewModel] OutOfMemoryError
ERROR [WebView] Failed to allocate memory
```

#### **Permission Issues**
```
ERROR [AppLogger] Failed to setup log file
WARN [PermissionHelper] Storage permission not granted
```

#### **WebView Crashes**
```
ERROR [WebView] WebView Error - URL: https://example.com
ERROR [ContentFilter] Error processing filter result
```

#### **Tab Management Issues**
```
ERROR [TabManager] Tab operation failed
WARN [BrowserViewModel] Cannot add tab - maximum limit reached
```

## 🛠️ Common Solutions

### **If App Crashes on Startup**

1. **Check Storage Permission**:
   - Go to Settings > Apps > Azan Mobile Browser > Permissions
   - Enable "Storage" permission
   - Enable "Files and media" permission (Android 11+)

2. **Clear App Data**:
   - Go to Settings > Apps > Azan Mobile Browser > Storage
   - Tap "Clear Data" (this will reset the app)

3. **Check Available Storage**:
   - Ensure you have at least 100MB free space
   - Clear some files if storage is full

### **If App Crashes While Browsing**

1. **Check Memory Usage**:
   - Close other apps to free up RAM
   - Restart your device
   - Limit the number of open tabs (max 5)

2. **Disable Content Filtering Temporarily**:
   - Open Settings in the browser
   - Turn off "Visual Content Analysis"
   - Turn off "Ad Blocking"

3. **Clear Browser Data**:
   - Go to Settings > Privacy & Security
   - Clear History, Downloads, and Favorites

### **If Companion App Issues**

1. **Check Accessibility Service**:
   - Go to Settings > Accessibility
   - Find "Noor-e-AhlulBayt Companion"
   - Make sure it's enabled

2. **Check Device Admin**:
   - Go to Settings > Security > Device Admin
   - Ensure companion app is listed and enabled

## 📋 Information to Collect for Support

When reporting crashes, please provide:

### **Device Information**
- Device model (e.g., Samsung Galaxy S21)
- Android version (e.g., Android 12)
- Available RAM and storage
- Other browsers installed

### **Crash Details**
- When does it crash? (startup, browsing, specific websites)
- How often? (always, sometimes, rarely)
- What were you doing when it crashed?
- Any error messages shown?

### **Log Information**
- Copy the crash report from logs
- Include the last 20-30 lines before the crash
- Note the timestamp of the crash

## 🔧 Advanced Debugging

### **Enable Verbose Logging**
The app automatically logs detailed information including:
- All tab operations (add, close, navigate)
- WebView errors and page loading
- Content filtering results
- Prayer time events
- Memory usage information

### **Log File Locations**
```
/storage/emulated/0/Documents/AzanBrowserLogs/
├── azan_browser_2025-08-01.log (today's log)
├── azan_browser_2025-07-31.log (yesterday's log)
└── ... (up to 5 recent log files)
```

### **Log File Rotation**
- New log file created daily
- Files rotate when they exceed 5MB
- Maximum 5 log files kept
- Old files automatically deleted

## 📞 Getting Help

### **Self-Diagnosis Steps**
1. **Check the logs** using methods above
2. **Try common solutions** listed in this guide
3. **Test with different websites** to isolate the issue
4. **Restart both apps** and try again

### **Reporting Issues**
If you need to report the crash:

1. **Collect crash logs** using the methods above
2. **Take screenshots** of error messages
3. **Note your device details** and Android version
4. **Describe the steps** that led to the crash
5. **Include the log files** or crash report text

### **Emergency Workarounds**
If the app keeps crashing:

1. **Use Safe Mode**:
   - Disable all content filtering
   - Use minimal tabs (1-2 only)
   - Avoid heavy websites

2. **Reset to Defaults**:
   - Clear all app data
   - Reinstall both apps
   - Set up from scratch

3. **Alternative Access**:
   - Use the companion app to check prayer times
   - Use another browser temporarily while debugging

## 🔄 Log Management

### **Viewing Logs in App**
- Settings > Debug & Support > View Debug Logs
- Tap refresh (🔄) to reload logs
- Tap delete (🗑) to clear all logs
- Use search to find specific errors

### **Sharing Logs**
- Settings > Debug & Support > Export Logs
- Logs can be shared via email or messaging
- Remove personal information before sharing

### **Log Privacy**
- Logs contain URLs you visit
- No passwords or personal data logged
- Clear logs regularly for privacy

---

## 📱 Quick Reference

### **Most Common Crash Causes**
1. **WorkManager initialization error** → Fixed in latest version (v1.0.1+)
2. **Storage permission denied** → Enable storage permissions
3. **Out of memory** → Close other apps, restart device
4. **WebView crashes** → Clear browser data, update WebView
5. **Tab limit exceeded** → Close some tabs (max 5)
6. **Corrupted data** → Clear app data and reinstall

### **Emergency Commands**
- **Clear all data**: Settings > Apps > Azan Mobile Browser > Storage > Clear Data
- **Reset permissions**: Settings > Apps > Azan Mobile Browser > Permissions > Reset
- **Force stop**: Settings > Apps > Azan Mobile Browser > Force Stop

### **Log File Quick Access**
```
File Manager → Documents → AzanBrowserLogs → azan_browser_[date].log
```

---

**Remember**: The logging system is designed to help you debug crashes. The more detailed information you can provide, the easier it will be to identify and fix the issue.

**Last Updated**: 2025-08-01  
**Guide Version**: 1.0