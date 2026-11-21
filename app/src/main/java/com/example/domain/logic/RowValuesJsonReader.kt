package com.example.domain.logic

/**
 * Lecteur JSON minimal en Kotlin pur pour extraire les valeurs de cellules,
 * options de sélection et aperçus sans dépendre de `org.json` (Android stub).
 */
internal object RowValuesJsonReader {

    fun parseObjectMap(json: String): Map<String, String?> {
        return try {
            val root = Parser(json).parseValue()
            if (root !is Obj) return emptyMap()
            root.entries.mapValues { (_, v) -> valueToString(v) }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun parseStringArray(json: String): List<String> {
        return try {
            val root = Parser(json).parseValue()
            if (root !is Arr) return emptyList()
            root.items.mapNotNull { valueToString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun valueToString(v: Val): String? = when (v) {
        is Str -> v.value
        is Num -> v.raw
        is Bool -> v.value.toString()
        Null -> null
        is Obj -> null
        is Arr -> null
    }

    private sealed interface Val
    private data class Obj(val entries: Map<String, Val>) : Val
    private data class Arr(val items: List<Val>) : Val
    private data class Str(val value: String) : Val
    private data class Num(val raw: String) : Val
    private data class Bool(val value: Boolean) : Val
    private data object Null : Val

    private class Parser(private val src: String) {
        private var pos = 0

        fun parseValue(): Val {
            skipWhitespace()
            if (pos >= src.length) error("EOF")
            return when (src[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> Str(parseString())
                't' -> consume("true", Bool(true))
                'f' -> consume("false", Bool(false))
                'n' -> consume("null", Null)
                '-', in '0'..'9' -> parseNumber()
                else -> error("Unexpected char at $pos")
            }
        }

        private fun consume(keyword: String, result: Val): Val {
            if (!src.startsWith(keyword, pos)) error("Invalid literal")
            pos += keyword.length
            return result
        }

        private fun parseObject(): Obj {
            pos++
            val map = LinkedHashMap<String, Val>()
            skipWhitespace()
            if (pos < src.length && src[pos] == '}') {
                pos++
                return Obj(map)
            }
            while (true) {
                skipWhitespace()
                if (pos >= src.length || src[pos] != '"') error("Expected key")
                val key = parseString()
                skipWhitespace()
                if (pos >= src.length || src[pos] != ':') error("Expected ':'")
                pos++
                map[key] = parseValue()
                skipWhitespace()
                if (pos >= src.length) error("Unclosed object")
                when (src[pos]) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        return Obj(map)
                    }
                    else -> error("Unexpected char in object")
                }
            }
        }

        private fun parseArray(): Arr {
            pos++
            val list = ArrayList<Val>()
            skipWhitespace()
            if (pos < src.length && src[pos] == ']') {
                pos++
                return Arr(list)
            }
            while (true) {
                list += parseValue()
                skipWhitespace()
                if (pos >= src.length) error("Unclosed array")
                when (src[pos]) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return Arr(list)
                    }
                    else -> error("Unexpected char in array")
                }
            }
        }

        private fun parseString(): String {
            pos++
            val sb = StringBuilder()
            while (pos < src.length) {
                val c = src[pos++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= src.length) error("Truncated escape")
                        when (val e = src[pos++]) {
                            '"', '\\', '/' -> sb.append(e)
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > src.length) error("Truncated \\u")
                                val hex = src.substring(pos, pos + 4)
                                sb.append(hex.toInt(16).toChar())
                                pos += 4
                            }
                            else -> sb.append(e)
                        }
                    }
                    else -> sb.append(c)
                }
            }
            error("Unclosed string")
        }

        private fun parseNumber(): Num {
            val start = pos
            if (pos < src.length && src[pos] == '-') pos++
            while (pos < src.length && src[pos] in '0'..'9') pos++
            if (pos < src.length && src[pos] == '.') {
                pos++
                while (pos < src.length && src[pos] in '0'..'9') pos++
            }
            if (pos < src.length && (src[pos] == 'e' || src[pos] == 'E')) {
                pos++
                if (pos < src.length && (src[pos] == '+' || src[pos] == '-')) pos++
                while (pos < src.length && src[pos] in '0'..'9') pos++
            }
            return Num(src.substring(start, pos))
        }

        private fun skipWhitespace() {
            while (pos < src.length && src[pos].isWhitespace()) pos++
        }
    }
}
