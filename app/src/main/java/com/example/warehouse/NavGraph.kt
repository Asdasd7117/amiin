package com.example.warehouse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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

    val roleText = user.role
    val isAdminCheck = roleText == "admin" || roleText.trim() == "admin"

    Scaffold(
        containerColor = C.bg,
        bottomBar = {
            Surface(color = C.surface) {
                Column {
                    Text(
                        text = "Role: '$roleText' | Admin: $isAdminCheck",
                        color = if (isAdminCheck) Color(0xFF00FF00) else Color(0xFFFF0000),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A2E)).padding(6.dp)
                    )

                    NavigationBar(containerColor = C.surface) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, null) },
                            label = { Text("الرئيسية") },
                            selected = false,
                            onClick = { nav.navigate("dash") { launchSingleTop = true } }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Add, null) },
                            label = { Text("إجازة") },
                            selected = false,
                            onClick = { nav.navigate("entry") { launchSingleTop = true } }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.List, null) },
                            label = { Text("سجلاتي") },
                            selected = false,
                            onClick = { nav.navigate("records") { launchSingleTop = true } }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.PieChart, null) },
                            label = { Text("الأرصدة") },
                            selected = false,
                            onClick = { nav.navigate("balance") { launchSingleTop = true } }
                        )
                        if (isAdminCheck) {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Settings, null) },
                                label = { Text("إدارة") },
                                selected = false,
                                onClick = { nav.navigate("admin") { launchSingleTop = true } }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = start, modifier = Modifier.padding(padding)) {
            composable("dash") { DashboardScreen(state) }
            composable("entry") { EntryScreen(state, vm, onDone = { nav.navigate("records") { launchSingleTop = true } }) }
            composable("records") { RecordsScreen(state) }
            composable("balance") { BalanceScreen(state) }
            composable("admin") { AdminScreen(state, vm) }
        }
    }
}
