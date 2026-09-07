package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.SlateSecondary
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.viewmodel.ConstructionViewModel
import com.example.util.SupabaseSyncManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudSyncDialog(
    viewModel: ConstructionViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val syncManager = viewModel.syncManager

    var url by remember { mutableStateOf(syncManager?.getSupabaseUrl() ?: "") }
    var anonKey by remember { mutableStateOf(syncManager?.getSupabaseKey() ?: "") }
    var autoSync by remember { mutableStateOf(syncManager?.isAutoSyncEnabled() ?: true) }
    var showKey by remember { mutableStateOf(false) }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var isManualSyncing by remember { mutableStateOf(false) }
    var syncResultMsg by remember { mutableStateOf<String?>(null) }

    val isConfigured = syncManager?.isConfigured() == true
    val lastSyncTime = syncManager?.getLastSyncTime() ?: 0L

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_cloud_sync"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isConfigured) ForestGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isConfigured) Icons.Default.CloudDone else Icons.Default.CloudSync,
                            contentDescription = "Cloud Sync",
                            tint = if (isConfigured) ForestGreen else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Phone-to-Phone Cloud Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Live Real-Time Data Sync Across All Devices",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Status Banner Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isConfigured) ForestGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isConfigured) Icons.Default.CheckCircle else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (isConfigured) ForestGreen else SafetyAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isConfigured) "🟢 Cloud Sync Active" else "⚪ Offline / Local Mode",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isConfigured) ForestGreen else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isConfigured) {
                                    if (lastSyncTime > 0) {
                                        val df = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                                        "Last synced: ${df.format(Date(lastSyncTime))}"
                                    } else "Connected. Ready to sync."
                                } else "Enter Supabase credentials to sync live across phones.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Quick Action Buttons if configured
                if (isConfigured) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isManualSyncing = true
                                syncResultMsg = null
                                viewModel.syncNow { success, msg ->
                                    isManualSyncing = false
                                    syncResultMsg = msg
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isManualSyncing,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_sync_now"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
                        ) {
                            if (isManualSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Syncing...")
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Sync Now")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Supabase SQL", SupabaseSyncManager.SQL_SETUP_SCRIPT)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "SQL Setup Script copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("btn_copy_sql"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy SQL")
                        }
                    }

                    if (syncResultMsg != null) {
                        Text(
                            text = syncResultMsg ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                HorizontalDivider()

                // Configuration Section Header
                Text(
                    text = "Supabase Cloud Configuration",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Project URL Field
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Supabase Project URL") },
                    placeholder = { Text("https://xyzcompany.supabase.co") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_supabase_url"),
                    shape = RoundedCornerShape(10.dp)
                )

                // Anon Key Field
                OutlinedTextField(
                    value = anonKey,
                    onValueChange = { anonKey = it },
                    label = { Text("Supabase Anon / API Key") },
                    placeholder = { Text("eyJhbGciOiJIUzI1NiIsIn...") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    trailingIcon = {
                        TextButton(onClick = { showKey = !showKey }) {
                            Text(if (showKey) "Hide" else "Show", fontSize = 11.sp)
                        }
                    },
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_supabase_key"),
                    shape = RoundedCornerShape(10.dp)
                )

                // Auto-Sync Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Sync in Background",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Syncs every 15s & instantly on new entries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoSync,
                        onCheckedChange = { autoSync = it },
                        modifier = Modifier.testTag("switch_auto_sync")
                    )
                }

                // Test Connection Button & Result
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (url.isBlank() || anonKey.isBlank()) {
                                testResult = Pair(false, "Please enter both URL and API Key")
                                return@OutlinedButton
                            }
                            isTesting = true
                            testResult = null
                            viewModel.testCloudConnection(url, anonKey) { success, msg ->
                                isTesting = false
                                testResult = Pair(success, msg)
                            }
                        },
                        enabled = !isTesting && url.isNotBlank() && anonKey.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_test_connection"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                            Text("Testing Connection...")
                        } else {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("⚡ Test Connection")
                        }
                    }
                }

                testResult?.let { (success, message) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (success) ForestGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (success) ForestGreen else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (success) ForestGreen else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Instructions Guide Box
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSecondary.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "💡 Kaise Setup Karein (3 Steps):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "1. supabase.com par login karein aur Project banayein.\n2. \"Copy SQL\" button daba kar SQL Editor me paste & RUN karein.\n3. Project Settings > API se URL aur anon public key copy karke yaha paste karein.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.saveCloudConfig(url, anonKey, autoSync)
                    Toast.makeText(context, "Cloud Sync settings saved!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                modifier = Modifier.testTag("btn_save_cloud_config"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save & Connect")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_close_cloud_dialog")
            ) {
                Text("Close")
            }
        }
    )
}
