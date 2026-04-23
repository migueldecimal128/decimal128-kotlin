package com.decimal128.decimal

import com.decimal128.decimal.Decimal.Companion.ZERO
import kotlin.test.Test
import kotlin.test.assertTrue

class TestFinance {

    val verbose = true

    @Test
    fun testMain() {
        var passed = 0
        var failed = 0

        fun check(
            label: String,
            expected: String,
            actual: Decimal,
            tol: String = "1e-15"
        ) {
            if (verbose)
                println(" check label:$label expected:$expected actual:$actual tol:$tol")
            val diff = (actual - expected.toDecimal()).abs()
            val ok = diff <= tol.toDecimal()
            assertTrue(diff <= tol.toDecimal())
            if (ok) {
                println("✓  $label")
                passed++
            } else {
                println("✗  $label")
                println("     expected : $expected")
                println("     actual   : $actual")
                println("     diff     : $diff")
                failed++
            }
        }

        // ── 1. Simple Interest ───────────────────────────────────────────────────
        println("\n── Simple Interest ───────────────────────────────────────")

        // $1 000 at 5% for 3 years → $150
        check(
            "P=1000 r=0.05 t=3", "150",
            simpleInterest("1000".toDecimal(), "0.05".toDecimal(), "3".toDecimal())
        )

        // ── 2. Compound Interest ─────────────────────────────────────────────────
        println("\n── Compound Interest ─────────────────────────────────────")

        // $1 000 at 5% for 10 years → $1 628.8946…
        check(
            "P=1000 r=0.05 n=10", "1628.894626777441406250",
            compoundInterest("1000".toDecimal(), "0.05".toDecimal(), 10)
        )

        // ── 3. Effective Annual Rate ─────────────────────────────────────────────
        println("\n── Effective Annual Rate ─────────────────────────────────")

        // 12% nominal, monthly compounding → EAR ≈ 12.6825%
        check(
            "nominal=0.12 monthly", "0.12682503013196972066",
            effectiveAnnualRate("0.12".toDecimal(), 12)
        )

        // ── 4. Mortgage Payment ──────────────────────────────────────────────────
        println("\n── Mortgage Payment ──────────────────────────────────────")

        // $200 000, 6%/yr → 0.5%/mo, 360 payments → PMT ≈ $1 199.1011
        check(
            "200k 6%/yr 30yr", "1199.10105030550478918292",
            mortgagePayment("200000".toDecimal(), "0.005".toDecimal(), 360)
        )

        // Zero-rate: $12 000 / 12 = $1 000
        check(
            "zero-rate 12k 12mo", "1000",
            mortgagePayment("12000".toDecimal(), ZERO, 12),
            tol = "1e-15"
        )

        // ── 5. Amortization Schedule ─────────────────────────────────────────────
        println("\n── Amortization Schedule ─────────────────────────────────")

        val sched = amortizationSchedule("200000".toDecimal(), "0.005".toDecimal(), 360)

        // First period: interest = 200 000 × 0.005 = $1 000
        check(
            "row1 interest", "1000",
            sched[0].interest, tol = "1e-10"
        )
        // Final balance must be ≈ 0
        check(
            "final balance ≈ 0", "0",
            sched.last().balance, tol = "0.01"
        )

        // ── 6. Annuity PV / FV ───────────────────────────────────────────────────
        println("\n── Annuity PV / FV ───────────────────────────────────────")

        // PV of $500/mo, 24 months, 1%/mo → $10 621.6936...
        check(
            "PV annuity 500/mo 24mo 1%", "10621.6936288139243718",
            presentValueAnnuity("500".toDecimal(), "0.01".toDecimal(), 24)
        )

        // FV of $200/mo, 36 months, 0.5%/mo → $7 867.2210...
        check(
            "FV annuity 200/mo 36mo 0.5%", "7867.2209929367732338",
            futureValueAnnuity("200".toDecimal(), "0.005".toDecimal(), 36)
        )

        // ── 7. NPV ───────────────────────────────────────────────────────────────
        println("\n── NPV ───────────────────────────────────────────────────")

        // Invest $1 000 now, receive $400/$500/$400, r=10% → NPV ≈ $65.2644
        val flows = listOf("-1000", "400", "500", "400").map { it.toDecimal() }

        check(
            "NPV r=0.10", "77.38542449286250939143",
            npv("0.10".toDecimal(), flows)
        )

        // Same flows at r=20% → NPV ≈ −$125.7716
        check(
            "NPV r=0.20 (negative)", "-87.96296296296296296296",
            npv("0.20".toDecimal(), flows)
        )

        // ── 8. IRR ───────────────────────────────────────────────────────────────
        println("\n── IRR ───────────────────────────────────────────────────")

        val irrResult = irr(flows)
        if (irrResult != null) {
            // IRR ≈ 14.4888%
            check("IRR ~14.33%", "0.14332259275356270085", irrResult)
            // NPV at IRR must be ≈ 0
            check(
                "NPV at IRR ≈ 0", "0",
                npv(irrResult, flows), tol = "1e-8"
            )
        } else {
            println("✗  IRR did not converge"); failed++
        }

        // ── 9. MIRR ──────────────────────────────────────────────────────────────
        println("\n── MIRR ──────────────────────────────────────────────────")

        // Excel MIRR(-1000,400,500,400  finance=10%, reinvest=12%) ≈ 12.6626%
        check(
            "MIRR classic", "0.13490286191516542854",
            mirr(flows, "0.10".toDecimal(), "0.12".toDecimal())
        )

        // ── 10. PV / FV / CAGR ───────────────────────────────────────────────────
        println("\n── PV / FV / CAGR ────────────────────────────────────────")

        // PV of $1 000 in 5 yrs at 8% → $680.5832
        check(
            "PV single 1000 5yr 8%", "680.58319703375316322003",
            presentValue("1000".toDecimal(), "0.08".toDecimal(), 5)
        )

        // FV of $500 for 7 yrs at 6% → $751.8224
        check(
            "FV single 500 7yr 6%", "751.8151294956800000",
            futureValue("500".toDecimal(), "0.06".toDecimal(), 7)
        )

        // CAGR: $1 000 → $2 000 in 10 yrs → 7.1773%
        check(
            "CAGR 1000→2000 10yr", "0.07177346253629316421",
            cagr("1000".toDecimal(), "2000".toDecimal(), 10)
        )

        // ── Summary ───────────────────────────────────────────────────────────────
        println("\n══════════════════════════════════════════════════════════")
        println("  $passed passed, $failed failed")
        println("══════════════════════════════════════════════════════════\n")
    }
}