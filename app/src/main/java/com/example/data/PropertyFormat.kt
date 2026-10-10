package com.example.data

/**
 * Sous-format des propriétés de type `number`. Le type reste `number` : seul l'affichage change.
 * `null` équivaut à [FORMAT_PLAIN] (comportement historique avant la migration 2 -> 3).
 */
const val FORMAT_PLAIN = "plain"
const val FORMAT_PERCENT = "percent"
const val FORMAT_DOLLAR = "dollar"

/** Valeurs acceptées, utilisées par la validation et par les sélecteurs d'interface. */
val NUMBER_FORMATS = listOf(FORMAT_PLAIN, FORMAT_PERCENT, FORMAT_DOLLAR)

/**
 * Valide et normalise le sous-format demandé pour une propriété.
 *
 * - `kind == "number"` : une absence de format vaut [FORMAT_PLAIN], une valeur inconnue est refusée.
 * - autre `kind` : le format doit rester nul, sinon la saisie serait silencieusement ignorée
 *   à l'affichage (seules les colonnes numériques consomment ce champ).
 *
 * @throws IllegalArgumentException si le format est incompatible avec le kind.
 */
fun resolvePropertyFormat(kind: String, format: String?): String? {
    if (kind != "number") {
        require(format == null) { "Le format est réservé aux propriétés de type nombre" }
        return null
    }
    require(format == null || format in NUMBER_FORMATS) { "Format inconnu: $format" }
    return format ?: FORMAT_PLAIN
}

/**
 * Lit une saisie utilisateur en nombre. Le symbole du format est accepté puis retiré :
 * `"$12.5"`, `"12.5 %"` et `"12,5"` donnent tous 12.5.
 */
fun parseNumberInput(raw: String?): Double? {
    if (raw.isNullOrBlank()) return null
    val cleaned = raw.trim()
        .replace(" ", "")
        .replace("\u00A0", "")
        .replace("$", "")
        .replace("%", "")
        .replace(",", ".")
    // `takeIf` écarte les littéraux acceptés par `toDoubleOrNull` mais inexploitables
    // en calcul ("Infinity", "1e400" ...), qui finiraient en NaN dans la somme du pied de tableau.
    return cleaned.toDoubleOrNull()?.takeIf { it.isFinite() }
}

/**
 * Met en forme une valeur numérique selon le sous-format de sa colonne.
 * [FORMAT_PLAIN] et tout format inconnu donnent le nombre nu.
 */
fun formatNumberValue(value: Double, format: String?): String {
    if (!value.isFinite()) return "—"
    val body = formatDecimal(value)
    return when (format) {
        FORMAT_PERCENT -> "$body %"
        FORMAT_DOLLAR -> "\$$body"
        else -> body
    }
}

// Deux décimales maximum, sans zéro inutile : 12.5 et non 12.50.
private fun formatDecimal(value: Double): String {
    if (value % 1.0 == 0.0) return value.toLong().toString()
    return String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
}
