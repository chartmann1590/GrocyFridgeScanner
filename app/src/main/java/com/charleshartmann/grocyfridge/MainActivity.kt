package com.charleshartmann.grocyfridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.charleshartmann.grocyfridge.ui.GrocyFridgeViewModel
import com.charleshartmann.grocyfridge.ui.screens.HistoryScreen
import com.charleshartmann.grocyfridge.ui.screens.HomeScreen
import com.charleshartmann.grocyfridge.ui.screens.OnboardingScreen
import com.charleshartmann.grocyfridge.ui.screens.SettingsScreen
import com.charleshartmann.grocyfridge.ui.theme.GrocyFridgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GrocyFridgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GrocyFridgeApp()
                }
            }
        }
    }
}

private data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun GrocyFridgeApp(viewModel: GrocyFridgeViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsState()
    val modelState by viewModel.modelState.collectAsState()
    val onboardingComplete by viewModel.onboardingComplete.collectAsState()

    val needsOnboarding = !onboardingComplete

    AnimatedContent(
        targetState = needsOnboarding,
        label = "main_route"
    ) { showOnboarding ->
        if (showOnboarding) {
            OnboardingScreen(
                isGrocyConfigured = settings.isComplete,
                modelState = modelState,
                modelSizeBytes = viewModel.modelSizeBytes,
                connectionTest = viewModel.connectionTest.collectAsState().value,
                onSaveGrocy = { viewModel.saveSettings(it) },
                onTestConnection = { url, key -> viewModel.testConnection(url, key) },
                onDownloadModel = { viewModel.downloadModel() },
                onFinish = { viewModel.completeOnboarding() },
                onSkip = { viewModel.completeOnboarding() }
            )
        } else {
            MainNavigation(viewModel)
        }
    }
}

@Composable
private fun MainNavigation(viewModel: GrocyFridgeViewModel) {
    val navController = rememberNavController()
    val navItems = listOf(
        NavItem("home", "Scanner", Icons.Filled.Home, Icons.Outlined.Home),
        NavItem("history", "History", Icons.Filled.History, Icons.Outlined.History),
        NavItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                navItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = {
                            Text(
                                item.label,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                HomeScreen(viewModel = viewModel)
            }
            composable("history") {
                HistoryScreen(viewModel = viewModel)
            }
            composable("settings") {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
