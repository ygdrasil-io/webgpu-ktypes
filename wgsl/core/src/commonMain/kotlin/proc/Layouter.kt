package io.ygdrasil.wgsl.proc

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.ir.*

/**
 * Memory alignment.
 */
data class Alignment(val value: Int) {
    /**
     * Rounds up an offset to the next aligned value.
     */
    fun roundUp(offset: Int): Int {
        if (value <= 1) return offset
        val mask = value - 1
        return (offset + mask) and mask.inv()
    }

    companion object {
        val ONE = Alignment(1)
    }
}

/**
 * Layout of a type.
 */
data class TypeLayout(
    val size: Int,
    val alignment: Alignment
)

/**
 * Calculates the memory layout of types.
 * 
 * This is a port of Naga's layouter.
 */
class Layouter {
    private val layouts = mutableListOf<TypeLayout>()

    /**
     * Update the layouts for all types in the module.
     */
    fun update(module: Module) {
        layouts.clear()
        // UniqueArena ensures that types are defined before they are used
        for (i in 0 until module.types.size) {
            val handle = Handle.create<Type>(i)
            val type = module.types[handle]
            layouts.add(calculateLayout(type, module))
        }
    }

    operator fun get(handle: Handle<Type>): TypeLayout {
        if (handle.index < 0 || handle.index >= layouts.size) {
            return TypeLayout(0, Alignment.ONE)
        }
        return layouts[handle.index]
    }

    private fun calculateLayout(type: Type, module: Module): TypeLayout {
        return when (val inner = type.inner) {
            is TypeInner.Scalar -> {
                val width = inner.width
                TypeLayout(width, Alignment(width))
            }
            is TypeInner.Vector -> {
                val scalarLayout = this[inner.scalar]
                val count = inner.size.value
                // vec3 alignment is the same as vec4
                val alignCount = if (count == 3) 4 else count
                val alignment = Alignment(scalarLayout.alignment.value * alignCount)
                TypeLayout(scalarLayout.size * count, alignment)
            }
            is TypeInner.Matrix -> {
                val scalarLayout = this[inner.scalar]
                val rows = inner.rows.value
                val columns = inner.columns.value
                
                // Matrix column layout
                val alignCount = if (rows == 3) 4 else rows
                val colAlignment = Alignment(scalarLayout.alignment.value * alignCount)
                val colStride = colAlignment.roundUp(scalarLayout.size * rows)
                
                TypeLayout(colStride * columns, colAlignment)
            }
            is TypeInner.Array -> {
                val elementLayout = this[inner.element]
                val stride = elementLayout.alignment.roundUp(elementLayout.size)
                val size = when (val s = inner.size) {
                    is ArraySize.Constant -> stride * s.value
                    is ArraySize.Dynamic -> 0
                }
                TypeLayout(size, elementLayout.alignment)
            }
            is TypeInner.Struct -> {
                var offset = 0
                var maxAlignment = 1
                for (member in inner.members) {
                    val memberLayout = this[member.type]
                    maxAlignment = maxOf(maxAlignment, memberLayout.alignment.value)
                    offset = memberLayout.alignment.roundUp(offset)
                    // We don't use member.offset here because we are calculating the default layout
                    offset += memberLayout.size
                }
                val alignment = Alignment(maxAlignment)
                TypeLayout(alignment.roundUp(offset), alignment)
            }
            is TypeInner.Pointer, is TypeInner.ValuePointer -> {
                TypeLayout(4, Alignment(4))
            }
            is TypeInner.Abstract -> {
                // Abstract types don't have a fixed layout in memory
                TypeLayout(8, Alignment(8))
            }
            else -> TypeLayout(0, Alignment.ONE)
        }
    }
}
