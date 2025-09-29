package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT
import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_NEGATIVE
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_ZERO
import com.decimal128.decimal.Class754.*
import com.decimal128.decimal.U256Compare.u256UnscaledCompare
import kotlin.math.max
import kotlin.math.min

internal const val MIN_SPECIAL_VALUE = Integer.MAX_VALUE - 2
internal const val NON_FINITE_INF = Integer.MAX_VALUE - 2
internal const val NON_FINITE_QNAN = Integer.MAX_VALUE - 1
internal const val NON_FINITE_SNAN = Integer.MAX_VALUE

const val CAPPED_EXP_MIN = -2000000000
const val CAPPED_EXP_MAX = 2000000000

val DEFAULT_128_CONTEXT = DecimalContext.newDecimal128Context()

class MutDec() : U256() {
    var sign = false
    var qExp = 0

    constructor(sign: Boolean, qExp: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long) : this() {
        this.u256Set256(dw3, dw2, dw1, dw0)
        this.qExp = qExp
        this.sign = sign
    }

    constructor(str: String): this() {
        DecimalParsePrint.decFromString(this, str, false, DEFAULT_128_CONTEXT)
    }

    constructor(str: String, zeroNanPayload: Boolean) : this() {
        DecimalParsePrint.decFromString(this, str, zeroNanPayload, DEFAULT_128_CONTEXT)
    }

    constructor(other: MutDec) : this(other.sign, other.qExp, other.dw3, other.dw2, other.dw1, other.dw0)

    companion object {
        fun newInstance(): MutDec = MutDec()

        fun newInfinity(sign: Boolean = false) = MutDec().setInfinite(sign)

        fun newAbs(x: MutDec, ctx: DecimalContext = DEFAULT_128_CONTEXT) = MutDec(x).mutateAbs()

        fun newAdd(x: MutDec, y: MutDec, ctx: DecimalContext = DEFAULT_128_CONTEXT) =
            addImpl(MutDec(), x, y.sign, y, ctx)

        fun newSub(x: MutDec, y: MutDec, ctx: DecimalContext = DEFAULT_128_CONTEXT) =
            addImpl(MutDec(), x, !y.sign, y, ctx)

        fun newMul(x: MutDec, y: MutDec, ctx: DecimalContext = DEFAULT_128_CONTEXT) =
            MutDec().setMul(x, y, ctx)

        fun newFma(x: MutDec, y: MutDec, z: MutDec, ctx: DecimalContext = DEFAULT_128_CONTEXT) =
            MutDec().setFma(x, y, z, ctx)

        fun newNegate(x: MutDec, ctx: DecimalContext = DEFAULT_128_CONTEXT) = MutDec(x).mutateNegate()

        fun newSquare(x: MutDec, ctx: DecimalContext = DEFAULT_128_CONTEXT) =
            MutDec().setSquare(x, ctx)

        fun newDiv(x: MutDec, y: MutDec, ctx: DecimalContext = DEFAULT_128_CONTEXT) = MutDec().setDiv(x, y, ctx)

        fun newReciprocal(x: MutDec, ctx: DecimalContext = DEFAULT_128_CONTEXT) =
            MutDec().setReciprocal(x, ctx)

        fun newIntegral(x: MutDec, ctx: DecimalContext = DEFAULT_128_CONTEXT) = MutDec(x).mutateToIntegral(ctx)

        fun newSqrt(x: MutDec, ctx: DecimalContext = DEFAULT_128_CONTEXT) =
            MutDec().setSqrt(x, ctx)

        private fun addImpl(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, ctx: DecimalContext): MutDec {
            val qMax = max(x.qExp, y.qExp)
            return when {
                qMax < MIN_SPECIAL_VALUE && x.qExp == y.qExp ->
                    unscaledFiniteAddImpl(z, x, ySign, y, ctx)

                qMax < MIN_SPECIAL_VALUE ->
                    scaledFiniteAddImpl(z, x, ySign, y, ctx)

                qMax == NON_FINITE_INF ->
                    infiniteAddImpl(z, x, ySign, y, ctx)

                else -> { z.setNaN(x, y, ctx); return z }
            }
        }

        private fun unscaledFiniteAddImpl(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, ctx: DecimalContext): MutDec {
            val xSign = x.sign
            if (x.bitLen == 0 && y.bitLen == 0 && xSign == ySign) {
                // IEEE754-2019 6.3 The sign bit
                // However, under all rounding-direction attributes,
                // when x is zero, x + x and x − (−x) have the sign of x.
                return z.set(x)
            }
            // FIXME
            //  removing this use of s256AddImpl would probably automagically
            //  solve the signed integer problem above
//            z.s256AddImpl(x.sign, x, ySign, y)
            z.qExp = x.qExp
            if (xSign == ySign) {
                z.u256SetAdd(x, y)
                z.sign = xSign
            } else {
                val cmp = u256UnscaledCompare(x, y)
                when {
                    (cmp > 0) -> {
                        z.u256SetSub(x, y)
                        z.sign = xSign and (z.bitLen > 0)
                    }
                    (cmp < 0) -> {
                        z.u256SetSub(y, x)
                        z.sign = ySign and (z.bitLen > 0)
                    }
                    else -> {
                        z.sign = false
                        z.u256SetZero()
                    }
                }
            }

            // IEEE754-2019 6.3 The sign bit
            // When the sum of two operands with opposite signs
            // (or the difference of two operands with like signs) is
            // exactly zero, the sign of that sum (or difference)
            // shall be +0 under all rounding-direction attributes except
            // roundTowardNegative; under that attribute, the sign of an
            // exact zero sum (or difference) shall be −0.
            z.sign = z.sign or ((z.bitLen == 0) and ctx.isRoundTowardNegative)
            return z.roundAndFinalize(EXACT, ctx)
        }

        private fun scaledFiniteAddImpl(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, ctx: DecimalContext): MutDec {
            val qX = x.qExp
            val qY = y.qExp
            val xSign = x.sign
            val qMax = max(qX, qY)
            val xIsZero = x.bitLen == 0
            val yIsZero = y.bitLen == 0
            when {
                qMax < MIN_SPECIAL_VALUE -> {
                    val residue = when {
                        xIsZero and yIsZero and (xSign == ySign) -> {
                            // IEEE754-2019 6.3 The sign bit
                            // However, under all rounding-direction attributes,
                            // when x is zero, x + x and x − (−x) have the sign of x.
                            z.setZero()
                            z.sign = xSign
                            z.qExp = Math.min(qX, qY)
                            return z
                        }
                        xSign == ySign -> {
                            z.sign = xSign
                            MagnitudeAddSub.magScaledAdd(z, x, y)
                        }
                        x.magnitudeCompareTo(y) >= 0 -> {
                            val res = MagnitudeAddSub.magSub(z, x, y)
                            z.sign = xSign and (z.bitLen > 0)
                            res
                        }
                        else -> {
                            z.sign = ySign
                            MagnitudeAddSub.magSub(z, y, x)
                        }
                    }
                    z.sign = z.sign or ((z.bitLen == 0) and ctx.isRoundTowardNegative)
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

        private fun infiniteAddImpl(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, ctx: DecimalContext): MutDec {
            val qX = x.qExp
            val qY = y.qExp
            check (qX == NON_FINITE_INF || qY == NON_FINITE_INF)
            if (qX == qY && x.sign != ySign) {
                z.setNaN(ctx)
                return ctx.signalInvalid(z)
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

    }

    fun sciExp() = qExp + (digitLen - (-digitLen ushr 31))

    fun setZero()  = setZero(false)

    fun setZero(sign: Boolean) {
        u256SetZero()
        this.qExp = 0
        this.sign = sign
    }

    private fun setNaN(x: MutDec, y: MutDec, ctx: DecimalContext) {
        val xQ = x.qExp
        val yQ = y.qExp
        val maxQ = max(xQ, yQ)
        check(maxQ >= NON_FINITE_QNAN)
        this.set(if (maxQ == xQ) x else y)
        if (maxQ == NON_FINITE_SNAN) {
            ctx.operandIsSignalingNaN(if (xQ == NON_FINITE_SNAN) x else y)
            ctx.signalInvalid(this)
        }
        // note that a sNaN gets mapped to a NaN with sNaN + Infinity ...
        // ... according to MFColishaw decTest
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    private fun setNaN(x: MutDec, y: MutDec, a: MutDec, ctx: DecimalContext) {
        val qX = x.qExp
        val qY = y.qExp
        val qA = a.qExp
        val qMaxXYA = max(max(qX, qY), qA)
        check(qMaxXYA >= NON_FINITE_QNAN)
        setZero()
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    private fun sNaNOperand() {
        throw RuntimeException("sNaN operand")
    }

    private fun setNaN(x: MutDec, ctx: DecimalContext) {
        val q = x.qExp
        check(q >= NON_FINITE_QNAN)
        setZero()
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    internal fun setNaN(ctx: DecimalContext) {
        setZero()
        sign = false
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    internal fun setNaN(payload: Int, ctx: DecimalContext) {
        sign = false
        u256Set64(payload.toLong())
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    internal fun setNaN(isSignaling: Boolean, sign: Boolean, payloadHi: Long, payloadLo: Long) {
        this.sign = sign
        this.qExp = if (isSignaling) NON_FINITE_SNAN else NON_FINITE_QNAN
        this.dw3 = 0
        this.dw2 = 0
        this.dw1 = payloadHi and ((1L shl (110 - 64)) - 1L)
        this.dw0 = payloadLo
    }

    fun setSNaN(ctx: DecimalContext) {
        setZero()
        qExp = NON_FINITE_SNAN
    }

    internal fun setSNaN(payload: Int, ctx: DecimalContext) {
        sign = false
        u256Set64(payload.toLong())
        qExp = NON_FINITE_SNAN
        //FIXME - see IEEE754r 6.2
    }

    fun setInfinite(sign: Boolean = false) {
        this.u256SetZero()
        this.qExp = NON_FINITE_INF
        this.sign = sign
    }

    fun set(l: Long): MutDec {
        this.qExp = 0
        this.sign = l < 0
        val mask = l shr 63
        val abs = (l xor mask) - mask
        u256Set64(abs)
        return this
    }

    fun setUnsigned(ul: Long): MutDec {
        this.qExp = 0
        this.sign = false
        u256Set64(ul)
        return this
    }

    fun set(x: MutDec): MutDec {
        u256Set(x)
        this.qExp = x.qExp
        this.sign = x.sign
        return this
    }

    fun set(str: String): MutDec {
        DecimalParsePrint.decFromString(this, str, false, DEFAULT_128_CONTEXT)
        return this
    }

    fun setBid128(bid128Hi: Long, bid128Lo: Long) = SerDeBid128.decodeBid128Longs(this, bid128Hi, bid128Lo)

    fun setDpd128(dpd128Hi: Long, dpd128Lo: Long) = SerDeDpd128.decodeDpd128Longs(this, dpd128Hi, dpd128Lo)

    fun setMaxFiniteMagnitude(ctx: DecimalContext): MutDec {
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

    fun setMinFiniteMagnitude(ctx: DecimalContext): MutDec {
        qExp = ctx.qTiny
        super.u256SetOne()
        return this
    }

    fun setNegate(x: MutDec): MutDec {
        set(x)
        this.sign = !this.sign
        return this
    }

    fun mutateNegate(): MutDec {
        when (this.qExp) {
            NON_FINITE_QNAN -> {}
            NON_FINITE_SNAN -> this.qExp = NON_FINITE_QNAN
            NON_FINITE_INF -> this.sign = !this.sign
            else -> this.sign = !this.sign && bitLen > 0
        }
        return this
    }

    fun mutateAbs(): MutDec {
        if (this.qExp >= NON_FINITE_QNAN)
            this.qExp = NON_FINITE_QNAN
        else
            this.sign = false
        return this
    }

    fun mutateToIntegral(ctx: DecimalContext): MutDec {
        // FIXME ... not tested and not correct
        when {
            qExp < MIN_SPECIAL_VALUE -> {
                if (qExp < 0) {
                    if (bitLen == 0) {
                        qExp = 0
                        return this
                    }
                    ctx.signalInexact(this)
                    val eExp = sciExp()
                    when (ctx.roundingDirection) {
                        ROUND_TIES_TO_EVEN, ROUND_TIES_TO_AWAY -> {
                            if (eExp <= -2) {
                                return MutDec()
                            }
                            val half = MutDec(false, -1, 0L, 0L, 0L, 5L)
                            val cmp = this.compareTo(half, ctx)
                            if (cmp < 0 || cmp == 0 && ctx.roundingDirection == ROUND_TIES_TO_EVEN) {
                                return MutDec()
                            } else {
                                return MutDec(false, 0, 0L, 0L, 0L, 1L)
                            }
                        }
                        ROUND_TOWARD_ZERO -> return MutDec()
                        ROUND_TOWARD_NEGATIVE -> {
                            if (sign)
                                return MutDec(false, 0, 0L, 0L, 0L, 1L)
                            else
                                return MutDec()
                        }
                        ROUND_TOWARD_POSITIVE -> {
                            if (!sign)
                                return MutDec(false, 0, 0L, 0L, 0L, 1L)
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

    fun add(y: MutDec) = newAdd(this, y)
    fun subtract(y: MutDec) = newSub(this, y)
    fun multiply(y: MutDec) = newMul(this, y)
    fun divide(y: MutDec) = newDiv(this, y)

    fun add(y: MutDec, ctx: DecimalContext) = newAdd(this, y, ctx)
    fun subtract(y: MutDec, ctx: DecimalContext) = newSub(this, y, ctx)
    fun multiply(y: MutDec, ctx: DecimalContext) = newMul(this, y, ctx)
    fun square(ctx: DecimalContext) = newSquare(this, ctx)
    fun divide(y: MutDec, ctx: DecimalContext) = newDiv(this, y, ctx)
    fun reciprocal(ctx: DecimalContext) = newReciprocal(this, ctx)
    fun sqrt(ctx: DecimalContext) = newSqrt(this, ctx)

    fun mutateAdd(y: MutDec, ctx: DecimalContext) { setAdd(this, y, ctx) }
    fun mutateSub(y: MutDec, ctx: DecimalContext) { setSub(this, y, ctx) }
    fun mutateMul(y: MutDec, ctx: DecimalContext) { setMul(this, y, ctx) }
    fun mutateSquare(ctx: DecimalContext) { setSquare(this, ctx) }
    fun mutateDiv(y: MutDec, ctx: DecimalContext) { setDiv(this, y, ctx) }
    fun mutateReciprocal(ctx: DecimalContext) { setReciprocal(this, ctx) }
    fun mutateSqrt(ctx: DecimalContext) { setSqrt(this, ctx) }


    // IEEE754-2008 5.4.1
    fun setAdd(x: MutDec, y: MutDec, ctx: DecimalContext) = addImpl(this, x, y.sign, y, ctx)

    // IEEE754-2008 5.4.1
    fun setSub(x: MutDec, y: MutDec, ctx: DecimalContext) = addImpl(this, x, !y.sign, y, ctx)

    // IEEE754-2008 5.4.1
    fun setMul(x: MutDec, y: MutDec, ctx: DecimalContext): MutDec {
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
                if (x.isZero() || y.isZero()) {
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

    fun setSquare(x: MutDec, ctx: DecimalContext): MutDec {
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
    fun setFma(x: MutDec, y: MutDec, a: MutDec, ctx: DecimalContext): MutDec {
        val qX = x.qExp
        val qY = y.qExp
        val qA = a.qExp
        val qMaxXY = max(qX, qY)
        val qMaxXYA = max(qMaxXY, qA)
        val productSign = x.sign xor y.sign
        when {
            qMaxXYA < MIN_SPECIAL_VALUE -> {
                val aT = if (this === a) MutDec(a) else a
                // multiply without roundAndFinalize .. remains exact
                this.u256SetMul(x, y)
                this.qExp = x.qExp + y.qExp
                this.sign = productSign
                // roundAndFinalize takes place here
                this.setAdd(this, aT, ctx)
            }
            qMaxXYA == NON_FINITE_INF -> when {
                (qMaxXY == NON_FINITE_INF) && (x.isZero() || y.isZero()) -> {
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

    fun setDiv(x: MutDec, y: MutDec, ctx: DecimalContext): MutDec {
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
                        qExp = ctx.qTiny
                    }
                }
            }
            else -> setNaN(x, y, ctx)
        }
        return this
    }

    fun setReciprocal(x: MutDec, ctx: DecimalContext): MutDec {
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

    fun setSqrt(x: MutDec, ctx: DecimalContext): MutDec {
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

    fun compareTo(other: MutDec, ctx: DecimalContext) : Int {
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

    fun magnitudeCompareTo(other: MutDec) : Int {
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

    fun magnitudeEQ(other: MutDec) : Boolean {
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


    fun mutateRoundToIntegral(x: MutDec, rd: DecRounding, ctx: DecimalContext): MutDec {
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

    fun setRoundToIntegralTiesToEven(x: MutDec, ctx: DecimalContext) =
        mutateRoundToIntegral(x, ROUND_TIES_TO_EVEN, ctx)

    fun setRoundToIntegralTiesToAway(x: MutDec, ctx: DecimalContext) =
        mutateRoundToIntegral(x, ROUND_TIES_TO_AWAY, ctx)

    fun setRoundToIntegralTowardZero(x: MutDec, ctx: DecimalContext) =
        mutateRoundToIntegral(x, ROUND_TOWARD_ZERO, ctx)

    fun setRoundToIntegralTowardPositive(x: MutDec, ctx: DecimalContext) =
        mutateRoundToIntegral(x, ROUND_TOWARD_POSITIVE, ctx)

    fun setRoundToIntegralTowardNegative(x: MutDec, ctx: DecimalContext) =
        mutateRoundToIntegral(x, ROUND_TOWARD_NEGATIVE, ctx)

    fun setNextUp(x: MutDec, ctx: DecimalContext) {
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

    fun setNextDown(x: MutDec, ctx: DecimalContext) {
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

    fun minNum(x: MutDec, y: MutDec, ctx: DecimalContext) = minNum_helper(x, y, 0, ctx)
    fun maxNum(x: MutDec, y: MutDec, ctx: DecimalContext) = minNum_helper(x, y, -1, ctx)

    private fun minNum_helper(x: MutDec, y: MutDec, invertCompareZeroOrNeg1: Int, ctx: DecimalContext) {
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

    fun minNumMag(x: MutDec, y: MutDec, ctx: DecimalContext) = minNumMag_helper(x, y, 0, ctx)
    fun maxNumMag(x: MutDec, y: MutDec, ctx: DecimalContext) = minNumMag_helper(x, y, -1, ctx)

    private fun minNumMag_helper(x: MutDec, y: MutDec, invertCompareZeroOrNeg1: Int, ctx: DecimalContext) {
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

    fun setScale(x: MutDec, pow10: Int, ctx: DecimalContext) {
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
    fun quantize(x: MutDec, y: MutDec, ctx: DecimalContext) {
        val targetQ = y.qExp
        setScale(x, -targetQ, ctx)
    }

    // IEEE754-2008 5.3.3
    fun scaleB(x: MutDec, pow10: Int, ctx: DecimalContext) {
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

    fun compareQuiet754(other: MutDec, ctx: DecimalContext): Compare754Result =
        compare754(other, false, ctx)

    fun compareSignaling754(other: MutDec, ctx: DecimalContext): Compare754Result =
        compare754(other, true, ctx)

    fun compare754(other: MutDec, isSignaling: Boolean, ctx: DecimalContext): Compare754Result {
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
        if (other is MutDec) {
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
    fun isFiniteNonZero() = qExp < NON_FINITE_INF && !u256IsZero()
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
    override fun toString(): String {
        return when {
            qExp == 0 -> Int256ParsePrint.int256ToString(sign, this)
            qExp >= MIN_SPECIAL_VALUE -> toSpecialValueString()
            qExp < 0 && sciExp() >= -6 -> toDecimalPointString()
            else -> toNormalizedScientificString()
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private /*inline*/ fun toDecimalPointString() : String {
        val digitsRightOfDecimal = -qExp
        val leadingZeroCount = Math.max(1 + digitsRightOfDecimal - digitLen, 0)
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
        val printedDigitLen = Math.max(digitLen, 1)
        val expELen = 1
        val expSignLen = if (eExp < 0) 1 else 0
        val expDigitLen = Math.max(U256Pow10.calcDigitLen64(Math.abs(eExp).toLong()), 1)
        val totalLen = signLen + decimalPointLen + printedDigitLen + expELen + expSignLen + expDigitLen
        val utf8 = ByteArray(totalLen)
        var i = Int256ParsePrint.int256ToUtf8(sign, this, utf8, 0)
        if (digitLen > 1) {
            val insertionPoint = signLen + 1
            moveBytesUp1(utf8, insertionPoint, digitLen - 1)
            utf8[insertionPoint] = '.'.code.toByte()
            ++i
        }
        utf8[i] = 'E'.code.toByte()
        val j = Int256ParsePrint.intToUtf8(eExp, utf8, i+1)
        check (j == utf8.size)
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
        if (u256IsZero())
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
            val i = Int256ParsePrint.int256ToUtf8(sign, this, utf8, 0)
            utf8[i] = 'E'.code.toByte()
            val j = Int256ParsePrint.intToUtf8(qExp, utf8, i + 1)
            check(j == utf8.size)
            return String(utf8)
        } else {
            return toSpecialValueString()
        }
    }

    fun calcDebugPrintLength(): Int {
        val signLen = if (sign) 1 else 0
        val coeffLen = Math.max(digitLen, 1)
        val expELen = 1
        val expSignLen = if (qExp < 0) 1 else 0
        val expDigitLen = Math.max(U256Pow10.calcDigitLen64(Math.abs(qExp).toLong()), 1)
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

    internal fun roundAndFinalize(inboundResidue: Residue, ctx: DecimalContext) =
        roundAndFinalize(inboundResidue, ctx.roundingDirection, ctx)

    private fun roundAndFinalize(inboundResidue: Residue, decRounding: DecRounding, ctx: DecimalContext): MutDec {
        val eMax = ctx.eMax
        val precision = ctx.precision
        if (qExp < MIN_SPECIAL_VALUE) {
            if (bitLen != 0) {
                var eExp = qExp + (digitLen - 1)
                var qMax = ctx.qMax
                // IEEE754-2008 7.5: detect tininess on the unrounded result
                val isTiny = (eExp < ctx.eMin)

                val excess = max(0, digitLen - precision)
                val myQTiny = ctx.qTiny - excess      // threshold for normalized

                // 2) Normalized result: round only if bd has >precision digits
                if (eExp <= eMax && qExp >= myQTiny) {
                    var totalResidue = inboundResidue
                    if (qExp > qMax) {
                        // clamp/fold-over
                        val qExcess = qExp - qMax
                        U256ScalePow10.u256ScaleUpPow10(this, this, qExcess)
                        check (digitLen <= precision)
                        qExp -= qExcess
                        check (qExp == qMax)
                        check (inboundResidue == EXACT)
                        return this
                    }
                    if (excess != 0) {
                        val roundingResidue = U256ScalePow10.u256ScaleDownPow10(this, this, excess)
                        qExp += excess
                        check(digitLen == precision)
                        check(eExp == qExp + (digitLen - 1))
                        totalResidue = roundingResidue.merge(inboundResidue)
                    }

                    if (totalResidue == EXACT) {
                        // 7.5 Underflow
                        // If the rounded result is exact, no flag is raised
                        // and no inexact exception is signaled.
                        return this
                    }

                    val roundUp = totalResidue.ulpRoundUp(decRounding.negate(sign), super.dw0)
                    if (roundUp)
                        super.u256MutateIncrement()
                    if (!roundUp || digitLen <= precision)
                        return if (isTiny) ctx.signalInexactUnderflow(this) else ctx.signalInexact(this)
                    check(digitLen == precision + 1)
                    // if we rolled into another digit because of roundup
                    // then the result is EXACTly divisible by 10
                    val residueExact = U256ScalePow10.u256ScaleDownPow10(this, this, 1)
                    check(residueExact == Residue.EXACT)
                    ++qExp
                    check(qExp + (digitLen - 1) == eExp + 1)
                    ++eExp
                    if (eExp <= eMax)
                        return ctx.signalInexact(this)
                    // rounding caused overflow
                    // fall into next conditional and flow over
                }

                // 1) Overflow => +/- Infinity
                if (eExp > eMax) {
                    check(! isTiny)
                    // overflow IEEE754-2008 7.4 Overflow page 37
                    if (decRounding.overflowsToInfinity(sign)) {
                        setInfinite(sign)
                    } else {
                        setMaxFiniteMagnitude(ctx)
                    }
                    return ctx.signalInexactOverflow(this)
                }

                // 7.5.1: subnormal rounding (tiny result stays nonzero)
                check(isTiny)
                val myQMin = ctx.qTiny - digitLen           // threshold for subnormal cohort
                val overlap = qExp - myQMin
                if (overlap >= 0) {
                    val excess2 = super.digitLen - overlap
                    val scaleResidue = U256ScalePow10.u256ScaleDownPow10(this, this, excess2)
                    qExp += excess2
                    check(digitLen <= precision)
                    check(eExp == qExp + (digitLen - 1))

                    val totalResidue = scaleResidue.merge(inboundResidue)
                    if (totalResidue == EXACT) {
                        // 7.5 Underflow
                        // If the rounded result is exact, no flag is raised
                        // and no inexact exception is signaled.
                        return this
                    }
                    val roundUp = totalResidue.ulpRoundUp(decRounding.negate(sign), super.dw0)
                    if (roundUp) {
                        super.u256MutateIncrement()
                        if (digitLen > precision) {
                            check(digitLen == precision + 1)
                            // if we rolled into another digit because of roundup
                            // then the result is definitely divisible by 10
                            val residueExact = U256ScalePow10.u256ScaleDownPow10(this, this, 1)
                            check(residueExact == Residue.EXACT)
                            ++qExp
                        }
                    }
                } else {
                    // underflow ... swamped non-zero value
                    if (decRounding.underflowsToZero(sign)) {
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
