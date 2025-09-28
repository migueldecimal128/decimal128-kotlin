package com.decimal128.decimal

data class DecimalTraps(
    val invalidHandler: DecimalTrapHandler?,
    val divByZeroHandler: DecimalTrapHandler?,
    val overflowHandler: DecimalTrapHandler?,
    val underflowHandler: DecimalTrapHandler?,
    val inexactHandler: DecimalTrapHandler?
    ) {

    fun signalInvalid(trapContext: DecimalTrapContext): Decimal {
        if (invalidHandler != null)
            return invalidHandler.execute(trapContext)
        return trapContext.decValue
    }

}
