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

    private fun lnImplFNZ(z: MutDec, x: MutDec, ctx: DecContext): MutDec {
        val ctx38 = DecContext.decimal128Extended38()
        val xSteal = x.steal
        val tmps = ctx.tmps
        var e = stealSciExp(xSteal)
        var k = extractKMostSigDigitRounded(x, ctx38)
        if (k == 10) { k = 1; e += 1 }

        // c' = x / (k * 10^e)  →  c' ∈ [0.9, 1.1)
        val divisor = tmps.mdecTrans1.set(k)
        divisor.qExp = e
        val cPrime = tmps.mdecTrans2.setDiv(x, divisor, ctx38)
        println("cPrime:$cPrime")

        // sqrt #1: c' = sqrt(c')  →  c' ∈ [~0.95, ~1.05)
        cPrime.setSqrt(cPrime, ctx38)
        println("after sqrt #1 cPrime:$cPrime")
        // sqrt #2: c' = sqrt(c')  →  c' ∈ [~0.975, ~1.025)
        cPrime.setSqrt(cPrime, ctx38)
        println("after sqrt #2 cPrime:$cPrime")

        println("cPrime = $cPrime  qExp = ${cPrime.qExp}")
        println("ONE = $ONE  qExp = ${ONE.qExp}")

        // z = c' - 1  →  |z| <= 0.025
        val zArg = tmps.mdecTrans3.setSub(cPrime, ONE, ctx38)
        println("zArg:$zArg")

        // Evaluate P(z) via Horner: P(z) = z*(1 + z*(P2 + z*(P3 + ... z*P9)))
        val pAcc = tmps.mdecTrans1
        val fmaPAcc = MutDec()
        pAcc.set(P9)
        fmaPAcc.set(P9)
        pAcc.setMul(pAcc, zArg, ctx38)
        pAcc.setAdd(pAcc, P8, ctx38)
        fmaPAcc.setFma(fmaPAcc, zArg, P8, ctx38)
        println("after P8 pAcc:$pAcc fmaPAcc:$fmaPAcc")
        pAcc.setMul(pAcc, zArg, ctx38)
        pAcc.setAdd(pAcc, P7, ctx38)
        fmaPAcc.setFma(fmaPAcc, zArg, P7, ctx38)
        println("after P7 pAcc:$pAcc fmaPAcc:$fmaPAcc")
        pAcc.setMul(pAcc, zArg, ctx38)
        pAcc.setAdd(pAcc, P6, ctx38)
        fmaPAcc.setFma(fmaPAcc, zArg, P6, ctx38)
        println("after P6 pAcc:$pAcc fmaPAcc:$fmaPAcc")
        pAcc.setMul(pAcc, zArg, ctx38)
        pAcc.setAdd(pAcc, P5, ctx38)
        fmaPAcc.setFma(fmaPAcc, zArg, P5, ctx38)
        println("after P5 pAcc:$pAcc fmaPAcc:$fmaPAcc")
        pAcc.setMul(pAcc, zArg, ctx38)
        pAcc.setAdd(pAcc, P4, ctx38)
        fmaPAcc.setFma(fmaPAcc, zArg, P4, ctx38)
        println("after P4 pAcc:$pAcc fmaPAcc:$fmaPAcc")
        pAcc.setMul(pAcc, zArg, ctx38)
        pAcc.setAdd(pAcc, P3, ctx38)
        fmaPAcc.setFma(fmaPAcc, zArg, P3, ctx38)
        println("after P3 pAcc:$pAcc fmaPAcc:$fmaPAcc")
        pAcc.setMul(pAcc, zArg, ctx38)
        pAcc.setAdd(pAcc, P2, ctx38)
        println("after P2:$pAcc")
        pAcc.setMul(pAcc, zArg, ctx38)
        pAcc.setAdd(pAcc, ONE, ctx38)   // +1
        println("after ONE:$pAcc")
        pAcc.setMul(pAcc, zArg, ctx38) // *z (factor out leading z)

        // Evaluate Q(z) via Horner: Q(z) = 1 + z*(Q1 + z*(Q2 + ... z*Q9))
        val qAcc = tmps.mdecTrans2
        val fmaQAcc = MutDec()
        qAcc.set(Q9)
        fmaQAcc.set(Q9)
        qAcc.setMul(qAcc, zArg, ctx38)
        qAcc.setAdd(qAcc, Q8, ctx38)
        fmaQAcc.setFma(fmaQAcc, zArg, Q8, ctx38)
        println("after Q8 qAcc:$qAcc fmaQAcc:$fmaQAcc")
        qAcc.setMul(qAcc, zArg, ctx38)
        qAcc.setAdd(qAcc, Q7, ctx38)
        fmaQAcc.setFma(fmaQAcc, zArg, Q7, ctx38)
        println("after Q7 qAcc:$qAcc fmaQAcc:$fmaQAcc")
        qAcc.setMul(qAcc, zArg, ctx38)
        qAcc.setAdd(qAcc, Q6, ctx38)
        fmaQAcc.setFma(fmaQAcc, zArg, Q6, ctx38)
        println("after Q6 qAcc:$qAcc fmaQAcc:$fmaQAcc")
        qAcc.setMul(qAcc, zArg, ctx38)
        qAcc.setAdd(qAcc, Q5, ctx38)
        fmaQAcc.setFma(fmaQAcc, zArg, Q5, ctx38)
        println("after Q5 qAcc:$qAcc fmaQAcc:$fmaQAcc")
        qAcc.setMul(qAcc, zArg, ctx38)
        qAcc.setAdd(qAcc, Q4, ctx38)
        fmaQAcc.setFma(fmaQAcc, zArg, Q4, ctx38)
        println("after Q4 qAcc:$qAcc fmaQAcc:$fmaQAcc")
        qAcc.setMul(qAcc, zArg, ctx38)
        qAcc.setAdd(qAcc, Q3, ctx38)
        fmaQAcc.setFma(fmaQAcc, zArg, Q3, ctx38)
        println("after Q3 qAcc:$qAcc fmaQAcc:$fmaQAcc")
        qAcc.setMul(qAcc, zArg, ctx38)
        qAcc.setAdd(qAcc, Q2, ctx38)
        fmaQAcc.setFma(fmaQAcc, zArg, Q2, ctx38)
        println("after Q2 qAcc:$qAcc fmaQAcc:$fmaQAcc")
        qAcc.setMul(qAcc, zArg, ctx38)
        qAcc.setAdd(qAcc, Q1, ctx38)
        fmaQAcc.setFma(fmaQAcc, zArg, Q1, ctx38)
        println("after Q1 qAcc:$qAcc fmaQAcc:$fmaQAcc")
        qAcc.setMul(qAcc, zArg, ctx38)
        qAcc.setAdd(qAcc, ONE, ctx38)  // +1
        println("after ONE qAcc:$qAcc")

        // r = P(z) / Q(z)  →  ln(c'')
        val r = tmps.mdecTrans1.setDiv(pAcc, qAcc, ctx38)
        println("pAcc/qAcc r:$r")

        // ln(x) = r*4 + ln(k) + e*ln(10)
        r.setMul(r, FOUR, ctx38)              // r * 4
        println("r * 4:$r")
        r.setAdd(r, LN[k], ctx38)            // + ln(k)
        println("+ LN[$k]: $r")
        val eLnTen = tmps.mdecTrans2.set(e)
        println("eLnTen: $eLnTen")
        r.setFma(eLnTen, LN10, r, ctx38)     // + e*ln(10)
        println("r + e*ln(10) :$r")

        // round to 34 digits and store in z
        return z.set(r, ctx)
    }

    private fun printLnConstants() {
        println(
            """
        Pade [9,9] numerator P(z):
          P0  = nada
          P1  = nada
          P2  = $P2
          P3  = $P3
          P4  = $P4
          P5  = $P5
          P6  = $P6
          P7  = $P7
          P8  = $P8
          P9  = $P9

        Pade [9,9] denominator Q(z):
          Q0  = nada
          Q1  = $Q1
          Q2  = $Q2
          Q3  = $Q3
          Q4  = $Q4
          Q5  = $Q5
          Q6  = $Q6
          Q7  = $Q7
          Q8  = $Q8
          Q9  = $Q9

    """.trimIndent()
        )
        for (k in 1..9)
            println("ln($k) = ${LN[k]}")

        println("ln(10) = $LN10")
    }

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

fun MutDec.setLn(x: MutDec, ctx: DecContext = DecContext.current()): MutDec =
    MutDecLn.mutDecLnImpl(this, x, ctx)
