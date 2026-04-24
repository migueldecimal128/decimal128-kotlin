package com.decimal128.decimal.benchmark

import com.decimal128.decimal.Decimal
import com.decimal128.decimal.toDecimal
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.math.BigDecimal
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
    @Param("compact", "full")
    lateinit var regime: String

    // Hoisted so BigDecimal's MathContext lookup never appears in the hot path.
    private val mc = MathContext.DECIMAL128

    private lateinit var aDec: Decimal
    private lateinit var bDec: Decimal
    private lateinit var aBig: BigDecimal
    private lateinit var bBig: BigDecimal

    @Setup(Level.Trial)
    fun setup() {
        val (aStr, bStr) = when (regime) {
            "compact" -> "123456789.12345678" to "987654321.87654321"
            "full"    -> "1234567890123456789012345678.901234" to
                    "9876543210987654321098765432.109876"
            else -> error("unknown regime: $regime")
        }
        aDec = aStr.toDecimal()
        bDec = bStr.toDecimal()
        aBig = BigDecimal(aStr)
        bBig = BigDecimal(bStr)
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
}