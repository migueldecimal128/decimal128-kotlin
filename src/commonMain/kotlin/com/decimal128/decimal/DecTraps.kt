package com.decimal128.decimal

class DecTraps internal constructor(
    private val trapHandlers: Array<DecTrapHandler?>) {

    constructor() : this(Array(DecException.entries.size) {null})

    fun hasTrapHandler(decException: DecException) = trapHandlers[decException.ordinal] != null

    fun signal(decExceptionContext: DecExceptionContext): Decimal {
        val exception = decExceptionContext.exception
        val trapHandler = trapHandlers[exception.ordinal]
        require (trapHandler != null)
        return trapHandler.signal(decExceptionContext)
    }

    fun withTrapHandler(trapHandler: DecTrapHandler?, vararg exceptions: DecException): DecTraps {
        val newTrapHandlers = trapHandlers.copyOf()
        for (exception in exceptions)
            newTrapHandlers[exception.ordinal] = trapHandler
        val newDecTraps = DecTraps(newTrapHandlers)
        return newDecTraps
    }

    override fun toString(): String {
        val activeHandlers = DecException.entries
            .filter { trapHandlers[it.ordinal] != null }
            .joinToString { it.name }
        return "DecTrapHandlers(enabled=[$activeHandlers])"
    }

    fun delayedTrap(ctx: DecContext) {
        // TODO
        throw RuntimeException("not impl")
    }

}
