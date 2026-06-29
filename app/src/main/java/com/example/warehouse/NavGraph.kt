package com.example.warehouse

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
                        NavItem(nav, "dash", "الرئيسية", Icons.Default.Home)
                    if (can(user, "request_leave"))
                        NavItem(nav, "entry", "إجازة", Icons.Default.Add)
                    if (can(user, "view_own_leaves"))
                        NavItem(nav, "records", "سجلاتي", Icons.Default.List)
                    if (can(user, "view_balances"))
                        NavItem(nav, "balance", "الأرصدة", Icons.Default.PieChart)
                    if (user.role == "admin")
                        NavItem(nav, "admin", "إدارة", Icons.Default.Settings)
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
fun NavItem(nav: NavController, route: String, label: String, icon: ImageVector) {
    val navBackStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    NavigationBarItem(
        selected = currentRoute == route,
        onClick = { nav.navigate(route) { launchSingleTop = true } },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = C.primary,
            selectedTextColor = C.primary,
            indicatorColor = C.primary.copy(alpha = 0.15f)
        )
    )
}
