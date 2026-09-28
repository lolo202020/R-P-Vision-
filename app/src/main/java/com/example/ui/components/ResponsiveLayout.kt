package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntry
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedContainer
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenContainer
import com.example.ui.theme.OnExpenseRedContainer
import com.example.ui.theme.OnIncomeGreenContainer
import com.example.ui.theme.SlateSecondary

/**
 * Screen Device Breakpoint Classification
 * - Small Mobile: < 480dp
 * - Mobile: 480dp - 767dp
 * - Tablet: 768dp - 1023dp
 * - Desktop: 1024dp - 1439dp
 * - Large Desktop: >= 1440dp
 */
enum class ScreenBreakpoint {
    SMALL_MOBILE,
    MOBILE,
    TABLET,
    DESKTOP,
    LARGE_DESKTOP
}

data class ResponsiveWindowInfo(
    val breakpoint: ScreenBreakpoint,
    val widthDp: Dp,
    val isMobile: Boolean,
    val isTablet: Boolean,
    val isDesktop: Boolean,
    val isWideScreen: Boolean, // Tablet or Desktop
    val statCardColumns: Int
)

@Composable
fun calculateResponsiveInfo(maxWidth: Dp): ResponsiveWindowInfo {
    val breakpoint = when {
        maxWidth < 480.dp -> ScreenBreakpoint.SMALL_MOBILE
        maxWidth < 768.dp -> ScreenBreakpoint.MOBILE
        maxWidth < 1024.dp -> ScreenBreakpoint.TABLET
        maxWidth < 1440.dp -> ScreenBreakpoint.DESKTOP
        else -> ScreenBreakpoint.LARGE_DESKTOP
    }

    val isMobile = maxWidth < 768.dp
    val isTablet = maxWidth in 768.dp..1023.dp
    val isDesktop = maxWidth >= 1024.dp
    val isWideScreen = maxWidth >= 768.dp

    val statCardColumns = when (breakpoint) {
        ScreenBreakpoint.SMALL_MOBILE -> 1
        ScreenBreakpoint.MOBILE -> 2
        ScreenBreakpoint.TABLET -> 2
        ScreenBreakpoint.DESKTOP -> 4
        ScreenBreakpoint.LARGE_DESKTOP -> 4
    }

    return ResponsiveWindowInfo(
        breakpoint = breakpoint,
        widthDp = maxWidth,
        isMobile = isMobile,
        isTablet = isTablet,
        isDesktop = isDesktop,
        isWideScreen = isWideScreen,
        statCardColumns = statCardColumns
    )
}

data class NavDestination(
    val index: Int,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

/**
 * Universal Responsive Navigation Shell
 * Adapts between Mobile Bottom Navigation and Desktop / Tablet Sidebar
 */
@Composable
fun ResponsiveAppShell(
    items: List<NavDestination>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    topBar: @Composable () -> Unit,
    floatingActionButton: (@Composable () -> Unit)? = null,
    brandSubtitle: String = "R P V C Enterprise",
    userName: String? = null,
    userRole: String? = null,
    onLogoutClick: (() -> Unit)? = null,
    content: @Composable (ResponsiveWindowInfo) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val windowInfo = calculateResponsiveInfo(maxWidth)
        var sidebarExpanded by remember { mutableStateOf(windowInfo.isDesktop) }

        if (windowInfo.isMobile) {
            // Mobile Layout: TopAppBar + Content + Bottom NavigationBar
            Scaffold(
                topBar = topBar,
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        items.forEach { item ->
                            NavigationBarItem(
                                selected = selectedIndex == item.index,
                                onClick = { onItemSelected(item.index) },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                },
                floatingActionButton = floatingActionButton ?: {}
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    content(windowInfo)
                }
            }
        } else {
            // Desktop & Tablet Layout: Persistent Left Sidebar + Main Content Area
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Desktop Sidebar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(if (sidebarExpanded) 240.dp else 80.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 16.dp, horizontal = if (sidebarExpanded) 12.dp else 6.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header / Brand identity
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = if (sidebarExpanded) Arrangement.SpaceBetween else Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RpvcBrandLogo(size = if (sidebarExpanded) 36.dp else 32.dp)
                                    if (sidebarExpanded) {
                                        Column {
                                            Text(
                                                text = "R P V C",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = brandSubtitle,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                if (sidebarExpanded) {
                                    IconButton(
                                        onClick = { sidebarExpanded = false },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronLeft,
                                            contentDescription = "Collapse Sidebar",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (!sidebarExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                IconButton(
                                    onClick = { sidebarExpanded = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Expand Sidebar",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))

                            // Navigation Items
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items.forEach { item ->
                                    val isSelected = selectedIndex == item.index
                                    val containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    }
                                    val contentColor = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = containerColor,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onItemSelected(item.index) }
                                            .testTag(item.testTag)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = if (sidebarExpanded) Arrangement.Start else Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.label,
                                                tint = contentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            if (sidebarExpanded) {
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = item.label,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = contentColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Profile / Sign-out Section
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            if (userName != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = if (sidebarExpanded) Arrangement.Start else Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = userName.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                        if (sidebarExpanded) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = userName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = userRole ?: "User",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (onLogoutClick != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ExpenseRed.copy(alpha = 0.1f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onLogoutClick() }
                                        .testTag("btn_sidebar_logout")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = if (sidebarExpanded) Arrangement.Start else Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = "Logout",
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        if (sidebarExpanded) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Sign Out",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = ExpenseRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Right Main Area: TopBar + Content
                Scaffold(
                    topBar = topBar,
                    floatingActionButton = floatingActionButton ?: {}
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        content(windowInfo)
                    }
                }
            }
        }
    }
}

/**
 * Desktop & Tablet Data Table for Day Book
 * Provides a responsive multi-column spreadsheet-like ledger view on wider screens
 */
@Composable
fun ResponsiveTransactionsTable(
    transactions: List<TransactionEntry>,
    onViewReceipt: (TransactionEntry) -> Unit,
    onEditTransaction: ((TransactionEntry) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(8.dp)
        ) {
            // Table Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Date", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(85.dp))
                Text("Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(75.dp))
                Text("Party / Vendor Name", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(180.dp))
                Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(130.dp))
                Text("Site / Project", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(120.dp))
                Text("Mode", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                Text("Debit (₹)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(100.dp))
                Text("Credit (₹)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(100.dp))
                Text("Actions", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(90.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Data Rows
            transactions.forEachIndexed { index, tx ->
                val isEven = index % 2 == 0
                val rowBackground = if (isEven) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBackground, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date
                    Text(
                        text = tx.dateFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(85.dp)
                    )

                    // Type Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (tx.type == "INCOME") IncomeGreenContainer else ExpenseRedContainer,
                        modifier = Modifier.width(75.dp)
                    ) {
                        Text(
                            text = tx.type,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = if (tx.type == "INCOME") OnIncomeGreenContainer else OnExpenseRedContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }

                    // Party / Vendor Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.width(180.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (tx.partyName.isNotBlank()) tx.partyName else "N/A",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Category
                    Text(
                        text = tx.category,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(130.dp)
                    )

                    // Site
                    Text(
                        text = tx.siteName,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(120.dp)
                    )

                    // Payment Mode
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.width(70.dp)
                    ) {
                        Text(
                            text = tx.paymentMode,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    // Debit (Expense)
                    Text(
                        text = if (tx.type == "EXPENSE") formatCurrency(tx.amount) else "—",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (tx.type == "EXPENSE") FontWeight.Bold else FontWeight.Normal,
                        color = if (tx.type == "EXPENSE") ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(100.dp)
                    )

                    // Credit (Income)
                    Text(
                        text = if (tx.type == "INCOME") formatCurrency(tx.amount) else "—",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (tx.type == "INCOME") FontWeight.Bold else FontWeight.Normal,
                        color = if (tx.type == "INCOME") IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(100.dp)
                    )

                    // Actions
                    Row(
                        modifier = Modifier.width(90.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onViewReceipt(tx) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "View Details / Receipt",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (onEditTransaction != null) {
                            IconButton(
                                onClick = { onEditTransaction(tx) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Transaction",
                                    tint = SlateSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Responsive Form Container
 * Automatically constrains and centers forms on desktop/tablet screens
 */
@Composable
fun ResponsiveFormContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 840.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
        ) {
            content()
        }
    }
}
