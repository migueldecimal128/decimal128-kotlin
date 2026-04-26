@file:Suppress("unused")
package com.decimal128.decimal.benchmark

import com.decimal128.decimal.Decimal
import com.decimal128.decimal.toDecimal
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(2)
open class ArithmeticBenchmark {

    // "compact" → 18 digits or fewer, hits BigDecimal's intCompact fast path
    // "full"    → 30+ digits, forces BigDecimal into BigInteger-backed storage
    @Param(
        "compact_aligned", "full_aligned",
        "compact_shift2", "full_shift2",
        "compact_shift10", "full_shift10",
        "compact_shift30", "full_shift30",
    )
    lateinit var regime: String

    // Hoisted so BigDecimal's MathContext lookup never appears in the hot path.
    private val mc = MathContext.DECIMAL128

    private lateinit var aDec: Decimal
    private lateinit var bDec: Decimal
    private lateinit var aBig: BigDecimal
    private lateinit var bBig: BigDecimal

    private lateinit var parseStr: String
    private lateinit var unscaledValueBig: BigInteger
    private var scaleBig: Int = 0

    @Setup(Level.Trial)
    fun setup() {
        val (aStr, bStr) = when (regime) {
            "compact_aligned" ->
                "-123456789012345678" to "9876543210123456"
            "full_aligned" ->
                "1234567890123456789012345678901234e-2" to "987654321098765432109876543210e-2"
            "compact_shift2" ->
                "123456789012345678e-4" to "222222222e-2"
            "full_shift2" ->
                "-602214076000000000000000e-2" to "99999999999999999999999999e-4"
            "compact_shift10" ->
                "1234560987654321e-10" to "565656565656e0"
            "full_shift10" ->
                "123456789012345678900987654321e-10" to "24242424243535353535e0"
            "compact_shift30" ->
                "1234567890e30" to "9876543210"
            "full_shift30" ->
                "-123456789012345678901234567890e-10" to "99999999999999999999e20"

            else -> error("unknown regime: $regime")
        }
        aDec = aStr.toDecimal()
        bDec = bStr.toDecimal()
        aBig = BigDecimal(aStr)
        bBig = BigDecimal(bStr)

        parseStr = aStr

        unscaledValueBig = aBig.unscaledValue()
        scaleBig = aBig.scale()
    }

    // ── Addition ──────────────────────────────────────────────────────────────

    @Benchmark
    fun decimalAdd(bh: Blackhole) {
        bh.consume(aDec + bDec)
    }

    @Benchmark
    fun bigDecimalAdd(bh: Blackhole) {
        bh.consume(aBig.add(bBig, mc))
    }

    // ── Subtraction ───────────────────────────────────────────────────────────

    @Benchmark
    fun decimalSubtract(bh: Blackhole) {
        bh.consume(aDec - bDec)
    }

    @Benchmark
    fun bigDecimalSubtract(bh: Blackhole) {
        bh.consume(aBig.subtract(bBig, mc))
    }

    // ── Multiplication ────────────────────────────────────────────────────────

    @Benchmark
    fun decimalMultiply(bh: Blackhole) {
        bh.consume(aDec * bDec)
    }

    @Benchmark
    fun bigDecimalMultiply(bh: Blackhole) {
        bh.consume(aBig.multiply(bBig, mc))
    }

    // ── Division ──────────────────────────────────────────────────────────────

    @Benchmark
    fun decimalDivide(bh: Blackhole) {
        bh.consume(aDec / bDec)
    }

    @Benchmark
    fun bigDecimalDivide(bh: Blackhole) {
        bh.consume(aBig.divide(bBig, mc))
    }

    // ── parse ──────────────────────────────────────────────────────────────

    @Benchmark
    fun decimalParse(bh: Blackhole) {
        bh.consume(parseStr.toDecimal())
    }

    @Benchmark
    fun bigDecimalParse(bh: Blackhole) {
        bh.consume(BigDecimal(parseStr))
    }

    // ── toString ──────────────────────────────────────────────────────────────

    @Benchmark
    fun decimalToString(bh: Blackhole) {
        bh.consume(aDec.toString())
    }

    @Benchmark
    fun bigDecimalToString(bh: Blackhole) {
        bh.consume(BigDecimal(unscaledValueBig, scaleBig).toString())
    }
}