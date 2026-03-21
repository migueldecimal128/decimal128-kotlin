// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_NEGATIVE
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_ZERO
import com.decimal128.decimal.Ieee754Class.*
import kotlin.math.max
import kotlin.math.min

internal const val NON_FINITE_INF = 16380
internal const val NON_FINITE_QNAN = 16381
internal const val NON_FINITE_SNAN = 16382

const val CAPPED_EXP_MIN = -7000
const val CAPPED_EXP_MAX = 7000

class MutDec() : C256(), Comparable<MutDec> {
    var type: Int = STEAL_TYPE_ZER
    var sign: Boolean
        get() = stealSignFlag(steal)
        set(value) {
            steal = stealWithSignFlag(steal, value)
        }
    val signMask: Int
        get() = stealSignMask(steal)
    val signBit: Int
        get() = stealSignBit(steal)


    var qExp = 0
    val eExp: Int
        get() = qExp + digitLen - 1

    fun bExpMin(): Int = calcBExpMin(bitLen, qExp)
    fun bExpMax(): Int = calcBExpMax(bitLen, qExp)

    companion object {

        fun decodeLittleEndianBid128(littleEndianLongs: LongArray) =
            decodeLittleEndianBid128(MutDec(), littleEndianLongs)
        fun decodeLittleEndianBid128(littleEndianBytes: ByteArray) =
            decodeLittleEndianBid128(MutDec(), littleEndianBytes)
        fun decodeBigEndianBid128(bigEndianLongs: LongArray) =
            decodeBigEndianBid128(MutDec(), bigEndianLongs)
        fun decodeBigEndianBid128(bigEndianBytes: ByteArray) =
            decodeBigEndianBid128(MutDec(), bigEndianBytes)

        fun decodeLittleEndianDpd128(littleEndianLongs: LongArray) =
            decodeLittleEndianDpd128(MutDec(), littleEndianLongs)
        fun decodeLittleEndianDpd128(littleEndianBytes: ByteArray) =
            decodeLittleEndianDpd128(MutDec(), littleEndianBytes)
        fun decodeBigEndianDpd128(bigEndianLongs: LongArray) =
            decodeBigEndianDpd128(MutDec(), bigEndianLongs)
        fun decodeBigEndianDpd128(bigEndianBytes: ByteArray) =
            decodeBigEndianDpd128(MutDec(), bigEndianBytes)

        private const val MAX_MASK = 1
        private const val MAG_MASK = 2
        private const val NUM_MASK = 4

        private const val MIN_OP = 0
        private const val MAX_OP = MAX_MASK
        private const val MIN_MAG_OP = MAG_MASK
        private const val MAX_MAG_OP = MAX_MASK or MAG_MASK
        private const val MIN_NUM_OP = NUM_MASK
        private const val MAX_NUM_OP = MAX_MASK or NUM_MASK
        private const val MIN_MAG_NUM_OP = MAG_MASK or NUM_MASK
        private const val MAX_MAG_NUM_OP = MAX_MASK or MAG_MASK or NUM_MASK

        private const val M_U32_DIV_1E1 = 0xCCCCCCCDL
        private const val S_U32_DIV_1E1 = 35

        private const val M_U32_DIV_1E2 = 0x51EB851FL
        private const val S_U32_DIV_1E2 = 37

        private const val M_U32_DIV_1E4 = 0x346DC5D7L
        private const val S_U32_DIV_1E4 = 43


    }

    internal fun validate(): Boolean {
        if (bitLen != calcBitLen256(dw3, dw2, dw1, dw0))
            return false
        if (digitLen != calcDigitLen256(bitLen, dw3, dw2, dw1, dw0))
            return false;
        when (type) {
            STEAL_TYPE_ZER -> {
                if (bitLen > 0)
                    return false
                if (qExp >= NON_FINITE_INF)
                    return false
            }
            STEAL_TYPE_FNZ -> {
                if (bitLen == 0)
                    return false
                if (qExp >= NON_FINITE_INF)
                    return false
            }
            STEAL_TYPE_INF -> {
                if (bitLen != 0)
                    return false
                if (qExp != NON_FINITE_INF)
                    return false
            }
            STEAL_NAN_SNAN -> {
                if (qExp != NON_FINITE_SNAN)
                    return false
            }
            STEAL_NAN_QNAN -> {
                if (qExp != NON_FINITE_QNAN)
                    return false
            }
            else -> return false
        }
        return true
    }

    // if the digitLen is non-zero then subtract 1
    // if digitLen == 0 then sciExp stays 0 ... 0e0
    fun sciExp() = qExp + (digitLen - (-digitLen ushr 31))

    fun setZero() = setZero(false)

    fun setZero(sign: Boolean): MutDec {
        c256SetZero()
        this.type = STEAL_TYPE_ZER
        this.qExp = 0
        this.sign = sign
        verify { validate() }
        return this
    }

    fun setZero(sign: Boolean = false, qExp: Int = 0, ctx: DecContext): MutDec {
        c256SetZero()
        this.type = STEAL_TYPE_ZER
        this.qExp = max(min(qExp, Q_MAX), Q_TINY)
        this.sign = sign
        verify { validate() }
        return this
    }

    fun setOne(sign: Boolean = false): MutDec {
        c256SetOne()
        this.type = STEAL_TYPE_FNZ
        this.qExp = 0
        this.sign = sign
        verify { validate() }
        return this
    }

    internal fun setNaNOperand(x: MutDec, ctx: DecContext): MutDec {
        val xQ = x.qExp
        verify { xQ >= NON_FINITE_QNAN }
        this.set(x)
        this.type = STEAL_NAN_QNAN
        this.qExp = NON_FINITE_QNAN
        if (xQ == NON_FINITE_SNAN)
            ctx.signalInvalid(this)
        verify { validate() }
        return this
    }

    internal fun setNaNOperand(x: MutDec, y: MutDec, ctx: DecContext): MutDec {
        val xQ = x.qExp
        val yQ = y.qExp
        val maxQ = max(xQ, yQ)
        verify { maxQ >= NON_FINITE_QNAN }
        this.set(if (maxQ == xQ) x else y)
        this.type = STEAL_NAN_QNAN
        this.qExp = NON_FINITE_QNAN
        if (maxQ == NON_FINITE_SNAN)
            ctx.signalInvalid(this)
        verify { validate() }
        return this
    }

    private fun sNaNOperand() {
        throw RuntimeException("sNaN operand")
    }

    internal fun setNaN(x: MutDec, ctx: DecContext): MutDec {
        // FIXME -- remove ctx from call NO! signal
        // FIXME -- shouldn't this be copying the payload x.coeff
        val q = x.qExp
        verify { q >= NON_FINITE_QNAN }
        setZero()
        type = STEAL_NAN_QNAN
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
        verify { validate() }
        return this
    }

    internal fun setNaN(ctx: DecContext) {
        setZero()
        sign = false
        type = STEAL_NAN_QNAN
        qExp = NON_FINITE_QNAN
        verify { validate() }
    }

    internal fun quietSNaN() {
        verify { isSignaling() }
        // FIXME -- this use of qExp has to stop
        verify { type == STEAL_NAN_SNAN }
        type = STEAL_NAN_QNAN
        qExp = NON_FINITE_QNAN
    }

    internal fun setNaNSignalInvalid(ctx: DecContext) {
        setNaN(ctx)
        ctx.signalInvalid(this)
        verify { validate() }
    }

    internal fun setNaN(payload: Int, ctx: DecContext) {
        sign = false
        c256Set64(payload.toLong())
        type = STEAL_NAN_QNAN
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
        verify { validate() }
    }

    internal fun setNaN(): MutDec {
        setZero()
        type = STEAL_NAN_QNAN
        qExp = NON_FINITE_QNAN
        verify { validate() }
        return this
    }

    internal fun setNaN(isSignaling: Boolean, sign: Boolean, payloadHi: Long, payloadLo: Long) {
        this.sign = sign
        this.type = if (isSignaling) STEAL_NAN_SNAN else STEAL_NAN_QNAN
        this.qExp = if (isSignaling) NON_FINITE_SNAN else NON_FINITE_QNAN
        c256Set128(payloadHi and ((1L shl (110 - 64)) - 1L), payloadLo)
        verify { validate() }
    }

    fun setSNaN(ctx: DecContext) {
        setZero()
        this.type = STEAL_NAN_SNAN
        qExp = NON_FINITE_SNAN
        verify { validate() }
    }

    fun setInfinite(sign: Boolean = false): MutDec {
        // NOTE miguel 2025-09-30
        //  Infinity must have coefficient zero (not one) because that is
        //  what is required for BID and DPD encoding.
        //  Changing the coefficient to one would make negation slightly
        //  easier, but isn't worth doing
        this.c256SetZero()
        this.type = STEAL_TYPE_INF
        this.qExp = NON_FINITE_INF
        this.sign = sign
        verify { validate() }
        return this
    }

    fun set(n: Int): MutDec = set(n.toLong())

    fun set(l: Long): MutDec {
        this.type = if (l == 0L) STEAL_TYPE_ZER else STEAL_TYPE_FNZ
        this.qExp = 0
        this.sign = l < 0
        val mask = l shr 63
        val abs = (l xor mask) - mask
        c256Set64(abs)
        verify { validate() }
        return this
    }

    fun setUnsigned(ul: Long): MutDec {
        this.type = if (ul == 0L) STEAL_TYPE_ZER else STEAL_TYPE_FNZ
        this.qExp = 0
        this.sign = false
        c256Set64(ul)
        verify { validate() }
        return this
    }

    fun set(l: Long, qExp: Int, ctx: DecContext): MutDec {
        this.type = if (l == 0L) STEAL_TYPE_ZER else STEAL_TYPE_FNZ
        this.qExp = capExponentRange(qExp)
        this.sign = l < 0
        val mask = l shr 63
        val abs = (l xor mask) - mask
        c256Set64(abs)
        verify { validate() }
        return this
    }

    fun set(x: MutDec): MutDec {
        if (this !== x) {
            c256Set(x)
            this.type = x.type
            this.qExp = x.qExp
            this.sign = x.sign
        }
        verify { validate() }
        return this
    }

    fun set(x: MutDec, ctx: DecContext): MutDec {
        if (this !== x) {
            c256Set(x)
            this.type = x.type
            this.qExp = x.qExp
            this.sign = x.sign
            if (qExp == NON_FINITE_SNAN) {
                this.type = STEAL_NAN_QNAN
                qExp = NON_FINITE_QNAN
                ctx.signalInvalid(this)
            }
        }
        verify { validate() }
        return this
    }

    fun set(xMagnitude: MutDec, sign: Boolean): MutDec {
        c256Set(xMagnitude)
        this.type = xMagnitude.type
        this.qExp = xMagnitude.qExp
        this.sign = sign
        verify { validate() }
        return this
    }

    fun set(x: Decimal): MutDec {
        val xSteal = x.steal
        this.dw1 = x.dw1
        this.dw0 = x.dw0
        this.type = xSteal and (if (x.isNaN()) STEAL_NAN_MASK else STEAL_TYPE_MASK)
        this.bitLen = stealBitLen(xSteal)
        this.digitLen = stealDigitLen(xSteal)
        this.qExp = stealQexp(xSteal)
        this.sign = stealSignFlag(xSteal)
        verify { validate() }
        return this
    }

    fun set(str: String) = set(str, DecContext.current())

    fun set(str: String, ctx: DecContext): MutDec {
        DecimalParsePrint.decFromString(this, str, ctx)
        verify { validate() }
        return this
    }

    fun setBid128(bid128Hi: Long, bid128Lo: Long): MutDec {
        decodeBid128Longs(this, bid128Hi, bid128Lo)
        verify { validate() }
        return this
    }

    fun setDpd128(dpd128Hi: Long, dpd128Lo: Long): MutDec {
        decodeDpd128Longs(this, dpd128Hi, dpd128Lo)
        verify { validate() }
        return this
    }

    fun setMaxFiniteMagnitude(ctx: DecContext): MutDec {
        type = STEAL_TYPE_FNZ
        qExp = Q_MAX
        // 0x378D8E6400000000uL.toLong(), 0x0001ED09BEAD87C0uL.toLong(),
        // 10000000000000000000000000000000000 (10**34)
        val pow10Offset = pow10Offset(ctx.precision) and POW10_BCE
        if (ctx.precision < MIN_POW10_DIGIT_LEN_128) {
            super.c256Set64(POW10[pow10Offset] - 1)
        } else if (ctx.precision < MIN_POW10_DIGIT_LEN_192) {
            super.c256Set128(POW10[pow10Offset + 1], POW10[pow10Offset] - 1)
        } else
            throw IllegalArgumentException()
        verify { validate() }
        return this
    }

    fun setMinFiniteMagnitude(ctx: DecContext): MutDec {
        type = STEAL_TYPE_FNZ
        qExp = Q_TINY
        super.c256SetOne()
        return this
    }

    fun setMinZeroMagnitude(ctx: DecContext): MutDec {
        type = STEAL_TYPE_ZER
        qExp = Q_TINY
        super.c256SetZero()
        return this
    }

    fun setNegate(x: MutDec) = set(x).mutateNegate()

    // NOTE
    //  that Colishaw's GDAS and Dectest require more complex handling
    //  of negate than what seems to be dictated by IEEE754-2019 ...
    //  which would simply be a sign change
    fun mutateNegate(): MutDec {
        this.sign = !this.sign
        return this
    }

    fun setAbs(x: MutDec) = set(x).mutateAbs()

    fun mutateAbs(): MutDec {
        // IEEE differs from GDAS/Colishaw
        this.sign = false
        return this
    }

    fun isNegative() = sign

    fun isNumber() : Boolean {
        return qExp <= NON_FINITE_INF
    }

    // IEEE754-2008 5.4.1
    fun setAdd(x: MutDec, y: MutDec, ctx: DecContext): MutDec = mutDecAddImpl(this, x, y.sign, y, ctx)

    // IEEE754-2008 5.4.1
    fun setSub(x: MutDec, y: MutDec, ctx: DecContext): MutDec = mutDecAddImpl(this, x, !y.sign, y, ctx)

    // IEEE754-2008 5.4.1
    fun setMul(x: MutDec, y: MutDec, ctx: DecContext): MutDec = mutDecMulImpl(this, x, y, ctx)

    fun setSquare(x: MutDec, ctx: DecContext): MutDec = mutDecSqrImpl(this, x, ctx)

    // IEEE754-2008 5.4.1
    fun setFma(x: MutDec, y: MutDec, a: MutDec, ctx: DecContext): MutDec = mutDecFmaImpl(this, x, y, a, ctx)

    fun setDiv(x: MutDec, y: MutDec, ctx: DecContext): MutDec = mutDecDivImpl(this, x, y, ctx)

    fun setDivInt(x: MutDec, y: MutDec, ctx: DecContext): MutDec = mutDecDivIntImpl(this, x, y, ctx)

    fun setReciprocal(x: MutDec, ctx: DecContext): MutDec = mutDecReciprocalImpl(this, x, ctx)

    fun setSqrt(x: MutDec, ctx: DecContext): MutDec = mutDecSqrtImpl(this, x, ctx)

    fun finiteCompareTo(other: MutDec): Int {
        verify { isFinite() && other.isFinite() }
        if (sign != other.sign) {
            if (this.isZero() && other.isZero())
                return 0
            // At least one is non-zero, signs differ
            return if (sign) -1 else 1
        }

        // Same sign - compare magnitudes
        val cmp = compareNumericMagnitudeTo(other, DecContext.current().tmps.pentad1)
        return if (sign) -cmp else cmp
    }

    fun infiniteCompareTo(other: MutDec): Int {
        verify { this.qExp <= NON_FINITE_INF && other.qExp <= NON_FINITE_INF }
        verify { this.qExp == NON_FINITE_INF || other.qExp == NON_FINITE_INF }
        return when {
            (sign != other.sign) -> if (sign) -1 else 1
            (qExp == NON_FINITE_INF) -> {
                if (other.qExp == NON_FINITE_INF) 0
                else if (sign) -1 else 1
            }

            else -> if (sign) 1 else -1
        }
    }

    // FIXME this is not the best, but is OK for now for testing
    fun partialCompareTo(other: MutDec, ctx: DecContext): MutDec {
        val md = MutDec()
        if (this.isZero() && other.isZero())
            return md.setZero(false)
        if (! isNaN() && !other.isNaN())
            return md.set(compareJavaStyleTo(other))
        md.setNaNOperand(this, other, ctx)
        return md
    }

    fun compareTotalOrderTo(other: MutDec): Int = mutDecCompareTotalOrder(this, other)

    fun compareNumericMagnitudeTo(other: MutDec, pentad: Pentad) : Int =
        mutDecCompareNumericMagnitude(this, other, pentad)

    fun compareTotalOrderMagTo(other: MutDec): Int = mutDecCompareTotalOrderMag(this, other)

    fun magnitudeEQ(other: MutDec) : Boolean {
        val thisIsZero = this.c256IsZero()
        val otherIsZero = other.c256IsZero()
        val bothAreZero = thisIsZero and otherIsZero
        val eitherIsZero = thisIsZero or otherIsZero
        if (this.sciExp() != other.sciExp())
            return bothAreZero
        if (! eitherIsZero) {
            val expDelta = this.qExp - other.qExp
            val pentad = DecContext.current().tmps.pentad1
            return when {
                expDelta == 0 -> c256UnscaledEQ(this, other)
                expDelta > 0 -> c256ScaledEQ(other, this, expDelta, pentad)
                else -> c256ScaledEQ(this, other, -expDelta, pentad)
            }
        }
        return bothAreZero
    }

    fun compareJavaStyleTo(other: MutDec) : Int = mutDecCompareJavaStyle(this, other)

    fun eqJavaStyleTo(other: MutDec) : Boolean = mutDecEqJavaStyle(this, other)

    fun setRoundToIntegral(x: MutDec, rounding: DecRounding, ctx: DecContext): MutDec {
        if (x.qExp >= 0) // this handles all special values as well
            return set(x, ctx)
        val xSign = x.sign
        if (x.c256IsZero())
            return setZero(xSign)
        val fracDigitLen = -x.qExp
        if (fracDigitLen >= x.digitLen) {
            // all fractional digits
            val residue: Residue
            if (fracDigitLen > x.digitLen)
                residue = Residue.LT_HALF
            else {
                residue = Residue.fromValueDecade(x)
                verify { residue != Residue.EXACT }
            }
            val roundUp = residue.ulpRoundUp(rounding.negate(xSign), 0L)
            if (! roundUp)
                setZero(xSign)
            else
                setOne(xSign)
            return ctx.signalInexact(this)
        }
        // integral and fractional digits
        val residue = c256SetScaleDownPow10(this, x, fracDigitLen, ctx.tmps.pentad1)
        type = if (this.c256IsZero()) STEAL_TYPE_ZER else STEAL_TYPE_FNZ
        qExp = 0
        sign = xSign
        return roundAndFinalize(residue, rounding, ctx)
    }

    fun setRoundToIntegralTiesToEven(x: MutDec, ctx: DecContext) =
        setRoundToIntegral(x, ROUND_TIES_TO_EVEN, ctx)

    fun setRoundToIntegralTiesToAway(x: MutDec, ctx: DecContext) =
        setRoundToIntegral(x, ROUND_TIES_TO_AWAY, ctx)

    fun setRoundToIntegralTowardZero(x: MutDec, ctx: DecContext) =
        setRoundToIntegral(x, ROUND_TOWARD_ZERO, ctx)

    fun setRoundToIntegralTowardPositive(x: MutDec, ctx: DecContext) =
        setRoundToIntegral(x, ROUND_TOWARD_POSITIVE, ctx)

    fun setRoundToIntegralTowardNegative(x: MutDec, ctx: DecContext) =
        setRoundToIntegral(x, ROUND_TOWARD_NEGATIVE, ctx)

    fun setRoundToIntegralExact(x: MutDec, ctx: DecContext): MutDec =
        setRoundToIntegral(x, ctx.decRounding, ctx)

    fun convertToLong(rounding: DecRounding, ctx: DecContext): Long {
        val signMask = signMask.toLong()
        val sign = sign
        val qExp = qExp
        val bitLen = bitLen
        val digitLen = digitLen
        val dw0 = dw0
        when {
            qExp == 0 -> {
                if (bitLen < 64)
                    return (dw0 xor signMask) - signMask
                if (dw0 == Long.MIN_VALUE && sign)
                    return Long.MIN_VALUE
                ctx.signalInvalid(this)
                return Long.MAX_VALUE - signMask
            }
            qExp >= NON_FINITE_INF -> {
                val ret =
                    if (qExp == NON_FINITE_INF && !sign) Long.MAX_VALUE
                    else Long.MIN_VALUE
                ctx.signalInvalid(this)
                return ret
            }
            bitLen == 0 -> return 0L
            qExp < 0 -> {
                val fracDigitLen = -qExp
                if (fracDigitLen >= digitLen) {
                    // all fractional digits
                    val residue: Residue
                    if (fracDigitLen > digitLen)
                        residue = Residue.LT_HALF
                    else {
                        residue = Residue.fromValueDecade(this)
                        verify { residue != Residue.EXACT }
                    }
                    val roundUp = residue.ulpRoundUp(rounding.negate(sign), 0L)
                    return ctx.signalInexact(
                        if (!roundUp)
                            0L
                        else
                            -signMask
                    )
                }
                // both integral and fractional digits
                val tmps = ctx.tmps
                val t = tmps.mdecArg1
                val residue = c256SetScaleDownPow10(t, this, fracDigitLen, tmps.pentad1)
                val roundUp = residue.ulpRoundUp(rounding.negate(sign), 0L)
                if (roundUp)
                    c256MutateIncrement(t)
                t.qExp = 0
                t.sign = sign
                // recursive call now that t.qExp == 0
                // where tail recursion when I need it?
                // even better ... GOTO
                return t.toLong(rounding, ctx)
            }
            else -> {
                if (digitLen + qExp <= 19) {
                    val result = dw0 * pow10_64(qExp)
                    if (result > 0)
                        return (result xor signMask) - signMask
                    // Long.MIN_VALUE && sign is not possible ...
                    // ... because we just multiplied by 10**qExp
                    // ... so the value ends in 0
                    // ... but Long.MIN_VALUE ends in 8
                }
                return ctx.signalInvalid(Long.MAX_VALUE - signMask)
            }
        }
    }

    fun convertToLongTiesToEven(x: MutDec, ctx: DecContext): Long  =
        convertToLong(ROUND_TIES_TO_EVEN, ctx)

    fun convertToLongTiesToAway(x: MutDec, ctx: DecContext): Long  =
        convertToLong(ROUND_TIES_TO_AWAY, ctx)

    fun convertToLongTowardZero(x: MutDec, ctx: DecContext): Long  =
        convertToLong(ROUND_TOWARD_ZERO, ctx)

    fun convertToLongTowardPositive(x: MutDec, ctx: DecContext): Long  =
        convertToLong(ROUND_TOWARD_POSITIVE, ctx)

    fun convertToLongTowardNegative(x: MutDec, ctx: DecContext): Long  =
        convertToLong(ROUND_TOWARD_NEGATIVE, ctx)

    fun convertToLongExact(x: MutDec, ctx: DecContext): Long =
        convertToLong(ctx.decRounding, ctx)

    /**
     * setNextUp and setNextDown are not considered arithmetic
     * operations. Therefore, flags are not set when they roll
     * over to Infinity
     */
    fun setNextUp(x: MutDec, ctx: DecContext): MutDec {
        set(x)
        when {
            qExp == NON_FINITE_SNAN -> {
                ctx.signalInvalid(this)
                qExp = NON_FINITE_QNAN
            }
            qExp == NON_FINITE_QNAN -> {}

            qExp == NON_FINITE_INF -> {
                if (sign)
                    setMaxFiniteMagnitude(ctx)
            }
            c256IsZero() -> {
                setMinFiniteMagnitude(ctx)
                sign = false
            }
            sign == false -> {
                // nextUp is not an arithmetic operation and
                // therefore flags do not get set
                verify { qExp <= Q_MAX }
                mutateNextAwayFromZero(ctx)
                if (qExp > Q_MAX)
                    setInfinite(sign = false)
            }
            else -> mutateNextTowardZero(ctx)
        }
        return this
    }

    fun setNextDown(x: MutDec, ctx: DecContext): MutDec {
        set(x)
        when {
            qExp == NON_FINITE_SNAN -> {
                ctx.signalInvalid(this)
                qExp = NON_FINITE_QNAN
            }
            qExp == NON_FINITE_QNAN -> { }
            qExp == NON_FINITE_INF -> {
                if (sign == false)
                    setMaxFiniteMagnitude(ctx)
            }
            c256IsZero() -> {
                setMinFiniteMagnitude(ctx)
                sign = true
            }

            sign -> {
                verify { qExp <= Q_MAX }
                mutateNextAwayFromZero(ctx)
                if (qExp > Q_MAX)
                    setInfinite(sign = true)
            }

            else -> mutateNextTowardZero(ctx)
        }
        return this
    }

    private fun mutateNextAwayFromZero(ctx: DecContext) {
        val headroom = min(ctx.precision - digitLen, qExp - Q_TINY)
        if (headroom > 0) {
            c256SetScaleUpPow10(this, this, headroom, ctx.tmps.pentad1)
            this.qExp -= headroom
        }
        c256MutateIncrement()
        if (digitLen > ctx.precision) { // rolled up a decade
            c256SetPow10(this, ctx.precision - 1)
            ++this.qExp
        }
    }

    private fun mutateNextTowardZero(ctx: DecContext) {
        val headroom =
            min(ctx.precision - digitLen + if (c256IsPowerOf10(this)) 1 else 0, qExp - Q_TINY)
        if (headroom > 0) {
            c256SetScaleUpPow10(this, this, headroom, ctx.tmps.pentad1)
            this.qExp -= headroom
        }
        c256MutateDecrement()
        if (c256IsZero())
            type = STEAL_TYPE_ZER
    }

    fun minNum(x: MutDec, y: MutDec, ctx: DecContext) = minNum_helper(x, y, 0, ctx)
    fun maxNum(x: MutDec, y: MutDec, ctx: DecContext) = minNum_helper(x, y, -1, ctx)

    private fun minNum_helper(x: MutDec, y: MutDec, invertCompareZeroOrNeg1: Int, ctx: DecContext) {
        val qMax = max(x.qExp, y.qExp)
        when {
            qMax <= NON_FINITE_INF -> {
                val cmp = (x.compareTo(y) xor invertCompareZeroOrNeg1) - invertCompareZeroOrNeg1
                set(if (cmp <= 0) x else y)
            }
            qMax == NON_FINITE_QNAN -> {
                set(if (x.qExp == NON_FINITE_QNAN) x else y)
            }
            else -> throw RuntimeException("somebody is a sNaN")
        }
    }

    fun minNumMag(x: MutDec, y: MutDec, env: DecContext) = minNumMag_helper(x, y, 0, env)
    fun maxNumMag(x: MutDec, y: MutDec, env: DecContext) = minNumMag_helper(x, y, -1, env)

    private fun minNumMag_helper(x: MutDec, y: MutDec, invertCompareZeroOrNeg1: Int, ctx: DecContext) {
        val qMax = max(x.qExp, y.qExp)
        when {
            qMax < NON_FINITE_INF -> {
                val cmp = (x.compareNumericMagnitudeTo(y, ctx.tmps.pentad1) xor invertCompareZeroOrNeg1) - invertCompareZeroOrNeg1
                set(if (cmp <= 0) x else y)
            }
            else -> minNum_helper(x, y, invertCompareZeroOrNeg1, ctx)
        }
    }

    internal inline fun capExponentRange(e: Int): Int {
        return min(max(e, CAPPED_EXP_MIN), CAPPED_EXP_MAX)
    }

    // IEEE754-2008 5.3.2
    fun setQuantize(x: MutDec, y: MutDec, ctx: DecContext): MutDec {
        // Handle NaN propagation
        // FIXME -- dispatching on qExp
        val qX = x.qExp
        val qY = y.qExp
        if (qX > NON_FINITE_INF || qY > NON_FINITE_INF)
            return setNaNOperand(x, y, ctx)
        // Handle infinity cases
        if (qX == NON_FINITE_INF || qY == NON_FINITE_INF) {
            if (qX == NON_FINITE_INF && qY == NON_FINITE_INF)
                return set(x)
            this.setNaN()
            ctx.signalInvalid(this)
            return this
        }

        // Both are finite
        val delta = qY - qX

        when {
            delta == 0 -> return set(x)

            delta > 0 -> {
                // Target exponent is larger: need to scale coefficient DOWN
                // This means truncating with rounding
                if (x.c256IsZero())
                    return setZero(x.sign, qY, ctx)
                // Scale down by delta positions
                val residue = c256SetScaleDownPow10(this, x, delta, ctx.tmps.pentad1)
                type = if (this.bitLen == 0) STEAL_TYPE_ZER else STEAL_TYPE_FNZ
                qExp = qY
                sign = x.sign
                if (residue != Residue.EXACT)
                    roundAndFinalize(residue, ctx.decRounding, ctx)
                return this
            }

            else -> {  // delta < 0
                if (x.c256IsZero())
                    return setZero(x.sign, qY, ctx)

                // Target exponent is smaller: need to scale coefficient UP
                val scaleAmount = -delta
                val resultDigitLen = x.digitLen + scaleAmount

                // Check if result would exceed precision
                if (resultDigitLen > ctx.precision) {
                    this.setNaN()
                    ctx.signalInvalid(this)
                    return this
                }

                // Scale up coefficient
                c256SetScaleUpPow10(this, x, scaleAmount, ctx.tmps.pentad1)
                this.qExp = qY
                this.sign = x.sign
                verify { digitLen <= ctx.precision }
                return this
            }
        }
    }

    // IEEE754-2008 5.3.3
    fun setScaleB(x: MutDec, pow10: Int, ctx: DecContext): MutDec {
        set(x)
        // FIXME -- dispatching on qExp
        when {
            isFinite() -> {
                val p10 = min(max(pow10, -99999), 99999)
                qExp = capExponentRange(qExp + p10)
                if (qExp > Q_MAX || qExp < Q_TINY)
                    return finalize(ctx)
            }
            isInfinite() -> {}
            else -> setNaNOperand(x, ctx)
        }
        return this
    }

    // IEEE754-2008 5.3.3
    fun setLogB(x: MutDec, env: DecContext): MutDec {
        val qX = x.qExp
        // FIXME -- dispatching on qExp
        when {
            x.isZero() -> {
                setInfinite(sign = true)
                env.signalDivByZero(this)
            }
            qX < NON_FINITE_INF -> {
                val logB = qX + x.digitLen - 1
                set(logB)
            }
            qX == NON_FINITE_INF ->
                setInfinite()
            else ->
                setNaNOperand(x, env)
        }
        return this
    }

    fun setStripTrailingZeros(x: MutDec, env: DecContext): MutDec =
        setStripTrailingZeros(x, env, maxToStrip = 99)

    fun setStripTrailingZeros(x: MutDec, ctx: DecContext, maxToStrip: Int): MutDec {
        val qX = x.qExp
        when {
            x.isZero() -> return setZero(x.sign)
            maxToStrip <= 0 -> return set(x)
            qX < NON_FINITE_INF -> {
                var ctzd = 0
                var remaining = maxToStrip
                val tmps = ctx.tmps
                val t = tmps.mdecArg1
                var t0 = x
                var m: Long = 0L
                while (remaining > 0) {
                    val divPow10 = min(9, remaining)
                    val divisor = pow10_64(divPow10)
                    m = DivDirect.divModX32(t, t0, divisor)
                    t.type = if (t.bitLen == 0) STEAL_TYPE_ZER else STEAL_TYPE_FNZ
                    if (m != 0L)
                        break
                    t0 = t
                    ctzd += divPow10
                    remaining -= divPow10
                    if (remaining == 0) {
                        t0.qExp = qX + maxToStrip
                        return set(t0)
                    }
                }
                ctzd += min(countTrailingZeroDigits32(m.toInt()), remaining)
                // cap when qExp gets clamped
                ctzd = min(ctzd, Q_MAX - qX)
                if (ctzd == 0)
                    return set(x)
                c256SetScaleDownPow10(this, x, ctzd, tmps.pentad1)
                type = STEAL_TYPE_FNZ
                qExp = qX + ctzd
                sign = x.sign
                return this
            }
            else -> return set(x, ctx)
        }
    }

    fun setMinimum(x: MutDec, y: MutDec, env: DecContext): MutDec =
        setMinMaxImpl(x, y, MIN_OP, env)

    fun setMinimumMagnitude(x: MutDec, y: MutDec, env: DecContext): MutDec =
        setMinMaxImpl(x, y, MIN_MAG_OP, env)

    fun setMinimumNumber(x: MutDec, y: MutDec, env: DecContext): MutDec =
        setMinMaxImpl(x, y, MIN_NUM_OP, env)

    fun setMinimumMagnitudeNumber(x: MutDec, y: MutDec, env: DecContext): MutDec =
        setMinMaxImpl(x, y, MIN_MAG_NUM_OP, env)

    fun setMaximum(x: MutDec, y: MutDec, env: DecContext): MutDec =
        setMinMaxImpl(x, y, MAX_OP, env)

    fun setMaximumMagnitude(x: MutDec, y: MutDec, env: DecContext): MutDec =
        setMinMaxImpl(x, y, MAX_MAG_OP, env)

    fun setMaximumNumber(x: MutDec, y: MutDec, env: DecContext): MutDec =
        setMinMaxImpl(x, y, MAX_NUM_OP, env)

    fun setMaximumMagnitudeNumber(x: MutDec, y: MutDec, env: DecContext): MutDec =
        setMinMaxImpl(x, y, MAX_MAG_NUM_OP, env)


    fun setMinMaxImpl(x: MutDec, y: MutDec, op: Int, ctx: DecContext): MutDec {
        val qX = x.qExp
        val qY = y.qExp
        val qMax = max(qX, qY)
        if (qMax <= NON_FINITE_INF) {
            var cmp = if ((op and MAG_MASK) != 0)
                x.compareNumericMagnitudeTo(y, ctx.tmps.pentad1)
            else
                x.compareTo(y)
            if (cmp == 0)
                cmp = x.compareTotalOrderTo(y)
            return set(if ((cmp >= 0) xor ((op and MAX_MASK) == 0)) x else y)
        }
        if ((op and NUM_MASK) != 0) {
            if (qX <= NON_FINITE_INF) {
                set(x)
                if (qY == NON_FINITE_SNAN)
                    ctx.signalInvalid(this)
                return this
            }
            if (qY <= NON_FINITE_INF) {
                set(y)
                if (qX == NON_FINITE_SNAN)
                    ctx.signalInvalid(this)
                return this
            }
        }
        return setNaNOperand(x, y, ctx)
    }

    fun setRemainderNear(x: MutDec, y: MutDec, ctx: DecContext): MutDec {
        // avoid aliasing issues
        val yT = if (this !== y) y else ctx.tmps.mdecDiv.set(y)
        val truncIsOdd: Boolean = setRemTruncImpl(x, yT, ctx)
        val tmps = ctx.tmps
        if (!isZero() && isFinite()) {
            val truncCtx = ctx.withRoundingAndNewFlags(ROUND_TOWARD_ZERO)
            val rem2 = if (sign) {
                tmps.mdecArg1.setAdd(this, yT, truncCtx)  // this + yT
            } else {
                tmps.mdecArg1.setSub(this, yT, truncCtx)  // this - yT
            }
            val cmp = compareNumericMagnitudeTo(rem2, tmps.pentad1)
             if (cmp > 0 || (cmp == 0) && truncIsOdd)
                this.set(rem2)
        }
        return this
    }

    fun setRemainderTruncate(x: MutDec, y: MutDec, env: DecContext): MutDec {
        setRemTruncImpl(x, y, env)
        return this
    }

    fun setRemTruncImpl(x: MutDec, y: MutDec, ctx: DecContext): Boolean {
        val qX = x.qExp
        val qY = y.qExp
        var quotientIsOdd = false
        // FIXME - dispatching on qExp
        when {
            qX < NON_FINITE_INF && qY < NON_FINITE_INF && !y.isZero() -> {
                // Compute n = nearest integer to x/y (ties to even)
                // setRemainder is an EXACT operation, so we will use a temp
                // environment so that INEXACT flag/trap does not get signaled.
                // use INTERNAL_TMP_ENV so that flag-setting
                val truncCtx = ctx.withRoundingAndNewFlags(ROUND_TOWARD_ZERO)
                val n = ctx.tmps.mdecArg1.setDiv(x, y, truncCtx)
                if (n.qExp < 0)
                    n.setRoundToIntegralExact(n, truncCtx)

                // save xSign ... in case of aliasing this === x
                val xSign = x.sign
                // Compute r = x - n*y
                // (-n) * y + x
                n.sign = !n.sign // negate n
                quotientIsOdd = (n.dw0.toInt() and 1) != 0
                this.setFma(n, y, x, truncCtx)

                if (this.c256IsZero()) {
                    this.qExp = min(qX, qY)
                    this.sign = xSign
                }
            }
            qX >= NON_FINITE_QNAN || qY >= NON_FINITE_QNAN -> {
                setNaNOperand(x, y, ctx)
                return false
            }
            qX == NON_FINITE_INF || y.isZero() -> {
                ctx.signalInvalid(setNaN())
                return false
            }
            else -> { // qY == NON_FINITE_INF ->
                verify { qY == NON_FINITE_INF }
                set(x)
            }
        }
        return quotientIsOdd
    }



    fun compareQuiet754(other: MutDec, env: DecContext): Compare754Result =
        compare754(other, false, env)

    fun compareSignaling754(other: MutDec, env: DecContext): Compare754Result =
        compare754(other, true, env)

    fun compare754(other: MutDec, isSignaling: Boolean, ctx: DecContext): Compare754Result {
        val qMax = max(qExp, other.qExp)
        return when {
            qMax < NON_FINITE_INF -> when {
                c256IsZero() -> when {
                    other.c256IsZero() -> IEEE754_EQ
                    other.sign -> IEEE754_LT
                    else -> IEEE754_GT
                }

                other.c256IsZero() -> when {
                    sign -> IEEE754_GT
                    else -> IEEE754_LT
                }

                else -> {
                    val cmp = compareNumericMagnitudeTo(other, ctx.tmps.pentad1)
                    Compare754Result(cmp)
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

    override fun compareTo(other: MutDec): Int = mutDecCompareJavaStyle(this, other)

    override fun equals(other: Any?) : Boolean =
        other is MutDec && eqJavaStyleTo(other)

    fun exactlyEQ(other: MutDec): Boolean {
        return dw0 == other.dw0 && dw1 == other.dw1 &&
                dw2 == other.dw2 && dw3 == other.dw3 &&
                qExp == other.qExp && sign == other.sign
    }

    override fun hashCode(): Int {
        var result = super.hashCode()  // includes sign + coefficient
        result = 31 * result + qExp
        return result
    }
    // 5.7.2 General operations

    fun valueClass(): Ieee754Class {
        return when {
            qExp == NON_FINITE_SNAN -> signalingNaN
            qExp == NON_FINITE_QNAN -> quietNaN
            qExp == NON_FINITE_INF ->
                return if (sign == false) positiveInfinity else negativeInfinity
            c256IsZero() ->
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
    fun isZero() = qExp < NON_FINITE_INF && c256IsZero()
    fun isFiniteNonZero() = qExp < NON_FINITE_INF && !c256IsZero()
    fun isSubnormal() = qExp < NON_FINITE_INF && sciExp() < -6143
    fun isInfinite() = qExp == NON_FINITE_INF
    fun isNaN() = qExp in NON_FINITE_QNAN..NON_FINITE_SNAN
    fun isSignaling() = qExp == NON_FINITE_SNAN
    fun isCanonical() = true
    fun radix() = 10

    fun isTotalOrder(other: MutDec): Boolean = compareTotalOrderTo(other) <= 0

    // 5.7.3 Decimal2 operation
    fun sameQuantum(x: MutDec): Boolean =
        (this.qExp == x.qExp) ||
                (this.qExp >= NON_FINITE_QNAN && x.qExp >= NON_FINITE_QNAN)

    // FIXME ... implement this so that there are fewer memory allocations
    override fun toString(): String = toString(toEngineeringExp = false)

    fun toString(toEngineeringExp: Boolean): String {
        return if (isFinite()) {
            when {
                qExp == 0 -> IntegerParsePrint.int256ToString(sign, this)
                qExp < 0 && sciExp() >= -6 -> toDecimalPointString()
                else -> toScientificString(toEngineeringExp)
            }
        } else {
            toSpecialValueString()
        }
    }

    private /*inline*/ fun toDecimalPointString() : String {
        val digitsRightOfDecimal = -qExp
        val leadingZeroCount = max(1 + digitsRightOfDecimal - digitLen, 0)
        val signLen = if (sign) 1 else 0
        val decimalPointLen = 1
        val totalLen = signLen + leadingZeroCount + decimalPointLen + digitLen
        val utf8 = ByteArray(totalLen)
        utf8[0] = '-'.code.toByte() // overwritten when positive
        for (i in signLen..leadingZeroCount) // there is one extra here
            utf8[i] = '0'.code.toByte()
        u256ToUtf8(utf8, signLen + leadingZeroCount)
        moveBytesUp1(utf8, totalLen - digitsRightOfDecimal - 1, digitsRightOfDecimal)
        //for (i in totalLen-1 downTo totalLen-digitsRightOfDecimal)
        //    utf8[i] = utf8[i - 1]
        utf8[totalLen - digitsRightOfDecimal - 1] = '.'.code.toByte()
        return String(utf8)
    }

    private fun moveBytesUp1(bytes: ByteArray, off: Int, len: Int) {
        for (i in off + len - 1 downTo off)
            bytes[i + 1] = bytes[i]
    }

    private /*inline*/ fun toNormalizedScientificString(): String {
        val eExp = sciExp()
        val signLen = if (sign) 1 else 0
        val decimalPointLen = if (digitLen > 1) 1 else 0
        val printedDigitLen = max(digitLen, 1)
        val expELen = 1
        val expSignLen = if (eExp < 0) 1 else 0
        val expDigitLen = max(calcDigitLen64(Math.abs(eExp).toLong()), 1)
        val totalLen = signLen + decimalPointLen + printedDigitLen + expELen + expSignLen + expDigitLen
        val utf8 = ByteArray(totalLen)
        var i = IntegerParsePrint.int256ToUtf8(sign, this, utf8, 0)
        if (digitLen > 1) {
            val insertionPoint = signLen + 1
            moveBytesUp1(utf8, insertionPoint, digitLen - 1)
            utf8[insertionPoint] = '.'.code.toByte()
            ++i
        }
        utf8[i] = 'E'.code.toByte()
        val j = IntegerParsePrint.int32ToUtf8(eExp, utf8, i + 1)
        verify { i + 1 + j == utf8.size }
        return String(utf8)
    }

    private /*inline*/ fun toScientificString(toEngineeringExp: Boolean): String {
        val eExp = sciExp()
        val expAdjustment = if (toEngineeringExp) (if (eExp >= 0) eExp else ((eExp % 3) + 3)) % 3 else 0
        val leftOfRadixPointCount = 1 + expAdjustment
        val adjustedExp = eExp - expAdjustment
        val signLen = if (sign) 1 else 0
        val decimalPointLen = if (digitLen > 1) 1 else 0
        val printedDigitLen = max(digitLen, 1)
        val additionalLeftOfPointZeroCount =
            if (digitLen == 0) 0 else max(0, 1 + expAdjustment - digitLen)
        val expELen = 1
        val expSignLen = if (eExp < 0) 1 else 0
        val expDigitLen = max(calcDigitLen64(Math.abs(eExp).toLong()), 1)
        val totalLen = signLen + decimalPointLen + additionalLeftOfPointZeroCount +
                printedDigitLen + expELen + expSignLen + expDigitLen
        val utf8 = ByteArray(totalLen)
        var i = IntegerParsePrint.int256ToUtf8(sign, this, utf8, 0)
        when {
            additionalLeftOfPointZeroCount > 0 -> {
                utf8[i++] = '0'.code.toByte()
                if (additionalLeftOfPointZeroCount == 2)
                    utf8[i++] = '0'.code.toByte()
            }

            digitLen > leftOfRadixPointCount -> {
                val insertionPoint = signLen + 1
                moveBytesUp1(utf8, insertionPoint, digitLen - 1)
                utf8[insertionPoint] = '.'.code.toByte()
                ++i
            }
        }
        utf8[i] = 'E'.code.toByte()
        val j = IntegerParsePrint.int32ToUtf8(adjustedExp, utf8, i + 1)
        verify { i + 1 + j == utf8.size }
        return String(utf8)
    }

    private fun toSpecialValueString() : String {
        if (isInfinite()) return if (sign) "-Infinity" else "Infinity"
        verify { isNaN() }
        val nanStr =
            if (isSignaling()) {
                if (sign) "-sNaN" else "sNaN"
            } else {
                if (sign) "-NaN" else "NaN"
            }
        if (c256IsZero())
            return nanStr
        val utf8 = ByteArray(nanStr.length + digitLen)
        for (i in nanStr.indices)
            utf8[i] = nanStr[i].code.toByte()
        u256ToUtf8(utf8, nanStr.length)
        return String(utf8)
    }

    fun toDebugString() : String {
        if (isFinite()) {
            val printLen = calcDebugPrintLength()
            val utf8 = ByteArray(printLen)
            val i = IntegerParsePrint.int256ToUtf8(sign, this, utf8, 0)
            utf8[i] = 'E'.code.toByte()
            val j = IntegerParsePrint.int32ToUtf8(qExp, utf8, i + 1)
            verify { i + 1 + j == utf8.size }
            return String(utf8)
        } else {
            return toSpecialValueString()
        }
    }

    fun calcDebugPrintLength(): Int {
        val signLen = if (sign) 1 else 0
        val coeffLen = max(digitLen, 1)
        val expELen = 1
        val expSignLen = if (qExp < 0) 1 else 0
        val expDigitLen = max(calcDigitLen64(Math.abs(qExp).toLong()), 1)
        return signLen + coeffLen + expELen + expSignLen + expDigitLen
    }

    fun encodeLittleEndianLongsBid128() = encodeLittleEndianBid128(LongArray(2))
    fun encodeLittleEndianBid128(littleEndianLongs: LongArray) = encodeLittleEndianBid128(this, littleEndianLongs)
    fun encodeLittleEndianBytesBid128() = encodeLittleEndianBid128(ByteArray(16))
    fun encodeLittleEndianBid128(littleEndianBytes: ByteArray) = encodeLittleEndianBid128(this, littleEndianBytes)

    fun encodeLittleEndianLongsDpd128() = encodeLittleEndianDpd128(LongArray(2))
    fun encodeLittleEndianDpd128(littleEndianLongs: LongArray) =
        encodeLittleEndianDpd128(littleEndianLongs, this, DecContext.current().tmps.knuthD)
    fun encodeLittleEndianBytesDpd128() = encodeLittleEndianDpd128(ByteArray(16))
    fun encodeLittleEndianDpd128(littleEndianBytes: ByteArray) =
        encodeLittleEndianDpd128(littleEndianBytes, this, DecContext.current().tmps.knuthD)

    fun encodeBigEndianLongsBid128() = encodeBigEndianBid128(LongArray(2))
    fun encodeBigEndianBid128(BigEndianLongs: LongArray) = encodeBigEndianBid128(this, BigEndianLongs)
    fun encodeBigEndianBytesBid128() = encodeBigEndianBid128(ByteArray(16))
    fun encodeBigEndianBid128(BigEndianBytes: ByteArray) = encodeBigEndianBid128(this, BigEndianBytes)

    fun encodeBigEndianLongsDpd128() = encodeBigEndianDpd128(LongArray(2))
    fun encodeBigEndianDpd128(bigEndianLongs: LongArray) =
        encodeBigEndianDpd128(bigEndianLongs, this, DecContext.current().tmps.knuthD)
    fun encodeBigEndianBytesDpd128() = encodeBigEndianDpd128(ByteArray(16))
    fun encodeBigEndianDpd128(bigEndianBytes: ByteArray) =
        encodeBigEndianDpd128(bigEndianBytes, this, DecContext.current().tmps.knuthD)

}
