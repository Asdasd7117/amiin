package com.example.warehouse

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Base64

val Context.ds by preferencesDataStore("warehouse_prefs")

class Repository(private val ctx: Context) {

    private val client get() = SupabaseClient.client

    // ═══════════════════════════════════════
    //  أدوات كلمة المرور
    // ═══════════════════════════════════════
    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray(Charsets.UTF_8))
        val digest = md.digest(password.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    // ═══════════════════════════════════════
    //  حساب أيام العمل
    // ═══════════════════════════════════════
    fun workDays(from: String, to: String): Int {
        if (from.isEmpty() || to.isEmpty()) return 0
        val f = LocalDate.parse(from)
        val t = LocalDate.parse(to)
        if (t.isBefore(f)) return 0
        var count = 0
        var d = f
        while (!d.isAfter(t)) {
            if (d.dayOfWeek != DayOfWeek.FRIDAY) count++
            d = d.plusDays(1)
        }
        return count
    }

    fun calcLeaveDays(from: String, to: String, halfDay: Boolean): Double {
        val raw = workDays(from, to)
        if (raw <= 0) return 0.0
        return if (halfDay) raw - 0.5 else raw.toDouble()
    }

    // ═══════════════════════════════════════
    //  Session (تذكرني)
    // ═══════════════════════════════════════
    suspend fun saveSession(userId: String, remember: Boolean) {
        ctx.ds.edit {
            this[stringPreferencesKey("user_id")] = userId
            this[booleanPreferencesKey("remember")] = remember
        }
    }

    suspend fun getSession(): String? {
        val prefs = ctx.ds.data.first()
        val remember = prefs[booleanPreferencesKey("remember")] ?: return null
        if (!remember) return null
        return prefs[stringPreferencesKey("user_id")]
    }

    suspend fun clearSession() {
        ctx.ds.edit { clear() }
    }

    // ═══════════════════════════════════════
    //  Auth
    // ═══════════════════════════════════════
    suspend fun login(email: String, password: String): User {
        val user = client.postgrest.from("users")
            .select { filter { eq("email", email.trim().lowercase()) } }
            .decodeSingleOrNull<User>()
            ?: throw Exception("بيانات الدخول غير صحيحة")

        val hash = hashPassword(password, user.salt)
        if (hash != user.hash) throw Exception("بيانات الدخول غير صحيحة")

        return user
    }

    suspend fun changePassword(userId: String, newPassword: String) {
        val salt = generateSalt()
        val hash = hashPassword(newPassword, salt)
        client.postgrest.from("users").update({
            set("hash", hash)
            set("salt", salt)
            set("must_change_pass", false)
        }) { filter { eq("id", userId) } }
    }

    // ═══════════════════════════════════════
    //  Employees - ✅ الإصلاح هنا
    // ═══════════════════════════════════════
    suspend fun getEmployees(): List<Employee> {
        return try {
            client.postgrest.from("employees")
                .select()
                .decodeList<Employee>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addEmployee(emp: Employee): Employee {
        // ✅ استخدام select() بعد insert للحصول على البيانات مع ID
        val result = client.postgrest.from("employees")
            .insert(emp) {
                select()
            }
            .decodeSingle<Employee>()
        return result
    }

    suspend fun updateEmployee(emp: Employee) {
        client.postgrest.from("employees").update(emp) {
            filter { eq("id", emp.id) }
        }
    }

    suspend fun deleteEmployee(id: String) {
        client.postgrest.from("employees").delete { 
            filter { eq("id", id) } 
        }
    }

    // ═══════════════════════════════════════
    //  Users
    // ═══════════════════════════════════════
    suspend fun getUsers(): List<User> {
        return try {
            client.postgrest.from("users").select().decodeList<User>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addUser(user: User): User {
        val result = client.postgrest.from("users")
            .insert(user) {
                select()
            }
            .decodeSingle<User>()
        return result
    }

    suspend fun deleteUser(id: String) {
        client.postgrest.from("users").delete { 
            filter { eq("id", id) } 
        }
    }

    // ═══════════════════════════════════════
    //  Leaves
    // ═══════════════════════════════════════
    suspend fun getLeaves(): List<Leave> {
        return try {
            client.postgrest.from("leaves").select().decodeList<Leave>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun submitLeave(leave: Leave): Leave {
        val result = client.postgrest.from("leaves")
            .insert(leave) {
                select()
            }
            .decodeSingle<Leave>()
        return result
    }

    suspend fun updateLeaveStatus(id: String, status: String, notes: String) {
        client.postgrest.from("leaves").update({
            set("status", status)
            set("manager_notes", notes)
        }) { filter { eq("id", id) } }
    }

    suspend fun deductBalance(empId: String, type: String, days: Double) {
        val emp = getEmployees().firstOrNull { it.id == empId } ?: return
        when (type) {
            "annual" -> client.postgrest.from("employees").update({
                set("annual", emp.annual - days)
            }) { filter { eq("id", empId) } }
            "allowance" -> client.postgrest.from("employees").update({
                set("allowance", emp.allowance - days)
            }) { filter { eq("id", empId) } }
            "container" -> client.postgrest.from("employees").update({
                set("container_days", emp.container_days - days)
            }) { filter { eq("id", empId) } }
        }
    }

    // ═══════════════════════════════════════
    //  Settings
    // ═══════════════════════════════════════
    suspend fun getSettings(): AppSettings {
        return try {
            client.postgrest.from("settings").select().decodeSingleOrNull<AppSettings>()
                ?: AppSettings()
        } catch (e: Exception) {
            AppSettings()
        }
    }

    // ═══════════════════════════════════════
    //  Notifications
    // ═══════════════════════════════════════
    suspend fun addNotification(n: NotificationItem) {
        client.postgrest.from("notifications").insert(n)
    }

    suspend fun getNotifications(userId: String): List<NotificationItem> {
        return try {
            client.postgrest.from("notifications")
                .select { filter { eq("user_id", userId) } }
                .decodeList<NotificationItem>()
                .sortedByDescending { it.created_at }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
