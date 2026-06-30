package com.example.warehouse

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Base64

val Context.ds by preferencesDataStore("warehouse_prefs")

class Repository(private val ctx: Context) {

    private val client get() = SupabaseClient.client

    // ---------------- SECURITY ----------------

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

    // ---------------- LOGIN (FIXED) ----------------
    suspend fun login(email: String, password: String): User {

        val cleanEmail = email.trim().lowercase()

        val user = client.postgrest
            .from("users")
            .select()
            .decodeList<User>()
            .firstOrNull {
                it.email.trim().lowercase() == cleanEmail
            } ?: throw Exception("بيانات الدخول غير صحيحة")

        val hash = hashPassword(password, user.salt)

        if (hash != user.hash) {
            throw Exception("كلمة المرور غير صحيحة")
        }

        return user
    }

    // ---------------- SESSION ----------------

    suspend fun saveSession(userId: String, remember: Boolean) {
        ctx.ds.edit {
            it[stringPreferencesKey("user_id")] = userId
            it[booleanPreferencesKey("remember")] = remember
        }
    }

    suspend fun getSession(): String? {
        val prefs = ctx.ds.data.first()
        val remember = prefs[booleanPreferencesKey("remember")] ?: return null
        if (!remember) return null
        return prefs[stringPreferencesKey("user_id")]
    }

    suspend fun clearSession() {
        ctx.ds.edit { it.clear() }
    }

    // ---------------- EMPLOYEES ----------------

    suspend fun getEmployees(): List<Employee> =
        client.postgrest.from("employees").select().decodeList<Employee>()

    suspend fun addEmployee(emp: Employee) {
        client.postgrest.from("employees").insert(emp)
    }

    suspend fun updateEmployee(emp: Employee) {
        client.postgrest.from("employees").update(emp) {
            filter { eq("id", emp.id) }
        }
    }

    suspend fun deleteEmployee(id: String) {
        client.postgrest.from("employees").delete { filter { eq("id", id) } }
    }

    // ---------------- USERS ----------------

    suspend fun getUsers(): List<User> =
        client.postgrest.from("users").select().decodeList<User>()

    suspend fun addUser(user: User) {
        client.postgrest.from("users").insert(user)
    }

    suspend fun deleteUser(id: String) {
        client.postgrest.from("users").delete { filter { eq("id", id) } }
    }

    // ---------------- LEAVES ----------------

    suspend fun getLeaves(): List<Leave> =
        client.postgrest.from("leaves").select().decodeList<Leave>()

    suspend fun submitLeave(leave: Leave) {
        client.postgrest.from("leaves").insert(leave)
    }

    suspend fun updateLeaveStatus(id: String, status: String, notes: String) {
        client.postgrest.from("leaves").update({
            buildJsonObject {
                put("status", status)
                put("manager_notes", notes)
            }
        }) {
            filter { eq("id", id) }
        }
    }

    // ---------------- BALANCE ----------------

    suspend fun deductBalance(empId: String, type: String, days: Double) {
        val emp = getEmployees().firstOrNull { it.id == empId } ?: return

        val newValue = when (type) {
            "annual" -> emp.annual - days
            "allowance" -> emp.allowance - days
            "container" -> emp.container_days - days
            else -> return
        }

        val colName = when (type) {
            "annual" -> "annual"
            "allowance" -> "allowance"
            else -> "container_days"
        }

        client.postgrest.from("employees").update({
            buildJsonObject { put(colName, newValue) }
        }) {
            filter { eq("id", empId) }
        }
    }

    // ---------------- SETTINGS ----------------

    suspend fun getSettings(): AppSettings =
        client.postgrest.from("settings")
            .select()
            .decodeSingleOrNull<AppSettings>() ?: AppSettings()

    // ---------------- NOTIFICATIONS ----------------

    suspend fun addNotification(n: NotificationItem) {
        client.postgrest.from("notifications").insert(n)
    }

    suspend fun getNotifications(userId: String): List<NotificationItem> =
        client.postgrest.from("notifications")
            .select { filter { eq("user_id", userId) } }
            .decodeList<NotificationItem>()
            .sortedByDescending { it.created_at }
}
