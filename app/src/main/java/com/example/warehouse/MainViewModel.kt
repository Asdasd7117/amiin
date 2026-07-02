package com.example.warehouse

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID

data class AppState(
    val user: User? = null,
    val employees: List<Employee> = emptyList(),
    val users: List<User> = emptyList(),
    val leaves: List<Leave> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val notifications: List<NotificationItem> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app)
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state

    // 🔥 NEW: عرض أخطاء واضحة
    private fun formatError(e: Exception): String {
        val msg = e.message ?: "UNKNOWN_ERROR"
        Log.e("MainViewModel", "Error: $msg", e)
        return when {
            msg.contains("JWT") -> "🔐 مشكلة صلاحيات السيرفر"
            msg.contains("network", true) -> "🌐 مشكلة اتصال بالإنترنت"
            msg.contains("JSON") -> "📦 خطأ في البيانات من السيرفر"
            msg.contains("duplicate") -> "⚠️ بيانات مكررة"
            msg.contains("not found", true) -> "❌ بيانات غير موجودة"
            msg.contains("invalid input syntax for type uuid") -> "⚠️ مشكلة في تنسيق المعرف"
            else -> "⚠️ خطأ: $msg"
        }
    }

    init {
        viewModelScope.launch {
            val savedId = repo.getSession()
            if (savedId != null) {
                loadAll(savedId)
            }
        }
    }

    fun loadAll(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val users = repo.getUsers()
                val user = users.firstOrNull { it.id == userId }

                if (user == null) {
                    repo.clearSession()
                    _state.value = AppState()
                    return@launch
                }

                val employees = repo.getEmployees()
                val leaves = repo.getLeaves()
                val settings = repo.getSettings()
                val notifications = repo.getNotifications(userId)

                Log.d("MainViewModel", "Loaded: ${employees.size} employees, ${users.size} users, ${leaves.size} leaves")

                _state.value = AppState(
                    user = user,
                    users = users,
                    employees = employees,
                    leaves = leaves,
                    settings = settings,
                    notifications = notifications
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(error = formatError(e))
            } finally {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    fun login(email: String, password: String, remember: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val user = repo.login(email, password)
                repo.saveSession(user.id, remember)
                loadAll(user.id)

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = formatError(e)
                )
            }
        }
    }

    fun changePassword(newPassword: String) {
        val userId = _state.value.user?.id ?: return
        viewModelScope.launch {
            try {
                repo.changePassword(userId, newPassword)
                loadAll(userId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = formatError(e))
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.clearSession()
            _state.value = AppState()
        }
    }

    fun submitLeave(
        from: String, to: String, type: String,
        halfDay: Boolean, notes: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _state.value.user ?: return
        val empId = user.staff_id

        if (empId == null) {
            onError("حساب المدير لا يرتبط بموظف")
            return
        }

        val emp = _state.value.employees.firstOrNull { it.id == empId }

        if (emp == null) {
            onError("الموظف غير موجود")
            return
        }

        val rawDays = repo.workDays(from, to)

        if (rawDays <= 0) {
            onError("التواريخ تقع في أيام عطلة")
            return
        }

        val days = repo.calcLeaveDays(from, to, halfDay)
        val bal = getBalance(emp)

        val available = when (type) {
            "annual" -> bal.annual
            "allowance" -> bal.allowance
            else -> bal.containerDays
        }

        if (days > available) {
            onError("الرصيد غير كافٍ (${available} يوم)")
            return
        }

        viewModelScope.launch {
            try {
                val leave = Leave(
                    id = UUID.randomUUID().toString(),
                    emp_id = empId,
                    employeeName = emp.name,
                    from = from,
                    to = to,
                    days = days,
                    type = type,
                    half_day = halfDay,
                    notes = notes,
                    manager_notes = "",
                    status = "pending",
                    created_at = LocalDateTime.now().toString()
                )

                repo.submitLeave(leave)
                loadAll(user.id)
                onSuccess()

            } catch (e: Exception) {
                onError(formatError(e))
            }
        }
    }

    // ✅ التحديث: إضافة موظف مع توليد UUID صحيح
    fun addEmployee(name: String, phone: String) {
        val userId = _state.value.user?.id ?: run {
            _state.value = _state.value.copy(error = "❌ المستخدم غير مسجل دخول")
            return
        }
        
        viewModelScope.launch {
            try {
                Log.d("MainViewModel", "➕ Adding employee: $name, $phone")
                
                // التحقق من البيانات
                if (name.isBlank()) {
                    _state.value = _state.value.copy(error = "❌ الاسم مطلوب")
                    return@launch
                }
                
                val newEmp = Employee(
                    id = UUID.randomUUID().toString(), // ✅ توليد UUID صحيح
                    name = name.trim(),
                    phone = phone.trim(),
                    annual = 0.0,
                    allowance = 0.0,
                    container_days = 0.0
                )
                
                // إضافة الموظف
                val result = repo.addEmployee(newEmp)
                Log.d("MainViewModel", "✅ Employee added with ID: ${result.id}")
                
                // انتظار قصير للتأكد من حفظ البيانات
                delay(1000)
                
                // إعادة تحميل البيانات
                Log.d("MainViewModel", "🔄 Reloading data...")
                loadAll(userId)
                
                // مسح رسالة الخطأ إذا نجحت العملية
                _state.value = _state.value.copy(error = null)
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "❌ Error adding employee", e)
                _state.value = _state.value.copy(error = formatError(e))
            }
        }
    }

    fun updateEmployee(emp: Employee) {
        val userId = _state.value.user?.id ?: return
        viewModelScope.launch {
            try {
                repo.updateEmployee(emp)
                delay(1000)
                loadAll(userId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = formatError(e))
            }
        }
    }

    fun deleteEmployee(id: String) {
        val userId = _state.value.user?.id ?: return
        viewModelScope.launch {
            try {
                repo.deleteEmployee(id)
                delay(1000)
                loadAll(userId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = formatError(e))
            }
        }
    }

    // ✅ التحديث: إضافة حقل phone في addUser
    fun addUser(name: String, email: String, phone: String, password: String, role: String, staffId: String?) {
        val currentUserId = _state.value.user?.id ?: return
        viewModelScope.launch {
            try {
                val salt = repo.generateSalt()
                val hash = repo.hashPassword(password, salt)

                val user = User(
                    id = UUID.randomUUID().toString(), // ✅ توليد UUID صحيح
                    name = name,
                    email = email.trim().lowercase(),
                    phone = phone.trim(),  // ✅ إضافة رقم الهاتف
                    role = role,
                    staff_id = staffId,
                    hash = hash,
                    salt = salt,
                    must_change_pass = true
                )

                repo.addUser(user)
                delay(1000)
                loadAll(currentUserId)

            } catch (e: Exception) {
                _state.value = _state.value.copy(error = formatError(e))
            }
        }
    }

    fun deleteUser(id: String) {
        val userId = _state.value.user?.id ?: return
        viewModelScope.launch {
            try {
                repo.deleteUser(id)
                delay(1000)
                loadAll(userId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = formatError(e))
            }
        }
    }

    fun reviewLeave(leaveId: String, status: String, notes: String) {
        val userId = _state.value.user?.id ?: return
        viewModelScope.launch {
            try {
                repo.updateLeaveStatus(leaveId, status, notes)

                if (status == "approved") {
                    val leave = repo.getLeaves().first { it.id == leaveId }
                    repo.deductBalance(leave.emp_id, leave.type, leave.days)

                    val empUser = _state.value.users.firstOrNull { it.staff_id == leave.emp_id }

                    if (empUser != null) {
                        repo.addNotification(
                            NotificationItem(
                                id = UUID.randomUUID().toString(), // ✅ توليد UUID صحيح
                                user_id = empUser.id,
                                title = if (status == "approved") "✅ تم قبول الإجازة" else "❌ تم رفض الإجازة",
                                message = notes.ifEmpty { "تم مراجعة طلبك" },
                                created_at = LocalDateTime.now().toString()
                            )
                        )
                    }
                }

                delay(1000)
                loadAll(userId)

            } catch (e: Exception) {
                _state.value = _state.value.copy(error = formatError(e))
            }
        }
    }
}
