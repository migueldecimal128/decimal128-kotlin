// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_NEGATIVE
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_ZERO
import com.decimal128.decimal.Ieee754Class.*
import com.decimal128.decimal.IntegerParsePrint.int256ToUtf8
import com.decimal128.decimal.IntegerParsePrint.int32ToUtf8
import com.decimal128.decimal.InvalidOperationReason.QUANTIZE_EXACTLY_ONE_OPERAND_IS_INFINITE
import com.decimal128.decimal.InvalidOperationReason.QUANTIZE_RESULT_WOULD_EXCEED_PRECISION
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal const val NON_FINITE_QNAN = 0
internal const val NON_FINITE_SNAN = 1

class MutDec() : C256(), Comparable<MutDec> {
    internal var type: Int
        inline get() = stealTyp(steal)
        set(value) {
            verify { value in 0..3 }
            steal = stealWithTyp(steal, value)
        }
    internal var sign: Boolean
        inline get() = stealSignFlag(steal)
        set(value) {
            steal = stealWithSignFlag(steal, value)
        }
    internal val signMask: Int
        inline get() = stealSignMask(steal)
    internal val signBit: Int
        inline get() = stealSignBit(steal)


    internal var qExp: Int
        inline get() = stealQExp(steal)
        set(value) {
            steal = stealWithQExp(steal, value)
        }

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
            STEAL_TYP_ZER -> {
                if (bitLen > 0)
                    return false
                if (qExp < Q_TINY || qExp > Q_MAX)
                    return false
            }
            STEAL_TYP_FNZ -> {
                if (bitLen == 0)
                    return false
                if (qExp < Q_TINY || qExp > Q_MAX)
                    return false
            }
            STEAL_TYP_INF -> {
                if (bitLen != 0)
                    return false
            }
            STEAL_TYP_NAN -> {
                if (qExp != NON_FINITE_SNAN && qExp != NON_FINITE_QNAN)
                    return false
            }
        }
        return true
    }

    fun isSignMinus() = stealSignFlag(steal)
    fun isNormal() = stealIsNormal(steal)
    fun isFinite() = stealIsFinite(steal)
    fun isZero() = stealIsZER(steal)
    fun isFiniteNonZero() = stealIsFNZ(steal)
    fun isSubnormal() = stealIsSubnormal(steal)
    fun isInfinite() = stealIsINF(steal)
    fun isNaN() = stealIsNAN(steal)
    fun isNaN(signaling: Boolean) = (if (signaling) stealIsSNAN(steal) else stealIsQNAN(steal))
    fun isSignaling() = stealIsSNAN(steal)
    fun isCanonical() = true
    fun radix() = 10

    // if the digitLen is non-zero then subtract 1
    // if digitLen == 0 then sciExp stays 0 ... 0e0
    fun sciExp() = stealSciExp(steal)

    fun setZero() = setZero(false)

    fun setZero(sign: Boolean): MutDec {
        this.dw3 = 0L; this.dw2= 0L; this.dw1 = 0L; this.dw0 = 0L
        this.steal = stealEncodeZER(sign, 0)
        verify { validate() }
        return this
    }

    fun setZero(sign: Boolean, qExp: Int): MutDec {
        val qExpCapped = max(min(qExp, Q_MAX), Q_TINY)
        this.dw3 = 0L; this.dw2= 0L; this.dw1 = 0L; this.dw0 = 0L
        this.steal = stealEncodeZER(sign, qExpCapped)
        verify { validate() }
        return this
    }

    internal inline fun setZeroWithQTiny(sign: Boolean): MutDec = setZero(sign, Q_TINY)

    fun setOne(sign: Boolean = false): MutDec {
        this.dw3 = 0L; this.dw2= 0L; this.dw1 = 0L
        this.dw0 = 1L
        this.steal = stealEncodeFNZ(sign, 0, PACKED_LENGTHS_1_1)
        verify { validate() }
        return this
    }

    internal fun setNaNOperand(x: MutDec, ctx: DecContext): MutDec {
        val stealX = x.steal
        verify { stealIsNAN(stealX) }
        this.set(x)
        if (stealIsSNAN(stealX)) {
            quietSNaN()
            ctx.signalInvalid(this)
        }
        verify { validate() }
        return this
    }

    internal fun setNaNOperand(x: MutDec, y: MutDec, ctx: DecContext, alwaysSignal: Boolean = false): MutDec {
        val stealX = x.steal
        val stealY = y.steal
        verify { stealHasNAN(stealX, stealY) }
        val preferSnan = ctx.decPrefs.propagatePreferSnan
        val takeY = !stealIsNAN(stealX) || (preferSnan && stealIsSNAN(stealY) && !stealIsSNAN(stealX))
        val theNaN = if (takeY) y else x
        set(theNaN)
        verify { stealIsNAN(this.steal) }
        val isSignaling = stealIsSNAN(stealX) or stealIsSNAN(stealY)
        if (!alwaysSignal && !isSignaling)
            return this
        if (!isSignaling)
            return ctx.signalInvalid(InvalidOperationReason.NAN_OPERAND, this)
        quietSNaN()
        return ctx.signalInvalid(InvalidOperationReason.SNAN_OPERAND, this)
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
        qExp = NON_FINITE_QNAN
    }

    internal fun setNaN(): MutDec {
        dw3 = 0L; dw2 = 0L; dw1 = 0L; dw0 = 0L
        steal = STEAL_NAN_QNAN
        return this
    }

    internal fun setNaN(isSignaling: Boolean, sign: Boolean, payloadHi: Long, payloadLo: Long) {
        this.dw3 = 0L; this.dw2 = 0L
        this.dw1 = payloadHi
        this.dw0 = payloadLo
        this.steal = stealEncodeNAN(if (sign) 1 else 0, if (isSignaling) 1 else 0, payloadHi, payloadLo)
        verify { validate() }
    }

    fun setSNaN(): MutDec {
        dw3 = 0L; dw2 = 0L; dw1 = 0L; dw0 = 0L
        steal = STEAL_NAN_SNAN
        return this
    }

    fun setInfinite(sign: Boolean = false): MutDec {
        this.dw3 = 0L; this.dw2 = 0L; this.dw1 = 0L; this.dw0 = 0L
        this.steal = stealEncodeINF(if (sign) 1 else 0)
        verify { validate() }
        return this
    }

    fun set(n: Int): MutDec = set(n.toLong())

    fun set(l: Long): MutDec {
        if (l != 0L) {
            val signBit = (l ushr 63).toInt()
            val mask = l shr 63
            val abs = (l xor mask) - mask
            this.dw3 = 0; this.dw2 = 0; this.dw1 = 0
            this.dw0 = abs
            this.steal = stealEncodeFNZ(signBit, qExp = 0, calcStealPackedLengths64(abs))
            verify { validate() }
        } else {
            setZero()
        }
        return this
    }

    fun setUnsigned(ul: Long): MutDec {
        if (ul != 0L) {
            this.dw3 = 0; this.dw2 = 0; this.dw1 = 0
            this.dw0 = ul
            this.steal = stealEncodeFNZ(signBit = 0, qExp = 0, calcStealPackedLengths64(ul))
            verify { validate() }
        } else {
            setZero()
        }
        return this
    }

    fun set(x: MutDec): MutDec {
        verify { (x.dw3 or x.dw2) == 0L }
        this.dw3 = 0L; this.dw2 = 0L
        this.dw1 = x.dw1; this.dw0 = x.dw0;
        this.steal = x.steal
        verify { validate() }
        return this
    }

    fun set(x: MutDec, ctx: DecContext): MutDec {
        val xSteal = x.steal
        this.dw3 = x.dw3; this.dw2 = x.dw2; this.dw1 = x.dw1; this.dw0 = x.dw0;
        this.steal = xSteal
        if (stealIsFNZ(xSteal))
            return finalizeFnz(stealSignFlag(steal), stealQExp(steal), ctx)
        if (stealIsSNAN(xSteal)) {
            quietSNaN()
            ctx.signalInvalid(InvalidOperationReason.SNAN_OPERAND, this)
        }
        verify { validate() }
        return this
    }

    fun set(xMagnitude: MutDec, sign: Boolean): MutDec {
        set(xMagnitude)
        this.steal = stealWithSignFlag(steal, sign)
        verify { validate() }
        return this
    }

    fun set(x: Decimal): MutDec {
        this.steal = x.steal
        this.dw3 = 0L; this.dw2 = 0L
        this.dw1 = x.dw1; this.dw0 = x.dw0
        verify { validate() }
        return this
    }

    fun set(str: String) = set(str, DecContext.current())

    fun set(str: String, ctx: DecContext): MutDec {
        DecimalParse.decFromString(this, str, ctx)
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

    fun setMaxFiniteMagnitude(sign: Boolean, ctx: DecContext): MutDec {
        val precision = ctx.precision
        verify { precision == 34 || precision == 38 }
        val pow10Offset = (precision shl 1) and POW10_BCE
        this.dw3 = 0L; this.dw2 = 0L
        this.dw1 = POW10[pow10Offset + 1]
        this.dw0 = POW10[pow10Offset    ] - 1
        val bitLen = pow10BitLen(precision)
        this.steal = stealEncodeFNZ(sign, Q_MAX, stealPackLengths(precision, bitLen))
        verify { validate() }
        return this
    }

    fun setMinFiniteMagnitude(sign: Boolean, ctx: DecContext): MutDec {
        this.dw3 = 0L; this.dw2 = 0L; this.dw1 = 0L
        this.dw0 = 1L
        this.steal = stealEncodeFNZ(sign, Q_TINY, PACKED_LENGTHS_1_1)
        return this
    }

    fun setNegate(x: MutDec) = set(x).mutateNegate()

    // NOTE
    //  Colishaw's GDAS and Dectest require more complex handling
    //  of negate than what is dictated by IEEE754-2019 ...
    //  which is simply a sign change as a non-computational operation
    fun mutateNegate(): MutDec {
        this.steal = stealWithNegation(steal)
        return this
    }

    fun setAbs(x: MutDec) = set(x).mutateAbs()

    fun mutateAbs(): MutDec {
        // IEEE differs from GDAS/Colishaw
        this.steal = stealWithAbsValue(steal)
        return this
    }

    fun isNegative() = stealSignFlag(steal)

    fun isNumber() : Boolean {
        return stealTyp(steal) != STEAL_TYP_NAN
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

    // FIXME this is not the best, but is OK for now for testing
    fun partialCompareTo(other: MutDec, ctx: DecContext): MutDec {
        val md = MutDec()
        if (!this.isZero() || !other.isZero()) {
            if (!isNaN() && !other.isNaN())
                return md.set(compareJavaStyleTo(other))
            md.setNaNOperand(this, other, ctx)
        }
        return md
    }

    fun compareTotalOrderTo(other: MutDec): Int = mutDecCompareTotalOrder(this, other)

    fun compareNumericMagnitudeTo(other: MutDec) : Int =
        mutDecCompareNumericMagnitude(this, other)

    fun compareTotalOrderMagTo(other: MutDec): Int = mutDecCompareTotalOrderMag(this, other)

    fun compareJavaStyleTo(other: MutDec) : Int = mutDecCompareJavaStyle(this, other)

    fun eqJavaStyleTo(other: MutDec) : Boolean = mutDecEqJavaStyle(this, other)

    fun setRoundToIntegral(x: MutDec, rounding: DecRounding, ctx: DecContext): MutDec {
        val xSteal = x.steal
        if (!stealIsFinite(xSteal) || stealQExp(xSteal) >= 0)
            return set(x, ctx)
        val xSign = stealSignFlag(xSteal)
        if (stealIsZER(xSteal))
            return setZero(xSign)
        val digitLen = stealDigitLen(xSteal)
        val fracDigitLen = -stealQExp(xSteal)
        if (fracDigitLen >= digitLen) {
            // all fractional digits
            val residue: Residue
            if (fracDigitLen > digitLen)
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
        return roundAndFinalizeFnz(xSign, 0, residue, rounding, ctx)
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
        val steal = steal
        val signMask = stealSignMask(steal).toLong()
        val sign = stealSignFlag(steal)
        val qExp = stealQExp(steal)
        val bitLen = stealBitLen(steal)
        val digitLen = stealDigitLen(steal)
        val dw0 = dw0
        when (stealTyp(steal)) {
            STEAL_TYP_FNZ -> when {
                qExp == 0 -> {
                    if (bitLen < 64)
                        return (dw0 xor signMask) - signMask
                    if (dw0 == Long.MIN_VALUE && sign)
                        return Long.MIN_VALUE
                    ctx.signalInvalid(this)
                    return Long.MAX_VALUE - signMask
                }

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
                    val tSteal = stealEncodeFNZ(sign, 0, stealPackedLengths(t.steal))
                    t.steal = tSteal
                    if (stealBitLen(tSteal) < 64)
                        return (t.dw0 xor signMask) - signMask
                    if (t.dw0 == Long.MIN_VALUE && sign)
                        return Long.MIN_VALUE
                    ctx.signalInvalid(t)
                    return Long.MAX_VALUE - signMask
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

            STEAL_TYP_ZER -> return 0L
            STEAL_TYP_INF -> {
                val ret = if (sign) Long.MIN_VALUE else Long.MAX_VALUE
                ctx.signalInvalid(this)
                return ret
            }

            else -> { // STEAL_TYP_NAN
                verify { isNaN() }
                ctx.signalInvalid(InvalidOperationReason.NAN_OPERAND, this)
                return Long.MIN_VALUE
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
    fun setNextUp(x: MutDec, ctx: DecContext): MutDec =
        setNextUpOrDown(isUp = true, x, ctx)

    fun setNextDown(x: MutDec, ctx: DecContext): MutDec =
        setNextUpOrDown(isUp = false, x, ctx)

    private fun setNextUpOrDown(isUp: Boolean, x: MutDec, ctx: DecContext): MutDec {
        set(x)
        val steal = steal
        val sign = stealSignFlag(steal)
        when (type) {
            STEAL_TYP_FNZ -> {
                if (sign == isUp) {
                    mutateNextTowardZero(ctx)
                } else {
                    verify { qExp <= Q_MAX }
                    mutateNextAwayFromZero(ctx)
                    if (qExp > Q_MAX)
                        setInfinite(sign)
                }
            }
            STEAL_TYP_ZER -> {
                setMinFiniteMagnitude(!isUp, ctx)
            }
            STEAL_TYP_INF -> {
                if (sign == isUp)
                    setMaxFiniteMagnitude(sign, ctx)
            }
            else -> {
                if (stealIsSNAN(steal)) {
                    quietSNaN()
                    ctx.signalInvalid(InvalidOperationReason.SNAN_OPERAND, this)
                }
            }
        }
        return this
    }

    private fun mutateNextAwayFromZero(ctx: DecContext) {
        val precision = ctx.precision
        val steal = steal
        val qExp = stealQExp(steal)
        val headroom = min(precision - stealDigitLen(steal), qExp - Q_TINY)
        if (headroom > 0) {
            c256SetScaleUpPow10(this, this, headroom, ctx.tmps.pentad1)
            this.qExp = qExp - headroom
        }
        // note that this could have changed digitLen ...
        // ... so don't steal digitLen from the local steal
        c256MutateIncrement()
        if (digitLen > precision) { // rolled up a decade
            c256SetPow10(this, precision - 1)
            this.qExp = qExp + 1
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
            steal = stealWithTyp(steal, STEAL_TYP_ZER)
    }

    fun minNum(x: MutDec, y: MutDec, ctx: DecContext) = minNum_helper(x, y, 0, ctx)
    fun maxNum(x: MutDec, y: MutDec, ctx: DecContext) = minNum_helper(x, y, -1, ctx)

    private fun minNum_helper(x: MutDec, y: MutDec, invertCompareZeroOrNeg1: Int, ctx: DecContext) {
        when {
            x.isNumber() && y.isNumber() -> {
                val cmp = (x.compareTo(y) xor invertCompareZeroOrNeg1) - invertCompareZeroOrNeg1
                set(if (cmp <= 0) x else y)
                return
            }
            x.isSignaling() || y.isSignaling() ->
                throw RuntimeException("somebody is a sNaN")
            x.isNaN() -> set(x)
            else -> set(y)
        }
    }

    fun minNumMag(x: MutDec, y: MutDec, env: DecContext) = minNumMag_helper(x, y, 0, env)
    fun maxNumMag(x: MutDec, y: MutDec, env: DecContext) = minNumMag_helper(x, y, -1, env)

    private fun minNumMag_helper(x: MutDec, y: MutDec, invertCompareZeroOrNeg1: Int, ctx: DecContext) {
        when {
            x.isSignaling() || y.isSignaling() -> throw RuntimeException("somebody is a sNaN")
            x.isNaN() -> set(x)
            y.isNaN() -> set(y)
            else -> {
                val cmp = (x.compareNumericMagnitudeTo(y) xor invertCompareZeroOrNeg1) - invertCompareZeroOrNeg1
                set(if (cmp <= 0) x else y)
            }
        }
    }

    // IEEE754-2008 5.3.2
    fun setQuantize(x: MutDec, y: MutDec, ctx: DecContext): MutDec {
        // Handle NaN propagation
        val xSteal = x.steal
        val ySteal = y.steal
        val binopSignature = binopSignatureOf(xSteal, ySteal)
        when (binopSignature) {
            ZER_ZER,
            FNZ_ZER,
            ZER_FNZ,
            FNZ_FNZ -> {
                // Both are finite
                val xSign = stealSignFlag(xSteal)
                val pentad = ctx.tmps.pentad1

                val xQ = stealQExp(xSteal)
                val yQ = stealQExp(ySteal)
                val delta = yQ - xQ

                when {
                    delta == 0 -> return set(x)

                    delta > 0 -> {
                        // Target exponent is larger: need to scale coefficient DOWN
                        // This means truncating with rounding
                        if (x.c256IsZero())
                            return setZero(xSign, yQ)
                        // Scale down by delta positions
                        val residue = c256SetScaleDownPow10(this, x, delta, pentad)
                        return roundAndFinalizeFinite(xSign, yQ, residue, ctx.decRounding, ctx)
                    }

                    else -> {  // delta < 0
                        if (stealIsZER(xSteal))
                            return setZero(xSign, yQ)

                        // Target exponent is smaller: need to scale coefficient UP
                        val scaleAmount = -delta
                        val resultDigitLen = x.digitLen + scaleAmount

                        // Check if result would exceed precision
                        if (resultDigitLen > ctx.precision) {
                            return ctx.setNanSignalInvalid(this, QUANTIZE_RESULT_WOULD_EXCEED_PRECISION)
                        }

                        // Scale up coefficient
                        c256SetScaleUpPow10(this, x, scaleAmount, pentad)
                        this.steal = stealEncodeFNZ(xSign, yQ, stealPackedLengths(this.steal))
                        verify { digitLen <= ctx.precision }
                        return this
                    }
                }
            }
            INF_INF -> return set(x)
            INF_ZER,
            ZER_INF,
            INF_FNZ,
            FNZ_INF -> return ctx.setNanSignalInvalid(this, QUANTIZE_EXACTLY_ONE_OPERAND_IS_INFINITE)
            else -> // NAN_FOUND
                return setNaNOperand(x, y, ctx)

        }
    }

    // IEEE754-2008 5.3.3
    fun setScaleB(x: MutDec, pow10: Int, ctx: DecContext): MutDec {
        set(x)
        val steal = steal
        val xSign = stealSignFlag(steal)
        when {
            stealIsFinite(steal) -> {
                val p10 = min(max(pow10, -100_000), 100_000)
                val targetQ = x.qExp + p10
                return (
                        if (x.bitLen == 0) setZero(xSign, targetQ)
                        else finalizeFnz(xSign, targetQ, ctx))
            }
            stealIsNAN(steal) -> setNaNOperand(x, ctx)
        }
        return this
    }

    // IEEE754-2008 5.3.3
    fun setLogB(x: MutDec, ctx: DecContext): MutDec {
        val xSteal = x.steal
        when (stealTyp(xSteal)) {
            STEAL_TYP_ZER -> {
                setInfinite(sign = true)
                ctx.signalDivByZero(this)
            }
            STEAL_TYP_FNZ -> set(stealSciExp(xSteal))
            STEAL_TYP_INF -> setInfinite()
            else -> setNaNOperand(x, ctx)
        }
        return this
    }

    fun setStripTrailingZeros(x: MutDec, env: DecContext): MutDec =
        setStripTrailingZeros(x, env, maxToStrip = 99)

    fun setStripTrailingZeros(x: MutDec, ctx: DecContext, maxToStrip: Int): MutDec {
        val xSteal = x.steal
        val xSign = stealSignFlag(xSteal)
        val xQ = stealQExp(xSteal)
        when {
            stealIsZER(xSteal) -> return setZero(xSign)
            maxToStrip <= 0 -> return set(x)
            stealIsFinite(xSteal) -> {
                var ctzd = 0
                var remaining = maxToStrip
                val tmps = ctx.tmps
                val t = tmps.mdecArg1
                var t0 = x
                var modulo: Long = 0L
                while (remaining > 0) {
                    val divPow10 = min(9, remaining)
                    val divisor = pow10_64(divPow10)
                    modulo = DivDirect.divModX32(t, t0, divisor)
                    t.type = if (t.bitLen == 0) STEAL_TYP_ZER else STEAL_TYP_FNZ
                    if (modulo != 0L)
                        break
                    // the modulo was all zeros
                    t0 = t
                    ctzd += divPow10
                    remaining -= divPow10
                    if (remaining == 0) {
                        // we have fully satisfied maxToStrip ... so we are done
                        t0.qExp = xQ + maxToStrip
                        return set(t0)
                    }
                }
                ctzd += min(countTrailingZeroDigits32(modulo.toInt()), remaining)
                // cap when qExp gets clamped
                ctzd = min(ctzd, Q_MAX - xQ)
                if (ctzd == 0)
                    return set(x)
                c256SetScaleDownPow10(this, x, ctzd, tmps.pentad1)
                this.steal = stealEncodeFNZ(xSign, xQ + ctzd, stealPackedLengths(this.steal))
                return this
            }
            else -> return set(x, ctx)
        }
    }

    fun setMinimum(x: MutDec, y: MutDec, env: DecContext): MutDec =
        mutDecSetMinMaxImpl(this, x, y, MIN_OP, env)

    fun setMinimumMagnitude(x: MutDec, y: MutDec, env: DecContext): MutDec =
        mutDecSetMinMaxImpl(this, x, y, MIN_MAG_OP, env)

    fun setMinimumNumber(x: MutDec, y: MutDec, env: DecContext): MutDec =
        mutDecSetMinMaxImpl(this, x, y, MIN_NUM_OP, env)

    fun setMinimumMagnitudeNumber(x: MutDec, y: MutDec, env: DecContext): MutDec =
        mutDecSetMinMaxImpl(this, x, y, MIN_MAG_NUM_OP, env)

    fun setMaximum(x: MutDec, y: MutDec, env: DecContext): MutDec =
        mutDecSetMinMaxImpl(this, x, y, MAX_OP, env)

    fun setMaximumMagnitude(x: MutDec, y: MutDec, env: DecContext): MutDec =
        mutDecSetMinMaxImpl(this, x, y, MAX_MAG_OP, env)

    fun setMaximumNumber(x: MutDec, y: MutDec, env: DecContext): MutDec =
        mutDecSetMinMaxImpl(this, x, y, MAX_NUM_OP, env)

    fun setMaximumMagnitudeNumber(x: MutDec, y: MutDec, env: DecContext): MutDec =
        mutDecSetMinMaxImpl(this, x, y, MAX_MAG_NUM_OP, env)

    fun setRemainderNear(x: MutDec, y: MutDec, ctx: DecContext): MutDec {
        // avoid aliasing issues
        val yT = if (this !== y) y else ctx.tmps.mdecDiv.set(y)
        val truncIsOdd: Boolean = mutDecSetRemTruncImpl(this, x, yT, ctx)
        if (isFiniteNonZero()) {
            val tmps = ctx.tmps
            val rem2 = tmps.mdecArg1
            val truncCtx = ctx.withRoundingAndNewFlags(ROUND_TOWARD_ZERO)
            if (sign) {
                rem2.setAdd(this, yT, truncCtx)  // this + yT
            } else {
                rem2.setSub(this, yT, truncCtx)  // this - yT
            }
            val cmp = compareNumericMagnitudeTo(rem2)
            if (cmp > 0 || (cmp == 0) && truncIsOdd)
                this.set(rem2)
        }
        return this
    }

    fun setRemainderTruncate(x: MutDec, y: MutDec, env: DecContext): MutDec {
        mutDecSetRemTruncImpl(this, x, y, env)
        return this
    }

    fun compareQuiet754(other: MutDec, env: DecContext): Compare754Result =
        mutDecCompare754Impl(this, other, false, env)

    fun compareSignaling754(other: MutDec, env: DecContext): Compare754Result =
        mutDecCompare754Impl(this, other, true, env)

    override fun compareTo(other: MutDec): Int = mutDecCompareJavaStyle(this, other)

    override fun equals(other: Any?) : Boolean =
        other is MutDec && eqJavaStyleTo(other)

    fun exactlyEQ(other: MutDec): Boolean {
        return dw0 == other.dw0 && dw1 == other.dw1 &&
                dw2 == other.dw2 && dw3 == other.dw3 &&
                steal == other.steal
    }

    override fun hashCode(): Int {
        throw UnsupportedOperationException("hashCode() not supported")
    }

    // 5.7.2 General operations
    fun valueClass(): Ieee754Class {
        val steal = steal
        val sign = stealSignFlag(steal)
        when (stealTyp(steal)) {
            STEAL_TYP_FNZ -> {
                if (stealSciExp(steal) >= -6143)
                    return if (sign) negativeNormal else positiveNormal
                return if (sign) negativeSubnormal else positiveSubnormal
            }
            STEAL_TYP_ZER -> return if (sign) negativeZero else positiveZero
            STEAL_TYP_INF -> return if (sign) negativeInfinity else positiveInfinity
            else -> // STEAL_TYP_NAN
                return if (stealIsSNAN(steal)) signalingNaN else quietNaN
        }
    }


    fun isTotalOrder(other: MutDec): Boolean = compareTotalOrderTo(other) <= 0

    // 5.7.3 Decimal2 operation
    fun sameQuantum(x: MutDec): Boolean {
        val thisSteal = this.steal
        val otherSteal = x.steal
        return when (binopSignatureOf(thisSteal, otherSteal)) {
            ZER_ZER,
            FNZ_ZER,
            ZER_FNZ,
            FNZ_FNZ -> stealQExp(thisSteal) == stealQExp(otherSteal)
            NAN_NAN,
            INF_INF -> true

            else -> false
        }
    }

    override fun toString(): String {
        val steal = steal
        if (!stealIsFinite(steal) || stealBitLen(steal) <= 127)
            return D128Print.d128ToString(steal, dw1, dw0, DecContext.current())
        // this is called only when the MutDec has not been normalized ...
        // ... only in the debugger

        return (if (steal < 0) "-" else "") + super.toString() + "E" + qExp.toString()
    }

    fun toString(ctx: DecContext): String {
        val steal = steal
        if (!stealIsFinite(steal) || stealBitLen(steal) <= 127)
            return D128Print.d128ToString(steal, dw1, dw0, ctx)
        // this is called only when the MutDec has not been normalized ...
        // ... only in the debugger

        return (if (steal < 0) "-" else "") + super.toString() + "E" + qExp.toString()
    }

    fun calcDebugPrintLength(): Int {
        val signLen = if (sign) 1 else 0
        val coeffLen = max(digitLen, 1)
        val expELen = 1
        val expSignLen = if (qExp < 0) 1 else 0
        val expDigitLen = max(calcDigitLen64(abs(qExp).toLong()), 1)
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
