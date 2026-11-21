package com.example.domain.logic

import com.example.domain.model.FORMAT_DOLLAR
import com.example.domain.model.FORMAT_PERCENT
import com.example.domain.model.FORMAT_PLAIN
import com.example.domain.model.NUMBER_FORMATS
import java.util.Locale

fun resolvePropertyFormat(kind: String, format: String?): String? {
    if (kind != "number") {
        require(format == null) { "Le format est réservé aux propriétés de type nombre" }
        return null
    }
    require(format == null || format in NUMBER_FORMATS) { "Format inconnu: $format" }
    return format ?: FORMAT_PLAIN
}

fun parseNumberInput(raw: String?): Double? {
    if (raw.isNullOrBlank()) return null
    val cleaned = raw.trim()
        .replace(" ", "")
        .replace("\u00A0", "")
        .replace("$", "")
        .replace("%", "")
        .replace(",", ".")
    return cleaned.toDoubleOrNull()?.takeIf { it.isFinite() }
}

fun formatNumberValue(value: Double, format: String?): String {
    if (!value.isFinite()) return "—"
    val body = formatDecimal(value)
    return when (format) {
        FORMAT_PERCENT -> "$body %"
        FORMAT_DOLLAR -> "\$$body"
        else -> body
    }
}

private fun formatDecimal(value: Double): String {
    if (value % 1.0 == 0.0) return value.toLong().toString()
    return String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
}
