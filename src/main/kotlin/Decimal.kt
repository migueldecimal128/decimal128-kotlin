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

class Decimal() : Coeff() {
    var qExp = 0
    var sign = false

    constructor(sign: Boolean, qExp: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long) : this() {
        this.coeffSet256(dw3, dw2, dw1, dw0)
        this.qExp = qExp
        this.sign = sign
    }

    constructor(str: String): this() {
        DecimalParsePrint.decFromString(this, str, DEFAULT_128_CONTEXT)
    }

    constructor(other: Decimal) : this(other.sign, other.qExp, other.dw3, other.dw2, other.dw1, other.dw0)

    fun sciExp() = qExp + (digitLen - 1)

    fun setZero()  = setZero(false)

    fun setZero(sign: Boolean) {
        coeffSetZero()
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
            ctx.setInvalid()
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

    fun setNaN(ctx: DecimalContext) {
        setZero()
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    fun setNaN(payload: Int, ctx: DecimalContext) {
        sign = false
        coeffSet64(payload.toLong())
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    fun setSNaN(ctx: DecimalContext) {
        setZero()
        qExp = NON_FINITE_SNAN
    }

    fun setInfinite(sign: Boolean) {
        this.coeffSetOne()
        this.qExp = NON_FINITE_INF
        this.sign = sign
    }

    fun setInt(l: Long) {
        this.qExp = 0
        this.sign = l < 0
        val mask = l shr 63
        val abs = (l xor mask) - mask
        coeffSet64(abs)
    }

    fun setUInt(ul: Long) {
        this.qExp = 0
        this.sign = false
        coeffSet64(ul)
    }

    fun set(x: Decimal) {
        coeffSet(x)
        this.qExp = x.qExp
        this.sign = x.sign
    }

    fun set(str: String) {
        DecimalParsePrint.decFromString(this, str, DEFAULT_128_CONTEXT)
    }

    fun setMag(x: Decimal) {
        coeffSet(x)
        this.qExp = x.qExp
    }

    fun setMaxFiniteMagnitude(ctx: DecimalContext) {
        qExp = ctx.qMax
        // 0x378D8E6400000000uL.toLong(), 0x0001ED09BEAD87C0uL.toLong(),
        // 10000000000000000000000000000000000 (10**34)
        val offset = CoeffPow10.pow10Offset(ctx.precision)
        if (ctx.precision < MIN_POW10_DIGIT_LEN_128) {
            super.coeffSet64(POW10[offset] - 1)
        } else if (ctx.precision < MIN_POW10_DIGIT_LEN_192) {
            super.coeffSet128(POW10[offset + 1], POW10[offset] - 1)
        } else
            throw IllegalArgumentException()
    }

    fun setMinFiniteMagnitude(ctx: DecimalContext) {
        qExp = ctx.qTiny
        super.coeffSetOne()
    }

    fun copy(x: Decimal) = set(x)

    fun setNegate(x: Decimal) {
        set(x)
        this.sign = !this.sign
    }

    fun mutateNegate() {
        this.sign = !this.sign
    }

    fun setAbs(x: Decimal) {
        set(x)
        this.sign = false
    }

    fun mutateAbs() {
        this.sign = false
    }

    fun copySign(x: Decimal, y: Decimal) {
        set(x)
        this.sign = y.sign
    }

    fun isNegative() = sign

    fun isNumber() : Boolean {
        return qExp <= NON_FINITE_INF
    }

    // IEEE754-2008 5.4.1
    fun add(x: Decimal, y: Decimal, ctx: DecimalContext) = addImpl(x, y.sign, y, ctx)

    // IEEE754-2008 5.4.1
    fun sub(x: Decimal, y: Decimal, ctx: DecimalContext) = addImpl(x, !y.sign, y, ctx)

    private fun addImpl(x: Decimal, ySign: Boolean, y: Decimal, ctx: DecimalContext) {
        val qX = x.qExp
        val qY = y.qExp
        val xSign = x.sign
        val qMax = max(qX, qY)
        val qMin = min(qX, qY)
        when {
            qMax < MIN_SPECIAL_VALUE -> {
                val residue = when {
                    ! (xSign xor ySign) -> {
                        this.sign = xSign
                        MagnitudeAddSub.magAdd(this, x, y)
                    }
                    (x.magnitudeCompareTo(y) >= 0) -> {
                        this.sign = xSign
                        MagnitudeAddSub.magSub(this, x, y)
                    }
                    else -> {
                        this.sign = ySign
                        MagnitudeAddSub.magSub(this, y, x)
                    }
                }
                roundAndFinalize(residue, ctx)
            }
            qMax == NON_FINITE_INF -> when {
                (xSign == ySign) -> {
                    setInfinite(xSign)
                }
                qMin == NON_FINITE_INF -> {
                    ctx.setInvalid()
                    setNaN(ctx)
                }
                qX == NON_FINITE_INF -> {
                    setInfinite(xSign)
                }
                else -> {
                    setInfinite(ySign)
                }
            }
            else -> {
                setNaN(x, y, ctx)
            }
        }
    }

    // IEEE754-2008 5.4.1
    fun mul(x: Decimal, y: Decimal, ctx: DecimalContext) {
        val qX = x.qExp
        val qY = y.qExp
        val productSign = x.sign xor y.sign
        val qMaxXY = max(qX, qY)
        when {
            qMaxXY < NON_FINITE_INF -> {
                this.coeffSetMul(x, y)
                this.qExp = x.qExp + y.qExp
                this.sign = productSign
                roundAndFinalize(EXACT, ctx)
            }
            qMaxXY == NON_FINITE_INF -> {
                if (x.coeffIsZero() || y.coeffIsZero()) {
                    ctx.setInvalid()
                    setNaN(ctx)
                } else {
                    setInfinite(productSign)
                }
            }
            else -> setNaN(x, y, ctx)
        }
    }

    fun sqr(x: Decimal, ctx: DecimalContext) {
        val qX = x.qExp
        when {
            qX < NON_FINITE_INF -> {
                this.coeffSetSqr(x)
                this.qExp = this.qExp shl 1
                this.sign = false
                roundAndFinalize(EXACT, ctx)            }
            qX == NON_FINITE_INF -> {
                setInfinite(false)
            }
            else -> setNaN(x, ctx)
        }
    }

    // IEEE754-2008 5.4.1
    fun fma(x: Decimal, y: Decimal, a: Decimal, ctx: DecimalContext) {
        val qX = x.qExp
        val qY = y.qExp
        val qA = a.qExp
        val qMaxXY = max(qX, qY)
        val qMaxXYA = max(qMaxXY, qA)
        val productSign = x.sign xor y.sign
        when {
            qMaxXYA < NON_FINITE_INF -> {
                var aT = if (this === a) Decimal(a) else a
                // multiply without roundAndFinalize .. remains exact
                this.coeffSetMul(x, y)
                this.qExp = x.qExp + y.qExp
                this.sign = productSign
                // roundAndFinalize takes place here
                this.add(this, aT, ctx)
            }
            qMaxXYA == NON_FINITE_INF -> when {
                (x.coeffIsZero() || y.coeffIsZero()) -> {
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
    }

    fun div(x: Decimal, y: Decimal, ctx: DecimalContext) {
        val qX = x.qExp
        val qY = y.qExp
        val quotientSign = x.sign xor y.sign
        val qMaxXY = max(qX, qY)
        when {
            qMaxXY < NON_FINITE_INF -> {
                when {
                    (y.bitLen > 0) -> {
                        val residue = MagnitudeDiv.magDiv(this, x, y)
                        this.sign = quotientSign
                        roundAndFinalize(residue, ctx)
                    }
                    (x.bitLen > 0) -> {
                        // finite division by zero
                        setInfinite(quotientSign)
                        ctx.setDivByZero()
                    }
                    else -> {
                        // zero divided by zero
                        setNaN(ctx)
                        ctx.setInvalid()
                    }
                }
            }
            qMaxXY == NON_FINITE_INF -> {
                when {
                    (qX == NON_FINITE_INF && qY == NON_FINITE_INF) -> {
                        ctx.setInvalid()
                        setNaN(ctx)
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
    }

    fun compareTo(other: Decimal, ctx: DecimalContext) : Int {
        val qMax = max(qExp, other.qExp)
        when {
            (qMax < MIN_SPECIAL_VALUE) -> {
                if (coeffIsZero()) {
                    if (other.coeffIsZero())
                        return 0
                    else
                        return if (other.sign) 1 else -1
                }
                if (other.coeffIsZero() || sign != other.sign) {
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
        val thisIsZero = coeffIsZero()
        val otherIsZero = other.coeffIsZero()
        val eitherIsZero = thisIsZero or otherIsZero
        when {
            !eitherIsZero -> {
                val cmpExpSci = this.sciExp().compareTo(other.sciExp())
                if (cmpExpSci != 0)
                    return cmpExpSci
                val expDelta = this.qExp - other.qExp
                val ret = when {
                    expDelta == 0 -> coeffUnscaledCompareTo(other)
                    expDelta > 0 -> -other.coeffScaledCompareTo(this, expDelta)
                    else -> coeffScaledCompareTo(other, -expDelta)
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
        val thisIsZero = this.coeffIsZero()
        val otherIsZero = other.coeffIsZero()
        val bothAreZero = thisIsZero and otherIsZero
        val eitherIsZero = thisIsZero or otherIsZero
        if (this.sciExp() != other.sciExp())
            return bothAreZero
        if (! eitherIsZero) {
            val expDelta = this.qExp - other.qExp
            return when {
                expDelta == 0 -> this.coeffUnscaledEQ(other)
                expDelta > 0 -> other.coeffScaledEQ(this, expDelta)
                else -> this.coeffScaledEQ(other, -expDelta)
            }
        }
        return bothAreZero
    }


    fun roundToIntegral(x: Decimal, rd: RoundingDirection, ctx: DecimalContext) {
        //FIXME - deal with special values
        if (qExp < 0) {
            val residue = this.coeffSetScaleDownPow10(x, -qExp)
            qExp = 0
            sign = x.sign
            roundAndFinalize(residue, rd, ctx)
        } else {
            set(x)
        }
    }

    fun roundToIntegralTiesToEven(x: Decimal, ctx: DecimalContext) =
        roundToIntegral(x, ROUND_TIES_TO_EVEN, ctx)

    fun roundToIntegralTiesToAway(x: Decimal, ctx: DecimalContext) =
        roundToIntegral(x, ROUND_TIES_TO_AWAY, ctx)

    fun roundToIntegralTowardZero(x: Decimal, ctx: DecimalContext) =
        roundToIntegral(x, ROUND_TOWARD_ZERO, ctx)

    fun roundToIntegralTowardPositive(x: Decimal, ctx: DecimalContext) =
        roundToIntegral(x, ROUND_TOWARD_POSITIVE, ctx)

    fun roundToIntegralTowardNegative(x: Decimal, ctx: DecimalContext) =
        roundToIntegral(x, ROUND_TOWARD_NEGATIVE, ctx)

    fun setNextUp(x: Decimal, ctx: DecimalContext) {
        set(x)
        when {
            qExp > NON_FINITE_INF -> { return }
            qExp == NON_FINITE_INF -> {
                if (sign)
                    setMaxFiniteMagnitude(ctx)
                return
            }
            coeffIsZero() -> {
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
            coeffIsZero() -> {
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
        if (headroom > 1 || headroom == 1 && !coeffIsAllNines(ctx.precision-1)) {
            this.coeffSetScaleUpPow10(this, headroom)
            this.qExp -= headroom
        }
        coeffMutateIncrement()
    }

    private fun mutateNextTowardZero(ctx: DecimalContext) {
        val headroom =
            min(ctx.precision - digitLen + if (coeffIsPowerOf10()) 1 else 0, qExp - ctx.qTiny)
        if (headroom > 0) {
            this.coeffSetScaleUpPow10(this, headroom)
            this.qExp -= headroom
        }
        coeffMutateDecrement()
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
                coeffIsZero() -> EXACT
                (p10 > 0) -> {
                    val headroom = ctx.precision - digitLen
                    val scaleUp = min(headroom, p10)
                    this.coeffSetScaleUpPow10(this, scaleUp)
                    qExp -= scaleUp
                    EXACT
                }
                (p10 < 0) -> this.coeffSetScaleDownPow10(this, -p10)
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
                coeffIsZero() -> when {
                    other.coeffIsZero() -> IEEE754_EQ
                    other.sign -> IEEE754_LT
                    else -> IEEE754_GT
                }

                other.coeffIsZero() -> when {
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
                    coeffIsZero() -> other.coeffIsZero()
                    other.coeffIsZero() -> false
                    else -> sign == other.sign && magnitudeEQ(other)
                }

                qMax == NON_FINITE_INF -> when {
                    qExp == NON_FINITE_INF && other.qExp == NON_FINITE_INF -> return sign == other.sign
                    else -> false
                }

                else -> throw RuntimeException("somebody is a NaN")
            }
        }
        return false
    }

    // 5.7.2 General operations

    fun valueClass(): Class754 {
        return when {
            qExp == NON_FINITE_SNAN -> signalingNaN
            qExp == NON_FINITE_QNAN -> quiteNaN
            qExp == NON_FINITE_INF ->
                return if (sign == false) positiveInfinity else negativeInfinity
            coeffIsZero() ->
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
    fun isZero() = qExp < NON_FINITE_INF && coeffIsZero()
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
                    qExp == NON_FINITE_QNAN -> "NaN" + super.coeffToNaNDiagnosticString()
                    qExp == NON_FINITE_SNAN -> "sNaN" + super.coeffToNaNDiagnosticString()
                    else -> "?que? $qExp"
                }
    }



    fun roundAndFinalize(inboundResidue: Residue, ctx: DecimalContext) =
        roundAndFinalize(inboundResidue, ctx.roundingDirection, ctx)

    fun roundAndFinalize(inboundResidue: Residue, roundingDirection: RoundingDirection, ctx: DecimalContext) {
        val eMax = ctx.eMax
        val eMin = ctx.eMin
        val precision = ctx.precision
        if (qExp < NON_FINITE_INF) {
            if (bitLen != 0) {
                var eExp = qExp + (digitLen - 1)
                // IEEE754-2008 7.5: detect tininess on the unrounded result
                if (eExp < eMin) {
                    ctx.setUnderflow() // IEEE754-2008 7.5 Underflow page 38
                }

                val excess = max(0, digitLen - precision)
                val myQTiny = ctx.qTiny - excess      // threshold for normalized

                // 2) Normalized result: round only if bd has >34 digits
                if (eExp <= eMax && qExp >= myQTiny) {
                    val totalResidue =
                        if (excess == 0) {
                            inboundResidue
                        } else {
                            val roundingResidue = CoeffScalePow10.coeffScaleDownPow10(this, this, excess)
                            qExp += excess
                            assert(digitLen == precision)
                            assert(eExp == qExp + (digitLen - 1))
                            roundingResidue.merge(inboundResidue)
                        }

                    if (totalResidue == EXACT)
                        return

                    ctx.setInexact()
                    val roundUp = totalResidue.ulpRoundUp(roundingDirection.negate(sign), super.dw0)
                    if (!roundUp)
                        return
                    super.coeffMutateIncrement()
                    if (digitLen <= precision)
                        return
                    assert(digitLen == precision + 1)
                    // if we rolled into another digit because of roundup
                    // then the result is EXACTly divisible by 10
                    val residueExact = CoeffScalePow10.coeffScaleDownPow10(this, this, 1)
                    assert(residueExact == Residue.EXACT)
                    ++qExp
                    assert(qExp + (digitLen - 1) == eExp + 1)
                    ++eExp
                    if (eExp <= eMax)
                        return
                    // rounding caused overflow
                    // fall into next conditional and flow over
                }

                // 1) Overflow => +/- Infinity
                if (eExp > eMax) {
                    // overflow IEEE754-2008 7.4 Overflow page 37
                    if (roundingDirection.overflowsToInfinity(sign)) {
                        super.coeffSetOne()
                        qExp = NON_FINITE_INF
                    } else {
                        setMaxFiniteMagnitude(ctx)
                    }
                    ctx.setOverflow()
                    ctx.setInexact()
                    return
                }

                // 7.5.1: subnormal rounding (tiny result stays nonzero)
                val myQMin = ctx.qTiny - digitLen           // threshold for subnormal cohort
                val overlap = qExp - myQMin
                if (overlap >= 0) {
                    val excess2 = super.digitLen - overlap
                    val scaleResidue = CoeffScalePow10.coeffScaleDownPow10(this, this, excess2)
                    qExp += excess2
                    assert(digitLen <= precision)
                    assert(eExp == qExp + (digitLen - 1))

                    val totalResidue = scaleResidue.merge(inboundResidue)
                    if (totalResidue == EXACT)
                        return
                    ctx.setInexact()
                    val roundUp = totalResidue.ulpRoundUp(roundingDirection.negate(sign), super.dw0)
                    if (!roundUp)
                        return
                    super.coeffMutateIncrement()
                    if (digitLen <= precision)
                        return
                    assert(digitLen == precision + 1)
                    // if we rolled into another digit because of roundup
                    // then the result is definitely divisible by 10
                    val residueExact = CoeffScalePow10.coeffScaleDownPow10(this, this, 1)
                    assert(residueExact == Residue.EXACT)
                    ++qExp
                    return
                }

                // underflow ... swamped non-zero value
                if (roundingDirection.underflowsToZero(sign)) {
                    super.coeffSetZero()
                    qExp = NON_FINITE_INF
                } else {
                    setMinFiniteMagnitude(ctx)
                }
                qExp = ctx.qTiny
                ctx.setUnderflow()
                ctx.setInexact()
                return
            }
            // zero case
            qExp = max(min(qExp, ctx.qMax), ctx.qTiny)
        }
    }

}
