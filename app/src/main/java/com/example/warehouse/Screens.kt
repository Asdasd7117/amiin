@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.warehouse

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Composable
fun LoginScreen(vm: MainViewModel) {
    val state by vm.state.collectAsState()
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var remember by remember { mutableStateOf(true) }

    Box(
        Modifier.fillMaxSize().background(C.bg).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = C.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                Modifier.padding(24.dp).widthIn(max = 400.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🏭", fontSize = 48.sp)
                Text("إجازات المخازن", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = C.text)
                Text("v5.1 — سجّل دخولك للمتابعة", color = C.text2, fontSize = 12.sp)

                Spacer(Modifier.height(20.dp))

                if (state.error != null) {
                    Text(state.error!!, color = C.err, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("كلمة المرور") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(remember, { remember = it }, colors = CheckboxDefaults.colors(checkedColor = C.primary))
                    Text("تذكرني على هذا الجهاز", color = C.text2, fontSize = 13.sp)
                }

                Button(
                    onClick = { vm.login(email, pass, remember) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !state.loading,
                    colors = ButtonDefaults.buttonColors(containerColor = C.primary)
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("🔓 دخول آمن", fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(state: AppState) {
    val user = state.user ?: return
    val emp = state.employees.firstOrNull { it.id == user.staff_id }
    val bal = emp?.let { getBalance(it) }

    val pendCount = state.leaves.count { it.status == "pending" }
    val okCount = state.leaves.count { it.status == "approved" }
    val upcoming = state.leaves.count {
        it.status == "approved" && runCatching { LocalDate.parse(it.from) >= LocalDate.now() }.getOrDefault(false)
    }

    LazyColumn(
        Modifier.fillMaxSize().background(C.bg).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = C.surface)) {
                Column(Modifier.padding(16.dp)) {
                    Text("مرحباً بك 👋", color = C.text2)
                    Text(user.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = C.text)
                    Text(
                        if (user.role == "admin") "🛡 مدير النظام" else "👤 موظف",
                        color = C.text2
                    )
                    if (emp != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatChip("${bal!!.annual}", "سنوي", C.acc, Modifier.weight(1f))
                            StatChip("${bal.allowance}", "منحة", C.p, Modifier.weight(1f))
                            StatChip("${bal.containerDays}", "وعاء", C.p2, Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("$pendCount", "معلقة", C.war, Modifier.weight(1f))
                StatBox("$okCount", "مقبولة", C.ok, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("${state.employees.size}", "الموظفين", C.p, Modifier.weight(1f))
                StatBox("$upcoming", "قادمة", C.p2, Modifier.weight(1f))
            }
        }

        item {
            Text("📅 الأيام المحجوزة (14 يوم قادم)", color = C.text2, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            CalendarStrip(state.leaves)
        }

        item { Text("⏰ آخر الإجازات المعلقة", color = C.text2, fontSize = 13.sp) }
        val pend = state.leaves.filter { it.status == "pending" }.take(5)
        if (pend.isEmpty()) {
            item { EmptyBox("لا توجد طلبات معلقة") }
        } else {
            items(pend) { LeaveCard(it) }
        }

        item { Text("✅ آخر الإجازات المقبولة", color = C.text2, fontSize = 13.sp) }
        val ok = state.leaves.filter { it.status == "approved" }.take(5)
        if (ok.isEmpty()) {
            item { EmptyBox("لا توجد إجازات مقبولة") }
        } else {
            items(ok) { LeaveCard(it) }
        }
    }
}

@Composable
fun EntryScreen(state: AppState, vm: MainViewModel, onDone: () -> Unit) {
    val user = state.user ?: return
    val emp = state.employees.firstOrNull { it.id == user.staff_id }
    val ctx = LocalContext.current

    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("annual") }
    var halfDay by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    var daysText by remember { mutableStateOf("") }

    LaunchedEffect(from, to, halfDay) {
        if (from.isNotEmpty() && to.isNotEmpty()) {
            val repo = Repository(ctx)
            val raw = repo.workDays(from, to)
            val real = if (halfDay) repo.calcLeaveDays(from, to, true) else raw.toDouble()
            daysText = if (real > 0) {
                if (halfDay) "$real يوم (نصف يوم مخصوم)" else "$real أيام عمل فعلية"
            } else ""
        }
    }

    fun pickDate(onPicked: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(ctx, { _, y, m, d ->
            onPicked("%04d-%02d-%02d".format(y, m + 1, d))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    Column(
        Modifier.fillMaxSize().background(C.bg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("📅 طلب إجازة جديد", fontSize = 18.sp, color = C.text)
        Text("تقديم لـ: ${user.name}", color = C.text2, fontSize = 13.sp)

        if (err.isNotEmpty()) {
            Text(err, color = C.err, fontSize = 13.sp)
        }

        if (emp != null) {
            val bal = getBalance(emp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("${bal.annual}", "سنوي", C.acc, Modifier.weight(1f))
                StatChip("${bal.allowance}", "منحة", C.p, Modifier.weight(1f))
                StatChip("${bal.containerDays}", "وعاء", C.p2, Modifier.weight(1f))
            }
        }

        OutlinedButton(onClick = { pickDate { from = it } }, Modifier.fillMaxWidth()) {
            Text(if (from.isEmpty()) "📅 تاريخ البداية" else from)
        }
        OutlinedButton(onClick = { pickDate { to = it } }, Modifier.fillMaxWidth()) {
            Text(if (to.isEmpty()) "📅 تاريخ النهاية" else to)
        }

        if (state.settings.allow_half_day) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🕐 نصف يوم فقط", Modifier.weight(1f), color = C.text)
                Switch(checked = halfDay, onCheckedChange = { halfDay = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = C.primary))
            }
        }

        OutlinedTextField(
            value = daysText, onValueChange = {}, readOnly = true,
            label = { Text("الأيام الفعلية") }, modifier = Modifier.fillMaxWidth()
        )

        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded, { expanded = it }) {
            OutlinedTextField(
                value = when (type) {
                    "annual" -> "الرصيد السنوي (21 يوم)"
                    "allowance" -> "المنحة الشهرية (2 يوم/شهر)"
                    else -> "الوعاء التراكمي"
                },
                onValueChange = {}, readOnly = true,
                label = { Text("نوع الرصيد") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded, { expanded = false }) {
                DropdownMenuItem({ Text("الرصيد السنوي") }, onClick = { type = "annual"; expanded = false })
                DropdownMenuItem({ Text("المنحة الشهرية") }, onClick = { type = "allowance"; expanded = false })
                DropdownMenuItem({ Text("الوعاء التراكمي") }, onClick = { type = "container"; expanded = false })
            }
        }

        OutlinedTextField(
            value = notes, onValueChange = { notes = it },
            label = { Text("ملاحظات") }, modifier = Modifier.fillMaxWidth(), minLines = 2
        )

        Button(
            onClick = {
                vm.submitLeave(from, to, type, halfDay, notes,
                    onSuccess = {
                        from = ""; to = ""; notes = ""; halfDay = false; daysText = ""
                        onDone()
                    },
                    onError = { err = it }
                )
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = C.primary)
        ) { Text("📤 إرسال الطلب للمراجعة") }
    }
}

@Composable
fun RecordsScreen(state: AppState) {
    val user = state.user ?: return
    val myLeaves = state.leaves.filter { it.emp_id == user.staff_id }

    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val filtered = myLeaves.filter {
        (query.isEmpty() || it.notes.contains(query, true) ||
                it.from.contains(query) || it.to.contains(query)) &&
                (statusFilter.isEmpty() || it.status == statusFilter)
    }

    Column(Modifier.fillMaxSize().background(C.bg).padding(16.dp)) {
        Text("📋 سجلاتي", fontSize = 18.sp, color = C.text)
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("🔍 بحث...") },
                modifier = Modifier.weight(1f), singleLine = true
            )
            ExposedDropdownMenuBox(expanded, { expanded = it }) {
                OutlinedTextField(
                    value = when (statusFilter) {
                        "pending" -> "معلق"
                        "approved" -> "مقبول"
                        "rejected" -> "مرفوض"
                        else -> "الكل"
                    },
                    onValueChange = {}, readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().width(110.dp)
                )
                ExposedDropdownMenu(expanded, { expanded = false }) {
                    DropdownMenuItem({ Text("الكل") }, onClick = { statusFilter = ""; expanded = false })
                    DropdownMenuItem({ Text("معلق") }, onClick = { statusFilter = "pending"; expanded = false })
                    DropdownMenuItem({ Text("مقبول") }, onClick = { statusFilter = "approved"; expanded = false })
                    DropdownMenuItem({ Text("مرفوض") }, onClick = { statusFilter = "rejected"; expanded = false })
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("${filtered.size} طلب", fontSize = 12.sp, color = C.text2)
        Spacer(Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            EmptyBox("لا توجد نتائج")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered) { LeaveCard(it) }
            }
        }
    }
}

@Composable
fun BalanceScreen(state: AppState) {
    LazyColumn(
        Modifier.fillMaxSize().background(C.bg).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("📊 أرصدة الموظفين", fontSize = 18.sp, color = C.text) }

        items(state.employees) { emp ->
            Card(colors = CardDefaults.cardColors(containerColor = C.surface)) {
                Column(Modifier.padding(14.dp)) {
                    Text(emp.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = C.text)
                    if (emp.phone.isNotEmpty()) {
                        Text("📱 ${emp.phone}", fontSize = 12.sp, color = C.text2)
                    }
                    Spacer(Modifier.height(10.dp))
                    val bal = getBalance(emp)
                    BalanceRow("الرصيد السنوي", "${bal.annual} يوم", C.acc)
                    BalanceRow("المنحة الشهرية", "${bal.allowance} يوم", C.p)
                    BalanceRow("الوعاء التراكمي", "${bal.containerDays} يوم", C.p2)
                }
            }
        }
    }
}

@Composable
fun AdminScreen(state: AppState, vm: MainViewModel) {
    val ctx = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    var showAddEmp by remember { mutableStateOf(false) }
    var showAddUser by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = C.bg,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingActionButton(
                    onClick = { 
                        val userId = state.user?.id ?: return@FloatingActionButton
                        vm.loadAll(userId)
                        Toast.makeText(ctx, "🔄 تم تحديث البيانات", Toast.LENGTH_SHORT).show()
                    },
                    containerColor = C.acc,
                    modifier = Modifier.size(50.dp)
                ) { Icon(Icons.Default.Refresh, "تحديث") }
                
                Spacer(Modifier.height(10.dp))
                
                FloatingActionButton(
                    onClick = { if (tab == 0) showAddEmp = true else showAddUser = true },
                    containerColor = C.primary
                ) { Icon(Icons.Default.Add, null) }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(C.err.copy(alpha = 0.2f))
                        .padding(12.dp)
                ) {
                    Text("❌ ${state.error}", color = C.err, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(C.surface)
                    .padding(8.dp)
            ) {
                Text(
                    "📊 عدد الموظفين: ${state.employees.size} | عدد المستخدمين: ${state.users.size}",
                    color = C.text2,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            TabRow(tab, containerColor = C.surface) {
                Tab(tab == 0, onClick = { tab = 0 }) { Text("الموظفون", Modifier.padding(14.dp)) }
                Tab(tab == 1, onClick = { tab = 1 }) { Text("المستخدمون", Modifier.padding(14.dp)) }
                Tab(tab == 2, onClick = { tab = 2 }) { Text("الطلبات", Modifier.padding(14.dp)) }
                Tab(tab == 3, onClick = { tab = 3 }) { Text("القادمة", Modifier.padding(14.dp)) }
            }

            when (tab) {
                0 -> EmployeesTab(state.employees, vm)
                1 -> UsersTab(state.users, state.employees, vm)
                2 -> LeavesReviewTab(state.leaves.filter { it.status == "pending" }, vm)
                3 -> UpcomingTab(state.leaves)
            }
        }
    }

    if (showAddEmp) {
        AddEmployeeDialog(
            onDismiss = { showAddEmp = false },
            onAdd = { name, phone ->
                vm.addEmployee(name, phone)
                showAddEmp = false
                Toast.makeText(ctx, "✅ تم إضافة الموظف: $name", Toast.LENGTH_SHORT).show()
            }
        )
    }
    if (showAddUser) {
        AddUserDialog(
            emps = state.employees,
            onDismiss = { showAddUser = false },
            onAdd = { name, email, pass, role, staffId ->
                vm.addUser(name, email, pass, role, staffId)
                showAddUser = false
                Toast.makeText(ctx, "✅ تم إضافة المستخدم: $name", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun EmployeesTab(emps: List<Employee>, vm: MainViewModel) {
    if (emps.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📭 لا يوجد موظفون", color = C.text2, fontSize = 16.sp)
                Text("اضغط + لإضافة موظف جديد", color = C.text2, fontSize = 12.sp)
            }
        }
        return
    }
    
    LazyColumn(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(emps) { emp ->
            var showEdit by remember { mutableStateOf(false) }
            Card(colors = CardDefaults.cardColors(containerColor = C.surface)) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(emp.name, fontWeight = FontWeight.Bold, color = C.text)
                        if (emp.phone.isNotEmpty()) Text("📱 ${emp.phone}", fontSize = 12.sp, color = C.text2)
                        Text("سنوي: ${emp.annual} | منحة: ${emp.allowance} | وعاء: ${emp.container_days}",
                            fontSize = 11.sp, color = C.acc)
                    }
                    IconButton({ showEdit = true }) { Icon(Icons.Default.Edit, null, tint = C.acc) }
                    IconButton({ vm.deleteEmployee(emp.id) }) {
                        Icon(Icons.Default.Delete, null, tint = C.err)
                    }
                }
            }
            if (showEdit) {
                EditEmployeeDialog(emp, { showEdit = false }, { vm.updateEmployee(it); showEdit = false })
            }
        }
    }
}

@Composable
fun UsersTab(users: List<User>, emps: List<Employee>, vm: MainViewModel) {
    val ctx = LocalContext.current
    LazyColumn(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(users) { u ->
            Card(colors = CardDefaults.cardColors(containerColor = C.surface)) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(u.name, fontWeight = FontWeight.Bold, color = C.text)
                            Text(u.email, fontSize = 12.sp, color = C.text2)
                            Text("🛡 ${u.role}", fontSize = 11.sp,
                                color = if (u.role == "admin") C.p else C.acc)
                        }
                        IconButton({ vm.deleteUser(u.id) }) {
                            Icon(Icons.Default.Delete, null, tint = C.err)
                        }
                    }
                    val emp = emps.firstOrNull { it.id == u.staff_id }
                    if (emp != null && emp.phone.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { sendCredentialsWA(emp.phone, u.email, emp.name, ctx) },
                            colors = ButtonDefaults.buttonColors(containerColor = C.wa),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("💬 إرسال بيانات الدخول عبر واتساب")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeavesReviewTab(leaves: List<Leave>, vm: MainViewModel) {
    if (leaves.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("✅ لا توجد طلبات معلقة", color = C.text2)
        }
        return
    }
    LazyColumn(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(leaves) { leave ->
            var showReview by remember { mutableStateOf(false) }
            LeaveCard(leave) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = { showReview = true },
                    colors = ButtonDefaults.buttonColors(containerColor = C.primary)) {
                    Text("📝 مراجعة")
                }
            }
            if (showReview) ReviewDialog(leave, { showReview = false },
                { status, notes -> vm.reviewLeave(leave.id, status, notes); showReview = false })
        }
    }
}

@Composable
fun UpcomingTab(leaves: List<Leave>) {
    val today = LocalDate.now().toString()
    val upcoming = leaves.filter { it.status == "approved" && it.from >= today }.sortedBy { it.from }

    LazyColumn(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("📅 الإجازات القادمة (${upcoming.size})", color = C.text2, fontSize = 13.sp) }
        if (upcoming.isEmpty()) item { EmptyBox("لا توجد إجازات قادمة") }
        else items(upcoming) { LeaveCard(it) }
    }
}

@Composable
fun AddEmployeeDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = C.surface,
        title = { Text("➕ إضافة موظف", color = C.text) },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("الاسم") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(phone, { phone = it }, label = { Text("الهاتف") })
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(name, phone) }, colors = ButtonDefaults.buttonColors(containerColor = C.primary)) {
                Text("إضافة")
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } })
}

@Composable
fun EditEmployeeDialog(emp: Employee, onDismiss: () -> Unit, onSave: (Employee) -> Unit) {
    var name by remember { mutableStateOf(emp.name) }
    var phone by remember { mutableStateOf(emp.phone) }
    var annual by remember { mutableStateOf(emp.annual.toString()) }
    var allowance by remember { mutableStateOf(emp.allowance.toString()) }
    var cntDays by remember { mutableStateOf(emp.container_days.toString()) }

    AlertDialog(onDismissRequest = onDismiss, containerColor = C.surface,
        title = { Text("✏️ تعديل: ${emp.name}", color = C.text) },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("الاسم") })
                OutlinedTextField(phone, { phone = it }, label = { Text("الهاتف") })
                OutlinedTextField(annual, { annual = it }, label = { Text("الرصيد السنوي") })
                OutlinedTextField(allowance, { allowance = it }, label = { Text("المنحة") })
                OutlinedTextField(cntDays, { cntDays = it }, label = { Text("الوعاء (أيام)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(emp.copy(name = name, phone = phone,
                    annual = annual.toDoubleOrNull() ?: emp.annual,
                    allowance = allowance.toDoubleOrNull() ?: emp.allowance,
                    container_days = cntDays.toDoubleOrNull() ?: emp.container_days))
            }, colors = ButtonDefaults.buttonColors(containerColor = C.primary)) { Text("حفظ") }
        },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } })
}

@Composable
fun AddUserDialog(emps: List<Employee>, onDismiss: () -> Unit,
                  onAdd: (String, String, String, String, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("employee") }
    var staffId by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(onDismissRequest = onDismiss, containerColor = C.surface,
        title = { Text("➕ إضافة مستخدم", color = C.text) },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("الاسم") })
                OutlinedTextField(email, { email = it }, label = { Text("البريد") })
                OutlinedTextField(pass, { pass = it }, label = { Text("كلمة المرور") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(role == "employee", { role = "employee" }); Text("موظف")
                    RadioButton(role == "admin", { role = "admin" }); Text("مدير")
                }
                if (role == "employee") {
                    ExposedDropdownMenuBox(expanded, { expanded = it }) {
                        OutlinedTextField(value = staffId?.let { id -> emps.firstOrNull { it.id == id }?.name } ?: "اختر موظف",
                            onValueChange = {}, readOnly = true, label = { Text("ربط بـ") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded, { expanded = false }) {
                            emps.forEach { e ->
                                DropdownMenuItem({ Text(e.name) }, onClick = { staffId = e.id; expanded = false })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(name, email, pass, role, staffId) },
                colors = ButtonDefaults.buttonColors(containerColor = C.primary)) { Text("إضافة") }
        },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } })
}

@Composable
fun ReviewDialog(leave: Leave, onDismiss: () -> Unit, onReview: (String, String) -> Unit) {
    var notes by remember { mutableStateOf(leave.manager_notes) }

    AlertDialog(onDismissRequest = onDismiss, containerColor = C.surface,
        title = { Text("📝 مراجعة طلب ${leave.employeeName}", color = C.text) },
        text = {
            Column {
                Text("📅 ${leave.from} → ${leave.to}", color = C.text2)
                Text("⏱ ${leave.days} يوم - ${leave.type}", color = C.text2)
                if (leave.notes.isNotEmpty()) Text("📝 ${leave.notes}", color = C.text2)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات المدير") }, minLines = 2)
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { onReview("approved", notes) },
                    colors = ButtonDefaults.buttonColors(containerColor = C.ok)) { Text("✅ قبول") }
                Button(onClick = { onReview("rejected", notes) },
                    colors = ButtonDefaults.buttonColors(containerColor = C.err)) { Text("❌ رفض") }
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } })
}

@Composable
fun StatBox(num: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(80.dp).clip(RoundedCornerShape(14.dp)).background(C.surface).padding(10.dp),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(num, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = C.text2)
        }
    }
}

@Composable
fun StatChip(num: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(60.dp).clip(RoundedCornerShape(12.dp)).background(C.surface2).padding(8.dp),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(num, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 10.sp, color = C.text2)
        }
    }
}

@Composable
fun EmptyBox(msg: String) {
    Box(Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(12.dp)).background(C.surface),
        contentAlignment = Alignment.Center) {
        Text(msg, color = C.text2, fontSize = 13.sp)
    }
}

@Composable
fun BalanceRow(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = C.text2)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun LeaveCard(leave: Leave, actions: @Composable (() -> Unit)? = null) {
    val (statusText, statusColor) = when (leave.status) {
        "approved" -> "✅ مقبول" to C.ok
        "rejected" -> "❌ مرفوض" to C.err
        else -> "⏰ معلق" to C.war
    }

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.surface).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(leave.employeeName, fontWeight = FontWeight.Bold, color = C.text)
                Text("${leave.from} → ${leave.to}", fontSize = 12.sp, color = C.text2)
            }
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(statusColor.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(statusText, color = statusColor, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("📅 ${leave.days} يوم", fontSize = 12.sp, color = C.text2)
            Text("🏷 ${leave.type}", fontSize = 12.sp, color = C.text2)
            if (leave.half_day) Text("🕐 نصف يوم", fontSize = 12.sp, color = C.p)
        }
        if (leave.notes.isNotEmpty()) Text("📝 ${leave.notes}", fontSize = 12.sp, color = C.text2)
        if (leave.manager_notes.isNotEmpty()) Text("💬 ${leave.manager_notes}", fontSize = 12.sp, color = C.acc)
        actions?.invoke()
    }
}

@Composable
fun CalendarStrip(leaves: List<Leave>) {
    val today = LocalDate.now()
    val daysAr = listOf("أحد","إثن","ثلا","أرب","خمي","جمع","سبت")

    Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 0 until 14) {
            val d = today.plusDays(i.toLong())
            val ds = d.toString()
            val isFri = d.dayOfWeek == DayOfWeek.FRIDAY
            val isToday = i == 0
            val bookedLeave = if (!isFri) leaves.firstOrNull { it.status != "rejected" && ds >= it.from && ds <= it.to } else null
            val bgColor = when {
                isToday -> C.primary
                isFri -> C.surface2
                bookedLeave != null -> C.ok.copy(alpha = 0.25f)
                else -> C.surface
            }
            Column(modifier = Modifier.width(52.dp).clip(RoundedCornerShape(12.dp)).background(bgColor).padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(daysAr[d.dayOfWeek.value % 7], fontSize = 10.sp, color = if (isToday) Color.White else C.text2)
                Text("${d.dayOfMonth}", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = if (isToday) Color.White else C.text)
                Text(bookedLeave?.employeeName?.split(" ")?.firstOrNull() ?: "", fontSize = 9.sp, color = C.ok, maxLines = 1)
            }
        }
    }
}

fun sendCredentialsWA(phone: String, email: String, name: String, ctx: android.content.Context) {
    val msg = "🏭 إجازات المخازن\n\nمرحباً $name،\nتم إنشاء حسابك بنجاح ✅\n\n📧 البريد: $email\n🔑 كلمة المرور: (اسأل المدير)\n\n⚠️ عند أول دخول ستُجبر على تغيير كلمة المرور."
    val cleanPhone = phone.replace(Regex("[^0-9]"), "").let { if (it.startsWith("0")) "20$it" else it }
    val url = "https://wa.me/$cleanPhone?text=${Uri.encode(msg)}"
    val intent = Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) }
    try { ctx.startActivity(intent) } catch (e: Exception) { Toast.makeText(ctx, "واتساب غير مثبت", Toast.LENGTH_SHORT).show() }
}
