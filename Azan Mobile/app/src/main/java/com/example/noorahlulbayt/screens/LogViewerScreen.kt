package com.example.noorahlulbayt.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.noorahlulbayt.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun LogViewerScreen(
    onBack: () -> Unit
) {
    var logFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedLogFile by remember { mutableStateOf<File?>(null) }
    var logContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        logFiles = AppLogger.getLogFiles()
        if (logFiles.isNotEmpty()) {
            selectedLogFile = logFiles.first()
            loadLogContent(logFiles.first()) { content ->
                logContent = content
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text("←", color = Color.White, fontSize = 24.sp)
            }
            Text(
                text = "Debug Logs",
                color = Color(0xFF006400),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Row {
                IconButton(onClick = {
                    selectedLogFile?.let { file ->
                        loadLogContent(file) { content ->
                            logContent = content
                        }
                    }
                }) {
                    Text("🔄", color = Color.White, fontSize = 20.sp)
                }
                IconButton(onClick = {
                    AppLogger.clearLogs()
                    logFiles = AppLogger.getLogFiles()
                    logContent = ""
                    selectedLogFile = null
                }) {
                    Text("🗑", color = Color.White, fontSize = 20.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Log directory info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Log Directory",
                    color = Color(0xFF006400),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                SelectionContainer {
                    Text(
                        text = AppLogger.getLogDirectory(),
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Log file selector
        if (logFiles.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Log Files (${logFiles.size})",
                        color = Color(0xFF006400),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    logFiles.forEach { file ->
                        val isSelected = file == selectedLogFile
                        val backgroundColor = if (isSelected) Color(0xFF006400) else Color.Transparent
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = file.name,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${file.length() / 1024}KB - ${java.text.SimpleDateFormat("MMM dd, HH:mm").format(java.util.Date(file.lastModified()))}",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                            
                            if (!isSelected) {
                                TextButton(
                                    onClick = {
                                        selectedLogFile = file
                                        isLoading = true
                                        loadLogContent(file) { content ->
                                            logContent = content
                                            isLoading = false
                                        }
                                    }
                                ) {
                                    Text("View", color = Color(0xFF006400))
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Log content
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedLogFile?.name ?: "No log selected",
                        color = Color(0xFF006400),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF006400)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (logContent.isEmpty() && !isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📄",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (logFiles.isEmpty()) "No log files found" else "Select a log file to view",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                            if (logFiles.isEmpty()) {
                                Text(
                                    text = "Logs will appear here when the app runs",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val lines = logContent.split("\n")
                        items(lines) { line ->
                            LogLine(line = line)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogLine(line: String) {
    val color = when {
        line.contains("ERROR") -> Color.Red
        line.contains("WARN") -> Color.Yellow
        line.contains("INFO") -> Color.Cyan
        line.contains("DEBUG") -> Color.Gray
        line.contains("CRASH") -> Color.Magenta
        else -> Color.White
    }
    
    SelectionContainer {
        Text(
            text = line,
            color = color,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun loadLogContent(file: File, onLoaded: (String) -> Unit) {
    try {
        val content = file.readText()
        // Show last 1000 lines to avoid memory issues
        val lines = content.split("\n")
        val displayLines = if (lines.size > 1000) {
            listOf("... (showing last 1000 lines) ...") + lines.takeLast(1000)
        } else {
            lines
        }
        onLoaded(displayLines.joinToString("\n"))
    } catch (e: Exception) {
        onLoaded("Error reading log file: ${e.message}")
    }
}