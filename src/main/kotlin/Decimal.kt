package com.decimal128

import com.decimal128.Residue.Companion.EXACT
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_NEGATIVE
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_ZERO
import com.decimal128.Class754.*
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.max
import kotlin.math.min

class Decimal() : Mag() {
    var sign = 0

    var DEFAULT_128_CONTEXT = DecimalContext.newDecimal128Context()

    constructor(sign: Int, qExp: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long) : this() {
        this.coeffSet256(dw3, dw2, dw1, dw0)
        this.qExp = qExp
        this.sign = sign and 1
    }

    constructor(bd: BigDecimal): this() {
        this.coeffSet(bd.abs().unscaledValue())
        this.qExp = -bd.scale()
        this.sign = bd.signum() ushr 31
        roundAndFinalize(EXACT, DEFAULT_128_CONTEXT)
    }

    constructor(str: String): this(BigDecimal(str))

    constructor(other: Decimal) : this(other.sign, other.qExp, other.dw3, other.dw2, other.dw1, other.dw0)

    fun setZero() {
        magSetZero()
        this.sign = 0
    }

    fun setZero(sign: Int) {
        assert((sign shr 1) == 0)
        this.sign = sign
    }

    private fun setNaN(x: Decimal, y: Decimal, ctx: DecimalContext) {
        val xQ = x.qExp
        val yQ = y.qExp
        val maxQ = max(xQ, yQ)
        assert(maxQ >= NON_FINITE_QNAN)
        magSetZero()
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
        magSetZero()
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    private fun sNaNOperand() {
        throw RuntimeException("sNaN operand")
    }

    private fun setNaN(x: Decimal, ctx: DecimalContext) {
        val q = x.qExp
        assert(q >= NON_FINITE_QNAN)
        magSetZero()
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    fun setNaN(ctx: DecimalContext) {
        magSetZero()
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    fun setNaN(payload: Int, ctx: DecimalContext) {
        sign = 0
        coeffSet64(payload.toLong())
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    fun setSNaN(ctx: DecimalContext) {
        magSetZero()
        qExp = NON_FINITE_SNAN
    }

    fun setInfinite(sign: Int) {
        this.coeffSetOne()
        this.qExp = NON_FINITE_INF
        this.sign = sign
    }

    fun setInt(l: Long) {
        this.qExp = 0
        this.sign = (l ushr 63).toInt()
        val mask = l shr 63
        val abs = (l xor mask) - mask
        coeffSet64(abs)
    }

    fun setUInt(ul: Long) {
        this.qExp = 0
        this.sign = 0
        coeffSet64(ul)
    }

    fun set(bi: BigInteger) {
        if (bi.bitLength() <= 256) {
            this.qExp = 0
            val sign = bi.signum() ushr 31
            this.sign = sign
            val biT = if (sign == 0) bi else bi.abs()
            val d0 = biT.toLong()
            val d1 = biT.shiftRight( 64).toLong()
            val d2 = biT.shiftRight(128).toLong()
            val d3 = biT.shiftRight(192).toLong()
            coeffSet256(d3, d2, d1, d0)
        }
        val bd = BigDecimal(bi)
        set(bd)
    }

    fun set(bd: BigDecimal) {
        this.coeffSet(bd.abs().unscaledValue())
        this.qExp = -bd.scale()
        this.sign = bd.signum() shr 31
        roundAndFinalize(EXACT, DEFAULT_128_CONTEXT)
    }

    fun set(x: Decimal) {
        magSet(x)
        this.sign = x.sign
    }

    fun copy(x: Decimal) = set(x)

    fun setNegate(x: Decimal) {
        set(x)
        this.sign = this.sign xor 1
    }

    fun mutateNegate() {
        this.sign = this.sign xor 1
    }

    fun setAbs(x: Decimal) {
        set(x)
        this.sign = 0
    }

    fun mutateAbs() {
        this.sign = 0
    }

    fun copySign(x: Decimal, y: Decimal) {
        this.sign = y.sign
        magSet(x)
    }

    fun isNegative() : Boolean {
        return sign == 1
    }

    fun isNumber() : Boolean {
        return qExp <= NON_FINITE_INF
    }

    // IEEE754-2008 5.4.1
    fun add(x: Decimal, y: Decimal, ctx: DecimalContext) = addImpl(x, y.sign, y, ctx)

    // IEEE754-2008 5.4.1
    fun sub(x: Decimal, y: Decimal, ctx: DecimalContext) = addImpl(x, y.sign xor 1, y, ctx)

    private fun addImpl(x: Decimal, ySign: Int, y: Decimal, ctx: DecimalContext) {
        val qX = x.qExp
        val qY = y.qExp
        val xSign = x.sign
        val qMax = max(qX, qY)
        val qMin = min(qX, qY)
        when {
            qMax < NON_FINITE_INF -> {
                val residue = when {
                    (xSign xor ySign) == 0 -> {
                        this.sign = xSign
                        this.magAdd(x, y, xSign, ctx)
                    }
                    (x.magCompareTo(y) >= 0) -> {
                        this.sign = xSign
                        this.magSub(x, y, xSign, ctx)
                    }
                    else -> {
                        this.sign = ySign
                        this.magSub(y, x, ySign, ctx)
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
                this.magMul(x, y, productSign, ctx)
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
                this.magSqr(x, ctx)
                this.sign = 0
                roundAndFinalize(EXACT, ctx)            }
            qX == NON_FINITE_INF -> {
                setInfinite(0)
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
                MagMul.magMul(this, x, y)
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
                        val residue = this.magDiv(x, y, quotientSign, ctx)
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



    fun compareTo(x: Decimal, ctx: DecimalContext) : Int {
        val qMax = max(qExp, x.qExp)
        when {
            (qMax < NON_FINITE_INF) -> {
                if (coeffIsZero()) {
                    if (x.coeffIsZero())
                        return 0
                    else
                        return (x.sign shl 1) - 1
                }
                if (x.coeffIsZero()) {
                    return 1 - (sign shl 1)
                }
                if (sign != x.sign)
                    return 1 - (sign shl 1)
                val cmp = magCompareTo(x)
                val ret = (cmp xor -sign) + sign
                return ret
            }

            (qMax == NON_FINITE_INF) -> when {
                (sign != x.sign) -> {
                    return 1 - (sign shl 1)
                }

                (qExp == NON_FINITE_INF) -> {
                    if (x.qExp == NON_FINITE_INF)
                        return 0
                    return 1 - (sign shl 1)
                }

                else -> {
                    return (sign shl 1) - 1
                }
            }
            else -> throw RuntimeException("somebody is a NaN")
        }
    }

    fun roundToIntegral(x: Decimal, rd: RoundingDirection, ctx: DecimalContext) {
        //FIXME - deal with special values
        if (qExp < 0) {
            sign = x.sign
            val residue = this.coeffSetScaleDownPow10(x, -qExp)
            qExp = 0
            roundAndFinalize(residue, rd, ctx)
        } else {
            magSet(x)
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
                if (sign == 1)
                    magSetMaxFinite(ctx)
                return
            }
            coeffIsZero() -> {
                magSetMinFinite(ctx)
                sign = 0
                return
            }
            sign == 0 -> mutateNextAwayFromZero(ctx)
            else -> mutateNextTowardZero(ctx)
        }
        roundAndFinalize(EXACT, ROUND_TOWARD_POSITIVE, ctx)
    }

    fun setNextDown(x: Decimal, ctx: DecimalContext) {
        set(x)
        when {
            qExp > NON_FINITE_INF -> { return }
            qExp == NON_FINITE_INF -> {
                if (sign == 0)
                    magSetMaxFinite(ctx)
                return
            }
            coeffIsZero() -> {
                magSetMinFinite(ctx)
                sign = 1
                return
            }

            sign != 0 -> mutateNextAwayFromZero(ctx)

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
                val cmp = (x.magCompareTo(y) xor invertCompareZeroOrNeg1) - invertCompareZeroOrNeg1
                set(if (cmp <= 0) x else y)
            }
            else -> minNum_helper(x, y, invertCompareZeroOrNeg1, ctx)
        }
    }

    fun setScale(x: Decimal, pow10: Int, ctx: DecimalContext) {
        set(x)
        val p10 = capExponentRange(pow10)
        if (qExp < NON_FINITE_INF) {
            val residue = when {
                (p10 > 0) -> magMutateScaleUpPow10(p10, sign, ctx)
                (p10 < 0) -> magMutateScaleDownPow10(-p10, sign, ctx)
                else -> return
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
                    (other.sign != 0) -> IEEE754_LT
                    else -> IEEE754_GT
                }

                other.coeffIsZero() -> when {
                    sign != 0 -> IEEE754_GT
                    else -> IEEE754_LT
                }

                else -> {
                    val cmp = magCompareTo(other)
                    Compare754Result(cmp + 1)
                }
            }

            qMax == NON_FINITE_INF -> when {
                qExp == NON_FINITE_INF -> when {
                    other.qExp == NON_FINITE_INF -> when {
                        sign == other.sign -> IEEE754_EQ
                        sign == 0 -> IEEE754_GT
                        else -> IEEE754_LT
                    }

                    sign == 0 -> IEEE754_GT
                    else -> IEEE754_LT
                }

                other.sign == 0 -> IEEE754_LT
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
                    else -> sign == other.sign && magEQ(other)
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
                return if (sign == 0) positiveInfinity else negativeInfinity
            coeffIsZero() ->
                return if (sign == 0) positiveZero else negativeZero
            sciExp() < -6143 ->
                return if (sign == 0) positiveSubnormal else negativeSubnormal
            sign == 0 ->
                return positiveNormal
            else ->
                negativeNormal
        }
    }

    fun isSignMinus() = sign == 1
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
        return (if (sign == 0) '+' else '-') + super.toString()
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
                        magSetMaxFinite(ctx)
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
                    magSetMinFinite(ctx)
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
