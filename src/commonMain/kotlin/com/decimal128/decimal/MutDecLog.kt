package com.decimal128.decimal

import com.decimal128.decimal.InvalidOperationReason.LOG_OF_NEG_NUMBER
import kotlin.math.max

/**
 * Computes the natural logarithm of [x], rounded to [ctx] precision.
 *
 * @receiver the [MutDec] to store the result in
 * @param x the input value; must be positive and finite
 * @param ctx the [DecContext] controlling precision and rounding
 * @return `this` containing `ln(x)`
 */
fun MutDec.setLn(x: MutDec, ctx: DecContext = DecContext.current()): MutDec =
    logDispatch(this, x, isLog10 = false, ctx)

fun MutDec.setExp(x: MutDec, ctx: DecContext = DecContext.current()): MutDec =
    expDispatch(this, x, isExp10 = false, ctx)

fun MutDec.setLog10(x: MutDec, ctx: DecContext = DecContext.current()): MutDec {
    return if (x.isExactPowerOf10()) {
        this.set(stealSciExp(x.steal))
    } else {
        logDispatch(this, x, isLog10 = true, ctx)
    }
}

fun MutDec.setExp10(x: MutDec, ctx: DecContext = DecContext.current()): MutDec =
    expDispatch(this, x, isExp10 = true, ctx)

private fun logDispatch(
    z: MutDec, x: MutDec,
    isLog10: Boolean, ctx: DecContext
): MutDec {
    val xSteal = x.steal
    when (stealTyp(xSteal)) {
        STEAL_TYP_FNZ -> when {
            stealSignFlag(xSteal) ->
                return ctx.setNanSignalInvalid(z, LOG_OF_NEG_NUMBER)
            else -> return logImplFNZ(z, x, isLog10, ctx)
        }

        STEAL_TYP_ZER -> return ctx.signalDivByZero(z.setInfinite(true))
        STEAL_TYP_INF -> when {
            stealSignFlag(xSteal) ->
                return ctx.setNanSignalInvalid(z, LOG_OF_NEG_NUMBER)
            else -> return z.setInfinite(false)
        }

        else -> return z.setNanOperandFound(x, ctx)
    }
}

private fun expDispatch(
    z: MutDec, x: MutDec, isExp10: Boolean, ctx: DecContext
): MutDec {
    val xSteal = x.steal
    when (stealTyp(xSteal)) {
        STEAL_TYP_FNZ -> return if (isExp10) exp10ImplFNZ(z, x, ctx) else expImplFNZ(z, x, ctx)
        STEAL_TYP_ZER -> return z.setOne()
        STEAL_TYP_INF -> when {
            stealSignFlag(xSteal) -> return z.setZero()
            else -> return z.setInfinite()
        }
        else -> return z.setNanOperandFound(x, ctx)
    }
}

private val ctx38 = DecContext.internal38()

private val ZERO = MutDec()
private val ONE = MutDec().setOne()
private val FOUR = MutDec().set(4)
private val EIGHT = MutDec().set(8)

private fun mutDecArrayOf(vararg strs: String): Array<MutDec> =
    Array(strs.size) { i ->
        when (val str = strs[i]) {
            "0" -> ZERO
            "1" -> ONE
            "4" -> FOUR
            else -> MutDec().set(str, ctx38)
        }
    }

private val padeLnPWeights = mutDecArrayOf(
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

private val padeLogQWeights = mutDecArrayOf(
    "1", // Q0
    "4.5", // Q1
    "8.4705882352941176470588235294117647059", // Q2
    "8.6470588235294117647058823529411764706", // Q3
    "5.1882352941176470588235294117647058824", // Q4
    "1.8529411764705882352941176470588235294", // Q5
    "0.38009049773755656108597285067873303167", // Q6
    "0.040723981900452488687782805429864253394", // Q7
    "0.0018510900863842040312628547922665569724", // Q8
    "0.000020567667626491155902920608802961744138", // Q9
)

private val LN = mutDecArrayOf(
    "0",
    "0", // LN1
    "0.69314718055994530941723212145817656808", // LN2
    "1.0986122886681096913952452369225257046",  // LN3
    "1.3862943611198906188344642429163531362",  // LN4
    "1.6094379124341003746007593332261876395",  // LN5
    "1.7917594692280550008124773583807022727",  // LN6
    "1.9459101490553133051053527434431797296",  // LN7
    "2.0794415416798359282516963643745297042",  // LN8
    "2.1972245773362193827904904738450514093",  // LN9

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
internal fun logImplFNZ(
    z: MutDec, x: MutDec, isLog10: Boolean,
    ctx: DecContext
): MutDec {
    val ctx38 = DecContext.decimal128Extended38()
    val xSteal = x.steal
    val tmps = ctx38.tmps
    val tmp1 = tmps.mdecTrans1
    val tmp2 = tmps.mdecTrans2
    val tmp3 = tmps.mdecTrans3
    var eExp = stealSciExp(xSteal)
    var k = extractKMostSigDigitRounded(x, ctx38)
    if (k == 10) {
        k = 1; eExp += 1
    }

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
    val pentad = tmps.pentad
    val xSteal = x.steal
    val digitLen = stealDigitLen(xSteal)
    verify { !stealSignFlag(xSteal) && digitLen > 0 }
    val residue = c256SetScaleDownPow10(z, x, digitLen - 1, pentad)
    z.roundAndFinalizeFnz(false, 0, residue, DecRounding.ROUND_TIES_TO_AWAY, ctx)
    verify { z.digitLen <= 2 && z.dw0 > 0 && z.dw0 <= 10 }
    return z.dw0.toInt()
}

private val padeExpPWeights = mutDecArrayOf(
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

private val padeExpQWeights = Array<MutDec>(10) { i ->
    val p = padeExpPWeights[i]
    if ((i and 1) == 0) p else MutDec().setNegate(p)
}

fun expImplFNZ(z: MutDec, x: MutDec, ctx: DecContext): MutDec {
    val xSteal = x.steal
    if (stealSciExp(xSteal) >= 6) {
        return if (stealSignFlag(xSteal))
            ctx.signalInexactUnderflow(z.setZero(false, Q_TINY))
        else
            ctx.setInfinitySignalInexactOverflow(z, false)
    }
    val ctx38 = DecContext.decimal128Extended38()
    val tmps = ctx.tmps
    val tmp1 = tmps.mdecTrans1
    val tmp2 = tmps.mdecTrans2
    val tmp3 = tmps.mdecTrans3
    val pentad = tmps.pentad

    // range reduce: n = round(x / ln(10)), r = x - n * ln(10)
    tmp1.setDiv(x, LN10, ctx38)
    tmp1.setRoundToIntegralTiesToAway(tmp1, ctx38)
    val n = tmp1.toLongOrMinValue().toInt()
    if (n != 0) {
        tmp2.setFullWidth(LN10_50)
        tmp3.setOne()
        tmp3.qExp = tmp2.qExp
        c256SetMul(tmp3, tmp2, tmp1, pentad)
        if (n < 0)
            tmp1.setAdd(x, tmp3, ctx38)
        else
            tmp1.setSub(x, tmp3, ctx38)
    } else {
        tmp1.set(x, ctx38)  // r = x exactly, no reduction needed
    }

    // three halvings: r' = r / 4
    val rPrime = tmp1.setDiv(tmp1, EIGHT, ctx38)

    val expRPrime = MutDec()
    padeEval(expRPrime, rPrime, padeExpPWeights, padeExpQWeights, ctx38)
    // reconstruct: square thrice, multiply by 10^n
    expRPrime.setSquare(expRPrime, ctx38)
    expRPrime.setSquare(expRPrime, ctx38)
    z.setSquare(expRPrime, ctx38)
    // always scale coefficient to max precision
    val headroom = max(ctx.precision - z.digitLen, 0)
    if (headroom > 0)
        c256SetScaleUpPow10(z, z, headroom, pentad)
    // round to ctx precision
    // result is always INEXACT
    val flags = ctx.decFlags
    flags.clear(DecException.INEXACT)
    z.finalizeFnz(false, expRPrime.qExp + n - headroom, ctx)
    return if (headroom > 0 && !flags.isSet(DecException.INEXACT))
        ctx.signalInexact(z)
    else
        z
}

private val LOG10 = mutDecArrayOf(
    "0",                                        // LOG10[0] unused
    "0",                                        // LOG10[1] = log10(1) = 0
    "0.30102999566398119521373889472449302677", // LOG10[2]
    "0.47712125471966243729502790325511530920", // LOG10[3]
    "0.60205999132796239042747778944898605354", // LOG10[4]
    "0.69897000433601880478626110527550697323", // LOG10[5]
    "0.77815125038364363250876679797960833597", // LOG10[6]
    "0.84509804001425683071221625859263619348", // LOG10[7]
    "0.90308998699194358564121668417347908030", // LOG10[8]
    "0.95424250943932487459005580651023061840", // LOG10[9]
)

private val padeLog10PWeights = mutDecArrayOf(
    "0.43429448190325182765112891891660508229", // P1 = 1/ln(10)
    "1.7371779276130073106045156756664203292",  // P2
    "2.8463319720816063410272517872132401717",  // P3
    "2.4588731695992934359659504968072493630",  // P4
    "1.2000578404356032119948106450651484554",  // P5
    "0.32870131375422589308497208372903835640", // P6
    "0.047008798315901434091907910454160000667",// P7
    "0.0029406817698361751096610054464456614331",// P8
    "0.000050539090998679135885762898261158410735",// P9
)

private val padeExp10PWeights = mutDecArrayOf(
    "1",                                                      // P0
    "1.1512925464970228420089957273421821038",                // P1
    "0.62375271887981153065431369277576678725",               // P2
    "0.20945220803021078496789615208818637811",               // P3
    "0.048228153190505110248516490071932743443",              // P4
    "0.0079321018999350207382307581722084196291",             // P5
    "0.00093663279953333980407088694976192281395",            // P6
    "0.000077024168636241020775343936951782874690",           // P7
    "0.0000040307886932288201762295127255197250322",          // P8
    "0.00000010312482175597367540252796938795052521",         // P9
)

private val padeExp10QWeights = Array<MutDec>(10) { i ->
    val p = padeExp10PWeights[i]
    if ((i and 1) == 0) p else MutDec().setNegate(p)
}

internal fun exp10ImplFNZ(z: MutDec, x: MutDec, ctx: DecContext): MutDec {
    val ctx38 = DecContext.decimal128Extended38()
    val tmps = ctx38.tmps
    val tmp1 = tmps.mdecTrans1

    // Exact fast path: if x is an integer, use the exponent shift trick
    // e.g. 10^3 = 1 * 10^3,  10^-2 = 1 * 10^-2
    // (This avoids transcendental error accumulation for the common case.)
    val l = x.toLongOrMinValue()
    if (l != Long.MIN_VALUE) {
        return when {
            l > 6144L -> ctx.setInfinitySignalInexactOverflow(z, false)
            l < -6176L -> ctx.signalInexactUnderflow(z.setZero(false, -6176))
            else -> z.setOne().finalizeFnz(false, l.toInt(), ctx)
        }
    }

    // General case: 10^x, |x| not integer
    // range reduce: n = round(x), r = x - n, |r| <= 0.5
    tmp1.setRoundToIntegralTiesToAway(x, ctx38)
    val nLong = tmp1.toLongOrMinValue()
    when {
        nLong == Long.MIN_VALUE ->
            return if (stealSignFlag(x.steal))
                ctx.signalInexactUnderflow(z.setZero(false, -6176))
            else
                ctx.setInfinitySignalInexactOverflow(z, false)
        nLong > 6200 -> return ctx.setInfinitySignalInexactOverflow(z, false)
        nLong < -6200 -> return ctx.signalInexactUnderflow(z.setZero(false, -6176))
    }
    val n = nLong.toInt()
    val r = tmps.mdecTrans2.setSub(x, tmp1, ctx38)  // exact, no transcendental

    // three halvings: r' = r / 8
    val rPrime = tmps.mdecTrans3.setDiv(r, EIGHT, ctx38)

    // Pade [9,9] evaluation of 10^r'
    val tenPowRPrime = MutDec()
    padeEval(tenPowRPrime, rPrime, padeExp10PWeights, padeExp10QWeights, ctx38)

    // reconstruct: square three times, multiply by 10^n
    tenPowRPrime.setSquare(tenPowRPrime, ctx38)
    tenPowRPrime.setSquare(tenPowRPrime, ctx38)
    z.setSquare(tenPowRPrime, ctx38)

    // always scale coefficient to max precision
    val headroom = max(ctx.precision - z.digitLen, 0)
    if (headroom > 0)
        c256SetScaleUpPow10(z, z, headroom, ctx.tmps.pentad)
    // round to ctx precision
    // result is always INEXACT
    val flags = ctx.decFlags
    flags.clear(DecException.INEXACT)
    z.finalizeFnz(false, z.qExp + n - headroom, ctx)
    return if (headroom > 0 && !flags.isSet(DecException.INEXACT))
        ctx.signalInexact(z)
    else
        z
}

private fun padeEval(
    result: MutDec,
    zArg: MutDec,
    P: Array<MutDec>,
    Q: Array<MutDec>,
    ctx: DecContext
) {
    check(P.size > 3 && Q.size > 3)
    val tmps = ctx.tmps
    val pAcc = tmps.mdecTrans1.set(P[P.size - 1])
    for (i in P.size - 2 downTo 0)
        pAcc.setFma(pAcc, zArg, P[i], ctx)

    val qAcc = tmps.mdecTrans2.set(Q[Q.size - 1])
    for (i in Q.size - 2 downTo 0)
        qAcc.setFma(qAcc, zArg, Q[i], ctx)
    result.setDiv(pAcc, qAcc, ctx)
}