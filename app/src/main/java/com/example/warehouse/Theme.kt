package com.example.warehouse

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object C {
    val bg = Color(0xFF0F0C29)
    val surface = Color(0xFF1A1736)
    val surface2 = Color(0xFF252247)
    val border = Color(0xFF2E2A5A)
    val text = Color(0xFFE8E8F5)
    val text2 = Color(0xFFA0A0C0)
    val acc = Color(0xFF22D3EE)      // سنوي
    val p = Color(0xFFA78BFA)        // منحة
    val p2 = Color(0xFFF472B6)       // وعاء
    val war = Color(0xFFFFA000)      // معلق
    val ok = Color(0xFF10B981)       // مقبول
    val err = Color(0xFFEF4444)      // مرفوض
    val primary = Color(0xFF06B6D4)
    val wa = Color(0xFF25D366)       // واتساب
}

@Composable
fun WarehouseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.primary,
            background = C.bg,
            surface = C.surface,
            surfaceVariant = C.surface2,
            onBackground = C.text,
            onSurface = C.text,
            onPrimary = Color.White
        ),
        content = content
    )
}