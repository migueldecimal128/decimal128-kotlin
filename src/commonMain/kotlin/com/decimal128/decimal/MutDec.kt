@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT
import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_NEGATIVE
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_ZERO
import com.decimal128.decimal.Ieee754Class.*
import com.decimal128.decimal.DecEnv.Companion.DECIMAL128
import com.decimal128.decimal.U256Compare.u256UnscaledCompare
import kotlin.math.max
import kotlin.math.min
import com.decimal128.decimal.U256Pow10.POW10
import com.decimal128.decimal.U256ScalePow10.u256ScaleUpPow10

internal const val MIN_SPECIAL_VALUE = 16381
internal const val NON_FINITE_INF = 16381
// Total order has sNaN < qNaN
// But, implementation needs to leave sNaN > qNaN
// because that makes it easier to distinguish the sNaN
// when converting from sNaN => qNaN
// See setNaN(x, y, env)
internal const val NON_FINITE_QNAN = 16382
internal const val NON_FINITE_SNAN = 16383

const val CAPPED_EXP_MIN = -16000
const val CAPPED_EXP_MAX = 16000

class MutDec() : C256() {
    var sign = false
    var qExp = 0
    val eExp: Int
        get() = qExp + digitLen - 1

    companion object {

        private fun addImpl(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, env: DecEnv): MutDec {
            //check (x.digitLen <= 38) // x is allowed more digits because of FMA
            check(y.digitLen <= 38)
            val qMax = max(x.qExp, y.qExp)
            when {
                qMax < NON_FINITE_INF -> {
                    val qMin = min(x.qExp, y.qExp)
                    when {
                        x.bitLen == 0 && y.bitLen == 0 -> {
                            // IEEE 754: Handle sign of -0 + -0 = -0
                            val sign = if (x.sign == ySign) {
                                ySign  // Both same sign → use that sign
                            } else {
                                env.isRoundTowardNegative()  // Different signs → +0 except roundTowardNegative
                            }
                            z.setZero(sign, qMin, env)
                        }
                        y.bitLen == 0 && x.qExp == qMin -> {
                            z.set(x)
                            z.finalize(env)
                        }
                        y.bitLen == 0 -> {
                            val gap = x.qExp - y.qExp
                            val headroom = env.precision - x.digitLen
                            // FMA adding 0 will lead to -headroom
                            val shiftLeft = max(0, min(headroom, gap))
                            z.qExp = x.qExp - shiftLeft
                            z.sign = x.sign
                            u256ScaleUpPow10(z, x, shiftLeft)
                            // we could be here because of FMA, so need to finalize
                            z.finalize(env)
                        }
                        x.bitLen == 0 && y.qExp == qMin -> {
                            z.set(y)
                            z.sign = ySign
                        }
                        x.bitLen == 0 -> {
                            val gap = y.qExp - x.qExp
                            val headroom = env.precision - y.digitLen
                            val shiftLeft = min(headroom, gap)
                            z.qExp = y.qExp - shiftLeft
                            z.sign = ySign
                            u256ScaleUpPow10(z, y, shiftLeft)
                        }
                        x.qExp == y.qExp -> unscaledFiniteNonZeroAddImpl(z, x, ySign, y, env)
                        else -> scaledFiniteNonZeroAddImpl(z, x, ySign, y, env)
                    }
                }
                qMax == NON_FINITE_INF -> infiniteAddImpl(z, x, ySign, y, env)
                else -> z.setNaNOperand(x, y, env)
            }
            return z
        }

        private fun unscaledFiniteNonZeroAddImpl(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, env: DecEnv): MutDec {
            check(x.bitLen > 0 && y.bitLen > 0)  // Optional: could remove in production
            check(x.qExp == y.qExp)
            val xSign = x.sign
            z.qExp = x.qExp
            // IEEE754-2019 6.3 The sign bit
            // When the sum of two operands with opposite signs
            // (or the difference of two operands with like signs) is
            // exactly zero, the sign of that sum (or difference)
            // shall be +0 under all rounding-direction attributes except
            // roundTowardNegative; under that attribute, the sign of an
            // exact zero sum (or difference) shall be −0.
            val isRoundTowardNegative = env.isRoundTowardNegative()
            if (xSign == ySign) {
                z.c256SetAdd(x, y)
                z.sign = xSign
            } else {
                val cmp = u256UnscaledCompare(x, y)
                when {
                    (cmp > 0) -> {
                        z.c256SetSub(x, y)
                        z.sign = if (z.bitLen > 0) xSign else isRoundTowardNegative
                    }
                    (cmp < 0) -> {
                        z.c256SetSub(y, x)
                        z.sign = if (z.bitLen > 0) ySign else isRoundTowardNegative
                    }
                    else -> {
                        z.c256SetZero()
                        z.sign = isRoundTowardNegative
                    }
                }
            }
            return z.finalize(env)
        }

        private fun scaledFiniteNonZeroAddImpl(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, env: DecEnv): MutDec {
            val qX = x.qExp
            val qY = y.qExp
            check (qX != qY)
            val xSign = x.sign
            val qMax = max(qX, qY)
            check (qMax < MIN_SPECIAL_VALUE)
            check (x.bitLen > 0 && y.bitLen > 0)
            val residue: Residue
            if (xSign == ySign) {
                    residue = MagnitudeAddSub.magScaledAdd(z, x, y, env)
                z.sign = xSign
            } else {
                val cmp = x.magnitudeCompareTo(y)
                when {
                    cmp > 0 -> {
                        residue = MagnitudeAddSub.magScaledSub(z, x, y, env)
                        z.sign = xSign
                    }
                    cmp < 0 -> {
                        residue = MagnitudeAddSub.magScaledSub(z, y, x, env)
                        z.sign = ySign
                    }
                    else -> {
                        // Magnitudes are equal and signs opposite → exact cancellation
                        // IEEE 754: sign is +0 except when rounding toward negative
                        z.setZero(env.isRoundTowardNegative(), min(qX, qY), env)
                        return z // I don't think I need to finalize in this case
                    }
                }
            }
            return z.roundAndFinalize(residue, env)
        }

        private fun infiniteAddImpl(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, env: DecEnv): MutDec {
            val qX = x.qExp
            val qY = y.qExp
            check (qX == NON_FINITE_INF || qY == NON_FINITE_INF)
            if (qX == qY && x.sign != ySign) {
                z.setNaN(env)
                return env.signalInvalid(z)
            } else {
                z.setInfinite(if (qX == NON_FINITE_INF) x.sign else ySign)
                return z
            }
        }

        fun decodeLittleEndianBid128(littleEndianLongs: LongArray) =
            SerDeBid128.decodeLittleEndianBid128(MutDec(), littleEndianLongs)
        fun decodeLittleEndianBid128(littleEndianBytes: ByteArray) =
            SerDeBid128.decodeLittleEndianBid128(MutDec(), littleEndianBytes)
        fun decodeBigEndianBid128(bigEndianLongs: LongArray) =
            SerDeBid128.decodeBigEndianBid128(MutDec(), bigEndianLongs)
        fun decodeBigEndianBid128(bigEndianBytes: ByteArray) =
            SerDeBid128.decodeBigEndianBid128(MutDec(), bigEndianBytes)

        fun decodeLittleEndianDpd128(littleEndianLongs: LongArray) =
            SerDeDpd128.decodeLittleEndianDpd128(MutDec(), littleEndianLongs)
        fun decodeLittleEndianDpd128(littleEndianBytes: ByteArray) =
            SerDeDpd128.decodeLittleEndianDpd128(MutDec(), littleEndianBytes)
        fun decodeBigEndianDpd128(bigEndianLongs: LongArray) =
            SerDeDpd128.decodeBigEndianDpd128(MutDec(), bigEndianLongs)
        fun decodeBigEndianDpd128(bigEndianBytes: ByteArray) =
            SerDeDpd128.decodeBigEndianDpd128(MutDec(), bigEndianBytes)

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

    }

    // if the digitLen is non-zero then subtract 1
    // if digitLen == 0 then sciExp stays 0 ... 0e0
    fun sciExp() = qExp + (digitLen - (-digitLen ushr 31))

    fun setZero() = setZero(false)

    fun setZero(sign: Boolean): MutDec {
        c256SetZero()
        this.qExp = 0
        this.sign = sign
        return this
    }

    fun setZero(sign: Boolean = false, qExp: Int = 0, env: DecEnv): MutDec {
        c256SetZero()
        this.qExp = max(min(qExp, env.qMax), env.qTiny)
        this.sign = sign
        return this
    }

    private fun setNaNOperand(x: MutDec, env: DecEnv): MutDec {
        val xQ = x.qExp
        check(xQ >= NON_FINITE_QNAN)
        this.set(x)
        this.qExp = NON_FINITE_QNAN
        if (xQ == NON_FINITE_SNAN)
            env.signalInvalid(this)
        return this
    }

    private fun setNaNOperand(x: MutDec, y: MutDec, env: DecEnv): MutDec {
        val xQ = x.qExp
        val yQ = y.qExp
        val maxQ = max(xQ, yQ)
        check(maxQ >= NON_FINITE_QNAN)
        this.set(if (maxQ == xQ) x else y)
        this.qExp = NON_FINITE_QNAN
        if (maxQ == NON_FINITE_SNAN)
            env.signalInvalid(this)
        return this
    }

    private fun sNaNOperand() {
        throw RuntimeException("sNaN operand")
    }

    private fun setNaN(x: MutDec, env: DecEnv) {
        val q = x.qExp
        check(q >= NON_FINITE_QNAN)
        setZero()
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    internal fun setNaN(env: DecEnv) {
        setZero()
        sign = false
        qExp = NON_FINITE_QNAN
    }

    internal fun setNaNSignalInvalid(env: DecEnv) {
        setNaN(env)
        env.signalInvalid(this)
    }

    internal fun setNaN(payload: Int, env: DecEnv) {
        sign = false
        c256Set64(payload.toLong())
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    internal fun setNaN() {
        setZero()
        qExp = NON_FINITE_QNAN
    }

    internal fun setNaN(isSignaling: Boolean, sign: Boolean, payloadHi: Long, payloadLo: Long) {
        this.sign = sign
        this.qExp = if (isSignaling) NON_FINITE_SNAN else NON_FINITE_QNAN
        this.dw3 = 0
        this.dw2 = 0
        this.dw1 = payloadHi and ((1L shl (110 - 64)) - 1L)
        this.dw0 = payloadLo
    }

    fun setSNaN(env: DecEnv) {
        setZero()
        qExp = NON_FINITE_SNAN
    }

    fun setInfinite(sign: Boolean = false) {
        // NOTE miguel 2025-09-30
        //  Infinity must have coefficient zero (not one) because that is
        //  what is required for BID and DPD encoding.
        //  Changing the coefficient to one would make negation slightly
        //  easier, but isn't worth doing
        this.c256SetZero()
        this.qExp = NON_FINITE_INF
        this.sign = sign
    }

    fun set(n: Int): MutDec = set(n.toLong())

    fun set(l: Long): MutDec {
        this.qExp = 0
        this.sign = l < 0
        val mask = l shr 63
        val abs = (l xor mask) - mask
        c256Set64(abs)
        return this
    }

    fun setUnsigned(ul: Long): MutDec {
        this.qExp = 0
        this.sign = false
        c256Set64(ul)
        return this
    }

    fun set(l: Long, qExp: Int, env: DecEnv): MutDec {
        this.qExp = env.capExponentRange(qExp)
        this.sign = l < 0
        val mask = l shr 63
        val abs = (l xor mask) - mask
        c256Set64(abs)
        return this
    }

    fun set(x: MutDec): MutDec {
        c256Set(x)
        this.qExp = x.qExp
        this.sign = x.sign
        return this
    }

    fun set(xMagnitude: MutDec, sign: Boolean): MutDec {
        c256Set(xMagnitude)
        this.qExp = xMagnitude.qExp
        this.sign = sign
        return this
    }

    fun set(x: DecOld): MutDec {
        this.dw1 = x.dw1
        this.dw0 = x.dw0
        this.bitLen = x.bitLen
        this.digitLen = x.digitLen
        this.qExp = x.qExp
        this.sign = x.sign
        return this
    }

    fun set(str: String) = set(str, DECIMAL128)

    fun set(str: String, env: DecEnv): MutDec {
        DecimalParsePrint.decFromString(this, str, env)
        return this
    }

    fun setBid128(bid128Hi: Long, bid128Lo: Long) = SerDeBid128.decodeBid128Longs(this, bid128Hi, bid128Lo)

    fun setDpd128(dpd128Hi: Long, dpd128Lo: Long) = SerDeDpd128.decodeDpd128Longs(this, dpd128Hi, dpd128Lo)

    fun setMaxFiniteMagnitude(env: DecEnv): MutDec {
        qExp = env.qMax
        // 0x378D8E6400000000uL.toLong(), 0x0001ED09BEAD87C0uL.toLong(),
        // 10000000000000000000000000000000000 (10**34)
        val offset = U256Pow10.pow10Offset(env.precision)
        if (env.precision < MIN_POW10_DIGIT_LEN_128) {
            super.c256Set64(POW10[offset] - 1)
        } else if (env.precision < MIN_POW10_DIGIT_LEN_192) {
            super.c256Set128(POW10[offset + 1], POW10[offset] - 1)
        } else
            throw IllegalArgumentException()
        return this
    }

    fun setMinFiniteMagnitude(env: DecEnv): MutDec {
        qExp = env.qTiny
        super.c256SetOne()
        return this
    }

    fun setMinZeroMagnitude(env: DecEnv): MutDec {
        qExp = env.qTiny
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

    fun mutateToIntegral(env: DecEnv): MutDec {
        // FIXME ... not tested and not correct
        when {
            qExp < MIN_SPECIAL_VALUE -> {
                if (qExp < 0) {
                    if (bitLen == 0) {
                        qExp = 0
                        return this
                    }
                    env.signalInexact(this)
                    val eExp = sciExp()
                    when (env.decRounding) {
                        ROUND_TIES_TO_EVEN, ROUND_TIES_TO_AWAY -> {
                            if (eExp <= -2) {
                                return MutDec()
                            }
                            val half = MutDec().set(5L, -1, env)
                            val cmp = this.compareTo(half)
                            if (cmp < 0 || cmp == 0 && env.decRounding == ROUND_TIES_TO_EVEN) {
                                // FIXME this is a mutate ... what am I doing creating new instances?
                                return MutDec()
                            } else {
                                return this.set(1)
                            }
                        }
                        ROUND_TOWARD_ZERO -> return MutDec()
                        ROUND_TOWARD_NEGATIVE -> {
                            if (sign)
                                return this.set(1)
                            else
                                return MutDec()
                        }
                        ROUND_TOWARD_POSITIVE -> {
                            if (!sign)
                                return this.set(1)
                            else
                                return MutDec()
                        }
                    }
                }
            }
            qExp == NON_FINITE_SNAN -> qExp = NON_FINITE_QNAN
        }
        return this
    }

    fun mutateCopySign(x: MutDec, y: MutDec): MutDec {
        val sign = y.sign
        set(x)
        this.sign = sign
        return this
    }

    fun isNegative() = sign

    fun isNumber() : Boolean {
        return qExp <= NON_FINITE_INF
    }

    // IEEE754-2008 5.4.1
    fun setAdd(x: MutDec, y: MutDec, env: DecEnv) = addImpl(this, x, y.sign, y, env)

    // IEEE754-2008 5.4.1
    fun setSub(x: MutDec, y: MutDec, env: DecEnv) = addImpl(this, x, !y.sign, y, env)

    // IEEE754-2008 5.4.1
    fun setMul(x: MutDec, y: MutDec, env: DecEnv): MutDec {
        val qX = x.qExp
        val qY = y.qExp
        val productSign = x.sign xor y.sign
        val qMaxXY = max(qX, qY)
        when {
            qMaxXY < MIN_SPECIAL_VALUE -> {
                this.c256SetMul(x, y)
                this.qExp = x.qExp + y.qExp
                this.sign = productSign
                return finalize(env)
            }
            qMaxXY == NON_FINITE_INF -> {
                if (x.isZero() || y.isZero()) {
                    setNaN()
                    return env.signalInvalid(this)
                } else {
                    setInfinite(productSign)
                }
            }
            else -> setNaNOperand(x, y, env)
        }
        return this
    }

    fun setSquare(x: MutDec, env: DecEnv): MutDec {
        val qX = x.qExp
        when {
            qX < MIN_SPECIAL_VALUE -> {
                this.c256SetSqr(x)
                this.qExp = this.qExp shl 1
                this.sign = false
                return finalize(env)            }
            qX == NON_FINITE_INF -> {
                setInfinite(false)
            }
            else -> setNaN(x, env)
        }
        return this
    }

    // IEEE754-2008 5.4.1
    fun setFma(x: MutDec, y: MutDec, a: MutDec, env: DecEnv): MutDec {
        val qX = x.qExp
        val qY = y.qExp
        val qA = a.qExp
        val qMaxXY = max(qX, qY)
        val qMaxXYA = max(qMaxXY, qA)
        val productSign = x.sign xor y.sign
        when {
            qMaxXYA < MIN_SPECIAL_VALUE -> {
                val aT = if (this === a) MutDec().set(a) else a
                // multiply without roundAndFinalize .. remains exact
                this.c256SetMul(x, y)
                this.qExp = x.qExp + y.qExp
                this.sign = productSign
                // roundAndFinalize takes place here
                this.setAdd(this, aT, env)
            }
            qMaxXYA == NON_FINITE_INF -> when {
                // addend is infinite
                (qA == NON_FINITE_INF) -> {
                    if ((qMaxXY < NON_FINITE_INF) || (productSign == a.sign))
                        this.set(a)
                    else {
                        this.setNaNSignalInvalid(env)
                    }
                }
                // if we are here then one of the product terms is INF
                // and the other is ZERO
                (x.isZero() || y.isZero()) -> {
                    check(qMaxXY == NON_FINITE_INF)
                    setNaNSignalInvalid(env)
                }
                else ->
                    setInfinite(productSign)
            }
            else -> {
                if (qX == qMaxXYA)
                    this.setNaNOperand(x, y, env)
                else
                    this.setNaNOperand(y, a, env)
            }
        }
        return this
    }

    fun setDiv(x: MutDec, y: MutDec, env: DecEnv): MutDec {
        val qX = x.qExp
        val qY = y.qExp
        val quotientSign = x.sign xor y.sign
        val qMaxXY = max(qX, qY)
        when {
            qMaxXY < MIN_SPECIAL_VALUE -> {
                when {
                    (y.bitLen > 0) -> {
                        val residue = MagnitudeDiv.magDiv(this, x, y, env)
                        this.sign = quotientSign
                        roundAndFinalize(residue, env)
                    }
                    (x.bitLen > 0) -> {
                        // finite division by zero
                        setInfinite(quotientSign)
                        return env.signalDivByZero(this)
                    }
                    else -> {
                        // zero divided by zero
                        setNaN(env)
                        return env.signalInvalid(this)
                    }
                }
            }
            qMaxXY == NON_FINITE_INF -> {
                when {
                    (qX == NON_FINITE_INF && qY == NON_FINITE_INF) -> {
                        setNaN(env)
                        return env.signalInvalid(this)
                    }

                    (qX == NON_FINITE_INF) -> {
                        setInfinite(quotientSign)
                    }

                    else -> {
                        setZero(quotientSign)
                        qExp = env.qTiny
                    }
                }
            }
            else -> setNaNOperand(x, y, env)
        }
        return this
    }

    fun setReciprocal(x: MutDec, env: DecEnv): MutDec {
        val qX = x.qExp
        val quotientSign = x.sign
        when {
            qX < MIN_SPECIAL_VALUE -> {
                val residue = MagnitudeInv.magInv(this, x, env)
                this.sign = quotientSign
                roundAndFinalize(residue, env)
            }
            qX == NON_FINITE_INF -> {
                setZero(quotientSign)
            }
            else -> setNaN(x, env)
        }
        return this
    }

    fun setSqrt(x: MutDec, env: DecEnv): MutDec {
        val qX = x.qExp
        when {
            x.sign -> {
                setNaN(env)
                return env.signalInvalid(this)
            }
            qX < MIN_SPECIAL_VALUE -> {
                when {
                    (x.bitLen > 0) -> {
                        val residue = MagnitudeSqrt.magSqrt(this, x)
                        this.sign = false
                        roundAndFinalize(residue, env)
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
            else -> setNaN(x, env)
        }
        return this
    }

    fun compareTo(other: MutDec) : Int {
        val qMax = max(qExp, other.qExp)
        when {
            (qMax < MIN_SPECIAL_VALUE) -> return finiteCompareTo(other)
            (qMax == NON_FINITE_INF) -> return infiniteCompareTo(other)
            else -> throw RuntimeException("somebody is a NaN")
        }
    }

    fun finiteCompareTo(other: MutDec): Int {
        check(qExp < NON_FINITE_INF && other.qExp < NON_FINITE_INF)
        if (sign != other.sign) {
            if (this.isZero() && other.isZero())
                return 0
            // At least one is non-zero, signs differ
            return if (sign) -1 else 1
        }

        // Same sign - compare magnitudes
        val cmp = magnitudeCompareTo(other)
        return if (sign) -cmp else cmp
    }

    fun infiniteCompareTo(other: MutDec): Int {
        check(this.qExp <= NON_FINITE_INF && other.qExp <= NON_FINITE_INF)
        check(this.qExp == NON_FINITE_INF || other.qExp == NON_FINITE_INF)
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
    fun partialCompareTo(other: MutDec, env: DecEnv): MutDec {
        val md = MutDec()
        if (this.qExp < NON_FINITE_QNAN && other.qExp < NON_FINITE_QNAN)
            return md.set(compareTo(other))
        md.setNaNOperand(this, other, env)
        return md
    }

    fun totalCompareTo(other: MutDec): Int {
        if (this.sign != other.sign) {
            return if (this.sign) -1 else 1
        }
        val cmp = magnitudeTotalCompareTo(other)
        return if (this.sign) -cmp else cmp
    }

    fun magnitudeCompareTo(other: MutDec) : Int {
        val qMax = max(qExp, other.qExp)
        when {
            (qMax < MIN_SPECIAL_VALUE) -> return finiteMagnitudeCompareTo(other)
            (qMax == NON_FINITE_INF) -> return infiniteMagnitudeCompareTo(other)
            else -> throw RuntimeException("somebody is a NaN")
        }
    }

    fun finiteMagnitudeCompareTo(other: MutDec) : Int {
        val thisIsZero = c256IsZero()
        val otherIsZero = other.c256IsZero()
        val eitherIsZero = thisIsZero or otherIsZero
        when {
            !eitherIsZero -> {
                val cmpExpSci = this.sciExp().compareTo(other.sciExp())
                if (cmpExpSci != 0)
                    return cmpExpSci
                val expDelta = this.qExp - other.qExp
                val ret = when {
                    expDelta == 0 -> c256UnscaledCompareTo(other)
                    expDelta > 0 -> -other.c256ScaledCompareTo(this, expDelta)
                    else -> c256ScaledCompareTo(other, -expDelta)
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

    fun infiniteMagnitudeCompareTo(other: MutDec): Int {
        check(this.qExp <= NON_FINITE_INF && other.qExp <= NON_FINITE_INF)
        check(this.qExp == NON_FINITE_INF || other.qExp == NON_FINITE_INF)
        return qExp.compareTo(other.qExp)
    }

    fun magnitudeTotalCompareTo(other: MutDec): Int {
        if (this.qExp < NON_FINITE_INF && other.qExp < NON_FINITE_INF) {
            // For finite values, compare by magnitude first
            val magCmp = magnitudeCompareTo(other)
            if (magCmp != 0)
                return magCmp

            // Same numeric value - order by exponent (lower exponent first)
            return this.qExp.compareTo(other.qExp)
        }

        // At least one is non-finite
        return when {
            this.qExp < NON_FINITE_INF -> -1  // this is finite, other is inf/NaN
            other.qExp < NON_FINITE_INF -> 1  // other is finite, this is inf/NaN

            // Both are non-finite
            this.qExp == NON_FINITE_INF && other.qExp == NON_FINITE_INF -> 0  // Both infinity
            this.qExp == NON_FINITE_INF -> -1  // this is inf, other is NaN (inf < NaN)
            other.qExp == NON_FINITE_INF -> 1   // other is inf, this is NaN

            // Both are NaN - but need to reverse: sNaN < qNaN in totalOrder
            this.qExp == NON_FINITE_SNAN && other.qExp == NON_FINITE_QNAN -> -1  // sNaN < qNaN
            this.qExp == NON_FINITE_QNAN && other.qExp == NON_FINITE_SNAN -> 1   // qNaN > sNaN

            // Both same NaN type - compare payloads
            else -> U256Compare.u256UnscaledCompare(this, other)
        }
    }

    fun magnitudeEQ(other: MutDec) : Boolean {
        val thisIsZero = this.c256IsZero()
        val otherIsZero = other.c256IsZero()
        val bothAreZero = thisIsZero and otherIsZero
        val eitherIsZero = thisIsZero or otherIsZero
        if (this.sciExp() != other.sciExp())
            return bothAreZero
        if (! eitherIsZero) {
            val expDelta = this.qExp - other.qExp
            return when {
                expDelta == 0 -> this.c256UnscaledEQ(other)
                expDelta > 0 -> other.c256ScaledEQ(this, expDelta)
                else -> this.c256ScaledEQ(other, -expDelta)
            }
        }
        return bothAreZero
    }


    fun mutateRoundToIntegral(x: MutDec, rounding: DecRounding, env: DecEnv): MutDec {
        //FIXME - deal with special values
        if (qExp < 0) {
            val residue = this.c256SetScaleDownPow10(x, -qExp)
            qExp = 0
            sign = x.sign
            return roundAndFinalize(residue, rounding, env)
        } else {
            return set(x)
        }
    }

    fun setRoundToIntegralTiesToEven(x: MutDec, env: DecEnv) =
        mutateRoundToIntegral(x, ROUND_TIES_TO_EVEN, env)

    fun setRoundToIntegralTiesToAway(x: MutDec, env: DecEnv) =
        mutateRoundToIntegral(x, ROUND_TIES_TO_AWAY, env)

    fun setRoundToIntegralTowardZero(x: MutDec, env: DecEnv) =
        mutateRoundToIntegral(x, ROUND_TOWARD_ZERO, env)

    fun setRoundToIntegralTowardPositive(x: MutDec, env: DecEnv) =
        mutateRoundToIntegral(x, ROUND_TOWARD_POSITIVE, env)

    fun setRoundToIntegralTowardNegative(x: MutDec, env: DecEnv) =
        mutateRoundToIntegral(x, ROUND_TOWARD_NEGATIVE, env)

    fun setNextUp(x: MutDec, env: DecEnv): MutDec {
        set(x)
        when {
            qExp > NON_FINITE_INF -> {
                if (qExp == NON_FINITE_SNAN) {
                    env.signalInvalid(this)
                    qExp = NON_FINITE_QNAN
                }
                return this
            }
            qExp == NON_FINITE_INF -> {
                if (sign)
                    setMaxFiniteMagnitude(env)
                return this
            }
            c256IsZero() -> {
                setMinFiniteMagnitude(env)
                sign = false
                return this
            }
            sign == false -> {
                // nextUp is not an arithmetic operation and
                // therefore flags do not get set
                check(qExp <= env.qMax)
                mutateNextAwayFromZero(env)
                if (qExp > env.qMax)
                    setInfinite(sign = false)
                return this
            }
            else -> mutateNextTowardZero(env)
        }
        return finalize(ROUND_TOWARD_POSITIVE, env)
    }

    fun setNextDown(x: MutDec, env: DecEnv): MutDec {
        set(x)
        when {
            qExp > NON_FINITE_INF -> {
                if (qExp == NON_FINITE_SNAN) {
                    env.signalInvalid(this)
                    qExp = NON_FINITE_QNAN
                }
                return this
            }
            qExp == NON_FINITE_INF -> {
                if (sign == false)
                    setMaxFiniteMagnitude(env)
                return this
            }
            c256IsZero() -> {
                setMinFiniteMagnitude(env)
                sign = true
                return this
            }

            sign -> {
                check(qExp <= env.qMax)
                mutateNextAwayFromZero(env)
                if (qExp > env.qMax)
                    setInfinite(sign = true)
                return this
            }

            else -> mutateNextTowardZero(env)
        }
        return finalize(ROUND_TOWARD_NEGATIVE, env)
    }

    private fun mutateNextAwayFromZero(env: DecEnv) {
        val headroom = min(env.precision - digitLen, qExp - env.qTiny)
        if (headroom > 1 || headroom == 1 && !c256IsAllNines(env.precision-1)) {
            this.c256SetScaleUpPow10(this, headroom)
            this.qExp -= headroom
        }
        c256MutateIncrement()
        if (digitLen > env.precision) { // rolled up a decade
            c256SetPow10(env.precision - 1)
            ++this.qExp
        }
    }

    private fun mutateNextTowardZero(env: DecEnv) {
        val headroom =
            min(env.precision - digitLen + if (c256IsPowerOf10()) 1 else 0, qExp - env.qTiny)
        if (headroom > 0) {
            this.c256SetScaleUpPow10(this, headroom)
            this.qExp -= headroom
        }
        c256MutateDecrement()
    }

    fun minNum(x: MutDec, y: MutDec, env: DecEnv) = minNum_helper(x, y, 0, env)
    fun maxNum(x: MutDec, y: MutDec, env: DecEnv) = minNum_helper(x, y, -1, env)

    private fun minNum_helper(x: MutDec, y: MutDec, invertCompareZeroOrNeg1: Int, env: DecEnv) {
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

    fun minNumMag(x: MutDec, y: MutDec, env: DecEnv) = minNumMag_helper(x, y, 0, env)
    fun maxNumMag(x: MutDec, y: MutDec, env: DecEnv) = minNumMag_helper(x, y, -1, env)

    private fun minNumMag_helper(x: MutDec, y: MutDec, invertCompareZeroOrNeg1: Int, env: DecEnv) {
        val qMax = max(x.qExp, y.qExp)
        when {
            qMax < NON_FINITE_INF -> {
                val cmp = (x.magnitudeCompareTo(y) xor invertCompareZeroOrNeg1) - invertCompareZeroOrNeg1
                set(if (cmp <= 0) x else y)
            }
            else -> minNum_helper(x, y, invertCompareZeroOrNeg1, env)
        }
    }

    inline fun capExponentRange(e: Int): Int {
        return min(max(e, CAPPED_EXP_MIN), CAPPED_EXP_MAX)
    }

    fun setScale(x: MutDec, pow10: Int, env: DecEnv) {
        set(x)
        val p10 = capExponentRange(pow10)
        if (qExp < NON_FINITE_INF) {
            qExp += p10
            val residue = when {
                c256IsZero() -> EXACT
                (p10 > 0) -> {
                    val headroom = env.precision - digitLen
                    val scaleUp = min(headroom, p10)
                    this.c256SetScaleUpPow10(this, scaleUp)
                    qExp -= scaleUp
                    EXACT
                }
                (p10 < 0) -> this.c256SetScaleDownPow10(this, -p10)
                else -> return // p10 == 0 .. no scaling
            }
            roundAndFinalize(residue, env)
        } else if (qExp == NON_FINITE_SNAN)
            sNaNOperand()
    }

    // IEEE754-2008 5.3.2
    fun quantize(x: MutDec, y: MutDec, env: DecEnv) {
        val targetQ = y.qExp
        setScale(x, -targetQ, env)
    }

    // IEEE754-2008 5.3.3
    fun scaleB(x: MutDec, pow10: Int, env: DecEnv) {
        set(x)
        when {
            qExp <= NON_FINITE_INF -> {
                val p10 = capExponentRange(pow10)
                qExp += p10
                if (qExp > env.qMax || qExp < env.qTiny)
                    finalize(env)
            }
            x.qExp <= NON_FINITE_QNAN -> {}
            else -> sNaNOperand()
        }
    }

    // IEEE754-2008 5.3.3
    fun setLogB(x: MutDec, env: DecEnv): MutDec {
        val qX = x.qExp
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

    fun setMinimum(x: MutDec, y: MutDec, env: DecEnv): MutDec =
        setMinMaxImpl(x, y, MIN_OP, env)

    fun setMinimumMagnitude(x: MutDec, y: MutDec, env: DecEnv): MutDec =
        setMinMaxImpl(x, y, MIN_MAG_OP, env)

    fun setMinimumNumber(x: MutDec, y: MutDec, env: DecEnv): MutDec =
        setMinMaxImpl(x, y, MIN_NUM_OP, env)

    fun setMinimumMagnitudeNumber(x: MutDec, y: MutDec, env: DecEnv): MutDec =
        setMinMaxImpl(x, y, MIN_MAG_NUM_OP, env)

    fun setMaximum(x: MutDec, y: MutDec, env: DecEnv): MutDec =
        setMinMaxImpl(x, y, MAX_OP, env)

    fun setMaximumMagnitude(x: MutDec, y: MutDec, env: DecEnv): MutDec =
        setMinMaxImpl(x, y, MAX_MAG_OP, env)

    fun setMaximumNumber(x: MutDec, y: MutDec, env: DecEnv): MutDec =
        setMinMaxImpl(x, y, MAX_NUM_OP, env)

    fun setMaximumMagnitudeNumber(x: MutDec, y: MutDec, env: DecEnv): MutDec =
        setMinMaxImpl(x, y, MAX_MAG_NUM_OP, env)


    fun setMinMaxImpl(x: MutDec, y: MutDec, op: Int, env: DecEnv): MutDec {
        val qX = x.qExp
        val qY = y.qExp
        val qMax = max(qX, qY)
        if (qMax <= NON_FINITE_INF) {
            var cmp = if ((op and MAG_MASK) != 0)
                x.magnitudeCompareTo(y)
            else
                x.compareTo(y)
            if (cmp == 0)
                cmp = x.totalCompareTo(y)
            return set(if ((cmp >= 0) xor ((op and MAX_MASK) == 0)) x else y)
        }
        if ((op and NUM_MASK) != 0) {
            if (qX <= NON_FINITE_INF) {
                set(x)
                if (qY == NON_FINITE_SNAN)
                    env.signalInvalid(this)
                return this
            }
            if (qY <= NON_FINITE_INF) {
                set(y)
                if (qX == NON_FINITE_SNAN)
                    env.signalInvalid(this)
                return this
            }
        }
        return setNaNOperand(x, y, env)
    }


    fun compareQuiet754(other: MutDec, env: DecEnv): Compare754Result =
        compare754(other, false, env)

    fun compareSignaling754(other: MutDec, env: DecEnv): Compare754Result =
        compare754(other, true, env)

    fun compare754(other: MutDec, isSignaling: Boolean, env: DecEnv): Compare754Result {
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
                env.operandIsSignalingNaN(guiltyParty)
                IEEE754_UNORDERED
            }
        }
    }

    override fun equals(other: Any?) : Boolean {
        if (other is MutDec) {
            val qMax = max(qExp, other.qExp)
            return when {
                qMax < NON_FINITE_INF -> when {
                    c256IsZero() -> other.c256IsZero()
                    other.c256IsZero() -> false
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

    fun totalOrder(x: MutDec) {
        throw RuntimeException("not impl")
    }
    // 5.7.3 Decimal operation
    fun sameQuantum(x: MutDec) = (this.qExp == x.qExp)

    // FIXME ... implement this so that there are fewer memory allocations
    override fun toString(): String = toString(toEngineeringExp = false)

    fun toString(toEngineeringExp: Boolean): String {
        return when {
            qExp == 0 -> IntegerParsePrint.int256ToString(sign, this)
            qExp >= MIN_SPECIAL_VALUE -> toSpecialValueString()
            qExp < 0 && sciExp() >= -6 -> toDecimalPointString()
            else -> toScientificString(toEngineeringExp)
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
        val expDigitLen = max(U256Pow10.calcDigitLen64(Math.abs(eExp).toLong()), 1)
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
        val j = IntegerParsePrint.int32ToUtf8(eExp, utf8, i+1)
        check (i + 1 + j == utf8.size)
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
        val expDigitLen = max(U256Pow10.calcDigitLen64(Math.abs(eExp).toLong()), 1)
        val totalLen = signLen + decimalPointLen + additionalLeftOfPointZeroCount +
                printedDigitLen + expELen + expSignLen + expDigitLen
        val utf8 = ByteArray(totalLen)
        var i = IntegerParsePrint.int256ToUtf8(sign, this, utf8, 0)
        when {
            additionalLeftOfPointZeroCount > 0 ->  {
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
        val j = IntegerParsePrint.int32ToUtf8(adjustedExp, utf8, i+1)
        check (i + 1 + j == utf8.size)
        return String(utf8)
    }

    private fun toSpecialValueString() : String {
        var nanStr = "NaN"
        when {
            qExp == NON_FINITE_INF -> {
                return if (sign) "-Infinity" else "Infinity"
            }

            qExp == NON_FINITE_QNAN -> {
                if (sign)
                    nanStr = "-NaN"
            }

            qExp == NON_FINITE_SNAN -> {
                nanStr = if (sign) "-sNaN" else "sNaN"
            }

            else -> throw RuntimeException("?que? $qExp")
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
        if (qExp < MIN_SPECIAL_VALUE) {
            val printLen = calcDebugPrintLength()
            val utf8 = ByteArray(printLen)
            val i = IntegerParsePrint.int256ToUtf8(sign, this, utf8, 0)
            utf8[i] = 'E'.code.toByte()
            val j = IntegerParsePrint.int32ToUtf8(qExp, utf8, i + 1)
            check(i + 1 + j == utf8.size)
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
        val expDigitLen = max(U256Pow10.calcDigitLen64(Math.abs(qExp).toLong()), 1)
        return signLen + coeffLen + expELen + expSignLen + expDigitLen
    }

    fun encodeLittleEndianLongsBid128() = encodeLittleEndianBid128(LongArray(2))
    fun encodeLittleEndianBid128(littleEndianLongs: LongArray) = SerDeBid128.encodeLittleEndianBid128(this, littleEndianLongs)
    fun encodeLittleEndianBytesBid128() = encodeLittleEndianBid128(ByteArray(16))
    fun encodeLittleEndianBid128(littleEndianBytes: ByteArray) = SerDeBid128.encodeLittleEndianBid128(this, littleEndianBytes)

    fun encodeLittleEndianLongsDpd128() = encodeLittleEndianDpd128(LongArray(2))
    fun encodeLittleEndianDpd128(littleEndianLongs: LongArray) = SerDeDpd128.encodeLittleEndianDpd128(littleEndianLongs, this)
    fun encodeLittleEndianBytesDpd128() = encodeLittleEndianDpd128(ByteArray(16))
    fun encodeLittleEndianDpd128(littleEndianBytes: ByteArray) = SerDeDpd128.encodeLittleEndianDpd128(littleEndianBytes, this)

    fun encodeBigEndianLongsBid128() = encodeBigEndianBid128(LongArray(2))
    fun encodeBigEndianBid128(BigEndianLongs: LongArray) = SerDeBid128.encodeBigEndianBid128(this, BigEndianLongs)
    fun encodeBigEndianBytesBid128() = encodeBigEndianBid128(ByteArray(16))
    fun encodeBigEndianBid128(BigEndianBytes: ByteArray) = SerDeBid128.encodeBigEndianBid128(this, BigEndianBytes)

    fun encodeBigEndianLongsDpd128() = encodeBigEndianDpd128(LongArray(2))
    fun encodeBigEndianDpd128(bigEndianLongs: LongArray) = SerDeDpd128.encodeBigEndianDpd128(bigEndianLongs, this)
    fun encodeBigEndianBytesDpd128() = encodeBigEndianDpd128(ByteArray(16))
    fun encodeBigEndianDpd128(bigEndianBytes: ByteArray) = SerDeDpd128.encodeBigEndianDpd128(bigEndianBytes, this)

}
