package com.example.noorahlulbayt.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onViewLogs: () -> Unit = {},
    onViewHistory: () -> Unit = {}
) {
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
                text = "Settings",
                color = Color(0xFF006400),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSection(
                    title = "Content Filtering",
                    items = listOf(
                        SettingItem(
                            title = "Keyword Filtering",
                            description = "Block pages with inappropriate keywords",
                            isEnabled = true,
                            onToggle = { /* TODO: Implement */ }
                        ),
                        SettingItem(
                            title = "Visual Content Analysis",
                            description = "AI-powered image content filtering",
                            isEnabled = true,
                            onToggle = { /* TODO: Implement */ }
                        ),
                        SettingItem(
                            title = "Ad Blocking",
                            description = "Block advertisements and trackers",
                            isEnabled = false,
                            onToggle = { /* TODO: Implement */ }
                        )
                    )
                )
            }
            
            item {
                SettingsSection(
                    title = "Privacy & Security",
                    items = listOf(
                        SettingItem(
                            title = "View History",
                            description = "Browse your browsing history",
                            isAction = true,
                            onAction = onViewHistory
                        ),
                        SettingItem(
                            title = "Clear History",
                            description = "Remove all browsing history",
                            isAction = true,
                            onAction = { /* TODO: Implement */ }
                        ),
                        SettingItem(
                            title = "Clear Downloads",
                            description = "Remove all download records",
                            isAction = true,
                            onAction = { /* TODO: Implement */ }
                        ),
                        SettingItem(
                            title = "Clear Favorites",
                            description = "Remove all bookmarks",
                            isAction = true,
                            onAction = { /* TODO: Implement */ }
                        )
                    )
                )
            }
            
            item {
                SettingsSection(
                    title = "Islamic Features",
                    items = listOf(
                        SettingItem(
                            title = "Prayer Time Blocking",
                            description = "Block browser during prayer times",
                            isEnabled = true,
                            onToggle = { /* TODO: Implement */ }
                        ),
                        SettingItem(
                            title = "Islamic Theme",
                            description = "Use Islamic color scheme and fonts",
                            isEnabled = true,
                            onToggle = { /* TODO: Implement */ }
                        )
                    )
                )
            }
            
            item {
                SettingsSection(
                    title = "Debug & Support",
                    items = listOf(
                        SettingItem(
                            title = "View Debug Logs",
                            description = "View app logs for troubleshooting crashes",
                            isAction = true,
                            onAction = onViewLogs
                        ),
                        SettingItem(
                            title = "Export Logs",
                            description = "Share logs for technical support",
                            isAction = true,
                            onAction = { /* TODO: Implement */ }
                        )
                    )
                )
            }
            
            item {
                SettingsSection(
                    title = "About",
                    items = listOf(
                        SettingItem(
                            title = "Version",
                            description = "1.0.0",
                            isInfo = true
                        ),
                        SettingItem(
                            title = "Open Source Licenses",
                            description = "View third-party licenses",
                            isAction = true,
                            onAction = { /* TODO: Implement */ }
                        )
                    )
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    items: List<SettingItem>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                color = Color(0xFF006400),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            items.forEachIndexed { index, item ->
                SettingRow(item = item)
                if (index < items.size - 1) {
                    Divider(
                        color = Color.Gray.copy(alpha = 0.3f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingRow(item: SettingItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 16.sp
            )
            Text(
                text = item.description,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        
        when {
            item.isAction -> {
                TextButton(
                    onClick = { item.onAction?.invoke() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF006400))
                ) {
                    Text("Action")
                }
            }
            item.isInfo -> {
                // Just display the description, no action needed
            }
            else -> {
                Switch(
                    checked = item.isEnabled,
                    onCheckedChange = item.onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF006400),
                        checkedTrackColor = Color(0xFF006400).copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

data class SettingItem(
    val title: String,
    val description: String,
    val isEnabled: Boolean = false,
    val isAction: Boolean = false,
    val isInfo: Boolean = false,
    val onToggle: ((Boolean) -> Unit)? = null,
    val onAction: (() -> Unit)? = null
)