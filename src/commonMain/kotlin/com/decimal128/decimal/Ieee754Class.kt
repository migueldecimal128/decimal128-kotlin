package com.decimal128.decimal

/**
 * Enumerates the ten IEEE-754-2019 *classes* of floating-point values.
 *
 * IEEE-754-2019 (§7.5.2 “General operations”) defines a mandatory
 * classification of every floating-point datum into one of ten
 * mutually exclusive categories: NaNs, infinities, zeros, subnormals,
 * and normals, each distinguished by sign and (for NaNs) signaling
 * behavior.  These classes apply uniformly to all decimal interchange
 * formats, including decimal128.
 *
 * The classes are:
 *
 *  • **signalingNaN** – a non-finite datum encoded as a signaling NaN
 *  • **quietNaN** – a non-finite datum encoded as a quiet NaN
 *
 *  • **negativeInfinity / positiveInfinity** – signed infinities
 *
 *  • **negativeZero / positiveZero** – signed zero values
 *
 *  • **negativeSubnormal / positiveSubnormal** – finite values whose
 *    *adjusted exponent* lies below the minimum normal exponent
 *
 *  • **negativeNormal / positiveNormal** – all other finite, non-zero values
 *
 * These classes correspond exactly to the categories returned by
 * IEEE-754’s `class(x)` general operation.  No rounding or exception
 * signaling is implied; this is purely a descriptive classification
 * of the operand's encoding.
 */
enum class Ieee754Class {
    signalingNaN,
    quietNaN,
    negativeInfinity,
    negativeNormal,
    negativeSubnormal,
    negativeZero,
    positiveZero,
    positiveSubnormal,
    positiveNormal,
    positiveInfinity
}
enum class Ieee754Class {
    signalingNaN,
    quietNaN,
    negativeInfinity,
    negativeNormal,
    negativeSubnormal,
    negativeZero,
    positiveZero,
    positiveSubnormal,
    positiveNormal,
    positiveInfinity
}
