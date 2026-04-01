package com.decimal128.decimal

internal object MutDecLn {

    val ctx38 = DecContext.decimal128Extended38()

    // Numerator P(z) coefficients
    // p[0] = 0, p[1] = 1 -- handled directly in Horner, not stored
    private val P2 = MutDec().set("4", ctx38)
    private val P3 = MutDec().set("6.55392156862745098039215686274509803921569", ctx38)
    private val P4 = MutDec().set("5.66176470588235294117647058823529411764706", ctx38)
    private val P5 = MutDec().set("2.76323529411764705882352941176470588235294", ctx38)
    private val P6 = MutDec().set("0.756862745098039215686274509803921568627451", ctx38)
    private val P7 = MutDec().set("0.108241758241758241758241758241758241758242", ctx38)
    private val P8 = MutDec().set("0.00677117000646412411118293471234647705235941", ctx38)
    private val P9 = MutDec().set("0.000116370557547028135263429381076439899969312", ctx38)

    // Denominator Q(z) coefficients
    // q[0] = 1 -- handled directly in Horner, not stored
    private val Q1 = MutDec().set("4.50000000000000000000000000000000000000000", ctx38)
    private val Q2 = MutDec().set("8.47058823529411764705882352941176470588235", ctx38)
    private val Q3 = MutDec().set("8.64705882352941176470588235294117647058824", ctx38)
    private val Q4 = MutDec().set("5.18823529411764705882352941176470588235294", ctx38)
    private val Q5 = MutDec().set("1.85294117647058823529411764705882352941176", ctx38)
    private val Q6 = MutDec().set("0.380090497737556561085972850678733031674208", ctx38)
    private val Q7 = MutDec().set("0.0407239819004524886877828054298642533936652", ctx38)
    private val Q8 = MutDec().set("0.00185109008638420403126285479226655697243933", ctx38)
    private val Q9 = MutDec().set("0.0000205676676264911559029206088029617441382147", ctx38)

    // ln(k) for k = 1..9
    private val LN1 = MutDec().set("0", ctx38)
    private val LN2 = MutDec().set("0.6931471805599453094172321214581765680755", ctx38)
    private val LN3 = MutDec().set("1.09861228866810969139524523692252570464749", ctx38)
    private val LN4 = MutDec().set("1.386294361119890618834464242916353136151", ctx38)
    private val LN5 = MutDec().set("1.6094379124341003746007593332261876395256", ctx38)
    private val LN6 = MutDec().set("1.79175946922805500081247735838070227272299", ctx38)
    private val LN7 = MutDec().set("1.94591014905531330510535274344317972963708", ctx38)
    private val LN8 = MutDec().set("2.0794415416798359282516963643745297042265", ctx38)
    private val LN9 = MutDec().set("2.19722457733621938279049047384505140929498", ctx38)

    // ln(10)
    private val LN10 = MutDec().set("2.3025850929940456840179914546843642076011", ctx38)

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
        var e = stealSciExp(xSteal)
        var k = extractKMostSigDigitRounded(x, ctx38)
        if (k == 10) { k = 1; e += 1 }

        // c' = x / (k * 10^e)  →  c' ∈ [0.9, 1.1)
        //val divisor = ctx.tmps.mdecTrans2.setIntWithSciExp(k, e)
        //val cPrime  = ctx.tmps.mdecTrans3.setDiv(x, divisor, ctx38)
        return MutDec()
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

        ln(k) for k=1..9:
          LN1  = $LN1
          LN2  = $LN2
          LN3  = $LN3
          LN4  = $LN4
          LN5  = $LN5
          LN6  = $LN6
          LN7  = $LN7
          LN8  = $LN8
          LN9  = $LN9

          LN10 = $LN10
    """.trimIndent()
        )
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
