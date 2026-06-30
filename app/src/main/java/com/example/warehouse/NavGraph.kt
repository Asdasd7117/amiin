package com.example.warehouse

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun MainNav() {
    val vm: MainViewModel = viewModel()
    val state by vm.state.collectAsState()
    val nav = rememberNavController()

    if (state.user == null) {
        LoginScreen(vm)
    } else {
        val user = state.user!!
        val start = if (state.settings.show_dash_on_login) "dash" else "records"

        Scaffold(
            containerColor = C.bg,
            bottomBar = {
                NavigationBar(containerColor = C.surface) {
                    if (can(user, "view_dashboard"))
                        MyNavItem(nav, "dash", "الرئيسية", Icons.Default.Home)
                    if (can(user, "request_leave"))
                        MyNavItem(nav, "entry", "إجازة", Icons.Default.Add)
                    if (can(user, "view_own_leaves"))
                        MyNavItem(nav, "records", "سجلاتي", Icons.Default.List)
                    if (can(user, "view_balances"))
                        MyNavItem(nav, "balance", "الأرصدة", Icons.Default.PieChart)
                    if (user.role == "admin")
                        MyNavItem(nav, "admin", "إدارة", Icons.Default.Settings)
                }
            }
        ) { padding ->
            NavHost(nav, start, Modifier.padding(padding)) {
                composable("dash") { DashboardScreen(state) }
                composable("entry") {
                    EntryScreen(state, vm, onDone = {
                        nav.navigate("records") { launchSingleTop = true }
                    })
                }
                composable("records") { RecordsScreen(state) }
                composable("balance") { BalanceScreen(state) }
                composable("admin") { AdminScreen(state, vm) }
            }
        }
    }
}

@Composable
fun MyNavItem(nav: NavController, route: String, label: String, icon: ImageVector) {
    val backStack by nav.currentBackStackEntryAsState()
    val isSelected = backStack?.destination?.route == route
    
    NavigationBarItem(
        selected = isSelected,
        onClick = { nav.navigate(route) { launchSingleTop = true } },
        icon = { Icon(imageVector = icon, contentDescription = label) },
        label = { Text(text = label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = C.primary,
            selectedTextColor = C.primary,
            indicatorColor = C.primary.copy(alpha = 0.15f)
        )
    )
}
