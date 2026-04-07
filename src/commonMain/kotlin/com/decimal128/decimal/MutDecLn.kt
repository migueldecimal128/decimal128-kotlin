package com.decimal128.decimal

internal object MutDecLn {

    val ctx38 = DecContext.decimal128Extended38()

    private val ZERO = MutDec()
    private val HALF = MutDec().set("0.5")
    private val ONE = MutDec().setOne()
    private val FOUR = MutDec().set(4)

    private val pLnStrings = arrayOf(
        "1",
        "4", // P2
        "6.5539215686274509803921568627450980392", // P3
        "5.6617647058823529411764705882352941176", // P4
        "2.7632352941176470588235294117647058824", // P5
        "0.75686274509803921568627450980392156863", // P6
        "0.10824175824175824175824175824175824176", // P7
        "0.0067711700064641241111829347123464770524", // P8
        "0.00011637055754702813526342938107643989997", // P9
    )

    private val pLn = Array<MutDec>(9) { i ->
        when (pLnStrings[i]) {
            "1" -> ONE
            "4" -> FOUR
            else -> MutDec().set(pLnStrings[i], ctx38)
        }
    }

    private val qLnStrings = arrayOf(
        "1", // Q0
        "4.5000000000000000000000000000000000000", // Q1
        "8.4705882352941176470588235294117647059", // Q2
        "8.6470588235294117647058823529411764706", // Q3
        "5.1882352941176470588235294117647058824", // Q4
        "1.8529411764705882352941176470588235294", // Q5
        "0.38009049773755656108597285067873303167", // Q6
        "0.040723981900452488687782805429864253394", // Q7
        "0.0018510900863842040312628547922665569724", // Q8
        "0.000020567667626491155902920608802961744138", // Q9
    )

    private val qLn = Array<MutDec>(10) { i ->
        when (qLnStrings[i]) {
            "1" -> ONE   // Q0 = 1, handled directly in Horner
            else -> MutDec().set(qLnStrings[i], ctx38)
        }
    }

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
    private val LN10_50 by lazy {
        val ln1050 = MutDec().set(LN10) // copies TYP flag and sets qExp
        c256SetFmaPow10(ln1050, ln1050, 12, 11014886287L)
        ln1050.qExp -= 12
        ln1050
    }

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

        val r = MutDec()
        padeEval(r, zArg, pLn, qLn, ctx38)
        r.setMul(r, zArg, ctx38)

        // ln(x) = r*4 + ln(k) + e*ln(10)
        r.setFma(r, FOUR, LN[k], ctx38) // r * 4 + ln(k)
        val eLnTen = tmps.mdecTrans2.set(e)
        r.setFma(eLnTen, LN10, r, ctx38)     // + e*ln(10)

        println("r:$r")
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

    // Pade [9,9] numerator coefficients for exp(x), |x| <= 0.144
    // P and Q share the same magnitudes, Q signs alternate
    private val EP2 = MutDec().set("0.11764705882352941176470588235294117647", ctx38)
    private val EP3 = MutDec().set("0.017156862745098039215686274509803921569", ctx38)
    private val EP4 = MutDec().set("0.0017156862745098039215686274509803921569", ctx38)
    private val EP5 = MutDec().set("0.00012254901960784313725490196078431372549", ctx38)
    private val EP6 = MutDec().set("0.0000062845651080945198592257415786827551533", ctx38)
    private val EP7 = MutDec().set("2.2444875386051856640091934209581268405E-7", ctx38)
    private val EP8 = MutDec().set("5.1011080422845128727481668658139246375E-9", ctx38)
    private val EP9 = MutDec().set("5.6678978247605698586090742953488051527E-11", ctx38)

    // Denominator Q coefficients -- same magnitude as P, alternating signs
    private val EQ1 = MutDec().set("-0.5", ctx38)
    private val EQ2 = EP2  // same as EP2
    private val EQ3 = MutDec().set("-0.017156862745098039215686274509803921569", ctx38)
    private val EQ4 = EP4  // same as EP4
    private val EQ5 = MutDec().set("-0.00012254901960784313725490196078431372549", ctx38)
    private val EQ6 = EP6  // same as EP6
    private val EQ7 = MutDec().set("-2.2444875386051856640091934209581268405E-7", ctx38)
    private val EQ8 = EP8  // same as EP8
    private val EQ9 = MutDec().set("-5.6678978247605698586090742953488051527E-11", ctx38)

    fun mutDecExpImpl(z: MutDec, x: MutDec, ctx: DecContext): MutDec {
        val xSteal = x.steal
        when (stealTyp(xSteal)) {
            STEAL_TYP_FNZ -> return expImplFNZ(z, x, ctx)
            STEAL_TYP_ZER -> return z.setOne() // exp(0) = 1
            STEAL_TYP_INF -> when {
                stealSignFlag(xSteal) -> return z.setZero()  // exp(-∞) = 0
                else -> return z.setInfinite()                   // exp(+∞) = +∞
            }

            else -> return z.setNanOperandFound(x, ctx)
        }
    }

    fun expImplFNZ(z: MutDec, x: MutDec, ctx: DecContext): MutDec {
        val ctx38 = DecContext.decimal128Extended38()
        val tmps = ctx.tmps
        val tmp1 = tmps.mdecTrans1
        val tmp2 = tmps.mdecTrans2
        val tmp3 = tmps.mdecTrans3
        val pentad = tmps.pentad1

        // range reduce: n = round(x / ln(10)), r = x - n * ln(10)
        tmp1.setDiv(x, LN10, ctx38)
        tmp1.setRoundToIntegralTiesToAway(tmp1, ctx38)
        val n = tmp1.dw0.toInt()
        println("n:$n tmp1:$tmp1")
        tmp2.setFullWidth(LN10_50)
        tmp3.setOne() // initialize type/sign
        tmp3.qExp = tmp2.qExp // = -50
        c256SetMul(tmp3, tmp2, tmp1, pentad)
        tmp1.setSub(x, tmp3, ctx38)
        println("r tmp1:$tmp1")

        // two halvings: r' = r / 4
        val rPrime = tmp1.setDiv(tmp1, FOUR, ctx38)
        println("rPrime:$rPrime")

        // Pade [9,9] evaluation of exp(r')
        // Evaluate P(r') via Horner
        val pAcc = tmp2.set(EP9)
        pAcc.setFma(pAcc, rPrime, EP8, ctx38)
        pAcc.setFma(pAcc, rPrime, EP7, ctx38)
        pAcc.setFma(pAcc, rPrime, EP6, ctx38)
        pAcc.setFma(pAcc, rPrime, EP5, ctx38)
        pAcc.setFma(pAcc, rPrime, EP4, ctx38)
        pAcc.setFma(pAcc, rPrime, EP3, ctx38)
        pAcc.setFma(pAcc, rPrime, EP2, ctx38)
        pAcc.setFma(pAcc, rPrime, HALF, ctx38)  // EP1 = 1/2
        pAcc.setFma(pAcc, rPrime, ONE, ctx38)   // EP0 = 1

        // Evaluate Q(r') via Horner
        val qAcc = tmp3.set(EQ9)
        qAcc.setFma(qAcc, rPrime, EQ8, ctx38)
        qAcc.setFma(qAcc, rPrime, EQ7, ctx38)
        qAcc.setFma(qAcc, rPrime, EQ6, ctx38)
        qAcc.setFma(qAcc, rPrime, EQ5, ctx38)
        qAcc.setFma(qAcc, rPrime, EQ4, ctx38)
        qAcc.setFma(qAcc, rPrime, EQ3, ctx38)
        qAcc.setFma(qAcc, rPrime, EQ2, ctx38)
        qAcc.setFma(qAcc, rPrime, EQ1, ctx38)  // EQ1 = -1/2
        qAcc.setFma(qAcc, rPrime, ONE, ctx38)  // EQ0 = 1Sonnet 4.6

        // exp(r') = P(r') / Q(r')
        val expRPrime = tmp2.setDiv(pAcc, qAcc, ctx38)
        // reconstruct: square twice, multiply by 10^n
        expRPrime.setSquare(expRPrime, ctx38)
        z.setSquare(expRPrime, ctx38)
        // round to ctx precision
        z.finalizeFnz(false, expRPrime.qExp + n, ctx)
        return z
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

fun MutDec.setExp(x: MutDec, ctx: DecContext = DecContext.current()): MutDec =
    MutDecLn.mutDecExpImpl(this, x, ctx)

private fun padeEval(
    result: MutDec,
    zArg: MutDec,
    P: Array<MutDec>,
    Q: Array<MutDec>,
    ctx: DecContext
): MutDec {
    check (P.size > 3 && Q.size > 3)
    val tmps = ctx.tmps
    val pAcc = tmps.mdecTrans1.set(P[P.size - 1])
    for (i in P.size - 2 downTo 0)
        pAcc.setFma(pAcc, zArg, P[i], ctx)

    val qAcc = tmps.mdecTrans2.set(Q[Q.size - 1])
    for (i in Q.size - 2 downTo 0)
        qAcc.setFma(qAcc, zArg, Q[i], ctx)

    return result.setDiv(pAcc, qAcc, ctx)
}