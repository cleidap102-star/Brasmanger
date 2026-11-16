package com.example.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatCurrency(amount: Long): String {
    val absVal = kotlin.math.abs(amount)
    val sign = if (amount < 0) "-" else ""
    return when {
        absVal >= 1_000_000_000L -> String.format(Locale("pt", "BR"), "%sR$ %.2fB", sign, absVal / 1_000_000_000.0)
        absVal >= 1_000_000L -> String.format(Locale("pt", "BR"), "%sR$ %.1fM", sign, absVal / 1_000_000.0)
        absVal >= 1_000L -> String.format(Locale("pt", "BR"), "%sR$ %d mil", sign, absVal / 1_000L)
        else -> "${sign}R$ $absVal"
    }
}

fun formatTimestampPtBr(epochMs: Long): String {
    if (epochMs <= 0L) return "Nunca sincronizado"
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    return sdf.format(Date(epochMs))
}
