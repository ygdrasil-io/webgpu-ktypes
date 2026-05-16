package io.ygdrasil.wgsl.proc

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.ir.*
import io.ygdrasil.wgsl.ir.Function

/**
 * Error during validation.
 */
data class ValidationError(
    val message: String,
    val span: Span? = null
)

/**
 * Semantic validator for Naga IR.
 * 
 * This is a simplified port of Naga's validator.
 */
class Validator {
    private val typifier = Typifier()
    private val layouter = Layouter()

    /**
     * Validates a module.
     * 
     * @return A list of validation errors. Empty if the module is valid.
     */
    fun validate(module: Module): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        
        try {
            // Update layouter and typifier base
            layouter.update(module)
            
            // Validate types
            for (i in 0 until module.types.size) {
                validateType(Handle.create(i), module, errors)
            }
            
            // Validate constants
            for (constant in module.constants) {
                validateConstant(constant, module, errors)
            }
            
            // Validate global variables
            for (variable in module.globalVariables) {
                validateGlobalVariable(variable, module, errors)
            }
            
            // Validate functions
            for (func in module.functions) {
                validateFunction(func, module, errors)
            }
        } catch (e: Exception) {
            errors.add(ValidationError("Internal validator error: ${e.message}"))
        }
        
        return errors
    }

    private fun validateType(handle: Handle<Type>, module: Module, errors: MutableList<ValidationError>) {
        val type = module.types[handle]
        when (val inner = type.inner) {
            is TypeInner.Vector -> {
                if (!module.types[inner.scalar].isScalar) {
                    errors.add(ValidationError("Vector scalar must be a scalar type"))
                }
            }
            is TypeInner.Matrix -> {
                if (!module.types[inner.scalar].isScalar) {
                    errors.add(ValidationError("Matrix scalar must be a scalar type"))
                }
            }
            is TypeInner.Array -> {
                // Check if element type is valid
                val elementLayout = layouter[inner.element]
                if (elementLayout.size == 0) {
                    errors.add(ValidationError("Array element type has zero size"))
                }
            }
            is TypeInner.Struct -> {
                for (member in inner.members) {
                    val memberLayout = layouter[member.type]
                    if (memberLayout.size == 0) {
                        errors.add(ValidationError("Struct member '${member.name}' has zero size"))
                    }
                }
            }
            else -> {}
        }
    }

    private fun validateConstant(constant: Constant, module: Module, errors: MutableList<ValidationError>) {
        // Basic check: constant type must exist
        if (constant.type.index < 0 || constant.type.index >= module.types.size) {
            errors.add(ValidationError("Constant has invalid type handle"))
        }
    }

    private fun validateGlobalVariable(variable: GlobalVariable, module: Module, errors: MutableList<ValidationError>) {
        // Check if init is constant
        if (variable.init != null) {
            // In a real validator, we would check if the expression is actually constant
            // using ConstantEvaluator
        }
    }

    private fun validateFunction(func: Function, module: Module, errors: MutableList<ValidationError>) {
        // Resolve types for all expressions in the function
        typifier.fill(module, func, func.expressions)
        
        // Validate each expression
        func.expressions.forEachWithHandle { handle, expr ->
            val res = typifier[handle]
            if (res.getInner(module) is TypeInner.Error) {
                errors.add(ValidationError("Expression $handle has invalid type in function '${func.name}'"))
            }
        }
    }
}
