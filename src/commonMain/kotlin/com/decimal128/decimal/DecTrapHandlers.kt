package com.decimal128.decimal

data class DecTrapHandlers(
    val invalidOperation: DecTrapHandler? = null,
    val divByZero: DecTrapHandler? = null,
    val overflow: DecTrapHandler? = null,
    val underflow: DecTrapHandler? = null,
    val inexact: DecTrapHandler? = null,
) {
    fun handlerFor(exception: DecException): DecTrapHandler? = when (exception) {
        DecException.INVALID_OPERATION -> invalidOperation
        DecException.DIV_BY_ZERO       -> divByZero
        DecException.OVERFLOW          -> overflow
        DecException.UNDERFLOW         -> underflow
        DecException.INEXACT           -> inexact
    }

    fun hasTrapHandler(exception: DecException) = handlerFor(exception) != null

    fun signal(trapContext: DecExceptionContext): Decimal {
        val handler = handlerFor(trapContext.exception)
        require(handler != null)
        return handler.signal(trapContext)
    }

    fun withTrapHandler(handler: DecTrapHandler?, vararg exceptions: DecException): DecTrapHandlers {
        var inv = invalidOperation
        var div = divByZero
        var ovf = overflow
        var unf = underflow
        var inex = inexact

        for (exception in exceptions) {
            when (exception) {
                DecException.INVALID_OPERATION -> inv = handler
                DecException.DIV_BY_ZERO       -> div = handler
                DecException.OVERFLOW          -> ovf = handler
                DecException.UNDERFLOW         -> unf = handler
                DecException.INEXACT           -> inex = handler
            }
        }

        return DecTrapHandlers(inv, div, ovf, unf, inex)
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
            invalidOperation = { throw InvalidOperationException(it.toString()) },
            divByZero        = { throw DivisionByZeroException(it.toString()) },
            overflow         = { throw OverflowException(it.toString()) },
            underflow        = { throw UnderflowException(it.toString()) },
            inexact          = { throw InexactException(it.toString()) },
        )
    }
}