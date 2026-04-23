# decimal128 for Kotlin Multiplatform

A pure-Kotlin implementation of **IEEE 754-2019 decimal128** floating-point arithmetic. It runs on Kotlin JVM, Native, and JavaScript with no external dependencies.

`Decimal` is designed to be a high-performance cross-platform alternative to Java `BigDecimal`. Instance memory footprint is much smaller (32 bytes, including object header on most JVMs) and heap allocation of intermediate temp values is explicitly kept to a minimum.

Unlike binary floating-point (`Double`, `Float`), `Decimal` is exact for values expressible as
`coefficient × 10^exponent`, with 34 digits of precision and controlled rounding, making it the
right choice for financial calculations and any domain where exact decimal rounding behavior matters.

---

## Table of Contents

- [Why Decimal128?](#why-decimal128)
- [Decimal vs Double](#decimal-vs-double)
- [Decimal vs Java BigDecimal](#decimal-vs-java-bigdecimal)
- [Value Space](#value-space)
- [Getting Started](#getting-started)
    - [Construction](#construction)
    - [Predefined Constants](#predefined-constants)
- [Arithmetic](#arithmetic)
- [Comparison](#comparison)
- [Rounding](#rounding)
- [Classification and Predicates](#classification-and-predicates)
- [Quantum and Exponent](#quantum-and-exponent)
- [Elementary Functions](#elementary-functions)
- [Conversion to Integer Types](#conversion-to-integer-types)
- [Interop: BID and DPD Encoding](#interop-bid-and-dpd-encoding)
- [DecContext and Rounding Modes](#deccontext-and-rounding-modes)
- [Financial Functions](#financial-functions)
- [Compliance and Testing](#compliance-and-testing)
- [Thread Safety](#thread-safety)
- [Bibliography](#bibliography)
- [License](#license)

---

## Why Decimal128?

Binary floating-point cannot represent most decimal fractions exactly. The SQL standard correctly distinguishes binary floating-point **approximate numeric values** from **exact numeric values**. The classic example:

```kotlin
println(0.1 + 0.2)          // 0.30000000000000004  (Double)
println("0.1".toDecimal() + "0.2".toDecimal())  // 0.3  (Decimal)
```

`Decimal128` stores exact values with an integer coefficient of up to 34 decimal digits, coupled with a base-10 exponent that aligns the radix point — so `4.3210` is represented exactly as `43210e-4` (i.e. `43210 × 10^−4`).

---

## Decimal vs Double

`Double` is the right choice for scientific and engineering work — it is hardware-accelerated
and its 64-bit binary representation provide adequate range and precision for 
most calculations. Nevertheless, it is the wrong choice when dealing
with financial numbers where exact decimal values matter and where
accounting standards dictate proper rounding with controlled rounding behavior. 

**Representation** — `Double` is a IEEE 754 `binary64` value that consumes 64 bits, 8 bytes,
with a 53-bit normalized significand and an 11 bit exponent. 
`Decimal` is a IEEE 754-2019 `decimal128` value with a 113-bit integer coefficient and
a larger exponent range ... more bits => greater precision and broader range. 

**Precision and Range** — `Double` provides approximately 15 significant decimal digits 
of precision up to approximately `10^308`. 
`Decimal` provides exactly 34 decimal digits of precision up to exactly `10^6144`. 

**Exactness** — Fractional bits to the right of the radix point in `Double`/`binary64`
represent negative powers of 2: `1/2 1/4 1/8 1/16` `0.5 0.25 0.125 0.0625`. 
Most decimal values with fractional components like `0.1`, `99.44`, or `0.05`
have no exact binary representation, so rounding errors accumulate silently
across calculations. 
`Decimal` represents these values exactly.

**Rounding control** — Arithmetic operations on `Double` round using the default
IEEE 754 `roundTiesToEven` with no application-level control in most programming languages. 
`Decimal` gives full control over rounding mode via `DecContext`,
which is essential for financial calculations where rounding behavior must comply
with regulatory or contractual requirements: 

> 1. Tax computation must be carried to the third decimal place, and
> 2. The tax must be rounded to a whole cent using a method that rounds up to the next
>   cent whenever the third decimal place is greater than four.

**Special values** — Both `Double` and `Decimal` support infinity, NaN, and signed zero. 
`Decimal` also supports `sNaN` signaling NaN values. 

**Flags** — Exception flags (e.g. `divByZero`, `overflow`, `underflow`) are generally
not accessible from high-level languages, including Kotlin and Java. 
The ThreadLocal `DecContext` object provides direct access to the exception flags
register and supports usder-defined trap handlers, 
in accordance with IEEE 754-2019 decimal128. 

---

## Decimal vs Java BigDecimal

Both implement decimal floating-point arithmetic but serve different goals.

**Standard conformance** — `Decimal` strictly follows IEEE 754-2019, including infinity, NaN,
signed zero, preferred exponents, and all exception flags. 
`BigDecimal` predates IEEE754-2008/2019 and supports none of these — operations
that would produce infinity or NaN throw `ArithmeticException` instead. 
The same person, Mike Cowlishaw, was the prime mover behind both `BigDecimal` and the
addition of decimal floating point to the IEEE754-2008/2019 standard. 

**Performance** — `BigDecimal` allocates at least one variable-length `BigInteger`
for every operation, relying heavily on heap allocation and garbage collection,
while polluting the CPU-cache.
`Decimal` is cache-friendly and allocation-light. 
Objects consume a fixed 32 bytes, including object header.
Reusable mutable temporaries minimize heap allocation during
operations and stay hot in the CPU-cache. 

**Transcendental and financial functions** — `Decimal` includes
`log10`, `exp10`, `ln`, `exp`,
`pow(n: Int)`, `pow(y: Decimal)`, `compound`, `squareRoot` and `rootn`, 
calculated with 38-digit intermediate precision rounded
to a final 34-digit precision. It also includes a suite of basic financial functions
covering compound interest, mortgage payments, amortization, NPV, IRR, and
present/future value.
`BigDecimal` has none of these.

**Rounding** — Both support multiple rounding modes. 
`Decimal` uses a ThreadLocal `DecContext`, allowing clean infix operator 
expressions without the need to explicitly pass the context
parameter to every expression. 

**Cross-platform** — `Decimal` targets Kotlin Multiplatform and runs identically
on JVM, Native, and JavaScript. `BigDecimal` is JVM-only.

`BigDecimal` is a pragmatic, widely-adopted, battle-tested implementation of
arbitrary-precision finite decimal values. `Decimal` is a complete IEEE 754-2019
decimal128 implementation with 34 decimal digits of precision that prioritizes
correctness, performance, and cross-platform consistency.

---

## Value Space

Every finite `Decimal` represents a value of the form:

```
(−1)^sign × coefficient × 10^qExp
```

| Field | Range |
|---|---|
| **sign** | 0 (positive) or 1 (negative) |
| **coefficient** | integer with up to **34 decimal digits** |
| **qExp** (quantum exponent) | **−6176** to **+6111** |

In addition to finite non-zero values, `Decimal` represents:

- **+0 and −0** — signed zeros; numerically equal but distinct encodings
- **±Infinity** — overflow sentinel
- **Quiet NaN (qNaN)** — propagating not-a-number, with an optional diagnostic payload
- **Signaling NaN (sNaN)** — trapping not-a-number, with an optional diagnostic payload


---

## Getting Started

### Construction

```kotlin
"3.14159".toDecimal()   // from String
42.toDecimal()          // from Int
42L.toDecimal()         // from Long
```

**String parsing** accepts:
- An optional leading sign (`+` or `-`)
- A decimal coefficient with optional `_` underscore digit separators
- An optional exponent (`E` or `e`) within the decimal128 range
- `"Infinity"`, `"Inf"`, `"+Infinity"`, `"-Infinity"` (and their `Inf` variants)
- `"NaN"` with an optional numeric payload

Coefficients with more than 34 significant digits are rounded to 34 digits using the current `DecContext`.

### Predefined Decimal Constants

| Constant | Value |
|---|---|
| `ZERO` | +0 |
| `ONE` | +1 |
| `TWO` | +2 |
| `TEN` | +10 |
| `INFINITY` | +∞ |
| `NEG_INFINITY` | −∞ |
| `NaN` | quiet NaN |

---

## Arithmetic

Standard Kotlin operators are available and use the current `DecContext` for rounding:

```kotlin
val a = "1.5".toDecimal()
val b = "2.3".toDecimal()

a + b          // addition
a - b          // subtraction
a * b          // multiplication
a * 3          // multiply by Int
a / b          // division
a / 2          // divide by Int
a % b          // remainder (truncated toward zero)
-a             // negation
```

Additional arithmetic operations:

```kotlin
a.square()                       // a²
a.squareRoot()                   // √a
a.fma(multiplier, addend)        // fused multiply-add: (a × multiplier) + addend
a.remainderTruncate(b)           // same as a % b
a.remainderNear(b)               // IEEE 754-2019 near-remainder
a.abs()                          // absolute value
a.negate()                       // sign flip
a.copySign(signDonor)            // magnitude of a with sign from signDonor
a.reciprocal()                   // 1 / a
```

### Elementary Functions

```kotlin
a.pow(n: Int)       // a^n  (integer power, IEEE 754-2019 pown)
a.pow(x: Decimal)   // a^x  (decimal power, IEEE 754-2019 pow)
a.compound(n: Int)  // (1 + a)^n  (compound interest)
a.rootn(n: Int)     // a^(1/n)  (nth root)
a.ln()              // natural logarithm
a.exp()             // e^a
a.log10()           // base-10 logarithm
a.exp10()           // 10^a
```

---

## Comparison

### Default: Java-style numeric ordering

`Decimal` implements `Comparable<Decimal>`. The default `compareTo` / `equals` and the `<`, `>`,
`<=`, `>=` operators use **Java-style numeric semantics**:

- Cohort members (`1.0` vs `1.00`) compare as **equal**
- For signed zeros and non-finite values, the same rules as `java.lang.Double`
  apply: **−0 < +0**, all NaNs compare equal and greater than every non-NaN
- No IEEE-754 signaling occurs
- Provided for alignment with `Double` and `BigDecimal` behavior

```kotlin
"1.0".toDecimal() == "1.0000".toDecimal()   // true
"-0".toDecimal()  <  "0".toDecimal()       // true
```

### Infix operators

```kotlin
a EQ b    // numeric equality
a NE b    // numeric inequality
```

### IEEE 754-2019 totalOrder

```kotlin
a.compareTotalOrderTo(b)      // −1, 0, +1 using IEEE totalOrder (§5.10)
a.isTotalOrder(b)             // true if totalOrder(a, b)
a.compareTotalOrderMagTo(b)   // totalOrder on magnitudes (ignoring sign)
a.isTotalOrderMag(b)          // true if totalOrderMag(a, b)
```

### IEEE 754-2019 quiet comparisons (§5.6.1)

Do **not** signal on quiet NaN operands:

```kotlin
a.compareQuietEqual(b)
a.compareQuietNotEqual(b)
a.compareQuietLess(b)
a.compareQuietLessEqual(b)
a.compareQuietGreater(b)
a.compareQuietGreaterEqual(b)
a.compareQuietUnordered(b)
a.compareQuietOrdered(b)
a.compareQuietLessUnordered(b)
a.compareQuietGreaterUnordered(b)
a.compareQuietNotLess(b)
a.compareQuietNotGreater(b)
a.compareQuiet(b)              // returns Compare754Result enum
```

### IEEE 754-2019 signaling comparisons (§5.6.1)

**Signal** `INVALID_OPERATION` if either operand is NaN:

```kotlin
a.compareSignalingEqual(b)
a.compareSignalingNotEqual(b)
a.compareSignalingLess(b)
a.compareSignalingLessEqual(b)
a.compareSignalingGreater(b)
a.compareSignalingGreaterEqual(b)
a.compareSignalingNotLess(b)
a.compareSignalingNotGreater(b)
a.compareSignalingLessUnordered(b)
a.compareSignalingGreaterUnordered(b)
```

---

## Rounding

All rounding methods implement IEEE 754-2019 §5.3.1. The available rounding modes are:

```kotlin
DecRounding.ROUND_TIES_TO_EVEN      // nearest, ties → even (banker's rounding)
DecRounding.ROUND_TIES_TO_AWAY      // nearest, ties → away from zero
DecRounding.ROUND_TOWARD_ZERO       // truncation
DecRounding.ROUND_TOWARD_POSITIVE   // ceiling
DecRounding.ROUND_TOWARD_NEGATIVE   // floor
```

### Round to integral value

```kotlin
a.roundToIntegralTiesToEven()        // nearest, ties → even (banker's rounding)
a.roundToIntegralTiesToAway()        // nearest, ties → away from zero
a.roundToIntegralTowardZero()        // truncation
a.roundToIntegralTowardPositive()    // ceiling
a.roundToIntegralTowardNegative()    // floor
a.roundToIntegral()                  // uses DecContext.current() rounding mode
```

Each method also has a `…SignalInexact()` variant that raises `DecException.INEXACT`
when the result differs from the input.

### Scale and quantize

```kotlin
a.withScale(2)            // rescale to 2 decimal places (e.g. "1.23")
a.quantize(reference)     // rescale to same quantum as reference (IEEE 754-2019 §5.3.2)
a.scaleB(n)               // a × 10^n  (IEEE 754-2019 §5.3.3 scaleB)
a.stripTrailingZeros()    // remove trailing fractional zeros
a.nextUp()                // smallest value > a  (IEEE 754-2019 §5.3.1)
a.nextDown()              // largest value < a   (IEEE 754-2019 §5.3.1)
```

---

## Classification and Predicates

```kotlin
a.ieeeClass()         // Ieee754Class enum: one of 10 IEEE-754-2019 classes
a.isFinite()          // normal, subnormal, or zero
a.isFiniteNonZero()   // finite and non-zero
a.isNormal()          // normal (not zero, subnormal, infinite, or NaN)
a.isSubnormal()       // finite, non-zero, below normal range (eExp < −6143)
a.isZero()            // ±0
a.isInfinite()        // ±∞
a.isNaN()             // quiet or signaling NaN
a.isSignaling()       // signaling NaN
a.isNegative()        // sign bit set (applies to zeros and NaNs too)
a.isSignMinus()       // alias for isNegative() — IEEE 754-2019 §5.7.2
a.isCanonical()       // canonical encoding per IEEE 754-2019 §5.7.2
a.isExactIntegral()   // finite and exactly an integer
a.isOddIntegral()     // finite, exactly an odd integer
a.isExactPowerOfTen() // finite and exactly a power of 10
```

---

## Quantum and Exponent

```kotlin
a.quantum()          // 10^qExp as a Decimal (unit in the last place)
a.quantumInt()       // qExp as an Int
a.eExponent()        // adjusted (scientific) exponent: qExp + digitLen − 1
a.precision()        // number of significant decimal digits in the coefficient
a.logB()             // eExp as a Decimal  (IEEE 754-2019 §5.3.3 logB)
a.isSameQuantum(b)   // true if a and b share the same encoded exponent
```

---

## Conversion to Integer Types

All conversion functions implement IEEE 754-2019 §5.8 and signal `INVALID_OPERATION`
(returning `MIN_VALUE`) on overflow, NaN, or infinity.

### To `Long`

```kotlin
a.toLongOrMinValue()            // exact only, no rounding, no signaling on failure
a.toLongTiesToEven()            // round half-to-even
a.toLongTiesToAway()            // round half-away-from-zero
a.toLongTowardZero()            // truncate
a.toLongTowardPositive()        // ceiling
a.toLongTowardNegative()        // floor
```

Each also has a `…SignalInexact()` variant.

### To `Int`

```kotlin
a.toIntTiesToEven()
a.toIntTiesToAway()
a.toIntTowardZero()
a.toIntTowardPositive()
a.toIntTowardNegative()
```

Each also has a `…SignalInexact()` variant.

---

## Interop: BID and DPD Encoding

`Decimal` supports both **Binary Integer Decimal (BID)** and **Densely Packed Decimal (DPD)**
interchange formats, as defined in IEEE 754-2019 §3.5, for reading serialized data from a
database or binary encoded packet.

### Decoding

```kotlin
// From two 64-bit longs
Decimal.decodeBid128(hi: Long, lo: Long)
Decimal.decodeDpd128(hi: Long, lo: Long)

// From a LongArray
Decimal.decodeBid128(longs, offset = 0, isLittleEndian = false)
Decimal.decodeDpd128(longs, offset = 0, isLittleEndian = false)

// From a ByteArray (16 bytes)
Decimal.decodeBid128(bytes, offset = 0, isLittleEndian = false)
Decimal.decodeDpd128(bytes, offset = 0, isLittleEndian = false)
```

### Encoding

```kotlin
a.encodeBid128(longs, offset = 0, isLittleEndian = false)
a.encodeDpd128(longs, offset = 0, isLittleEndian = false)
a.encodeBid128(bytes, offset = 0, isLittleEndian = false)
a.encodeDpd128(bytes, offset = 0, isLittleEndian = false)
```

---

## DecContext and Rounding Modes

For production use, prefer context-aware arithmetic so rounding and overflow behavior is
explicit. Use `ctx.eval { … }` to run a block under a specific `DecContext`:

```kotlin
val ctx = DecContext.decimal128Kotlin()   // default: round-half-to-even, throws on invalid input

ctx.eval {
    val result = a + b      // uses ctx for rounding
}
```

The rounding mode can be overridden with `.with()`:

```kotlin
val ctx = DecContext.decimal128IEEE().with(DecRounding.ROUND_TOWARD_ZERO)
```

Available rounding modes (`DecRounding`):

| Mode | Description |
|---|---|
| `ROUND_TIES_TO_EVEN` | Round half to even (banker's rounding) — **default** |
| `ROUND_TIES_TO_AWAY` | Round half away from zero |
| `ROUND_TOWARD_ZERO` | Truncation |
| `ROUND_TOWARD_POSITIVE` | Ceiling |
| `ROUND_TOWARD_NEGATIVE` | Floor |

### Invalid input behavior

| Context | Behavior on invalid string |
|---|---|
| `DecContext.decimal128Kotlin()` | Throws `IllegalArgumentException` (Kotlin-idiomatic) |
| `DecContext.decimal128IEEE()` | Signals `INVALID_OPERATION`, returns `NaN` (IEEE 754-2019) |

---

## Compliance and Testing

`Decimal` is fully compliant with IEEE 754-2019 and the IBM/Cowlishaw General Decimal Arithmetic
Specification (GDAS).

The following test suites pass on all supported platforms (JVM, Native, and JavaScript):

- **IBM/Cowlishaw DecTest** — the full QA suite for decNumber, including proper rounding, flag
  signaling, and DPD-encoded values
- **IBM FPTest** — test vectors for decimal128
- **Intel libbid bid_128** — test vectors from the Intel Decimal Floating Point library

All tests are included in the source distribution on GitHub.

---



`Decimal` values are immutable. Instances may be freely shared across threads without synchronization.

`DecContext` is not thread-safe and must be kept thread-local. Coroutines require special care
if you are making any modifications to the thread-local `DecContext`, including rounding, trap
handling, and flag inspection.

---

## Extension Functions

```kotlin
"3.14".toDecimal()      // String → Decimal
42.toDecimal()          // Int    → Decimal
42L.toDecimal()         // Long   → Decimal

listOf(a, b, c).sum()   // Iterable<Decimal>.sum() — returns ZERO for empty collections
```

---

## Financial Functions

The `com.decimal.finance` package provides common financial functions out-of-the-box:

**Interest**

```kotlin
simpleInterest(principal, rate, periods)       // principal × rate × periods
simpleInterest(principal, rate, numPeriods)    // overload with Int periods
compoundInterest(principal, rate, numPeriods)  // principal × (1 + rate)^n
effectiveAnnualRate(nominalRate, timesPerYear) // EAR: (1 + r/n)^n − 1
```

**Mortgage and Annuities**

```kotlin
mortgagePayment(principal, periodicRate, numPayments)  // fixed-rate PMT
amortizationSchedule(principal, periodicRate, numPayments) // List<AmortizationRow>
presentValueAnnuity(payment, periodicRate, numPeriods)  // PV of ordinary annuity
futureValueAnnuity(payment, periodicRate, numPeriods)   // FV of ordinary annuity
```

**Cash Flow Analysis**

```kotlin
npv(rate, cashFlows)                                    // Net Present Value
irr(cashFlows, guess, tolerance, maxIter)               // Internal Rate of Return
mirr(cashFlows, financeRate, reinvestRate)              // Modified IRR
```

**Single Cash-Flow Helpers**

```kotlin
presentValue(futureValue, rate, numPeriods)             // FV / (1+r)^n
futureValue(presentValue, rate, numPeriods)             // PV × (1+r)^n
cagr(presentValue, futureValue, numPeriods)             // Compound Annual Growth Rate
```

---

## Bibliography

**Mike Cowlishaw — speleotrove.com/decimal**
Mike Cowlishaw carried the banner for decimal floating-point for many years as an IBM Fellow.
His work includes the decNumber reference implementation written in C and the decTest suite of
test vectors.
https://speleotrove.com/decimal/

**Intel Decimal Floating-Point Math Library**
A C implementation of IEEE 754-2019 decimal floating-point arithmetic.
https://www.intel.com/content/www/us/en/developer/articles/tool/intel-decimal-floating-point-math-library.html

**What Every Computer Scientist Should Know About Floating-Point Arithmetic** — David Goldberg
The classic paper documenting the behavioral quirks of binary floating-point.
https://docs.oracle.com/cd/E19957-01/806-3568/ncg_goldberg.html

**SQL:1999 Standard (ISO/IEC 9075-2:1999)**
The SQL database standard defines `DECIMAL` as an exact numeric type with a defined precision and scale.
https://courses.cms.caltech.edu/cs123/sql99std/ansi-iso-9075-2-1999.pdf

---

## License

MIT — see [LICENSE](LICENSE).