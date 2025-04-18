package com.decimal128

import java.math.BigInteger
import kotlin.math.max
import kotlin.math.min

class Ular {
    companion object {

        fun toString(ular:LongArray) = toString(ular, 0, ular.size)

        fun toString(ular:LongArray, off:Int, len:Int) :String {
            return toBigInteger(ular, off, len).toString()
        }

        fun toHexString(ular:LongArray) = toString(ular, 0, ular.size)

        fun toHexString(ular:LongArray, off:Int, len: Int) : String {
            return "0x" + toBigInteger(ular, off, len).toString(16)
        }

        fun toBigInteger(ular: LongArray) = toBigInteger(ular, 0, ular.size)

        fun toBigInteger(ular: LongArray, off: Int, len: Int) : BigInteger {
            require(off >= 0 && len >= 0 && (off + len) <= ular.size)
            var bi = BigInteger.ZERO
            for (i in 0..<len) {
                val l = ular[off + i]
                val lo = l and 0xFFFFFFFFL
                val hi = l ushr 32
                bi = bi or BigInteger(lo.toString()).shiftLeft(64 * i)
                bi = bi or BigInteger(hi.toString()).shiftLeft(64 * i + 32)
            }
            return bi
        }

        fun from(bi: BigInteger) : LongArray {
            val bitLen = bi.bitLength()
            if (bitLen == 0)
                return longArrayOf(0)
            val len = (bitLen + 63) / 64
            val a = LongArray(len)
            for (i in 0..len)
                a[i] = bi.shiftRight(i * 64).toLong()
            return a
        }

        fun from(str: String) : LongArray {
            val bi = if (str.startsWith("0x")) BigInteger(str.substring(2), 16) else BigInteger(str)
            return from(bi)
        }

        fun from(dw: Long) = longArrayOf(dw)

        fun from(dw1: Long, dw0: Long) = longArrayOf(dw0, dw1)

        fun from(dw2: Long, dw1: Long, dw0: Long) = longArrayOf(dw0, dw1, dw2)

        fun from(dw3: Long, dw2: Long, dw1: Long, dw0: Long) = longArrayOf(dw0, dw1, dw2, dw3)

        fun signFlip(dw: Long) = dw xor Long.MIN_VALUE
        fun lo32(dw: Long) = dw and 0xFFFFFFFFL

        fun add(sum: LongArray, x: LongArray, y: LongArray) = add(sum, 0, sum.size, x, 0, x.size, y, 0, y.size)

        fun add(sum: LongArray, sumOff: Int, sumLen: Int, x: LongArray, xOff: Int, xLen: Int, y: LongArray, yOff: Int, yLen: Int) {
            require(sumOff >= 0 && sumLen >= 0 && (sumOff + sumLen) <= sum.size)
            require(xOff >= 0 && xLen >= 0 && (xOff + xLen) <= x.size)
            require(yOff >= 0 && yLen >= 0 && (yOff + yLen) <= y.size)
            val maxLen = max(xLen, yLen)
            require(sumLen >= maxLen)
            val minLen = min(xLen, yLen)
            var i = 0
            var carry = 0L
            while (i < minLen) {
                val xT = x[xOff + i]
                val yT = y[yOff + i]
                val lo = (xT and 0xFFFFFFFFL) + (yT and 0xFFFFFFFFL) + carry
                val hi = (xT ushr 32) + (yT ushr 32) + (lo ushr 32)
                carry = (hi ushr 32)
                val sT = (hi shl 32) or lo32(lo)
                sum[sumOff + i++] = sT
            }
            /*
            while (i < xLen)
                sum[sumOff+]
*/

        }

        fun mul(prod: LongArray, prodOff: Int, prodLen: Int, x: LongArray, xOff: Int, xLen: Int, y: LongArray, yOff: Int, yLen: Int) {

        }


    }
}