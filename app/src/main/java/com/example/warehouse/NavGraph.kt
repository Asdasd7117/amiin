package com.example.warehouse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    // ✅ متغير لإظهار/إخفاء لوحة التشخيص
    var showDebug by remember { mutableStateOf(true) }

    if (state.user == null) {
        LoginScreen(vm)
        return
    }

    val user = state.user!!
    val start = if (state.settings.show_dash_on_login) "dash" else "records"

    // ✅ حساب القيم التشخيصية
    val roleRaw = user.role
    val roleTrimmed = user.role.trim()
    val roleLower = user.role.trim().lowercase()
    val isAdminDirect = user.role == "admin"
    val isAdminTrimmed = user.role.trim() == "admin"
    val isAdminLower = user.role.trim().lowercase() == "admin"
    val canAccess = can(user, "view_dashboard")
    val permissionsText = user.permissions.joinToString(", ")

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

                    // ✅ عرض تبويب الإدارة بناءً على عدة شروط للاختبار
                    if (user.role.trim().lowercase() == "admin")
                        MyNavItem(nav, "admin", "إدارة", Icons.Default.Settings)
                }
            }
        }
    ) { padding ->

        Column(modifier = Modifier.padding(padding)) {

            // ✅ لوحة التشخيص - تظهر في أعلى الشاشة
            if (showDebug) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2D1B4E))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            " معلومات التشخيص",
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        IconButton(onClick = { showDebug = false }) {
                            Icon(Icons.Default.Close, "إغلاق", tint = Color.White)
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    DebugRow("البريد:", user.email, Color(0xFF00FF00))
                    DebugRow("الاسم:", user.name, Color.White)
                    DebugRow("role الخام:", "'$roleRaw'", Color(0xFFFF6B6B))
                    DebugRow("role بعد trim:", "'$roleTrimmed'", Color(0xFFFFA500))
                    DebugRow("role بعد lowercase:", "'$roleLower'", Color(0xFF00BFFF))

                    Spacer(Modifier.height(4.dp))

                    DebugRow(
                        "role == admin:",
                        if (isAdminDirect) "✅ نعم" else "❌ لا",
                        if (isAdminDirect) Color(0xFF00FF00) else Color(0xFFFF0000)
                    )
                    DebugRow(
                        "trim == admin:",
                        if (isAdminTrimmed) "✅ نعم" else "❌ لا",
                        if (isAdminTrimmed) Color(0xFF00FF00) else Color(0xFFFF0000)
                    )
                    DebugRow(
                        "lowercase == admin:",
                        if (isAdminLower) "✅ نعم" else "❌ لا",
                        if (isAdminLower) Color(0xFF00FF00) else Color(0xFFFF0000)
                    )

                    Spacer(Modifier.height(4.dp))

                    DebugRow("can(view_dashboard):", if (canAccess) "✅" else "❌", Color.White)
                    DebugRow("الصلاحيات:", permissionsText, Color(0xFFDDA0DD))

                    Spacer(Modifier.height(4.dp))

                    // ✅ تنبيه إذا لم يكن مديراً
                    if (!isAdminLower) {
                        Text(
                            "⚠️ المشكلة: role ليس 'admin'!\n" +
                            "القيمة الحالية: '$roleRaw'\n" +
                            "نفذ هذا SQL:\n" +
                            "UPDATE users SET role='admin' WHERE email='${user.email}';",
                            color = Color(0xFFFF4444),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        Text(
                            "✅ الحساب مدير - يجب أن يظهر تبويب الإدارة",
                            color = Color(0xFF00FF00),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                // زر صغير لإعادة إظهار التشخيص
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2D1B4E))
                        .clickable { showDebug = true }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(" إظهار التشخيص", color = Color(0xFFFFD700), fontSize = 12.sp)
                }
            }

            // ✅ محتوى التطبيق الرئيسي
            NavHost(
                navController = nav,
                startDestination = start,
                modifier = Modifier.weight(1f)
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
}

// ✅ دالة مساعدة لعرض صفوف التشخيص
@Composable
fun DebugRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFFAAAAAA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 11.sp)
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
