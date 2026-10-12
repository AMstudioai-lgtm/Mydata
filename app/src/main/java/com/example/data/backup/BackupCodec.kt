package com.example.data.backup

// Version du format data.json. Toute archive d'une autre version est refusée
// à l'import avec un message explicite (cf. BackupCodec.decode).
const val BACKUP_VERSION = 1

// Miroirs purs des entités Room (aucune dépendance Android/Room) pour que
// la sérialisation reste testable en JUnit pur sur la JVM.
data class BackupJournal(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val userId: String?
)

data class BackupProperty(
    val id: String,
    val journalId: String,
    val name: String,
    val kind: String,
    val format: String?,
    val optionsJson: String,
    val sortOrder: Int,
    val updatedAt: Long,
    val deletedAt: Long?,
    val userId: String?
)

data class BackupRow(
    val id: String,
    val journalId: String,
    val valuesJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val userId: String?
)

data class BackupPayload(
    val version: Int = BACKUP_VERSION,
    val journals: List<BackupJournal> = emptyList(),
    val properties: List<BackupProperty> = emptyList(),
    val rows: List<BackupRow> = emptyList()
)

// Échec de lecture : le message est déjà en français pour remonter tel quel.
class BackupFormatException(message: String) : IllegalArgumentException(message)

// Codec JSON autonome (aucune librairie) : org.json est un stub Android inutilisable
// en test JVM, et aucune autre dépendance JSON n'est garantie côté unit-test.
object BackupCodec {

    fun encode(payload: BackupPayload): String = buildString {
        append("{\"version\":")
        append(payload.version)
        append(",\"journals\":[")
        payload.journals.forEachIndexed { i, j ->
            if (i > 0) append(',')
            append("{\"id\":")
            str(j.id)
            append(",\"name\":")
            str(j.name)
            append(",\"created_at\":")
            append(j.createdAt)
            append(",\"updated_at\":")
            append(j.updatedAt)
            append(",\"deleted_at\":")
            numOrNull(j.deletedAt)
            append(",\"user_id\":")
            strOrNull(j.userId)
            append('}')
        }
        append("],\"properties\":[")
        payload.properties.forEachIndexed { i, p ->
            if (i > 0) append(',')
            append("{\"id\":")
            str(p.id)
            append(",\"journal_id\":")
            str(p.journalId)
            append(",\"name\":")
            str(p.name)
            append(",\"kind\":")
            str(p.kind)
            append(",\"format\":")
            strOrNull(p.format)
            append(",\"options_json\":")
            str(p.optionsJson)
            append(",\"sort_order\":")
            append(p.sortOrder)
            append(",\"updated_at\":")
            append(p.updatedAt)
            append(",\"deleted_at\":")
            numOrNull(p.deletedAt)
            append(",\"user_id\":")
            strOrNull(p.userId)
            append('}')
        }
        append("],\"rows\":[")
        payload.rows.forEachIndexed { i, r ->
            if (i > 0) append(',')
            append("{\"id\":")
            str(r.id)
            append(",\"journal_id\":")
            str(r.journalId)
            append(",\"values_json\":")
            str(r.valuesJson)
            append(",\"created_at\":")
            append(r.createdAt)
            append(",\"updated_at\":")
            append(r.updatedAt)
            append(",\"deleted_at\":")
            numOrNull(r.deletedAt)
            append(",\"user_id\":")
            strOrNull(r.userId)
            append('}')
        }
        append("]}")
    }

    fun decode(json: String): BackupPayload {
        val root = JsonParser(json).parse()
        if (root !is JsonObj) throw BackupFormatException("Sauvegarde illisible : objet racine attendu.")
        val version = root.long("version", "version")
        if (version != BACKUP_VERSION.toLong()) {
            throw BackupFormatException("Version de sauvegarde non prise en charge : $version.")
        }
        return BackupPayload(
            version = version.toInt(),
            journals = root.arr("journals").map { item ->
                if (item !is JsonObj) throw BackupFormatException("Sauvegarde illisible : journal invalide.")
                BackupJournal(
                    id = item.string("id"),
                    name = item.string("name"),
                    createdAt = item.long("created_at", "journal"),
                    updatedAt = item.long("updated_at", "journal"),
                    deletedAt = item.optLong("deleted_at"),
                    userId = item.optString("user_id")
                )
            },
            properties = root.arr("properties").map { item ->
                if (item !is JsonObj) throw BackupFormatException("Sauvegarde illisible : propriété invalide.")
                BackupProperty(
                    id = item.string("id"),
                    journalId = item.string("journal_id"),
                    name = item.string("name"),
                    kind = item.string("kind"),
                    format = item.optString("format"),
                    optionsJson = item.string("options_json"),
                    sortOrder = item.long("sort_order", "propriété").toInt(),
                    updatedAt = item.long("updated_at", "propriété"),
                    deletedAt = item.optLong("deleted_at"),
                    userId = item.optString("user_id")
                )
            },
            rows = root.arr("rows").map { item ->
                if (item !is JsonObj) throw BackupFormatException("Sauvegarde illisible : ligne invalide.")
                BackupRow(
                    id = item.string("id"),
                    journalId = item.string("journal_id"),
                    valuesJson = item.string("values_json"),
                    createdAt = item.long("created_at", "ligne"),
                    updatedAt = item.long("updated_at", "ligne"),
                    deletedAt = item.optLong("deleted_at"),
                    userId = item.optString("user_id")
                )
            }
        )
    }

    private fun StringBuilder.str(value: String) {
        append('"')
        for (c in value) {
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                else -> if (c < ' ') append("\\u" + c.code.toString(16).padStart(4, '0')) else append(c)
            }
        }
        append('"')
    }

    private fun StringBuilder.strOrNull(value: String?) {
        if (value == null) append("null") else str(value)
    }

    private fun StringBuilder.numOrNull(value: Long?) {
        if (value == null) append("null") else append(value)
    }
}

// --- Modèle JSON minimal -----------------------------------------------------

private sealed interface JsonVal
private data class JsonObj(val map: Map<String, JsonVal>) : JsonVal
private data class JsonArr(val items: List<JsonVal>) : JsonVal
private data class JsonStr(val value: String) : JsonVal
private data class JsonNum(val value: Long) : JsonVal
private object JsonNull : JsonVal

private fun JsonObj.string(key: String): String {
    val v = map[key] ?: throw BackupFormatException("Sauvegarde illisible : champ \"$key\" manquant.")
    if (v !is JsonStr) throw BackupFormatException("Sauvegarde illisible : champ \"$key\" invalide.")
    return v.value
}

private fun JsonObj.optString(key: String): String? {
    val v = map[key] ?: return null
    if (v is JsonNull) return null
    if (v !is JsonStr) throw BackupFormatException("Sauvegarde illisible : champ \"$key\" invalide.")
    return v.value
}

private fun JsonObj.long(key: String, quoi: String): Long {
    val v = map[key] ?: throw BackupFormatException("Sauvegarde illisible : champ \"$key\" manquant ($quoi).")
    if (v !is JsonNum) throw BackupFormatException("Sauvegarde illisible : champ \"$key\" invalide ($quoi).")
    return v.value
}

private fun JsonObj.optLong(key: String): Long? {
    val v = map[key] ?: return null
    if (v is JsonNull) return null
    if (v !is JsonNum) throw BackupFormatException("Sauvegarde illisible : champ \"$key\" invalide.")
    return v.value
}

private fun JsonObj.arr(key: String): List<JsonVal> {
    val v = map[key] ?: throw BackupFormatException("Sauvegarde illisible : liste \"$key\" manquante.")
    if (v !is JsonArr) throw BackupFormatException("Sauvegarde illisible : liste \"$key\" invalide.")
    return v.items
}

// Descente récursive stricte : tout caractère inattendu lève BackupFormatException.
private class JsonParser(private val src: String) {
    private var pos = 0

    fun parse(): JsonVal {
        val v = value()
        blank()
        if (pos != src.length) throw BackupFormatException("Sauvegarde illisible : contenu après la fin du document.")
        return v
    }

    private fun value(): JsonVal {
        blank()
        if (pos >= src.length) throw BackupFormatException("Sauvegarde illisible : fin de document inattendue.")
        return when (src[pos]) {
            '{' -> obj()
            '[' -> arr()
            '"' -> JsonStr(str())
            'n' -> lit("null")
            '-', in '0'..'9' -> num()
            else -> throw BackupFormatException("Sauvegarde illisible : caractère inattendu à la position $pos.")
        }
    }

    // Le schéma n'émet que null/objets/tableaux/chaînes/entiers : booléens refusés.
    private fun lit(word: String): JsonVal {
        if (!src.startsWith(word, pos)) throw BackupFormatException("Sauvegarde illisible : mot-clé invalide à la position $pos.")
        pos += word.length
        return JsonNull
    }

    private fun obj(): JsonObj {
        pos++ // {
        val m = LinkedHashMap<String, JsonVal>()
        blank()
        if (pos < src.length && src[pos] == '}') {
            pos++
            return JsonObj(m)
        }
        while (true) {
            blank()
            if (pos >= src.length || src[pos] != '"') {
                throw BackupFormatException("Sauvegarde illisible : clé attendue à la position $pos.")
            }
            val k = str()
            blank()
            if (pos >= src.length || src[pos] != ':') {
                throw BackupFormatException("Sauvegarde illisible : « : » attendu à la position $pos.")
            }
            pos++
            m[k] = value()
            blank()
            if (pos >= src.length) throw BackupFormatException("Sauvegarde illisible : objet non fermé.")
            when (src[pos]) {
                ',' -> pos++
                '}' -> {
                    pos++
                    return JsonObj(m)
                }
                else -> throw BackupFormatException("Sauvegarde illisible : caractère inattendu à la position $pos.")
            }
        }
    }

    private fun arr(): JsonArr {
        pos++ // [
        val items = ArrayList<JsonVal>()
        blank()
        if (pos < src.length && src[pos] == ']') {
            pos++
            return JsonArr(items)
        }
        while (true) {
            items += value()
            blank()
            if (pos >= src.length) throw BackupFormatException("Sauvegarde illisible : tableau non fermé.")
            when (src[pos]) {
                ',' -> pos++
                ']' -> {
                    pos++
                    return JsonArr(items)
                }
                else -> throw BackupFormatException("Sauvegarde illisible : caractère inattendu à la position $pos.")
            }
        }
    }

    private fun str(): String {
        pos++ // "
        val sb = StringBuilder()
        while (true) {
            if (pos >= src.length) throw BackupFormatException("Sauvegarde illisible : chaîne non fermée.")
            val c = src[pos++]
            when (c) {
                '"' -> return sb.toString()
                '\\' -> {
                    if (pos >= src.length) throw BackupFormatException("Sauvegarde illisible : échappement tronqué.")
                    when (val e = src[pos++]) {
                        '"', '\\', '/' -> sb.append(e)
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (pos + 4 > src.length) throw BackupFormatException("Sauvegarde illisible : séquence \\u tronquée.")
                            val hex = src.substring(pos, pos + 4)
                            val code = hex.toIntOrNull(16)
                                ?: throw BackupFormatException("Sauvegarde illisible : séquence \\u invalide.")
                            sb.append(code.toChar())
                            pos += 4
                        }
                        else -> throw BackupFormatException("Sauvegarde illisible : échappement invalide.")
                    }
                }
                else -> {
                    if (c < ' ') throw BackupFormatException("Sauvegarde illisible : caractère de contrôle non échappé.")
                    sb.append(c)
                }
            }
        }
    }

    private fun num(): JsonNum {
        val start = pos
        if (pos < src.length && src[pos] == '-') pos++
        if (pos >= src.length || src[pos] !in '0'..'9') {
            throw BackupFormatException("Sauvegarde illisible : nombre invalide à la position $start.")
        }
        while (pos < src.length && src[pos] in '0'..'9') pos++
        // Le schéma n'émet que des entiers : décimales et exposants refusés.
        if (pos < src.length && (src[pos] == '.' || src[pos] == 'e' || src[pos] == 'E')) {
            throw BackupFormatException("Sauvegarde illisible : nombre décimal inattendu.")
        }
        val raw = src.substring(start, pos)
        val v = raw.toLongOrNull() ?: throw BackupFormatException("Sauvegarde illisible : nombre hors limites.")
        return JsonNum(v)
    }

    private fun blank() {
        while (pos < src.length && (src[pos] == ' ' || src[pos] == '\n' || src[pos] == '\r' || src[pos] == '\t')) pos++
    }
}
