package com.decimal128.finance

import com.decimal128.decimal.Decimal
import com.decimal128.decimal.sum
import com.decimal128.decimal.toDecimal
import kotlin.test.Test

class TestMortgageAmortization {
    val verbose = true

    @Test
    fun testMortgage() {
        val principal = "500_000.00".toDecimal()
        val annualRate = "0.055".toDecimal()
        val durationYears = 30

        val schedule = amortizationToString(principal, annualRate, durationYears)
        if (verbose)
            println(schedule)
    }

    fun amortizationToString(
        principal: Decimal,
        annualRate: Decimal,
        durationYears: Int
    ): String {
        val monthlyRate   = annualRate / 12
        val durationMonths = durationYears * 12
        val amortization  = amortizationSchedule(principal, monthlyRate, durationMonths)

        val totalPayment   = amortization.map { it.payment }.sum()
        val totalInterest  = amortization.map { it.interest }.sum()
        val totalPrincipal = amortization.map { it.principalPaid }.sum()

        return buildString {
            appendLine("Mortgage Summary")
            appendLine("================")
            appendLine("Principal        : ${principal.withScale(2)}")
            appendLine("Annual Rate      : ${(annualRate * "100".toDecimal()).withScale(3)}%")
            appendLine("Monthly Rate     : ${(monthlyRate * "100".toDecimal()).withScale(6)}%")
            appendLine("Duration         : $durationYears years ($durationMonths payments)")
            appendLine("Monthly Payment  : ${amortization[0].payment.withScale(2)}")
            appendLine("Total Payment    : ${totalPayment.withScale(2)}")
            appendLine("Total Interest   : ${totalInterest.withScale(2)}")
            appendLine("Total Principal  : ${totalPrincipal.withScale(2)}")
            appendLine()
            appendLine("Period | Payment       | Interest      | Principal     | Balance")
            appendLine("-------|---------------|---------------|---------------|---------------")
            for (row in amortization) {
                appendLine("${row.period.toString().padStart(6)} | " +
                        "${row.payment.withScale(2)}".padStart(13) + " | " +
                        "${row.interest.withScale(2)}".padStart(13) + " | " +
                        "${row.principalPaid.withScale(2)}".padStart(13) + " | " +
                        "${row.balance.withScale(2)}".padStart(13))
            }
            appendLine("-------|---------------|---------------|---------------|---------------")
            appendLine("TOTAL  | " +
                    "${totalPayment.withScale(2)}".padStart(13) + " | " +
                    "${totalInterest.withScale(2)}".padStart(13) + " | " +
                    "${totalPrincipal.withScale(2)}".padStart(13))
        }
    }
}