package com.example.warehouse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class User(
    val id: String = UUID.randomUUID().toString(),
    val email: String = "",
    val name: String = "",
    val phone: String = "",  // ✅ إضافة حقل رقم الهاتف
    val role: String = "employee",           // admin أو employee
    val staff_id: String? = null,            // ربط بـ Employee
    val hash: String = "",
    val salt: String = "",
    val must_change_pass: Boolean = false,
    val permissions: List<String> = listOf(
        "view_dashboard", "view_own_leaves", "request_leave", "view_balances"
    )
)

@Serializable
data class Employee(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val phone: String = "",
    val annual: Double = 21.0,
    val allowance: Double = 0.0,
    val container_days: Double = 0.0
)

@Serializable
data class Leave(
    val id: String = UUID.randomUUID().toString(),
    val emp_id: String = "",
    @SerialName("employee_name") val employeeName: String = "",
    @SerialName("from_date") val from: String = "",
    @SerialName("to_date") val to: String = "",
    val days: Double = 0.0,
    val type: String = "annual",
    val half_day: Boolean = false,
    val notes: String = "",
    val status: String = "pending",
    val manager_notes: String = "",
    val created_at: String = ""
)

@Serializable
data class AppSettings(
    val id: String = "global",
    val show_dash_on_login: Boolean = true,
    val allow_half_day: Boolean = true,
    val half_day_minutes: Int = 240,
    val manager_phone: String = ""
)

@Serializable
data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val user_id: String = "",
    val title: String = "",
    val message: String = "",
    val read: Boolean = false,
    val created_at: String = ""
)

// حساب الأرصدة
data class Balance(
    val annual: Double,
    val allowance: Double,
    val containerDays: Double
)

fun getBalance(emp: Employee): Balance = Balance(
    annual = emp.annual,
    allowance = emp.allowance,
    containerDays = emp.container_days
)

// فحص الصلاحيات
fun can(user: User, permission: String): Boolean {
    if (user.role == "admin") return true
    return permission in user.permissions
}
