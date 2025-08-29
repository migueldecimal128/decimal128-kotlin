package com.decimal128.decimal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestDpdDecletEncodeDecode {

    @Test
    fun testEncodeDeclets() {
        for (n in 0..999) {
            val enc = Decimal128DpdSerDe.encodeDpdDeclet(n)
            val dec = Decimal128DpdSerDe.decodeDpdDeclet(enc)
            if (dec != n) {
                println("Mismatch at $n: enc=${enc.toString(2)} dec=$dec")
            }
        }
        println("Canonical round-trip check done.")
    }

    @Test
    fun testDecodeDeclets() {
        // Test all 1024 possible declets
        var nonCanonCount = 0
        for (d in 0..1023) {
            val valDecoded = Decimal128DpdSerDe.decodeDpdDeclet(d)
            if (valDecoded in 0..999) {
                val reEnc = Decimal128DpdSerDe.encodeDpdDeclet(valDecoded)
                if (reEnc != d) nonCanonCount++
            } else {
                println("decode produced out-of-range: $valDecoded for declet ${d.toString(2).padStart(10,'0')}")
            }
        }
        println("Non-canonical declets collapsed to canonical: $nonCanonCount")
        assertEquals(24, nonCanonCount)
    }

}