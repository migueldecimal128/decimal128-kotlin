@file:Suppress("NOTHING_TO_INLINE")
package com.decimal128.hugeint

import kotlin.math.absoluteValue

private inline fun U32(n: Int) = n.toLong() and 0xFFFF_FFFFL

class MutHugeInt(var sign: Boolean, var magia1: IntArray, var len1: Int, var magia2: IntArray) {
    constructor() : this (false, IntArray(4), 0, Magia.ZERO)

    fun setZero() : MutHugeInt {
        sign = false
        len1 = 0
        return this
    }

    fun set(n: Int) : MutHugeInt {
        sign = n < 0
        len1 = (n or -n) shr 31
        magia1[0] = n.absoluteValue
        return this

    }

    fun set(w: UInt) : MutHugeInt {
        sign = false
        len1 = if (w == 0u) 0 else 1
        magia1[0] = w.toInt()
        return this
    }

    fun set(l: Long) : MutHugeInt {
        if (l == l.toInt().toLong())
            return set(l.toInt())
        sign = l < 0L
        val abs = l.absoluteValue
        len1 = 2
        magia1[0] = l.toInt()
        magia1[1] = (l ushr 32).toInt()
        return this
    }

    fun set(dw: ULong) : MutHugeInt {
        if (dw == dw.toUInt().toULong())
            return set(dw.toUInt())
        sign = false
        len1 = 2
        magia1[0] = dw.toInt()
        magia1[1] = (dw shr 32).toInt()
        return this
    }

    fun set(hi: HugeInt) : MutHugeInt = set(Magia.nonZeroLimbLen(hi.magia), hi.magia)

    fun set(mhi: MutHugeInt) : MutHugeInt = set(mhi.len1, mhi.magia1)

    private fun set(otherLen: Int, otherMagia: IntArray) : MutHugeInt {
        if (magia1.size < otherLen) {
            if (magia2.size < otherLen) {
                magia2 = if (magia1.size > magia2.size) magia1 else magia2
                magia1 = Magia.newWithLimbLenRoundedUp(otherLen)
            } else {
                val t = magia2
                magia2 = magia1
                magia1 = t
            }
        }
        len1 = otherLen
        System.arraycopy(otherMagia, 0, magia1, 0, otherLen)
        return this
    }

    operator fun plusAssign(n: Int) = mutateAddImpl(n < 0, n.absoluteValue.toUInt())
    operator fun plusAssign(w: UInt) = mutateAddImpl(false, w)
    operator fun plusAssign(l: Long) = mutateAddImpl(l < 0, l.absoluteValue.toULong())
    operator fun plusAssign(dw: ULong) = mutateAddImpl(false, dw)

    private fun mutateAddImpl(otherSign: Boolean, w: UInt) {
        TODO()
    }

    private fun mutateAddImpl(otherSign: Boolean, dw: ULong) {
        TODO()
    }

}
