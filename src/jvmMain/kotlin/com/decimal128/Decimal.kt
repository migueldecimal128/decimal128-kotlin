package com.decimal128

import com.decimal128.Residue.Companion.EXACT
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_NEGATIVE
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_ZERO
import com.decimal128.Class754.*
import kotlin.math.max
import kotlin.math.min

internal const val MIN_SPECIAL_VALUE = 1000000000
internal const val NON_FINITE_INF = 1000000000
internal const val NON_FINITE_QNAN = 1000000001
internal const val NON_FINITE_SNAN = 1000000002

const val CAPPED_EXP_MIN = -25000
const val CAPPED_EXP_MAX = 25000

val DEFAULT_128_CONTEXT = DecimalContext.newDecimal128Context()

class Decimal() : S256() {
    var qExp = 0

    constructor(sign: Boolean, qExp: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long) : this() {
        this.u256Set256(dw3, dw2, dw1, dw0)
        this.qExp = qExp
        this.sign = sign
    }

    constructor(str: String): this() {
        DecimalParsePrint.decFromString(this, str, DEFAULT_128_CONTEXT)
    }

    constructor(other: Decimal) : this(other.sign, other.qExp, other.dw3, other.dw2, other.dw1, other.dw0)

    companion object {
        fun newInstance(): Decimal = Decimal()

        fun newInfinity(sign: Boolean = false) = Decimal().setInfinite(sign)

        fun newAdd(x: Decimal, y: Decimal, ctx: DecimalContext = DEFAULT_128_CONTEXT) =
            addImpl(Decimal(), x, y.sign, y, ctx)

        fun newSub(x: Decimal, y: Decimal, ctx: DecimalContext = DEFAULT_128_CONTEXT) =
            addImpl(Decimal(), x, !y.sign, y, ctx)

        fun newMul(x: Decimal, y: Decimal, ctx: DecimalContext = DEFAULT_128_CONTEXT) =
            Decimal().setMul(x, y, ctx)

        fun newSquare(x: Decimal, ctx: DecimalContext = DEFAULT_128_CONTEXT) =
            Decimal().setSquare(x, ctx)

        fun newDiv(x: Decimal, y: Decimal, ctx: DecimalContext = DEFAULT_128_CONTEXT) = Decimal().setDiv(x, y, ctx)

        fun newReciprocal(x: Decimal, ctx: DecimalContext = DEFAULT_128_CONTEXT) =
            Decimal().setReciprocal(x, ctx)

        fun newSqrt(x: Decimal, ctx: DecimalContext = DEFAULT_128_CONTEXT) =
            Decimal().setSqrt(x, ctx)

        private fun addImpl(z: Decimal, x: Decimal, ySign: Boolean, y: Decimal, ctx: DecimalContext): Decimal {
            val xQ = x.qExp
            if (xQ == y.qExp && xQ < MIN_SPECIAL_VALUE) {
                z.qExp = xQ
                z.s256AddImpl(x, ySign, y)
                return z.roundAndFinalize(EXACT, ctx)
            } else {
                return scaledAddImpl(z, x, ySign, y, ctx)
            }
        }

        private fun scaledAddImpl(z: Decimal, x: Decimal, ySign: Boolean, y: Decimal, ctx: DecimalContext): Decimal {
            val qX = x.qExp
            val qY = y.qExp
            val xSign = x.sign
            val qMax = max(qX, qY)
            when {
                qMax < MIN_SPECIAL_VALUE -> {
                    val residue = when {
                        ! (xSign xor ySign) -> {
                            z.sign = xSign
                            MagnitudeAddSub.magScaledAdd(z, x, y)
                        }
                        (x.magnitudeCompareTo(y) >= 0) -> {
                            z.sign = xSign
                            MagnitudeAddSub.magSub(z, x, y)
                        }
                        else -> {
                            z.sign = ySign
                            MagnitudeAddSub.magSub(z, y, x)
                        }
                    }
                    return z.roundAndFinalize(residue, ctx)
                }
                qMax == NON_FINITE_INF -> when {
                    (xSign != ySign) && (qX == qY) -> {
                        z.setNaN(ctx)
                        return ctx.signalInvalid(z)
                    }
                    else -> {
                        z.setInfinite(if (qX == NON_FINITE_INF) xSign else ySign)
                    }
                }
                else -> {
                    z.setNaN(x, y, ctx)
                }
            }
            return z
        }


    }

    fun sciExp() = qExp + (digitLen - 1)

    fun setZero()  = setZero(false)

    fun setZero(sign: Boolean) {
        u256SetZero()
        this.qExp = 0
        this.sign = sign
    }

    private fun setNaN(x: Decimal, y: Decimal, ctx: DecimalContext) {
        val xQ = x.qExp
        val yQ = y.qExp
        val maxQ = max(xQ, yQ)
        assert(maxQ >= NON_FINITE_QNAN)
        setZero()
        if (maxQ == NON_FINITE_SNAN) {
            ctx.operandIsSignalingNaN(if (xQ == NON_FINITE_SNAN) x else y)
            ctx.signalInvalid(this)
        }
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    private fun setNaN(x: Decimal, y: Decimal, a: Decimal, ctx: DecimalContext) {
        val qX = x.qExp
        val qY = y.qExp
        val qA = a.qExp
        val qMaxXYA = max(max(qX, qY), qA)
        assert(qMaxXYA >= NON_FINITE_QNAN)
        setZero()
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    private fun sNaNOperand() {
        throw RuntimeException("sNaN operand")
    }

    private fun setNaN(x: Decimal, ctx: DecimalContext) {
        val q = x.qExp
        assert(q >= NON_FINITE_QNAN)
        setZero()
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    internal fun setNaN(ctx: DecimalContext) {
        s256SetZero()
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    internal fun setNaN(payload: Int, ctx: DecimalContext) {
        sign = false
        u256Set64(payload.toLong())
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    fun setSNaN(ctx: DecimalContext) {
        setZero()
        qExp = NON_FINITE_SNAN
    }

    fun setInfinite(sign: Boolean = false) {
        // It is important that the coefficient of Infinity be non-zero because
        // multiply (for example) checks to see if the other operand is zero.
        // so we want the coefficient.isZero() to fail for Infinity
        this.u256SetOne()
        this.qExp = NON_FINITE_INF
        this.sign = sign
    }

    fun set(l: Long): Decimal {
        this.qExp = 0
        this.sign = l < 0
        val mask = l shr 63
        val abs = (l xor mask) - mask
        u256Set64(abs)
        return this
    }

    fun setUnsigned(ul: Long): Decimal {
        this.qExp = 0
        this.sign = false
        u256Set64(ul)
        return this
    }

    fun set(x: Decimal): Decimal {
        u256Set(x)
        this.qExp = x.qExp
        this.sign = x.sign
        return this
    }

    fun set(str: String): Decimal {
        DecimalParsePrint.decFromString(this, str, DEFAULT_128_CONTEXT)
        return this
    }

    fun setMaxFiniteMagnitude(ctx: DecimalContext): Decimal {
        qExp = ctx.qMax
        // 0x378D8E6400000000uL.toLong(), 0x0001ED09BEAD87C0uL.toLong(),
        // 10000000000000000000000000000000000 (10**34)
        val offset = U256Pow10.pow10Offset(ctx.precision)
        if (ctx.precision < MIN_POW10_DIGIT_LEN_128) {
            super.u256Set64(POW10[offset] - 1)
        } else if (ctx.precision < MIN_POW10_DIGIT_LEN_192) {
            super.u256Set128(POW10[offset + 1], POW10[offset] - 1)
        } else
            throw IllegalArgumentException()
        return this
    }

    fun setMinFiniteMagnitude(ctx: DecimalContext): Decimal {
        qExp = ctx.qTiny
        super.u256SetOne()
        return this
    }

    fun setNegate(x: Decimal): Decimal {
        set(x)
        this.sign = !this.sign
        return this
    }

    fun mutateNegate(): Decimal {
        this.sign = !this.sign
        return this
    }

    fun mutateAbs(): Decimal {
        this.sign = false
        return this
    }

    fun mutateCopySign(x: Decimal, y: Decimal): Decimal {
        val sign = y.sign
        set(x)
        this.sign = sign
        return this
    }

    fun isNegative() = sign

    fun isNumber() : Boolean {
        return qExp <= NON_FINITE_INF
    }

    fun add(y: Decimal) = newAdd(this, y)
    fun subtract(y: Decimal) = newSub(this, y)
    fun multiply(y: Decimal) = newMul(this, y)
    fun divide(y: Decimal) = newDiv(this, y)

    fun add(y: Decimal, ctx: DecimalContext) = newAdd(this, y, ctx)
    fun subtract(y: Decimal, ctx: DecimalContext) = newSub(this, y, ctx)
    fun multiply(y: Decimal, ctx: DecimalContext) = newMul(this, y, ctx)
    fun square(ctx: DecimalContext) = newSquare(this, ctx)
    fun divide(y: Decimal, ctx: DecimalContext) = newDiv(this, y, ctx)
    fun reciprocal(ctx: DecimalContext) = newReciprocal(this, ctx)
    fun sqrt(ctx: DecimalContext) = newSqrt(this, ctx)

    fun mutateAdd(y: Decimal, ctx: DecimalContext) { setAdd(this, y, ctx) }
    fun mutateSub(y: Decimal, ctx: DecimalContext) { setSub(this, y, ctx) }
    fun mutateMul(y: Decimal, ctx: DecimalContext) { setMul(this, y, ctx) }
    fun mutateSquare(ctx: DecimalContext) { setSquare(this, ctx) }
    fun mutateDiv(y: Decimal, ctx: DecimalContext) { setDiv(this, y, ctx) }
    fun mutateReciprocal(ctx: DecimalContext) { setReciprocal(this, ctx) }
    fun mutateSqrt(ctx: DecimalContext) { setSqrt(this, ctx) }


    // IEEE754-2008 5.4.1
    fun setAdd(x: Decimal, y: Decimal, ctx: DecimalContext) = addImpl(this, x, y.sign, y, ctx)

    // IEEE754-2008 5.4.1
    fun setSub(x: Decimal, y: Decimal, ctx: DecimalContext) = addImpl(this, x, !y.sign, y, ctx)

    // IEEE754-2008 5.4.1
    fun setMul(x: Decimal, y: Decimal, ctx: DecimalContext): Decimal {
        val qX = x.qExp
        val qY = y.qExp
        val productSign = x.sign xor y.sign
        val qMaxXY = max(qX, qY)
        when {
            qMaxXY < MIN_SPECIAL_VALUE -> {
                this.u256SetMul(x, y)
                this.qExp = x.qExp + y.qExp
                this.sign = productSign
                return roundAndFinalize(EXACT, ctx)
            }
            qMaxXY == NON_FINITE_INF -> {
                if (x.u256IsZero() || y.u256IsZero()) {
                    setNaN(ctx)
                    return ctx.signalInvalid(this)
                } else {
                    setInfinite(productSign)
                }
            }
            else -> setNaN(x, y, ctx)
        }
        return this
    }

    fun setSquare(x: Decimal, ctx: DecimalContext): Decimal {
        val qX = x.qExp
        when {
            qX < MIN_SPECIAL_VALUE -> {
                this.u256SetSqr(x)
                this.qExp = this.qExp shl 1
                this.sign = false
                return roundAndFinalize(EXACT, ctx)            }
            qX == NON_FINITE_INF -> {
                setInfinite(false)
            }
            else -> setNaN(x, ctx)
        }
        return this
    }

    // IEEE754-2008 5.4.1
    fun setFma(x: Decimal, y: Decimal, a: Decimal, ctx: DecimalContext): Decimal {
        val qX = x.qExp
        val qY = y.qExp
        val qA = a.qExp
        val qMaxXY = max(qX, qY)
        val qMaxXYA = max(qMaxXY, qA)
        val productSign = x.sign xor y.sign
        when {
            qMaxXYA < MIN_SPECIAL_VALUE -> {
                val aT = if (this === a) Decimal(a) else a
                // multiply without roundAndFinalize .. remains exact
                this.u256SetMul(x, y)
                this.qExp = x.qExp + y.qExp
                this.sign = productSign
                // roundAndFinalize takes place here
                this.setAdd(this, aT, ctx)
            }
            qMaxXYA == NON_FINITE_INF -> when {
                (qMaxXY == NON_FINITE_INF) && (x.u256IsZero() || y.u256IsZero()) -> {
                    setNaN(ctx)
                }
                (qA == NON_FINITE_INF) -> {
                    if ((qMaxXY < NON_FINITE_INF) || (productSign == a.sign))
                        this.set(a)
                    else
                        this.setNaN(ctx)
                }
            }
            else -> {
                this.setNaN(x, y, ctx)
            }
        }
        return this
    }

    fun setDiv(x: Decimal, y: Decimal, ctx: DecimalContext): Decimal {
        val qX = x.qExp
        val qY = y.qExp
        val quotientSign = x.sign xor y.sign
        val qMaxXY = max(qX, qY)
        when {
            qMaxXY < MIN_SPECIAL_VALUE -> {
                when {
                    (y.bitLen > 0) -> {
                        val residue = MagnitudeDiv.magDiv(this, x, y, ctx)
                        this.sign = quotientSign
                        roundAndFinalize(residue, ctx)
                    }
                    (x.bitLen > 0) -> {
                        // finite division by zero
                        setInfinite(quotientSign)
                        return ctx.signalDivByZero(this)
                    }
                    else -> {
                        // zero divided by zero
                        setNaN(ctx)
                        return ctx.signalInvalid(this)
                    }
                }
            }
            qMaxXY == NON_FINITE_INF -> {
                when {
                    (qX == NON_FINITE_INF && qY == NON_FINITE_INF) -> {
                        setNaN(ctx)
                        return ctx.signalInvalid(this)
                    }

                    (qX == NON_FINITE_INF) -> {
                        setInfinite(quotientSign)
                    }

                    else -> {
                        setZero(quotientSign)
                    }
                }
            }
            else -> setNaN(x, y, ctx)
        }
        return this
    }

    fun setReciprocal(x: Decimal, ctx: DecimalContext): Decimal {
        val qX = x.qExp
        val quotientSign = x.sign
        when {
            qX < MIN_SPECIAL_VALUE -> {
                val residue = MagnitudeInv.magInv(this, x, ctx)
                this.sign = quotientSign
                roundAndFinalize(residue, ctx)
            }
            qX == NON_FINITE_INF -> {
                setZero(quotientSign)
            }
            else -> setNaN(x, ctx)
        }
        return this
    }

    fun setSqrt(x: Decimal, ctx: DecimalContext): Decimal {
        val qX = x.qExp
        when {
            x.sign -> {
                setNaN(ctx)
                return ctx.signalInvalid(this)
            }
            qX < MIN_SPECIAL_VALUE -> {
                when {
                    (x.bitLen > 0) -> {
                        val residue = MagnitudeSqrt.magSqrt(this, x)
                        this.sign = false
                        roundAndFinalize(residue, ctx)
                    }
                    else -> {
                        // sqrt of zero
                        setZero(false)
                        qExp = qX shr 1
                    }
                }
            }
            qX == NON_FINITE_INF -> {
                setInfinite(false)
            }
            else -> setNaN(x, ctx)
        }
        return this
    }

    fun compareTo(other: Decimal, ctx: DecimalContext) : Int {
        val qMax = max(qExp, other.qExp)
        when {
            (qMax < MIN_SPECIAL_VALUE) -> {
                if (u256IsZero()) {
                    if (other.u256IsZero())
                        return 0
                    else
                        return if (other.sign) 1 else -1
                }
                if (other.u256IsZero() || sign != other.sign) {
                    return if (sign) -1 else 1
                }
                val cmp = magnitudeCompareTo(other)
                val ret = if (sign) -cmp else cmp
                return ret
            }

            (qMax == NON_FINITE_INF) -> when {
                (sign != other.sign) -> {
                    return if (sign) -1 else 1
                }

                (qExp == NON_FINITE_INF) -> {
                    if (other.qExp == NON_FINITE_INF)
                        return 0
                    return if (sign) -1 else 1
                }
                else -> {
                    return if (sign) 1 else -1
                }
            }
            else -> throw RuntimeException("somebody is a NaN")
        }
    }

    fun magnitudeCompareTo(other: Decimal) : Int {
        val thisIsZero = u256IsZero()
        val otherIsZero = other.u256IsZero()
        val eitherIsZero = thisIsZero or otherIsZero
        when {
            !eitherIsZero -> {
                val cmpExpSci = this.sciExp().compareTo(other.sciExp())
                if (cmpExpSci != 0)
                    return cmpExpSci
                val expDelta = this.qExp - other.qExp
                val ret = when {
                    expDelta == 0 -> u256UnscaledCompareTo(other)
                    expDelta > 0 -> -other.u256ScaledCompareTo(this, expDelta)
                    else -> u256ScaledCompareTo(other, -expDelta)
                }
                return ret
            }
            thisIsZero -> {
                return if (otherIsZero) 0 else -1
            }
            else -> {
                return 1
            }
        }
    }

    fun magnitudeEQ(other: Decimal) : Boolean {
        val thisIsZero = this.u256IsZero()
        val otherIsZero = other.u256IsZero()
        val bothAreZero = thisIsZero and otherIsZero
        val eitherIsZero = thisIsZero or otherIsZero
        if (this.sciExp() != other.sciExp())
            return bothAreZero
        if (! eitherIsZero) {
            val expDelta = this.qExp - other.qExp
            return when {
                expDelta == 0 -> this.u256UnscaledEQ(other)
                expDelta > 0 -> other.u256ScaledEQ(this, expDelta)
                else -> this.u256ScaledEQ(other, -expDelta)
            }
        }
        return bothAreZero
    }


    fun mutateRoundToIntegral(x: Decimal, rd: RoundingDirection, ctx: DecimalContext): Decimal {
        //FIXME - deal with special values
        if (qExp < 0) {
            val residue = this.u256SetScaleDownPow10(x, -qExp)
            qExp = 0
            sign = x.sign
            return roundAndFinalize(residue, rd, ctx)
        } else {
            return set(x)
        }
    }

    fun setRoundToIntegralTiesToEven(x: Decimal, ctx: DecimalContext) =
        mutateRoundToIntegral(x, ROUND_TIES_TO_EVEN, ctx)

    fun setRoundToIntegralTiesToAway(x: Decimal, ctx: DecimalContext) =
        mutateRoundToIntegral(x, ROUND_TIES_TO_AWAY, ctx)

    fun setRoundToIntegralTowardZero(x: Decimal, ctx: DecimalContext) =
        mutateRoundToIntegral(x, ROUND_TOWARD_ZERO, ctx)

    fun setRoundToIntegralTowardPositive(x: Decimal, ctx: DecimalContext) =
        mutateRoundToIntegral(x, ROUND_TOWARD_POSITIVE, ctx)

    fun setRoundToIntegralTowardNegative(x: Decimal, ctx: DecimalContext) =
        mutateRoundToIntegral(x, ROUND_TOWARD_NEGATIVE, ctx)

    fun setNextUp(x: Decimal, ctx: DecimalContext) {
        set(x)
        when {
            qExp > NON_FINITE_INF -> { return }
            qExp == NON_FINITE_INF -> {
                if (sign)
                    setMaxFiniteMagnitude(ctx)
                return
            }
            u256IsZero() -> {
                setMinFiniteMagnitude(ctx)
                sign = false
                return
            }
            sign == false -> mutateNextAwayFromZero(ctx)
            else -> mutateNextTowardZero(ctx)
        }
        roundAndFinalize(EXACT, ROUND_TOWARD_POSITIVE, ctx)
    }

    fun setNextDown(x: Decimal, ctx: DecimalContext) {
        set(x)
        when {
            qExp > NON_FINITE_INF -> { return }
            qExp == NON_FINITE_INF -> {
                if (sign == false)
                    setMaxFiniteMagnitude(ctx)
                return
            }
            u256IsZero() -> {
                setMinFiniteMagnitude(ctx)
                sign = true
                return
            }

            sign -> mutateNextAwayFromZero(ctx)

            else -> mutateNextTowardZero(ctx)
        }
        roundAndFinalize(EXACT, ROUND_TOWARD_NEGATIVE, ctx)
    }

    private fun mutateNextAwayFromZero(ctx: DecimalContext) {
        val headroom = min(ctx.precision - digitLen, qExp - ctx.qTiny)
        if (headroom > 1 || headroom == 1 && !u256IsAllNines(ctx.precision-1)) {
            this.u256SetScaleUpPow10(this, headroom)
            this.qExp -= headroom
        }
        u256MutateIncrement()
    }

    private fun mutateNextTowardZero(ctx: DecimalContext) {
        val headroom =
            min(ctx.precision - digitLen + if (u256IsPowerOf10()) 1 else 0, qExp - ctx.qTiny)
        if (headroom > 0) {
            this.u256SetScaleUpPow10(this, headroom)
            this.qExp -= headroom
        }
        u256MutateDecrement()
    }

    fun minNum(x: Decimal, y: Decimal, ctx: DecimalContext) = minNum_helper(x, y, 0, ctx)
    fun maxNum(x: Decimal, y: Decimal, ctx: DecimalContext) = minNum_helper(x, y, -1, ctx)

    private fun minNum_helper(x: Decimal, y: Decimal, invertCompareZeroOrNeg1: Int, ctx: DecimalContext) {
        val qMax = max(x.qExp, y.qExp)
        when {
            qMax <= NON_FINITE_INF -> {
                val cmp = (x.compareTo(y, ctx) xor invertCompareZeroOrNeg1) - invertCompareZeroOrNeg1
                set(if (cmp <= 0) x else y)
            }
            qMax == NON_FINITE_QNAN -> {
                set(if (x.qExp == NON_FINITE_QNAN) x else y)
            }
            else -> throw RuntimeException("somebody is a sNaN")
        }
    }

    fun minNumMag(x: Decimal, y: Decimal, ctx: DecimalContext) = minNumMag_helper(x, y, 0, ctx)
    fun maxNumMag(x: Decimal, y: Decimal, ctx: DecimalContext) = minNumMag_helper(x, y, -1, ctx)

    private fun minNumMag_helper(x: Decimal, y: Decimal, invertCompareZeroOrNeg1: Int, ctx: DecimalContext) {
        val qMax = max(x.qExp, y.qExp)
        when {
            qMax < NON_FINITE_INF -> {
                val cmp = (x.magnitudeCompareTo(y) xor invertCompareZeroOrNeg1) - invertCompareZeroOrNeg1
                set(if (cmp <= 0) x else y)
            }
            else -> minNum_helper(x, y, invertCompareZeroOrNeg1, ctx)
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun capExponentRange(e: Int): Int {
        return min(max(e, CAPPED_EXP_MIN), CAPPED_EXP_MAX)
    }

    fun setScale(x: Decimal, pow10: Int, ctx: DecimalContext) {
        set(x)
        val p10 = capExponentRange(pow10)
        if (qExp < NON_FINITE_INF) {
            qExp += p10
            val residue = when {
                u256IsZero() -> EXACT
                (p10 > 0) -> {
                    val headroom = ctx.precision - digitLen
                    val scaleUp = min(headroom, p10)
                    this.u256SetScaleUpPow10(this, scaleUp)
                    qExp -= scaleUp
                    EXACT
                }
                (p10 < 0) -> this.u256SetScaleDownPow10(this, -p10)
                else -> return // p10 == 0 .. no scaling
            }
            roundAndFinalize(residue, ctx)
        } else if (qExp == NON_FINITE_SNAN)
            sNaNOperand()
    }

    // IEEE754-2008 5.3.2
    fun quantize(x: Decimal, y: Decimal, ctx: DecimalContext) {
        val targetQ = y.qExp
        setScale(x, -targetQ, ctx)
    }

    // IEEE754-2008 5.3.3
    fun scaleB(x: Decimal, pow10: Int, ctx: DecimalContext) {
        set(x)
        when {
            qExp <= NON_FINITE_INF -> {
                val p10 = capExponentRange(pow10)
                qExp += p10
                if (qExp > ctx.qMax || qExp < ctx.qTiny)
                    roundAndFinalize(Residue.EXACT, ctx)
            }
            x.qExp <= NON_FINITE_QNAN -> {}
            else -> sNaNOperand()
        }
    }

    // IEEE754-2008 5.3.3
    fun logB(): Int {
        return qExp
    }

    fun compareQuiet754(other: Decimal, ctx: DecimalContext): Compare754Result =
        compare754(other, false, ctx)

    fun compareSignaling754(other: Decimal, ctx: DecimalContext): Compare754Result =
        compare754(other, true, ctx)

    fun compare754(other: Decimal, isSignaling: Boolean, ctx: DecimalContext): Compare754Result {
        val qMax = max(qExp, other.qExp)
        return when {
            qMax < NON_FINITE_INF -> when {
                u256IsZero() -> when {
                    other.u256IsZero() -> IEEE754_EQ
                    other.sign -> IEEE754_LT
                    else -> IEEE754_GT
                }

                other.u256IsZero() -> when {
                    sign -> IEEE754_GT
                    else -> IEEE754_LT
                }

                else -> {
                    val cmp = magnitudeCompareTo(other)
                    Compare754Result(cmp + 1)
                }
            }

            qMax == NON_FINITE_INF -> when {
                qExp == NON_FINITE_INF -> when {
                    other.qExp == NON_FINITE_INF -> when {
                        sign == other.sign -> IEEE754_EQ
                        sign == false -> IEEE754_GT
                        else -> IEEE754_LT
                    }

                    sign == false -> IEEE754_GT
                    else -> IEEE754_LT
                }

                other.sign == false -> IEEE754_LT
                else -> IEEE754_GT
            }

            !isSignaling && qMax == NON_FINITE_QNAN -> IEEE754_UNORDERED
            else -> {
                val guiltyParty = when {
                    qExp == NON_FINITE_SNAN -> this
                    other.qExp == NON_FINITE_SNAN -> other
                    qExp == NON_FINITE_QNAN -> this
                    else -> other
                }
                ctx.operandIsSignalingNaN(guiltyParty)
                IEEE754_UNORDERED
            }
        }
    }

    override fun equals(other: Any?) : Boolean {
        if (other is Decimal) {
            val qMax = max(qExp, other.qExp)
            return when {
                qMax < NON_FINITE_INF -> when {
                    u256IsZero() -> other.u256IsZero()
                    other.u256IsZero() -> false
                    else -> sign == other.sign && magnitudeEQ(other)
                }

                qMax == NON_FINITE_INF -> when {
                    qExp == NON_FINITE_INF && other.qExp == NON_FINITE_INF -> return sign == other.sign
                    else -> false
                }
                // FIXME ... and ... Why haven't my unit tests flushed out this case?
                else -> throw RuntimeException("somebody is a NaN")
            }
        }
        return false
    }

    override fun hashCode(): Int {
        var result = super.hashCode()  // includes sign + coefficient
        result = 31 * result + qExp
        return result
    }
    // 5.7.2 General operations

    fun valueClass(): Class754 {
        return when {
            qExp == NON_FINITE_SNAN -> signalingNaN
            qExp == NON_FINITE_QNAN -> quiteNaN
            qExp == NON_FINITE_INF ->
                return if (sign == false) positiveInfinity else negativeInfinity
            u256IsZero() ->
                return if (sign == false) positiveZero else negativeZero
            sciExp() < -6143 ->
                return if (sign == false) positiveSubnormal else negativeSubnormal
            sign == false ->
                return positiveNormal
            else ->
                negativeNormal
        }
    }

    fun isSignMinus() = sign
    fun isNormal() = qExp < NON_FINITE_INF && sciExp() >= -6143
    fun isFinite() = qExp < NON_FINITE_INF
    fun isZero() = qExp < NON_FINITE_INF && u256IsZero()
    fun isSubnormal() = qExp < NON_FINITE_INF && sciExp() < -6143
    fun isInfinite() = qExp == NON_FINITE_INF
    fun isNaN() = qExp in NON_FINITE_QNAN..NON_FINITE_SNAN
    fun isSignaling() = qExp == NON_FINITE_SNAN
    fun isCanonical() = true
    fun radix() = 10

    fun totalOrder(x: Decimal) {
        throw RuntimeException("not impl")
    }
    // 5.7.3 Decimal operation
    fun sameQuantum(x: Decimal) = (this.qExp == x.qExp)

    override fun toString() : String {
        return (if (sign) '-' else '+') +
                when {
                    (qExp < MIN_SPECIAL_VALUE) -> super.toString() + "E" + qExp
                    qExp == NON_FINITE_INF -> "Inf"
                    qExp == NON_FINITE_QNAN -> "NaN" + super.u256ToNaNDiagnosticString()
                    qExp == NON_FINITE_SNAN -> "sNaN" + super.u256ToNaNDiagnosticString()
                    else -> "?que? $qExp"
                }
    }

    internal fun roundAndFinalize(inboundResidue: Residue, ctx: DecimalContext) =
        roundAndFinalize(inboundResidue, ctx.roundingDirection, ctx)

    private fun roundAndFinalize(inboundResidue: Residue, roundingDirection: RoundingDirection, ctx: DecimalContext): Decimal {
        val eMax = ctx.eMax
        val precision = ctx.precision
        if (qExp < MIN_SPECIAL_VALUE) {
            if (bitLen != 0) {
                var eExp = qExp + (digitLen - 1)
                // IEEE754-2008 7.5: detect tininess on the unrounded result
                val isTiny = (eExp < ctx.eMin)

                val excess = max(0, digitLen - precision)
                val myQTiny = ctx.qTiny - excess      // threshold for normalized

                // 2) Normalized result: round only if bd has >precision digits
                if (eExp <= eMax && qExp >= myQTiny) {
                    val totalResidue =
                        if (excess == 0) {
                            inboundResidue
                        } else {
                            val roundingResidue = U256ScalePow10.u256ScaleDownPow10(this, this, excess)
                            qExp += excess
                            assert(digitLen == precision)
                            assert(eExp == qExp + (digitLen - 1))
                            roundingResidue.merge(inboundResidue)
                        }

                    if (totalResidue == EXACT) {
                        // 7.5 Underflow
                        // If the rounded result is exact, no flag is raised
                        // and no inexact exception is signaled.
                        return this
                    }

                    val roundUp = totalResidue.ulpRoundUp(roundingDirection.negate(sign), super.dw0)
                    if (roundUp)
                        super.u256MutateIncrement()
                    if (!roundUp || digitLen <= precision)
                        return if (isTiny) ctx.signalInexactUnderflow(this) else ctx.signalInexact(this)
                    assert(digitLen == precision + 1)
                    // if we rolled into another digit because of roundup
                    // then the result is EXACTly divisible by 10
                    val residueExact = U256ScalePow10.u256ScaleDownPow10(this, this, 1)
                    assert(residueExact == Residue.EXACT)
                    ++qExp
                    assert(qExp + (digitLen - 1) == eExp + 1)
                    ++eExp
                    if (eExp <= eMax)
                        return ctx.signalInexact(this)
                    // rounding caused overflow
                    // fall into next conditional and flow over
                }

                // 1) Overflow => +/- Infinity
                if (eExp > eMax) {
                    assert(! isTiny)
                    // overflow IEEE754-2008 7.4 Overflow page 37
                    if (roundingDirection.overflowsToInfinity(sign)) {
                        super.u256SetOne()
                        qExp = NON_FINITE_INF
                    } else {
                        setMaxFiniteMagnitude(ctx)
                    }
                    return ctx.signalInexactOverflow(this)
                }

                // 7.5.1: subnormal rounding (tiny result stays nonzero)
                assert(isTiny)
                val myQMin = ctx.qTiny - digitLen           // threshold for subnormal cohort
                val overlap = qExp - myQMin
                if (overlap >= 0) {
                    val excess2 = super.digitLen - overlap
                    val scaleResidue = U256ScalePow10.u256ScaleDownPow10(this, this, excess2)
                    qExp += excess2
                    assert(digitLen <= precision)
                    assert(eExp == qExp + (digitLen - 1))

                    val totalResidue = scaleResidue.merge(inboundResidue)
                    if (totalResidue == EXACT) {
                        // 7.5 Underflow
                        // If the rounded result is exact, no flag is raised
                        // and no inexact exception is signaled.
                        return this
                    }
                    val roundUp = totalResidue.ulpRoundUp(roundingDirection.negate(sign), super.dw0)
                    if (roundUp) {
                        super.u256MutateIncrement()
                        if (digitLen > precision) {
                            assert(digitLen == precision + 1)
                            // if we rolled into another digit because of roundup
                            // then the result is definitely divisible by 10
                            val residueExact = U256ScalePow10.u256ScaleDownPow10(this, this, 1)
                            assert(residueExact == Residue.EXACT)
                            ++qExp
                        }
                    }
                } else {
                    // underflow ... swamped non-zero value
                    if (roundingDirection.underflowsToZero(sign)) {
                        super.u256SetZero()
                        qExp = ctx.qTiny
                    } else {
                        setMinFiniteMagnitude(ctx)
                    }
                }
                return ctx.signalInexactUnderflow(this)
            }
            // zero case
            qExp = max(min(qExp, ctx.qMax), ctx.qTiny)
        }
        return this
    }

}
