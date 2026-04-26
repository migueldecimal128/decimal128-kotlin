package com.decimal128.decimal

open class DecimalArithmeticException(message: String = "") : ArithmeticException(message)

class InvalidOperationException(message: String = "") : DecimalArithmeticException(message)
class DivideByZeroException(message: String = "") : DecimalArithmeticException(message)
class OverflowException(message: String = "") : DecimalArithmeticException(message)
class UnderflowException(message: String = "") : DecimalArithmeticException(message)
class InexactException(message: String = "") : DecimalArithmeticException(message)