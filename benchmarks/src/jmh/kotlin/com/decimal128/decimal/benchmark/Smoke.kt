package com.decimal128.decimal.benchmark

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole

@State(Scope.Benchmark)
open class Smoke {
    @Benchmark
    fun noop(bh: Blackhole) {
        bh.consume(1 + 1)
    }
}