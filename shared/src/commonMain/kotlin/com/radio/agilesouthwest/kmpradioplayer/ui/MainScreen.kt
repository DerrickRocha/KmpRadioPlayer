package com.radio.agilesouthwest.kmpradioplayer.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.radio.agilesouthwest.kmpradioplayer.ui.navigation.RadioDestination
import com.radio.agilesouthwest.kmpradioplayer.ui.screens.FavoritesScreen
import com.radio.agilesouthwest.kmpradioplayer.ui.screens.StationsScreen
import com.radio.agilesouthwest.kmpradioplayer.ui.screens.TagsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val destinations = listOf(
        RadioDestination.Tags,
        RadioDestination.Stations(),
        RadioDestination.Favorites
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when {
                        currentDestination?.hasRoute<RadioDestination.Tags>() == true -> "Tags"
                        currentDestination?.hasRoute<RadioDestination.Stations>() == true -> "Stations"
                        currentDestination?.hasRoute<RadioDestination.Favorites>() == true -> "Favorites"
                        else -> "Radio Player"
                    }
                    Text(title)
                }
            )
        },
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.hasRoute(destination::class) } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            val route = if (destination is RadioDestination.Stations) {
                                RadioDestination.Stations()
                            } else {
                                destination
                            }
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = RadioDestination.Tags,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<RadioDestination.Tags> {
                TagsScreen(onTagClick = { tagName ->
                    navController.navigate(RadioDestination.Stations(tagName))
                })
            }
            composable<RadioDestination.Stations> { backStackEntry ->
                val stations: RadioDestination.Stations = backStackEntry.toRoute()
                StationsScreen(tagName = stations.tagName)
            }
            composable<RadioDestination.Favorites> {
                FavoritesScreen()
            }
        }
    }
}
