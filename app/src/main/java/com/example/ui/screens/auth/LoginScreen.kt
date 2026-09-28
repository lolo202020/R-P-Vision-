package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Site
import com.example.ui.components.CloudSyncDialog
import com.example.ui.components.RpvcBrandHeader
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.OnAmberContainer
import com.example.ui.theme.SlateSecondary
import com.example.ui.viewmodel.ConstructionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: ConstructionViewModel,
    onLoginSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sites by viewModel.allSites.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0 = Site Incharge, 1 = Admin
    var mobileNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(loginError) {
        if (!loginError.isNullOrBlank()) {
            isSubmitting = false
        }
    }

    var selectedSite by remember { mutableStateOf<Site?>(null) }
    var siteDropdownExpanded by remember { mutableStateOf(false) }

    var showCloudSyncDialog by remember { mutableStateOf(false) }

    if (showCloudSyncDialog) {
        CloudSyncDialog(
            viewModel = viewModel,
            onDismiss = { showCloudSyncDialog = false }
        )
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Left Row with Cloud Sync Option on the LEFT side of the screen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Start, // Positioned on the LEFT side
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isConfigured = viewModel.syncManager?.isConfigured() == true

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isConfigured) ForestGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showCloudSyncDialog = true }
                    .testTag("btn_login_left_cloud_sync")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isConfigured) Icons.Default.CloudDone else Icons.Default.CloudSync,
                        contentDescription = "Cloud Sync",
                        tint = if (isConfigured) ForestGreen else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isConfigured) "🟢 Cloud Sync Active" else "☁️ Cloud Sync",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isConfigured) ForestGreen else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // RPVC Official Brand Header & Logo
        RpvcBrandHeader(
            logoSize = 88.dp,
            showTagline = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Role Tabs: Site Incharge vs Admin
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = {
                        selectedTabIndex = 0
                        mobileNumber = ""
                        password = ""
                        selectedSite = null
                    },
                    modifier = Modifier.testTag("tab_site_incharge"),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Site Incharge", fontWeight = FontWeight.Bold)
                        }
                    }
                )

                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                        mobileNumber = "9621803006"
                        password = "20262026"
                        selectedSite = null
                    },
                    modifier = Modifier.testTag("tab_admin"),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Admin (View Only)", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Role Info Banner
        if (selectedTabIndex == 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AmberContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = OnAmberContainer,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Site Incharge Login: Enter Income/Expense for your assigned site only. Other site data is restricted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnAmberContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Admin Console: VIEW-ONLY mode. Executive dashboards, financial reports & audit day books across all sites.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main Login Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row inside Login Panel: Left side Cloud Sync button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isConfigured = viewModel.syncManager?.isConfigured() == true

                    // LEFT SIDE Cloud Sync Option
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isConfigured) ForestGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showCloudSyncDialog = true }
                            .testTag("btn_panel_left_cloud_sync")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isConfigured) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                contentDescription = "Cloud Sync Settings",
                                tint = if (isConfigured) ForestGreen else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isConfigured) "Sync On 🟢" else "Cloud Sync ☁️",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isConfigured) ForestGreen else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = if (selectedTabIndex == 0) "Site Incharge Sign In" else "Admin Executive Sign In",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Mobile Number
                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { input ->
                        mobileNumber = input
                        if (selectedTabIndex == 0 && input.trim().length >= 5) {
                            val matched = sites.firstOrNull { it.mobile.trim() == input.trim() }
                            if (matched != null) {
                                selectedSite = matched
                            }
                        }
                    },
                    label = { Text("Mobile Number (User ID)") },
                    placeholder = { Text("Enter 10-digit mobile") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_mobile"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    placeholder = { Text("Enter password") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_password"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Site Selector (Only for Site Incharge)
                if (selectedTabIndex == 0) {
                    ExposedDropdownMenuBox(
                        expanded = siteDropdownExpanded,
                        onExpandedChange = { siteDropdownExpanded = !siteDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedSite?.let { "${it.name} (${it.code})" } ?: "Select Assigned Site (Optional)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Site / Project") },
                            leadingIcon = { Icon(Icons.Default.Apartment, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("dropdown_site_select"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = siteDropdownExpanded,
                            onDismissRequest = { siteDropdownExpanded = false }
                        ) {
                            sites.forEach { site ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "${site.name} [${site.code}]",
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Incharge: ${site.inchargeName} • Mobile: ${site.mobile.ifBlank { "Not set" }}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedSite = site
                                        if (mobileNumber.isBlank() && site.mobile.isNotBlank()) {
                                            mobileNumber = site.mobile
                                        }
                                        siteDropdownExpanded = false
                                    },
                                    modifier = Modifier.testTag("site_option_${site.id}")
                                )
                            }
                        }
                    }
                }

                // Error Message if any
                if (!loginError.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = loginError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Login Button
                Button(
                    onClick = {
                        if (!isSubmitting) {
                            isSubmitting = true
                            viewModel.login(
                                mobile = mobileNumber,
                                pass = password,
                                selectedSiteId = if (selectedTabIndex == 0) selectedSite?.id else null,
                                onRoleDecided = { role ->
                                    isSubmitting = false
                                    onLoginSuccess(role)
                                }
                            )
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_login_submit"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = if (selectedTabIndex == 0) "Login to Site Console" else "Login to Admin Console",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
    }
}
