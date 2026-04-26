// SPDX-License-Identifier: MIT

/**
 * Financial functions built on Decimal (IEEE 754-2019 decimal128).
 *
 * Construction helpers used throughout:
 *   "0.005".toDecimal()   — String extension, exact decimal literal
 *   42.toDecimal()        — Int/Long extension
 *
 * pow(Int)     — exact integer power  (IEEE 754-2019 pown)
 * pow(Decimal) — decimal power        (IEEE 754-2019 pow, uses ln/exp internally)
 *
 * All arithmetic uses DecContext.current() — set it once on your thread for
 * consistent rounding across an entire calculation.
 */

package com.decimal128.decimal

import com.decimal128.decimal.Decimal.Companion.ONE
import com.decimal128.decimal.Decimal.Companion.ZERO


// ─────────────────────────────────────────────────────────────────────────────
// 1. Simple & Compound Interest
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Simple interest earned over [periods] periods.
 *
 *   interest = principal × rate × periods
 */
fun simpleInterest(
    principal: Decimal,
    rate: Decimal,
    periods: Decimal
): Decimal = principal * rate * periods

/**
 * Simple interest earned over int [nPeriods] periods.
 *
 *   interest = principal × rate × periods
 */
fun simpleInterest(
    principal: Decimal,
    rate: Decimal,
    numPeriods: Int
): Decimal = principal * rate * numPeriods

/**
 * Future value with compound interest.
 *
 *   FV = principal × (1 + rate)^periods
 *
 * Uses [pow(Int)][Decimal.pow] for exact integer exponentiation.
 */
fun compoundInterest(
    principal: Decimal,
    rate: Decimal,
    numPeriods: Int
): Decimal = principal * rate.compound(numPeriods)

/**
 * Effective Annual Rate — converts a nominal annual rate compounded
 * [timesPerYear] times into an equivalent annual rate.
 *
 *   EAR = (1 + nominalRate / n)^n − 1
 */
fun effectiveAnnualRate(
    nominalRate: Decimal,
    timesPerYear: Int
): Decimal = (nominalRate / timesPerYear).compound(timesPerYear) - ONE

// ─────────────────────────────────────────────────────────────────────────────
// 2. Mortgage / Annuity Payment
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Fixed-rate mortgage / annuity payment (PMT).
 *
 *   PMT = PV × r / (1 − (1+r)^−n)
 *
 * Special case r = 0 → PMT = PV / n  (interest-free loan).
 *
 * @param principal    Loan amount (present value)
 * @param periodicRate Interest rate per period (annual rate ÷ 12 for monthly)
 * @param numPayments  Total number of payments
 */
fun mortgagePayment(
    principal: Decimal,
    periodicRate: Decimal,
    numPayments: Int
): Decimal {
    if (periodicRate.isZero()) return principal / numPayments
    val factor = periodicRate.compound(numPayments)   // (1+r)^n
    return principal * periodicRate * factor / (factor - ONE)
}

/**
 * One row in an amortization schedule.
 */
data class AmortizationRow(
    val period:        Int,
    val payment:       Decimal,
    val interest:      Decimal,
    val principalPaid: Decimal,
    val balance:       Decimal
)

/**
 * Full amortization schedule — one [AmortizationRow] per payment period.
 *
 * The final balance is clamped to exactly ZERO when the residual is
 * smaller than half a cent (rounding artifact of fixed payment amount).
 */
fun amortizationSchedule(
    principal: Decimal,
    periodicRate: Decimal,
    numPayments: Int
): List<AmortizationRow> {
    val payment  = mortgagePayment(principal, periodicRate, numPayments)
    val halfCent = "0.005".toDecimal()
    val rows     = mutableListOf<AmortizationRow>()
    var balance  = principal

    for (p in 1..numPayments) {
        val interest      = balance * periodicRate
        val principalPaid = payment - interest
        balance           = balance - principalPaid
        if (p == numPayments && balance.abs() < halfCent) balance = ZERO
        rows += AmortizationRow(p, payment, interest, principalPaid, balance)
    }
    return rows
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Present Value & Future Value of Annuities
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Present Value of an ordinary annuity (payments at *end* of period).
 *
 *   PV = PMT × (1 − (1+r)^−n) / r
 */
fun presentValueAnnuity(
    payment: Decimal,
    periodicRate: Decimal,
    numPeriods: Int
): Decimal {
    if (periodicRate.isZero()) return payment * numPeriods
    val discount = ONE / periodicRate.compound(numPeriods)
    return payment * (ONE - discount) / periodicRate
}

/**
 * Future Value of an ordinary annuity (payments at *end* of period).
 *
 *   FV = PMT × ((1+r)^n − 1) / r
 */
fun futureValueAnnuity(
    payment: Decimal,
    periodicRate: Decimal,
    numPeriods: Int
): Decimal {
    if (periodicRate.isZero()) return payment * numPeriods
    val growth = periodicRate.compound(numPeriods) - ONE
    return payment * growth / periodicRate
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Net Present Value (NPV)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Net Present Value of a series of cash flows discounted at [rate].
 *
 * Convention: `cashFlows[0]` is the *immediate* cash flow at t = 0
 * (typically the negative initial investment).  `cashFlows[k]` is the
 * cash flow at end of period k.
 *
 *   NPV = Σ CF_t / (1+r)^t   for t = 0..n
 */
fun npv(
    rate: Decimal,
    cashFlows: List<Decimal>
): Decimal {
    var result = ZERO
    for ((t, cf) in cashFlows.withIndex()) {
        result = result + cf / rate.compound(t)
    }
    return result
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Internal Rate of Return (IRR)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Internal Rate of Return — the discount rate that makes NPV = 0.
 *
 * Uses Newton-Raphson iteration with the analytical derivative:
 *
 *   NPV'(r) = Σ −t × CF_t / (1+r)^(t+1)
 *
 * @param cashFlows  At least two cash flows; must change sign at least once.
 * @param guess      Initial rate estimate (default 10%).
 * @param tolerance  Convergence threshold on the Newton step (default 1e-10).
 * @param maxIter    Maximum iterations (default 200).
 * @return           IRR as a decimal rate, or `null` if it did not converge.
 */
fun irr(
    cashFlows: List<Decimal>,
    guess: Decimal     = "0.1".toDecimal(),
    tolerance: Decimal = "1e-10".toDecimal(),
    maxIter: Int       = 200
): Decimal? {
    require(cashFlows.size >= 2) { "Need at least 2 cash flows" }

    var rate = guess

    repeat(maxIter) {
        var npvVal   = ZERO
        var npvDeriv = ZERO
        var divisor  = ONE
        val onePlusR = ONE + rate

        for ((t, cf) in cashFlows.withIndex()) {
            npvVal   = npvVal + cf / divisor
            if (t > 0) {
                // derivative term: -t * CF_t / (1+r)^(t+1)
                npvDeriv = npvDeriv - cf * t / (divisor * onePlusR)
            }
            divisor = divisor * onePlusR
        }

        if (npvDeriv.isZero()) return null

        val step = npvVal / npvDeriv
        rate = rate - step

        if (step.abs() < tolerance) return rate
    }

    return null  // did not converge
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Modified IRR (MIRR)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Modified Internal Rate of Return.
 *
 * Reinvests positive flows at [reinvestRate] and discounts negative flows
 * at [financeRate], eliminating the multiple-root problem of plain IRR.
 *
 *   MIRR = (FV_positives / |PV_negatives|)^(1/(n−1)) − 1
 *
 * Uses [pow(Decimal)][Decimal.pow] for the fractional exponent.
 */
fun mirr(
    cashFlows: List<Decimal>,
    financeRate: Decimal,
    reinvestRate: Decimal
): Decimal {
    val n = cashFlows.size

    var pvNeg = ZERO
    var fvPos = ZERO

    for ((t, cf) in cashFlows.withIndex()) {
        when {
            cf < ZERO -> pvNeg = pvNeg + cf / financeRate.compound(t)
            cf > ZERO -> fvPos = fvPos + cf * reinvestRate.compound(n - 1 - t)
        }
    }

    require(!pvNeg.isZero()) { "No negative cash flows found" }

    return (fvPos / (-pvNeg)).rootn(n - 1) - ONE
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. Single Cash-Flow Helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Present Value of a single future cash flow.
 *
 *   PV = FV / (1+r)^n
 */
fun presentValue(
    futureValue: Decimal,
    rate: Decimal,
    numPeriods: Int
): Decimal = futureValue / rate.compound(numPeriods)

/**
 * Future Value of a single present cash flow.
 *
 *   FV = PV × (1+r)^n
 */
fun futureValue(
    presentValue: Decimal,
    rate: Decimal,
    numPeriods: Int
): Decimal = presentValue * rate.compound(numPeriods)

/**
 * Compound Annual Growth Rate (CAGR) between a present and future value.
 *
 *   CAGR = (FV/PV)^(1/n) − 1
 *
 * Uses [pow(Decimal)][Decimal.pow] for the fractional exponent.
 */
fun cagr(
    presentValue: Decimal,
    futureValue: Decimal,
    numPeriods: Int
): Decimal {
    require(presentValue > 0) { "presentValue must be positive" }
    require(futureValue  > 0) { "futureValue must be positive"  }
    return (futureValue / presentValue).rootn(numPeriods) - ONE
}

