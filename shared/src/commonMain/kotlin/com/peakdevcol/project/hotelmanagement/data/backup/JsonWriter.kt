package com.peakdevcol.project.hotelmanagement.data.backup

/**
 * Writes maps, lists, texts, numbers, booleans and nulls as JSON. It is written by hand because
 * the backup is the only place that needs it, and it keeps the keys sorted so two backups of the
 * same data are the same text.
 */
internal object JsonWriter {
    fun write(value: Any?): String = StringBuilder().also { append(it, value, indent = 0) }.toString()

    private fun append(out: StringBuilder, value: Any?, indent: Int) {
        when (value) {
            null -> out.append("null")
            is Boolean -> out.append(value)
            is Double -> out.append(if (value.isFinite()) value else "null")
            is Float -> out.append(if (value.isFinite()) value else "null")
            is Number -> out.append(value)
            is Map<*, *> -> appendObject(out, value, indent)
            is Iterable<*> -> appendArray(out, value.toList(), indent)
            else -> appendText(out, value.toString())
        }
    }

    private fun appendObject(out: StringBuilder, map: Map<*, *>, indent: Int) {
        if (map.isEmpty()) {
            out.append("{}")
            return
        }
        out.append("{\n")
        map.entries.sortedBy { it.key.toString() }.forEachIndexed { index, (key, value) ->
            if (index > 0) out.append(",\n")
            out.append(INDENT.repeat(indent + 1))
            appendText(out, key.toString())
            out.append(": ")
            append(out, value, indent + 1)
        }
        out.append('\n').append(INDENT.repeat(indent)).append('}')
    }

    private fun appendArray(out: StringBuilder, list: List<*>, indent: Int) {
        if (list.isEmpty()) {
            out.append("[]")
            return
        }
        out.append("[\n")
        list.forEachIndexed { index, value ->
            if (index > 0) out.append(",\n")
            out.append(INDENT.repeat(indent + 1))
            append(out, value, indent + 1)
        }
        out.append('\n').append(INDENT.repeat(indent)).append(']')
    }

    private fun appendText(out: StringBuilder, text: String) {
        out.append('"')
        text.forEach { character ->
            when (character) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                else -> if (character < ' ') out.append(unicodeEscape(character)) else out.append(character)
            }
        }
        out.append('"')
    }

    private fun unicodeEscape(character: Char): String = "\\u" + character.code.toString(16).padStart(4, '0')

    private const val INDENT = "  "
}
