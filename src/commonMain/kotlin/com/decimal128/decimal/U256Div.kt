package com.decimal128.decimal

import com.decimal128.decimal.U256Compare.u256UnscaledCompare
import com.decimal128.decimal.Residue.Companion.EXACT
import com.decimal128.decimal.Residue.Companion.GT_HALF
import com.decimal128.decimal.Residue.Companion.LT_HALF

@Suppress("NOTHING_TO_INLINE")
private inline fun getShiftedLeft(v: IntArray, i: Int, shift: Int): Long {
    return ((v[i] shl shift) or ((v[i - (-i ushr 31)] ushr -shift) and (-shift shr 31))).toLong() and MASK32
    //if (i == 0) {
    //    v[0] shl s
    //} else {
    //    (v[i] shl s) or if (s != 0) (v[i - 1] ushr (32 - s)) else 0
}

object U256Div {

    fun u256DivX64(z: U256, x: U256, y0: Long): Residue {
        val rem = u256DivModX64(z, x, y0)
        val residue = Residue.residueFromRemainderDivisor(rem, y0)
        return residue
    }

    fun u256DivModX64(z: U256?, x: U256, y0: Long): Long {
        if ((y0 shr 1) == 0L) {
            if (y0 == 0L)
                throw RuntimeException("div by zero")
            z?.u256Set(x)
            return 0
        }
        val x0 = x.dw0
        if (x.bitLen <= 64) {
            val quot = unsignedDiv(x0, y0)
            val rem = unsignedMod(x0, y0)
            z?.u256Set64(quot)
            return rem
        }
        if ((y0 and (y0 - 1)) == 0L) {
            // y0 is an exact power of 2 ... just shift right.
            // 0 and 1 cases handled above, so if we are here then ntz >= 1
            val ntz = y0.countTrailingZeroBits()
            val mask = (1L shl ntz) - 1L
            val rem = x0 and mask
            z?.u256SetShiftRight(x, ntz)
            return rem
        }
        //TODO at this point I know that x.bitLen >= y.bitLen and x > y
        // if (bitLenDelta < some-small-number) then I should use repeated subtraction
        if ((y0 ushr 32) == 0L) {
            return DivDirect.divModX32(z, x, y0)
        }
        return DivKnuth.knuthDivModX64(z, x, y0)
    }

    fun u256Div(z: U256, x: U256, y: U256) = u256DivMod(z, null, x, y)

    fun u256Mod(z: U256, x: U256, y: U256) = u256DivMod(null, z, x, y)

    fun u256DivMod(quot: U256?, rem: U256?, x: U256, y: U256): Residue {
        if (y.bitLen <= 64) {
            val y0 = y.dw0
            val r0 = u256DivModX64(quot, x, y0)
            if (rem != null) {
                rem.u256Set64(r0)
                return EXACT
            }
            val residue = Residue.residueFromRemainderDivisor(r0, y0)
            return residue
        }
        val bitLenDelta = x.bitLen - y.bitLen
        if (bitLenDelta < 0) {
            rem?.u256Set(x)
            val residue = when {
                rem != null || x.bitLen == 0 -> EXACT
                bitLenDelta <= -2 -> LT_HALF
                else -> Residue.residueFromRemainderDivisor(x, y)
            }
            quot?.u256SetZero()
            return residue
        }
        if (bitLenDelta == 0) {
            val cmp = u256UnscaledCompare(x, y)
            if (cmp < 0) {
                rem?.u256Set(x)
                val residue = if (rem != null) EXACT else GT_HALF
                quot?.u256SetZero()
                return residue
            } else if (cmp > 0) {
                val t = when {
                    rem != null && rem !== y -> rem
                    quot != null && quot !== y -> quot
                    else -> Int256()
                }
                t.u256SetSub(x, y)
                val residue = Residue.residueFromRemainderDivisor(t, y)
                rem?.u256Set(t)
                quot?.u256SetOne()
                return residue
            } else {
                rem?.u256SetZero()
                quot?.u256SetOne()
                return EXACT
            }
        }
        check(bitLenDelta >= 0)
        //TODO at this point I know that x.bitLen >= y.bitLen and x > y
        // if (bitLenDelta < some-small-number) then I should use repeated subtraction
        return DivKnuth.knuthDivideWrapper(quot, rem, x, y)
    }

}
