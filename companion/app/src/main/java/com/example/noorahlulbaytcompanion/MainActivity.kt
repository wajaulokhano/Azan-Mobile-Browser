package com.example.noorahlulbaytcompanion

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private lateinit var settingsViewModel: SettingsViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)
        settingsViewModel = SettingsViewModel(application)
        
        setContent {
            CompanionAppScreen(
                settingsViewModel = settingsViewModel,
                onEnableDeviceAdmin = { enableDeviceAdmin() },
                onOpenAccessibilitySettings = { openAccessibilitySettings() },
                onOpenPrivateDNSSettings = { openPrivateDNSSettings() }
            )
        }
    }
    
    private fun enableDeviceAdmin() {
        if (!devicePolicyManager.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required for browser control")
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "Device Admin already enabled", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
    
    private fun openPrivateDNSSettings() {
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        startActivity(intent)
    }
}

@Composable
fun CompanionAppScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    onEnableDeviceAdmin: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenPrivateDNSSettings: () -> Unit
) {
    val context = LocalContext.current
    
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF006400), // Green
            background = Color.Black,
            surface = Color.Black,
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Azan Mobile Browser Companion",
                color = Color(0xFF006400),
                fontSize = 24.sp,
                fontFamily = FontFamily.Default,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Settings Section
                item {
                    SettingsSection(
                        title = "Browser Control",
                        settings = listOf(
                            SettingItem(
                                title = "Azan Blocking",
                                description = "Block browser during prayer times",
                                isEnabled = settingsViewModel.azanBlockingEnabled.collectAsState().value,
                                onToggle = { settingsViewModel.setAzanBlocking(it) }
                            ),
                            SettingItem(
                                title = "Browser Blocking",
                                description = "Block other browsers",
                                isEnabled = settingsViewModel.browserBlockingEnabled.collectAsState().value,
                                onToggle = { settingsViewModel.setBrowserBlocking(it) }
                            )
                        )
                    )
                }
                
                // Prayer Times Section
                item {
                    PrayerTimesSection(
                        prayerTimes = settingsViewModel.prayerTimes.collectAsState().value,
                        onRefresh = { settingsViewModel.refreshPrayerTimes() }
                    )
                }
                
                // Admin Section
                item {
                    AdminSection(
                        onEnableDeviceAdmin = onEnableDeviceAdmin,
                        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                        onOpenPrivateDNSSettings = onOpenPrivateDNSSettings
                    )
                }
                
                // PIN Section
                item {
                    PINSection(
                        onSetupPIN = { settingsViewModel.showPINSetup() },
                        onChangePIN = { settingsViewModel.showPINChange() }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    settings: List<SettingItem>
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
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            settings.forEach { setting ->
                SettingRow(setting = setting)
                if (setting != settings.last()) {
                    Divider(color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun SettingRow(setting: SettingItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = setting.title,
                color = Color.White,
                fontSize = 16.sp
            )
            Text(
                text = setting.description,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        Switch(
            checked = setting.isEnabled,
            onCheckedChange = setting.onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF006400),
                checkedTrackColor = Color(0xFF006400).copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun PrayerTimesSection(
    prayerTimes: List<PrayerTime>,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = "Prayer Times",
                    color = Color(0xFF006400),
                    fontSize = 18.sp
                )
                IconButton(onClick = onRefresh) {
                    Text("🔄", fontSize = 20.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            prayerTimes.forEach { prayerTime ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = prayerTime.name,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = prayerTime.time,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
                if (prayerTime != prayerTimes.last()) {
                    Divider(color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun AdminSection(
    onEnableDeviceAdmin: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenPrivateDNSSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Administrative",
                color = Color(0xFF006400),
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Button(
                onClick = onEnableDeviceAdmin,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006400))
            ) {
                Text("Enable Device Admin")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onOpenAccessibilitySettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006400))
            ) {
                Text("Accessibility Settings")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onOpenPrivateDNSSettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006400))
            ) {
                Text("Private DNS Settings")
            }
        }
    }
}

@Composable
fun PINSection(
    onSetupPIN: () -> Unit,
    onChangePIN: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "PIN Override",
                color = Color(0xFF006400),
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Button(
                onClick = onSetupPIN,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006400))
            ) {
                Text("Setup PIN")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onChangePIN,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006400))
            ) {
                Text("Change PIN")
            }
        }
    }
}

data class SettingItem(
    val title: String,
    val description: String,
    val isEnabled: Boolean,
    val onToggle: (Boolean) -> Unit
)

data class PrayerTime(
    val name: String,
    val time: String
) 