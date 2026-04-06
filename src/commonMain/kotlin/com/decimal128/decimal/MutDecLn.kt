package com.decimal128.decimal

internal object MutDecLn {

    val ctx38 = DecContext.decimal128Extended38()

    // Numerator P(z) coefficients
// p[0] = 0, p[1] = 1 -- handled directly in Horner, not stored
    private val P2 = MutDec().set("4", ctx38)
    private val P3 = MutDec().set("6.5539215686274509803921568627450980392", ctx38)
    private val P4 = MutDec().set("5.6617647058823529411764705882352941176", ctx38)
    private val P5 = MutDec().set("2.7632352941176470588235294117647058824", ctx38)
    private val P6 = MutDec().set("0.75686274509803921568627450980392156863", ctx38)
    private val P7 = MutDec().set("0.10824175824175824175824175824175824176", ctx38)
    private val P8 = MutDec().set("0.0067711700064641241111829347123464770524", ctx38)
    private val P9 = MutDec().set("0.00011637055754702813526342938107643989997", ctx38)

    // Denominator Q(z) coefficients
// q[0] = 1 -- handled directly in Horner, not stored
    private val Q1 = MutDec().set("4.5000000000000000000000000000000000000", ctx38)
    private val Q2 = MutDec().set("8.4705882352941176470588235294117647059", ctx38)
    private val Q3 = MutDec().set("8.6470588235294117647058823529411764706", ctx38)
    private val Q4 = MutDec().set("5.1882352941176470588235294117647058824", ctx38)
    private val Q5 = MutDec().set("1.8529411764705882352941176470588235294", ctx38)
    private val Q6 = MutDec().set("0.38009049773755656108597285067873303167", ctx38)
    private val Q7 = MutDec().set("0.040723981900452488687782805429864253394", ctx38)
    private val Q8 = MutDec().set("0.0018510900863842040312628547922665569724", ctx38)
    private val Q9 = MutDec().set("0.000020567667626491155902920608802961744138", ctx38)

    private val ZERO = MutDec()
    private val ONE = MutDec().setOne()
    private val FOUR = MutDec().set(4)

    // ln(k) for k = 0 + 1..9
    private val LN = arrayOf(
        ZERO,
        ZERO, // LN1
        MutDec().set("0.69314718055994530941723212145817656808", ctx38), // LN2
        MutDec().set("1.0986122886681096913952452369225257046", ctx38),  // LN3
        MutDec().set("1.3862943611198906188344642429163531362", ctx38),  // LN4
        MutDec().set("1.6094379124341003746007593332261876395", ctx38),  // LN5
        MutDec().set("1.7917594692280550008124773583807022727", ctx38),  // LN6
        MutDec().set("1.9459101490553133051053527434431797296", ctx38),  // LN7
        MutDec().set("2.0794415416798359282516963643745297042", ctx38),  // LN8
        MutDec().set("2.1972245773362193827904904738450514093", ctx38),  // LN9
    )

    // ln(10)
    private val LN10 = MutDec().set("2.3025850929940456840179914546843642076", ctx38)
    fun mutDecLnImpl(z: MutDec, x: MutDec, ctx: DecContext): MutDec {
        val xSteal = x.steal
        when (stealTyp(xSteal)) {
            STEAL_TYP_FNZ -> when {
                stealSignFlag(xSteal) -> return ctx.signalInvalid(z.setNaN())  // ln(negative) = NaN
                else -> return lnImplFNZ(z, x, ctx)
            }

            STEAL_TYP_ZER -> return ctx.signalDivByZero(z.setInfinite(true)) // ln(0) = -∞
            STEAL_TYP_INF -> when {
                stealSignFlag(xSteal) ->
                    return ctx.setNanSignalInvalid(
                        z,
                        InvalidOperationReason.LOG_OF_NEG_INFINITY
                    )  // ln(-∞) = NaN
                else -> return z.setInfinite(false)                  // ln(+∞) = +∞
            }

            else -> return z.setNanOperandFound(x, ctx)
        }
    }

    /**
     * Computes the natural logarithm of a positive, finite, non-zero decimal value.
     *
     * Uses argument reduction followed by a [9,9] Padé rational approximation:
     *
     * 1. **Argument reduction**: Write `x = k * 10^e` where `k` is the most significant
     *    digit of `x` (rounded), so `c' = x / (k * 10^e) ∈ [0.9, 1.1)`.
     * 2. **Double sqrt reduction**: Apply sqrt twice to bring `c'` into `[~0.975, ~1.025)`,
     *    so that `z = c' - 1` satisfies `|z| <= 0.025`.
     * 3. **Padé evaluation**: Evaluate `ln(c'') ≈ P9(z) / Q9(z)` where P9 and Q9 are
     *    degree-9 polynomials via Horner's method.
     * 4. **Reconstruction**: `ln(x) = 4 * r + ln(k) + e * ln(10)` where the factor of 4
     *    compensates for the two sqrt reductions.
     *
     * All intermediate computation is performed at 38-digit precision ([DecContext.decimal128Extended38])
     * and the final result is rounded to [ctx] precision.
     *
     * @param z the output [MutDec] to store the result in
     * @param x the input value; must be positive, finite, and non-zero
     * @param ctx the [DecContext] controlling precision and rounding of the final result
     * @return [z] containing `ln(x)`, rounded to [ctx] precision
     */
    private fun lnImplFNZ(z: MutDec, x: MutDec, ctx: DecContext): MutDec {
        val ctx38 = DecContext.decimal128Extended38()
        val xSteal = x.steal
        val tmps = ctx.tmps
        val tmp1 = tmps.mdecTrans1
        val tmp2 = tmps.mdecTrans2
        val tmp3 = tmps.mdecTrans3
        var e = stealSciExp(xSteal)
        var k = extractKMostSigDigitRounded(x, ctx38)
        if (k == 10) { k = 1; e += 1 }

        // c' = x / (k * 10^e)  →  c' ∈ [0.9, 1.1)
        val divisor = tmp1.set(k)
        divisor.qExp = e
        val cPrime = tmp2.setDiv(x, divisor, ctx38)

        // sqrt #1: c' = sqrt(c')  →  c' ∈ [~0.95, ~1.05)
        cPrime.setSqrt(cPrime, ctx38, reduceToPreferredQExp = false)
        // sqrt #2: c' = sqrt(c')  →  c' ∈ [~0.975, ~1.025)
        cPrime.setSqrt(cPrime, ctx38, reduceToPreferredQExp = false)
        // z = c' - 1  →  |z| <= 0.025
        val zArg = tmp3.setSub(cPrime, ONE, ctx38)

        // Evaluate P(z) via Horner: P(z) = z*(1 + z*(P2 + z*(P3 + ... z*P9)))
        val pAcc = tmp1.set(P9)
        pAcc.setFma(pAcc, zArg, P8, ctx38)
        pAcc.setFma(pAcc, zArg, P7, ctx38)
        pAcc.setFma(pAcc, zArg, P6, ctx38)
        pAcc.setFma(pAcc, zArg, P5, ctx38)
        pAcc.setFma(pAcc, zArg, P4, ctx38)
        pAcc.setFma(pAcc, zArg, P3, ctx38)
        pAcc.setFma(pAcc, zArg, P2, ctx38)
        pAcc.setFma(pAcc, zArg, ONE, ctx38)
        pAcc.setMul(pAcc, zArg, ctx38) // *z (factor out leading z)

        // Evaluate Q(z) via Horner: Q(z) = 1 + z*(Q1 + z*(Q2 + ... z*Q9))
        val qAcc = tmp2.set(Q9)
        qAcc.setFma(qAcc, zArg, Q8, ctx38)
        qAcc.setFma(qAcc, zArg, Q7, ctx38)
        qAcc.setFma(qAcc, zArg, Q6, ctx38)
        qAcc.setFma(qAcc, zArg, Q5, ctx38)
        qAcc.setFma(qAcc, zArg, Q4, ctx38)
        qAcc.setFma(qAcc, zArg, Q3, ctx38)
        qAcc.setFma(qAcc, zArg, Q2, ctx38)
        qAcc.setFma(qAcc, zArg, Q1, ctx38)
        qAcc.setFma(qAcc, zArg, ONE, ctx38)

        // r = P(z) / Q(z)  →  ln(c'')
        val r = tmp3.setDiv(pAcc, qAcc, ctx38)
        // ln(x) = r*4 + ln(k) + e*ln(10)
        r.setFma(r, FOUR, LN[k], ctx38) // r * 4 + ln(k)
        val eLnTen = tmps.mdecTrans2.set(e)
        r.setFma(eLnTen, LN10, r, ctx38)     // + e*ln(10)

        // round to ctx.precision and store in z
        return z.set(r, ctx)
    }

    /**
     * Extracts the most significant digit of [x], rounded to nearest.
     *
     * For example, `1.85` returns `2` and `1.23` returns `1`.
     * Returns a value in `[1, 10]`; callers should treat `10` as `1` with exponent bumped by 1.
     *
     * @param x the input value; must be positive, finite, and non-zero
     * @param ctx the [DecContext] used for rounding
     * @return the most significant digit of [x] rounded to nearest, in `[1, 10]`
     */
    internal fun extractKMostSigDigitRounded(x: MutDec, ctx: DecContext): Int {
        val tmps = ctx.tmps
        val z = tmps.mdecTrans1
        val pentad = tmps.pentad1
        val xSteal = x.steal
        val digitLen = stealDigitLen(xSteal)
        verify { !stealSignFlag(xSteal) && digitLen > 0 }
        val residue = c256SetScaleDownPow10(z, x, digitLen - 1, pentad)
        z.roundAndFinalizeFnz(false, 0, residue, DecRounding.ROUND_TIES_TO_AWAY, ctx)
        verify { z.digitLen <= 2 &&  z.dw0 > 0 && z.dw0 <= 10 }
        return z.dw0.toInt()
    }
}

/**
 * Computes the natural logarithm of [x], rounded to [ctx] precision.
 *
 * @receiver the [MutDec] to store the result in
 * @param x the input value; must be positive and finite
 * @param ctx the [DecContext] controlling precision and rounding
 * @return `this` containing `ln(x)`
 */
fun MutDec.setLn(x: MutDec, ctx: DecContext = DecContext.current()): MutDec =
    MutDecLn.mutDecLnImpl(this, x, ctx)
