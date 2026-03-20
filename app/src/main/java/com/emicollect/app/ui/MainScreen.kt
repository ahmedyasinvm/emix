package com.emicollect.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.emicollect.app.ui.analytics.AnalyticsScreen
import com.emicollect.app.ui.home.DashboardScreen
import com.emicollect.app.ui.settings.SettingsScreen
import com.emicollect.app.ui.theme.*

@Composable
fun MainScreen(
    onAddCustomerClick: () -> Unit,
    onCustomerClick: (Long) -> Unit
) {
    val navController = rememberNavController()

    val items = listOf(
        NavigationItem("Home", "home", Icons.Filled.Home, Icons.Outlined.Home),
        NavigationItem("Analytics", "analytics", Icons.Filled.PieChart, Icons.Outlined.PieChart),
        NavigationItem("Settings", "settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 0.dp,
                modifier = Modifier.height(72.dp)
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.1f else 1.0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "nav_scale"
                    )

                    NavigationBarItem(
                        icon = {
                            Box(contentAlignment = Alignment.Center) {
                                // Active indicator pill
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .width(48.dp)
                                            .height(32.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        EmeraldPrimary.copy(alpha = 0.25f),
                                                        EmeraldMuted.copy(alpha = 0.15f)
                                                    )
                                                )
                                            )
                                    )
                                }
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .scale(scale)
                                )
                            }
                        },
                        label = {
                            Text(
                                item.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = if (isSelected) 11.sp else 10.sp
                                )
                            )
                        },
                        selected = isSelected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldLight,
                            selectedTextColor = EmeraldLight,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            indicatorColor = Color.Transparent
                        ),
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                DashboardScreen(
                    onAddCustomerClick = onAddCustomerClick,
                    onCustomerClick = onCustomerClick
                )
            }
            composable("analytics") {
                AnalyticsScreen()
            }
            composable("settings") {
                SettingsScreen()
            }
        }
    }
}

data class NavigationItem(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
