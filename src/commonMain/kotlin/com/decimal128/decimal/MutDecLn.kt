package com.decimal128.decimal

internal object MutDecLn {

    internal fun logDispatch(z: MutDec, x: MutDec,
                             isLog10: Boolean, ctx: DecContext): MutDec {
        val xSteal = x.steal
        when (stealTyp(xSteal)) {
            STEAL_TYP_FNZ -> when {
                stealSignFlag(xSteal) -> return ctx.signalInvalid(z.setNaN())
                else -> return logImplFNZ(z, x, isLog10, ctx)
            }
            STEAL_TYP_ZER -> return ctx.signalDivByZero(z.setInfinite(true))
            STEAL_TYP_INF -> when {
                stealSignFlag(xSteal) -> return ctx.setNanSignalInvalid(z, InvalidOperationReason.LOG_OF_NEG_INFINITY)
                else -> return z.setInfinite(false)
            }
            else -> return z.setNanOperandFound(x, ctx)
        }
    }

    val ctx38 = DecContext.decimal128Extended38()

    private val ZERO = MutDec()
    private val HALF = MutDec().set("0.5")
    private val ONE = MutDec().setOne()
    private val FOUR = MutDec().set(4)
    private val EIGHT = MutDec().set(8)

    private val padeLnPWeightStrings = arrayOf(
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

    private val padeLnPWeights = Array<MutDec>(9) { i ->
        val str = padeLnPWeightStrings[i]
        when (str) {
            "1" -> ONE
            "4" -> FOUR
            else -> MutDec().set(str, ctx38)
        }
    }

    private val padeLogQWeightStrings = arrayOf(
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

    private val padeLogQWeights = Array<MutDec>(10) { i ->
        val str = padeLogQWeightStrings[i]
        when (str) {
            "1" -> ONE   // Q0 = 1, handled directly in Horner
            else -> MutDec().set(str, ctx38)
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
    private fun logImplFNZ(z: MutDec, x: MutDec, isLog10: Boolean,
                          ctx: DecContext): MutDec {
        val ctx38 = DecContext.decimal128Extended38()
        val xSteal = x.steal
        val tmps = ctx.tmps
        val tmp1 = tmps.mdecTrans1
        val tmp2 = tmps.mdecTrans2
        val tmp3 = tmps.mdecTrans3
        var eExp = stealSciExp(xSteal)
        var k = extractKMostSigDigitRounded(x, ctx38)
        if (k == 10) { k = 1; eExp += 1 }

        // c' = x / (k * 10^e)  →  c' ∈ [0.9, 1.1)
        val divisor = tmp1.set(k)
        divisor.qExp = eExp
        val cPrime = tmp2.setDiv(x, divisor, ctx38)

        // sqrt #1: c' = sqrt(c')  →  c' ∈ [~0.95, ~1.05)
        cPrime.setSqrt(cPrime, ctx38, reduceToPreferredQExp = false)
        // sqrt #2: c' = sqrt(c')  →  c' ∈ [~0.975, ~1.025)
        cPrime.setSqrt(cPrime, ctx38, reduceToPreferredQExp = false)
        // z = c' - 1  →  |z| <= 0.025
        val zArg = tmp3.setSub(cPrime, ONE, ctx38)

        val r = tmps.mdecTrans1
        val pWeights: Array<MutDec> = if (isLog10) padeLog10PWeights else padeLnPWeights
        padeEval(r, zArg, pWeights, padeLogQWeights, ctx38)
        r.setMul(r, zArg, ctx38)
        val eVal = tmps.mdecTrans2.set(eExp)

        if (isLog10) {
            // log10(x) = r*4 + log10(k) + e
            r.setFma(r, FOUR, LOG10[k], ctx38)
            r.setAdd(r, eVal, ctx38)
        } else {
            // ln(x) = r*4 + ln(k) + e*ln(10)
            r.setFma(r, FOUR, LN[k], ctx38)
            r.setFma(eVal, LN10, r, ctx38)     // + e*ln(10)
        }

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

    private val padeExpPWeightStrings = arrayOf(
        "1",
        "0.5",
        "0.11764705882352941176470588235294117647",
        "0.017156862745098039215686274509803921569",
        "0.0017156862745098039215686274509803921569",
        "0.00012254901960784313725490196078431372549",
        "0.0000062845651080945198592257415786827551533",
        "2.2444875386051856640091934209581268405E-7",
        "5.1011080422845128727481668658139246375E-9",
        "5.6678978247605698586090742953488051527E-11",
    )
    private val padeExpPWeights = Array<MutDec>(10) { i ->
        val str = padeExpPWeightStrings[i]
        when (str) {
            "1" -> ONE   // Q0 = 1, handled directly in Horner
            else -> MutDec().set(str, ctx38)
        }
    }

    private val padeExpQWeights = Array<MutDec>(10) { i ->
        val p = padeExpPWeights[i]
        if ((i and 1) == 0) p else MutDec().setNegate(p)
        }

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

        println("\nexp($x)")

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

        // three halvings: r' = r / 4
        val rPrime = tmp1.setDiv(tmp1, EIGHT, ctx38)
        println("rPrime:$rPrime")

        val expRPrime = MutDec()
        padeEval(expRPrime, rPrime, padeExpPWeights, padeExpQWeights, ctx38)
        // reconstruct: square thrice, multiply by 10^n
        expRPrime.setSquare(expRPrime, ctx38)
        expRPrime.setSquare(expRPrime, ctx38)
        z.setSquare(expRPrime, ctx38)
        // round to ctx precision
        z.finalizeFnz(false, expRPrime.qExp + n, ctx)
        return z
    }

    private val LOG10 = arrayOf(
        ZERO,                                                                          // LOG10[0] unused
        ZERO,                                                                          // LOG10[1] = log10(1) = 0
        MutDec().set("0.30102999566398119521373889472449302677", ctx38), // LOG10[2]
        MutDec().set("0.47712125471966243729502790325511530920", ctx38), // LOG10[3]
        MutDec().set("0.60205999132796239042747778944898605354", ctx38), // LOG10[4]
        MutDec().set("0.69897000433601880478626110527550697323", ctx38), // LOG10[5]
        MutDec().set("0.77815125038364363250876679797960833597", ctx38), // LOG10[6]
        MutDec().set("0.84509804001425683071221625859263619348", ctx38), // LOG10[7]
        MutDec().set("0.90308998699194358564121668417347908030", ctx38), // LOG10[8]
        MutDec().set("0.95424250943932487459005580651023061840", ctx38), // LOG10[9]
    )

    private val padeLog10PWeights = arrayOf(
        MutDec().set("0.43429448190325182765112891891660508229", ctx38), // P0 = log10(e)
        MutDec().set("0.43429448190325182765112891891660508229", ctx38), // P1 = log10(e)
        MutDec().set("1.7371779276130073106045156756664203292", ctx38),  // P2
        MutDec().set("2.8463319720816063410272517872132401717", ctx38),  // P3
        MutDec().set("2.4588731695992934359659504968072493630", ctx38),  // P4
        MutDec().set("1.2000578404356032119948106450651484554", ctx38),  // P5
        MutDec().set("0.32870131375422589308497208372903835640", ctx38), // P6
        MutDec().set("0.047008798315901434091907910454160000667", ctx38),// P7
        MutDec().set("0.0029406817698361751096610054464456614331", ctx38),// P8
        MutDec().set("0.000050539090998679135885762898261158410735", ctx38),// P9
    )

    fun mutDecTenPowImpl(z: MutDec, x: MutDec, ctx: DecContext): MutDec {
        val xSteal = x.steal
        when (stealTyp(xSteal)) {
            STEAL_TYP_FNZ -> return tenPowImplFNZ(z, x, ctx)
            STEAL_TYP_ZER -> return z.setOne()          // 10^0 = 1
            STEAL_TYP_INF -> when {
                stealSignFlag(xSteal) -> return z.setZero()     // 10^(-∞) = 0
                else -> return z.setInfinite()                  // 10^(+∞) = +∞
            }
            else -> return z.setNanOperandFound(x, ctx)
        }
    }

    private fun tenPowImplFNZ(z: MutDec, x: MutDec, ctx: DecContext): MutDec {
        val ctx38 = DecContext.decimal128Extended38()
        val tmps = ctx.tmps
        val tmp1 = tmps.mdecTrans1

        // Exact fast path: if x is an integer, use the exponent shift trick
        // e.g. 10^3 = 1 * 10^3,  10^-2 = 1 * 10^-2
        // (This avoids transcendental error accumulation for the common case.)
        val xSteal = x.steal
        val sciExp = stealSciExp(xSteal)
        if (sciExp >= 0) {
            TODO()
            /*
            // x is a (possibly large) integer: 10^x = set(1) with exponent = x.toLong()
            // Only feasible if x fits in a reasonable exponent range
            val xAsLong = x.toLongExact()   // returns null if not exactly representable or too large
            if (xAsLong != null && xAsLong in DecContext.MIN_EXPONENT..DecContext.MAX_EXPONENT) {
                z.setOne()
                z.qExp = xAsLong.toInt()
                return z.finalizeFnz(false, z.qExp, ctx)
            }

             */
        }

        // General case: 10^x = exp(x * ln(10))
        // Multiply at high precision using LN10_50 for the range-reduction step,
        // but for the product fed into exp we just need ~38 digits.
        tmp1.setMul(x, LN10, ctx38)        // tmp1 = x * ln(10)
        return expImplFNZ(z, tmp1, ctx)    // z = exp(x * ln(10)), rounds to ctx inside
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
    MutDecLn.logDispatch(this, x, isLog10 = false, ctx)

fun MutDec.setExp(x: MutDec, ctx: DecContext = DecContext.current()): MutDec =
    MutDecLn.mutDecExpImpl(this, x, ctx)

fun MutDec.setLog10(x: MutDec, ctx: DecContext = DecContext.current()): MutDec =
    MutDecLn.logDispatch(this, x, isLog10 = true, ctx)

fun MutDec.setTenPow(x: MutDec, ctx: DecContext = DecContext.current()): MutDec =
    MutDecLn.mutDecTenPowImpl(this, x, ctx)

private fun padeEval(
    result: MutDec,
    zArg: MutDec,
    P: Array<MutDec>,
    Q: Array<MutDec>,
    ctx: DecContext
) {
    check (P.size > 3 && Q.size > 3)
    val tmps = ctx.tmps
    val pAcc = tmps.mdecTrans1.set(P[P.size - 1])
    for (i in P.size - 2 downTo 0)
        pAcc.setFma(pAcc, zArg, P[i], ctx)

    val qAcc = tmps.mdecTrans2.set(Q[Q.size - 1])
    for (i in Q.size - 2 downTo 0)
        qAcc.setFma(qAcc, zArg, Q[i], ctx)
    result.setDiv(pAcc, qAcc, ctx)
    println(" --> padeEval pAcc:$pAcc, qAcc:$qAcc")
    println("  -> padeEval result:$result")
}