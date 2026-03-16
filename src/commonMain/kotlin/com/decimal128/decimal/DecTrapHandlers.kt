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
        var result = this
        for (exception in exceptions)
            result = when (exception) {
                DecException.INVALID_OPERATION -> result.copy(invalidOperation = handler)
                DecException.DIV_BY_ZERO       -> result.copy(divByZero = handler)
                DecException.OVERFLOW          -> result.copy(overflow = handler)
                DecException.UNDERFLOW         -> result.copy(underflow = handler)
                DecException.INEXACT           -> result.copy(inexact = handler)
            }
        return result
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