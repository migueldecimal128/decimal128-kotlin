package com.decimal128

import java.math.BigInteger
import kotlin.math.max
import kotlin.math.min
import java.lang.Math.unsignedMultiplyHigh
import com.decimal128.UlarMul.Companion.ularMul

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

        fun setZero(z:LongArray, zOff:Int, zLen:Int) {
            for (i in 0..<zLen)
                z[zOff + i] = 0L
        }

        fun set(z:LongArray, zOff:Int, zLen:Int, x:LongArray, xOff:Int, xLen:Int) {
            val minLen = min(zLen, xLen)
            var i = 0
            while (i < minLen) {
                z[zOff + i] = x[xOff + i]
                ++i
            }
            while (i < zLen)
                z[zOff + i++] = 0L
        }

        fun from(bi: BigInteger) : LongArray {
            val bitLen = bi.bitLength()
            val len = (bitLen + 63) / 64
            val a = LongArray(len)
            for (i in 0..<len)
                a[i] = bi.shiftRight(i * 64).toLong()
            return a
        }

        fun from(str: String) : LongArray {
            val bi = if (str.startsWith("0x")) BigInteger(str.substring(2), 16) else BigInteger(str)
            return from(bi)
        }

        fun from(dw: Long) = if (dw == 0L) LongArray(0) else longArrayOf(dw)

        fun from(dw1: Long, dw0: Long) = longArrayOf(dw0, dw1)

        fun from(dw2: Long, dw1: Long, dw0: Long) = longArrayOf(dw0, dw1, dw2)

        fun from(dw3: Long, dw2: Long, dw1: Long, dw0: Long) = longArrayOf(dw0, dw1, dw2, dw3)

        fun from(dwordLen:Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long) : LongArray {
            val ular = LongArray(max(dwordLen, 4))
            ular[0] = dw0
            ular[1] = dw1
            ular[2] = dw2
            ular[3] = dw3
            return ular
        }

        fun from(dwordLen:Int, src:LongArray, srcOff:Int, srcLen:Int) : LongArray {
            val ular = LongArray(max(4, max(dwordLen, srcLen)))
            for (i in 0..<srcLen)
                ular[i] = src[srcOff + i]
            return ular
        }

        fun signFlip(dw: Long) = dw xor Long.MIN_VALUE
        fun lo32(dw: Long) = dw and 0xFFFFFFFFL

        fun add(sum: LongArray, x: LongArray, y: LongArray) = add(sum, 0, sum.size, x, 0, x.size, y, 0, y.size)

        fun add(z: LongArray, zOff: Int, zLen: Int, x: LongArray, xOff: Int, xLen: Int, y: LongArray, yOff: Int, yLen: Int) {
            require(zOff >= 0 && zLen >= 0 && (zOff + zLen) <= z.size)
            require(xOff >= 0 && xLen >= 0 && (xOff + xLen) <= x.size)
            require(yOff >= 0 && yLen >= 0 && (yOff + yLen) <= y.size)
            val maxLen = max(xLen, yLen)
            require(zLen >= maxLen)
            val minLen = min(xLen, yLen)
            var carry = 0L
            var i = 0
            while (i < minLen) {
                val xT = x[xOff + i]
                val yT = y[yOff + i]
                val (carryT, zT) = sumU64(xT, yT, carry)
                z[zOff + i] = zT
                carry = carryT
                ++i
            }
            while (i < xLen) {
                val xT = x[xOff + i]
                val (carryT, zT) = sumU64(xT, carry)
                z[zOff + i] = zT
                carry = carryT
                ++i
            }
            while (i < yLen) {
                val yT = y[yOff + i]
                val (carryT, zT) = sumU64(yT, carry)
                z[zOff + i] = zT
                carry = carryT
                ++i
            }
            while (i < zLen) {
                z[zOff + i] = carry
                carry = 0L
                ++i
            }
            require(carry == 0L)
        }

        fun mul(z:LongArray, x: LongArray, y:LongArray) {
            mul(z, 0, z.size, x, 0, x.size, y, 0, y.size)
        }

        fun mul(z:LongArray, zOff:Int, zLen:Int, x:LongArray, xOff:Int, xLen:Int, y:LongArray, yOff:Int, yLen:Int) {
            if (xLen >= yLen)
                ularMul(z, zOff, zLen, x, xOff, xLen, y, yOff, yLen)
            else
                ularMul(z, zOff, zLen, y, yOff, yLen, x, xOff, xLen)
        }

        fun toBigInteger(x0:Long) : BigInteger {
            var bi = BigInteger.ZERO
            val x0Lo = x0 and 0xFFFFFFFFL
            val x0Hi = x0 ushr 32
            bi = bi or BigInteger(x0Lo.toString()).shiftLeft(0)
            bi = bi or BigInteger(x0Hi.toString()).shiftLeft(32)
            return bi
        }

        fun toBigInteger(x1:Long, x0:Long) : BigInteger {
            var bi = BigInteger.ZERO
            val x0Lo = x0 and 0xFFFFFFFFL
            val x0Hi = x0 ushr 32
            val x1Lo = x1 and 0xFFFFFFFFL
            val x1Hi = x1 ushr 32
            bi = bi or BigInteger(x0Lo.toString()).shiftLeft(0)
            bi = bi or BigInteger(x0Hi.toString()).shiftLeft(32)
            bi = bi or BigInteger(x1Lo.toString()).shiftLeft(64)
            bi = bi or BigInteger(x1Hi.toString()).shiftLeft(96)
            return bi
        }

        fun toBigInteger(x2:Long, x1:Long, x0:Long) : BigInteger {
            var bi = BigInteger.ZERO
            val x0Lo = x0 and 0xFFFFFFFFL
            val x0Hi = x0 ushr 32
            val x1Lo = x1 and 0xFFFFFFFFL
            val x1Hi = x1 ushr 32
            val x2Lo = x2 and 0xFFFFFFFFL
            val x2Hi = x2 ushr 32
            bi = bi or BigInteger(x0Lo.toString()).shiftLeft(0)
            bi = bi or BigInteger(x0Hi.toString()).shiftLeft(32)
            bi = bi or BigInteger(x1Lo.toString()).shiftLeft(64)
            bi = bi or BigInteger(x1Hi.toString()).shiftLeft(96)
            bi = bi or BigInteger(x2Lo.toString()).shiftLeft(128)
            bi = bi or BigInteger(x2Hi.toString()).shiftLeft(160)
            return bi
        }

        fun toBigInteger(x3:Long, x2:Long, x1:Long, x0:Long) : BigInteger {
            var bi = BigInteger.ZERO
            val x0Lo = x0 and 0xFFFFFFFFL
            val x0Hi = x0 ushr 32
            val x1Lo = x1 and 0xFFFFFFFFL
            val x1Hi = x1 ushr 32
            val x2Lo = x2 and 0xFFFFFFFFL
            val x2Hi = x2 ushr 32
            val x3Lo = x3 and 0xFFFFFFFFL
            val x3Hi = x3 ushr 32
            bi = bi or BigInteger(x0Lo.toString()).shiftLeft(0)
            bi = bi or BigInteger(x0Hi.toString()).shiftLeft(32)
            bi = bi or BigInteger(x1Lo.toString()).shiftLeft(64)
            bi = bi or BigInteger(x1Hi.toString()).shiftLeft(96)
            bi = bi or BigInteger(x2Lo.toString()).shiftLeft(128)
            bi = bi or BigInteger(x2Hi.toString()).shiftLeft(160)
            bi = bi or BigInteger(x3Lo.toString()).shiftLeft(192)
            bi = bi or BigInteger(x3Hi.toString()).shiftLeft(224)
            return bi
        }

        fun toBigInteger(x4:Long, x3:Long, x2:Long, x1:Long, x0:Long) : BigInteger {
            var bi = BigInteger.ZERO
            val x0Lo = x0 and 0xFFFFFFFFL
            val x0Hi = x0 ushr 32
            val x1Lo = x1 and 0xFFFFFFFFL
            val x1Hi = x1 ushr 32
            val x2Lo = x2 and 0xFFFFFFFFL
            val x2Hi = x2 ushr 32
            val x3Lo = x3 and 0xFFFFFFFFL
            val x3Hi = x3 ushr 32
            val x4Lo = x4 and 0xFFFFFFFFL
            val x4Hi = x4 ushr 32
            bi = bi or BigInteger(x0Lo.toString()).shiftLeft(0)
            bi = bi or BigInteger(x0Hi.toString()).shiftLeft(32)
            bi = bi or BigInteger(x1Lo.toString()).shiftLeft(64)
            bi = bi or BigInteger(x1Hi.toString()).shiftLeft(96)
            bi = bi or BigInteger(x2Lo.toString()).shiftLeft(128)
            bi = bi or BigInteger(x2Hi.toString()).shiftLeft(160)
            bi = bi or BigInteger(x3Lo.toString()).shiftLeft(192)
            bi = bi or BigInteger(x3Hi.toString()).shiftLeft(224)
            bi = bi or BigInteger(x4Lo.toString()).shiftLeft(256)
            bi = bi or BigInteger(x4Hi.toString()).shiftLeft(288)
            return bi
        }

    }
}