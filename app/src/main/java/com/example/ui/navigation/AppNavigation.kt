package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.network.AppUpdateInfo
import com.example.data.network.UpdateChecker
import com.example.di.AppContainer
import com.example.ui.components.UpdateAvailableDialog
import com.example.ui.screens.admins.AdminsScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.bookings.BookingsScreen
import com.example.ui.screens.buses.BusesScreen
import com.example.ui.screens.counters.CountersScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.districts.DistrictsScreen
import com.example.ui.screens.fares.FaresScreen
import com.example.ui.screens.minicoaches.MiniCoachesScreen
import com.example.ui.screens.operators.OperatorsScreen
import com.example.ui.screens.routes.RoutesScreen
import com.example.ui.screens.schedules.SchedulesScreen
import com.example.ui.screens.tours.TourPackagesScreen
import com.example.ui.screens.settings.SettingsScreen

@Composable
fun AppNavigation(appContainer: AppContainer) {
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }

    // One-time check per app launch: is a newer signed APK published on GitHub Releases?
    LaunchedEffect(Unit) {
        updateInfo = UpdateChecker.checkForUpdate()
    }

    updateInfo?.let { info ->
        UpdateAvailableDialog(info = info, onDismiss = { updateInfo = null })
    }

    val isLoggedIn by appContainer.adminRepository.isLoggedIn.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = isLoggedIn

    if (!isLoggedIn) {
        LoginScreen(
            adminRepository = appContainer.adminRepository,
            // isLoggedIn flipping to true (from sessionManager) already swaps this whole
            // branch out for the Scaffold+NavHost below, whose NavHost starts at
            // Screen.Dashboard.route. Calling navController.navigate() here would crash,
            // because at this point (still in the LoginScreen branch) that NavHost hasn't
            // been composed yet, so its graph isn't set.
            onLoginSuccess = {}
        )
    } else {
        Scaffold(
            bottomBar = {
                AnimatedVisibility(visible = showBottomBar) {
                    NavigationBar {
                        BottomNavScreens.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    // Bottom-nav items are true section HOME buttons.
                                    // Every tap must return to that section's root screen,
                                    // even when the user is several screens deep. We always
                                    // clear the current section stack back to the graph start
                                    // and do NOT restore a previously saved nested state.
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = false
                                        }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.icon,
                                        contentDescription = screen.titleBn
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.bottomLabel,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                // Bottom Bar Screens
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        dashboardRepository = appContainer.dashboardRepository,
                        onNavigateTo = { route ->
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Screen.Bookings.route) {
                    BookingsScreen(
                        bookingRepository = appContainer.bookingRepository
                    )
                }

                composable(Screen.Buses.route) {
                    BusesScreen(
                        busRepository = appContainer.busRepository,
                        operatorRepository = appContainer.operatorRepository,
                        cloudinaryUploader = appContainer.cloudinaryUploader
                    )
                }

                composable(Screen.Counters.route) {
                    CountersScreen(
                        counterRepository = appContainer.counterRepository,
                        busRepository = appContainer.busRepository,
                        districtRepository = appContainer.districtRepository,
                        cloudinaryUploader = appContainer.cloudinaryUploader
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        adminRepository = appContainer.adminRepository,
                        onNavigateTo = { route ->
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        },
                        onLogout = {
                            appContainer.adminRepository.logout()
                        }
                    )
                }

                // Submodule Screens
                composable(Screen.Operators.route) {
                    OperatorsScreen(
                        operatorRepository = appContainer.operatorRepository,
                        cloudinaryUploader = appContainer.cloudinaryUploader,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Routes.route) {
                    RoutesScreen(
                        routeRepository = appContainer.routeRepository,
                        districtRepository = appContainer.districtRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Schedules.route) {
                    SchedulesScreen(
                        scheduleRepository = appContainer.scheduleRepository,
                        busRepository = appContainer.busRepository,
                        routeRepository = appContainer.routeRepository,
                        districtRepository = appContainer.districtRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Fares.route) {
                    FaresScreen(
                        fareRepository = appContainer.fareRepository,
                        busRepository = appContainer.busRepository,
                        routeRepository = appContainer.routeRepository,
                        districtRepository = appContainer.districtRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.MiniCoaches.route) {
                    MiniCoachesScreen(
                        miniCoachRepository = appContainer.miniCoachRepository,
                        cloudinaryUploader = appContainer.cloudinaryUploader,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.TourPackages.route) {
                    TourPackagesScreen(
                        tourPackageRepository = appContainer.tourPackageRepository,
                        cloudinaryUploader = appContainer.cloudinaryUploader,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Districts.route) {
                    DistrictsScreen(
                        districtRepository = appContainer.districtRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Admins.route) {
                    AdminsScreen(
                        adminRepository = appContainer.adminRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
