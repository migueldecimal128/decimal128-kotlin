package com.decimal128.decimal

import com.decimal128.decimal.DecException.DIVIDE_BY_ZERO
import com.decimal128.decimal.DecException.INEXACT
import com.decimal128.decimal.DecException.INVALID_OPERATION
import com.decimal128.decimal.DecException.OVERFLOW
import com.decimal128.decimal.DecException.UNDERFLOW

data class DecTrapHandlers(
    val invalidOperation: DecTrapHandler? = null,
    val divideByZero: DecTrapHandler? = null,
    val overflow: DecTrapHandler? = null,
    val underflow: DecTrapHandler? = null,
    val inexact: DecTrapHandler? = null,
) {
    fun handlerFor(exception: DecException): DecTrapHandler? = when (exception) {
        INVALID_OPERATION -> invalidOperation
        DIVIDE_BY_ZERO -> divideByZero
        OVERFLOW -> overflow
        UNDERFLOW -> underflow
        INEXACT -> inexact
    }

    fun hasTrapHandler(exception: DecException) = handlerFor(exception) != null

    fun signal(trapContext: DecExceptionContext): Decimal {
        val handler = handlerFor(trapContext.exception)
        require(handler != null) { "No trap handler for ${trapContext.exception}" }
        return handler.signal(trapContext)
    }

    fun withTrapHandler(handler: DecTrapHandler?, vararg exceptions: DecException): DecTrapHandlers {
        var inv = invalidOperation
        var dbz = divideByZero
        var ovr = overflow
        var und = underflow
        var inx = inexact

        for (exception in exceptions) {
            when (exception) {
                INVALID_OPERATION -> inv = handler
                DIVIDE_BY_ZERO    -> dbz = handler
                OVERFLOW          -> ovr = handler
                UNDERFLOW         -> und = handler
                INEXACT           -> inx = handler
            }
        }
        return DecTrapHandlers(inv, dbz, ovr, und, inx)
    }

    fun withThrownException(vararg exceptions: DecException): DecTrapHandlers {
        var inv: DecTrapHandler? = invalidOperation
        var dbz: DecTrapHandler? = divideByZero
        var ovf: DecTrapHandler? = overflow
        var und: DecTrapHandler? = underflow
        var inx: DecTrapHandler? = inexact
        for (exception in exceptions) {
            when (exception) {
                INVALID_OPERATION -> inv = DecTrapHandler(::throwInvalidOperationException)
                DIVIDE_BY_ZERO    -> dbz = DecTrapHandler(::throwDivideByZeroException)
                OVERFLOW          -> ovf = DecTrapHandler(::throwOverflowException)
                UNDERFLOW         -> und = DecTrapHandler(::throwUnderflowException)
                INEXACT           -> inx = DecTrapHandler(::throwInexactException)
            }
        }
        return DecTrapHandlers(inv, dbz, ovf, und, inx)
    }

    fun delayedTrap(ctx: DecContext) {
        // TODO
        throw RuntimeException("not impl")
    }

    override fun toString(): String {
        val active = DecException.entries
            .filter { hasTrapHandler(it) }
            .joinToString { it.name }
        return "DecTraps(enabled=[$active])"
    }

    companion object {
        val NONE = DecTrapHandlers()
        val THROWING = DecTrapHandlers(
            invalidOperation = ::throwInvalidOperationException,
            divideByZero     = ::throwDivideByZeroException,
            overflow         = ::throwOverflowException,
            underflow        = ::throwUnderflowException,
            inexact          = ::throwInexactException
        )
    }

}

fun throwInvalidOperationException(exceptionContext: DecExceptionContext): Nothing =
    throw InvalidOperationException("Decimal: invalidOperation: $exceptionContext")

fun throwDivideByZeroException(exceptionContext: DecExceptionContext): Nothing =
    throw DivideByZeroException("Decimal: divideByZero: $exceptionContext")

fun throwOverflowException(exceptionContext: DecExceptionContext): Nothing =
    throw OverflowException("Decimal: overflow: $exceptionContext")

fun throwUnderflowException(exceptionContext: DecExceptionContext): Nothing =
    throw UnderflowException("Decimal: underflow: $exceptionContext")

fun throwInexactException(exceptionContext: DecExceptionContext): Nothing =
    throw InexactException("Decimal: inexact: $exceptionContext")
