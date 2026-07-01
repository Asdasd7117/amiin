package com.example.warehouse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val roleText = user.role
    val isAdminCheck = roleText == "admin" || roleText.trim() == "admin"

    Scaffold(
        containerColor = C.bg,
        bottomBar = {
            Surface(color = C.surface) {
                Column {
                    // شريط التشخيص
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1A1A2E))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Role: '$roleText' | Admin: $isAdminCheck",
                            color = if (isAdminCheck) Color(0xFF00FF00) else Color(0xFFFF0000),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // ✅ الحل: بناء قائمة العناصر بشكل ديناميكي
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MyNavItem(nav, "dash", "الرئيسية", Icons.Default.Home)
                        MyNavItem(nav, "entry", "إجازة", Icons.Default.Add)
                        MyNavItem(nav, "records", "سجلاتي", Icons.Default.List)
                        MyNavItem(nav, "balance", "الأرصدة", Icons.Default.PieChart)
                        
                        // ✅ إضافة تبويب الإدارة مباشرة بدون if
                        if (isAdminCheck) {
                            MyNavItem(nav, "admin", "إدارة", Icons.Default.Settings)
                        }
                    }
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
    val color = if (selected) C.primary else Color.Gray

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
