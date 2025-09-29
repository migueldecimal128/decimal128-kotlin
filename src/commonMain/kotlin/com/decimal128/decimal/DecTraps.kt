package com.decimal128.decimal

data class DecTraps(
    val invalidHandler: DecTrapHandler? = null,
    val divByZeroHandler: DecTrapHandler? = null,
    val overflowHandler: DecTrapHandler? = null,
    val underflowHandler: DecTrapHandler? = null,
    val inexactHandler: DecTrapHandler? = null
    ) {

    fun signalInvalid(trapContext: DecTrapContext): MutDec {
        if (invalidHandler != null)
            return invalidHandler.execute(trapContext)
        return trapContext.decValue
    }

    fun delayedTrap(decEnv: DecEnv) {
        // TODO
        throw RuntimeException("not impl")
    }

}
