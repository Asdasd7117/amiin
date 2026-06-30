package com.example.warehouse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
        return
    }

    val user = state.user!!
    val start = if (state.settings.show_dash_on_login) "dash" else "records"

    Scaffold(
        containerColor = C.bg,
        bottomBar = {
            Surface(color = C.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

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
        }
    ) { padding ->

        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.padding(padding)
        ) {

            composable("dash") {
                DashboardScreen(state)
            }

            composable("entry") {
                EntryScreen(
                    state,
                    vm,
                    onDone = {
                        nav.navigate("records") {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable("records") {
                RecordsScreen(state)
            }

            composable("balance") {
                BalanceScreen(state)
            }

            composable("admin") {
                AdminScreen(state, vm)
            }
        }
    }
}

@Composable
fun MyNavItem(
    nav: NavController,
    route: String,
    label: String,
    icon: ImageVector
) {

    val backStack by nav.currentBackStackEntryAsState()
    val selected = backStack?.destination?.route == route

    val color =
        if (selected) C.primary else Color.Gray

    Row(
        modifier = Modifier
            .clickable {
                nav.navigate(route) {
                    launchSingleTop = true
                }
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color
        )

        Text(
            text = label,
            color = color,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}
