package com.decimal128.decimal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestDpdDecletEncodeDecode {

    val verbose = false

    @Test
    fun testEncodeDeclets() {
        if (verbose) {
            println()
            println("ENCODE binary -> declet")
        }
        for (n in 0L..999L) {
            val enc = encodeDpdDeclet(n)
            val dec = decodeDpdDeclet(enc)
            if (verbose)
                println("binary:$n 0x${n.toString(16)}-> declet:$enc 0x${enc.toString(16)}")
            if (dec != n) {
                println("Mismatch at $n: enc=${enc.toString(2)} dec=$dec")
            }
        }
        if (verbose)
            println("Canonical round-trip check done.")
    }

    @Test
    fun testDecodeDeclets() {
        if (verbose) {
            println()
            println("DECODE declet -> binary")
        }
        // Test all 1024 possible declets
        var nonCanonCount = 0
        for (d in 0L..1023L) {
            val valDecoded = decodeDpdDeclet(d)
            if (verbose)
                println("declet:$d 0x${d.toString(16)}-> binary:$valDecoded 0x${valDecoded.toString(16)}")
            if (valDecoded in 0L..999L) {
                val reEnc = encodeDpdDeclet(valDecoded)
                if (reEnc != d) nonCanonCount++
            } else {
                println("decode produced out-of-range: $valDecoded for declet ${d.toString(2).padStart(10,'0')}")
            }
        }
        if (verbose)
            println("Non-canonical declets collapsed to canonical: $nonCanonCount")
        assertEquals(24, nonCanonCount)
    }

}