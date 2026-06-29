package com.example.warehouse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
                _state.value = AppState(
                    user = user,
                    users = users,
                    employees = repo.getEmployees(),
                    leaves = repo.getLeaves(),
                    settings = repo.getSettings(),
                    notifications = repo.getNotifications(userId)
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            } finally {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    // ═══════════════════════════════════════
    //  Auth
    // ═══════════════════════════════════════
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
                    error = e.message ?: "فشل تسجيل الدخول"
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
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.clearSession()
            _state.value = AppState()
        }
    }

    // ═══════════════════════════════════════
    //  Submit Leave
    // ═══════════════════════════════════════
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
                    emp_id = empId,
                    emp_name = emp.name,
                    from = from,
                    to = to,
                    days = days,
                    type = type,
                    half_day = halfDay,
                    notes = notes,
                    created_at = LocalDateTime.now().toString()
                )
                repo.submitLeave(leave)
                loadAll(user.id)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "فشل الإرسال")
            }
        }
    }

    // ═══════════════════════════════════════
    //  Admin Operations
    // ═══════════════════════════════════════
    fun addEmployee(name: String, phone: String) {
        val userId = _state.value.user?.id ?: return
        viewModelScope.launch {
            repo.addEmployee(Employee(name = name, phone = phone))
            loadAll(userId)
        }
    }

    fun updateEmployee(emp: Employee) {
        val userId = _state.value.user?.id ?: return
        viewModelScope.launch {
            repo.updateEmployee(emp)
            loadAll(userId)
        }
    }

    fun deleteEmployee(id: String) {
        val userId = _state.value.user?.id ?: return
        viewModelScope.launch {
            repo.deleteEmployee(id)
            loadAll(userId)
        }
    }

    fun addUser(name: String, email: String, password: String, role: String, staffId: String?) {
        val currentUserId = _state.value.user?.id ?: return
        viewModelScope.launch {
            try {
                val salt = repo.generateSalt()
                val hash = repo.hashPassword(password, salt)
                val user = User(
                    name = name,
                    email = email.trim().lowercase(),
                    role = role,
                    staff_id = staffId,
                    hash = hash,
                    salt = salt,
                    must_change_pass = true
                )
                repo.addUser(user)
                loadAll(currentUserId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun deleteUser(id: String) {
        val userId = _state.value.user?.id ?: return
        viewModelScope.launch {
            repo.deleteUser(id)
            loadAll(userId)
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
                    // إشعار للموظف
                    val empUser = _state.value.users.firstOrNull { it.staff_id == leave.emp_id }
                    if (empUser != null) {
                        repo.addNotification(NotificationItem(
                            user_id = empUser.id,
                            title = if (status == "approved") "✅ تم قبول الإجازة" else "❌ تم رفض الإجازة",
                            message = notes.ifEmpty { "تم مراجعة طلبك" },
                            created_at = LocalDateTime.now().toString()
                        ))
                    }
                }
                loadAll(userId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }
}