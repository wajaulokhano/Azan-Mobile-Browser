package com.example.noorahlulbayt.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.noorahlulbayt.BrowserViewModel
import com.example.noorahlulbayt.models.HistoryEntry
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    browserViewModel: BrowserViewModel = viewModel(),
    onNavigateToUrl: (String) -> Unit,
    onBack: () -> Unit
) {
    val history by browserViewModel.history.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
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
                text = "History",
                color = Color(0xFF006400),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { browserViewModel.clearHistory() }) {
                Text("🗑", color = Color.White, fontSize = 20.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { 
                searchQuery = it
                if (it.isNotEmpty()) {
                    browserViewModel.searchHistory(it)
                } else {
                    // Load all history when search is cleared
                    // This will be handled by the ViewModel automatically
                }
            },
            placeholder = { Text("Search history...", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF006400),
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { browserViewModel.searchHistory(searchQuery) }
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (history.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📚",
                        fontSize = 64.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No results found" else "No history yet",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Try a different search term" else "Visited pages will appear here",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            // History list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history) { entry ->
                    HistoryItem(
                        entry = entry,
                        onClick = { onNavigateToUrl(entry.url) },
                        onRemove = { browserViewModel.removeHistoryEntry(entry) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryItem(
    entry: HistoryEntry,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = entry.url,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDate(entry.dateVisited),
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                    if (entry.visitCount > 1) {
                        Text(
                            text = " • ${entry.visitCount} visits",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            IconButton(onClick = onRemove) {
                Text("🗑", fontSize = 20.sp)
            }
        }
    }
}

private fun formatDate(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    val days = diff / (24 * 60 * 60 * 1000)
    
    return when {
        days == 0L -> {
            val hours = diff / (60 * 60 * 1000)
            if (hours == 0L) {
                val minutes = diff / (60 * 1000)
                "${minutes}m ago"
            } else {
                "${hours}h ago"
            }
        }
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
    }
}