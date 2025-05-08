package generator.domain

class DescriptorClass(val name: String, var parameter: List<Parameter>) {

    var kDoc: KDoc? = null

    class Parameter(val name: String, var type: String, var defaultValue: String? = null) {
        var kDoc: KDoc? = null
        var annotations: List<String> = emptyList()

        override fun toString(): String = StringBuilder().apply {
            kDoc?.let { append(it) }
            annotations.forEach { append("\t$it\n") }
            append("\toverride val $name: $type")
            if (defaultValue != null) append(" = $defaultValue")
        }.toString()
    }

    override fun toString(): String = StringBuilder().apply {
        kDoc?.let { append(it) }
        append("data class ${name.removePrefix("GPU")}(\n")
        (parameter.filter { it.defaultValue == null } +
        parameter.filter { it.defaultValue != null }).let {
            append(it.joinToString(",\n") { "$it" })
        }
        append("\n): $name\n")
    }.toString()
}