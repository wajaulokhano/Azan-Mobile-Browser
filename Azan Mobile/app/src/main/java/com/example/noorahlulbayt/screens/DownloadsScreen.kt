package com.example.noorahlulbayt.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.noorahlulbayt.BrowserViewModel
import com.example.noorahlulbayt.models.Download
import com.example.noorahlulbayt.models.DownloadStatus

@Composable
fun DownloadsScreen(
    browserViewModel: BrowserViewModel = viewModel(),
    onBack: () -> Unit
) {
    val downloads by browserViewModel.downloads.collectAsState()
    
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
                text = "Downloads",
                color = Color(0xFF006400),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (downloads.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⬇",
                        fontSize = 64.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No downloads yet",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Downloaded files will appear here",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            // Downloads list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(downloads) { download ->
                    DownloadItem(
                        download = download,
                        onCancel = { browserViewModel.cancelDownload(download.id) },
                        onDelete = { browserViewModel.deleteDownload(download) }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadItem(
    download: Download,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.filename,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = download.url,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                
                when (download.status) {
                    DownloadStatus.DOWNLOADING -> {
                        IconButton(onClick = onCancel) {
                            Text("⏸", fontSize = 20.sp)
                        }
                    }
                    DownloadStatus.COMPLETED -> {
                        IconButton(onClick = onDelete) {
                            Text("🗑", fontSize = 20.sp)
                        }
                    }
                    else -> {
                        IconButton(onClick = onDelete) {
                            Text("🗑", fontSize = 20.sp)
                        }
                    }
                }
            }
            
            // Progress bar for downloading files
            if (download.status == DownloadStatus.DOWNLOADING && download.fileSize > 0) {
                val progress = download.downloadedBytes.toFloat() / download.fileSize.toFloat()
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF006400)
                )
                Text(
                    text = "${(progress * 100).toInt()}% - ${formatBytes(download.downloadedBytes)} / ${formatBytes(download.fileSize)}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            
            // Status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusColor = when (download.status) {
                    DownloadStatus.COMPLETED -> Color(0xFF006400)
                    DownloadStatus.DOWNLOADING -> Color.Yellow
                    DownloadStatus.FAILED -> Color.Red
                    DownloadStatus.CANCELLED -> Color.Gray
                    DownloadStatus.PENDING -> Color.Blue
                }
                
                Text(
                    text = "●",
                    color = statusColor,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = download.status.name,
                    color = statusColor,
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        bytes >= 1024 -> "${bytes / 1024} KB"
        else -> "$bytes B"
    }
}