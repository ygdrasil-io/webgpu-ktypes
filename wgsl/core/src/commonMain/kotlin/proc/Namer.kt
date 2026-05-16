package io.ygdrasil.wgsl.proc

/**
 * Generates unique names for identifiers in the target shading language.
 * 
 * This is a port of Naga's namer.
 */
class Namer {
    private val reserved = mutableSetOf<String>()
    private val used = mutableSetOf<String>()

    /**
     * Resets the namer with a set of reserved keywords.
     */
    fun reset(reservedKeywords: Collection<String> = emptySet()) {
        reserved.clear()
        reserved.addAll(reservedKeywords)
        used.clear()
    }

    /**
     * Generates a unique name for a suggested identifier.
     * 
     * @param suggested The suggested name for the identifier.
     * @return A unique and sanitized name.
     */
    fun call(suggested: String): String {
        var name = suggested
        
        // Basic sanitization: only allow alphanumeric and underscores
        // Replace invalid characters with underscore
        name = name.map { if (it.isLetterOrDigit() || it == '_') it else '_' }
                   .joinToString("")
        
        // Ensure name does not start with a digit
        if (name.isEmpty() || name[0].isDigit()) {
            name = "_$name"
        }

        var candidate = name
        var index = 1
        
        // Loop until we find a name that is not reserved and not used
        while (reserved.contains(candidate) || used.contains(candidate)) {
            candidate = "${name}_$index"
            index++
        }
        
        used.add(candidate)
        return candidate
    }

    companion object {
        /**
         * Common reserved keywords for WGSL.
         */
        val WGSL_RESERVED = setOf(
            "array", "atomic", "bool", "break", "case", "continue", "continuing",
            "default", "discard", "else", "enable", "f32", "f16", "fallthrough",
            "false", "fn", "for", "if", "i32", "let", "loop", "mat2x2", "mat2x3",
            "mat2x4", "mat3x2", "mat3x3", "mat3x4", "mat4x2", "mat4x3", "mat4x4",
            "override", "ptr", "return", "struct", "switch", "true", "type",
            "u32", "var", "vec2", "vec3", "vec4", "bitcast"
        )

        /**
         * Common reserved keywords for Metal Shading Language (MSL).
         */
        val MSL_RESERVED = setOf(
            "alignas", "alignof", "as_type", "auto", "break", "case", "char",
            "class", "const", "constexpr", "continue", "decltype", "default",
            "delete", "do", "double", "dynamic_cast", "else", "enum", "explicit",
            "export", "extern", "false", "float", "for", "friend", "goto", "if",
            "inline", "int", "long", "mutable", "namespace", "new", "noexcept",
            "nullptr", "operator", "private", "protected", "public", "register",
            "reinterpret_cast", "return", "short", "signed", "sizeof", "static",
            "static_assert", "static_cast", "struct", "switch", "template",
            "this", "thread_local", "throw", "true", "try", "typedef", "typeid",
            "typename", "union", "unsigned", "using", "virtual", "void",
            "volatile", "wchar_t", "while", "device", "constant", "thread",
            "threadgroup", "threadgroup_imageblock"
        )
    }
}
