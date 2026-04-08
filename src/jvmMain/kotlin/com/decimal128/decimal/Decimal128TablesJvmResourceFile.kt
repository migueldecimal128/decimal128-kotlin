package com.decimal128.decimal

import java.io.DataInputStream

private val resourceTablesPair: Pair<LongArray, ByteArray> = loadResourceTables()

@JvmField
internal actual val DWORD_TABLES: LongArray = resourceTablesPair.first

@JvmField
internal actual val POW10: LongArray = DWORD_TABLES

@JvmField
internal actual val BYTE_TABLES: ByteArray = resourceTablesPair.second

fun loadResourceTables(): Pair<LongArray, ByteArray> {

    val stream = object {}.javaClass.getResourceAsStream(RESOURCE_TABLE_PATHNAME)
        ?: error("Resource not found: $RESOURCE_TABLE_PATHNAME")

    val dis = DataInputStream(stream.buffered())
    dis.use {
        // read and verify header
        val version = dis.readInt()
        check(version == EXPECTED_TABLE_VERSION)
        { "decimal128_tables.bin version mismatch: expected 0x${EXPECTED_TABLE_VERSION.toString(16)}, got 0x${version.toString(16)}" }

        val dwordTablesSize = dis.readInt()
        check(dwordTablesSize == EXPECTED_DWORD_TABLES_COUNT)
        { "decimal128_tables.bin dword table size mismatch: expected $EXPECTED_DWORD_TABLES_COUNT, got $dwordTablesSize" }

        val byteTablesSize = dis.readInt()
        check(byteTablesSize == EXPECTED_BYTE_TABLES_COUNT)
        { "decimal128_tables.bin byte table size mismatch: expected $EXPECTED_BYTE_TABLES_COUNT, got $byteTablesSize" }

        val headerDwordFnv1a = dis.readInt()
        check(headerDwordFnv1a == EXPECTED_DWORD_TABLES_FNV1A)
        { "decimal128_tables.bin HEADER headerDwordFnv1a mismatch" }

        val headerByteFnv1a = dis.readInt()
        check(headerByteFnv1a == EXPECTED_BYTE_TABLES_FNV1A)
        { "decimal128_tables.bin HEADER headerByteFnv1a mismatch" }

        val dwordTables = LongArray(DWORD_TABLES_SIZE_POW_2)
        val byteTables = ByteArray(BYTE_TABLES_SIZE_POW_2)
        // read dword table
        for (i in 0..<dwordTablesSize)
            dwordTables[i] = dis.readLong()

        // read byte table
        for (i in 0..<byteTablesSize)
            byteTables[i] = dis.readByte()

        // verify checksums
        val actualDwordFnv1a = fnv1aHash(dwordTables)
        if (actualDwordFnv1a != EXPECTED_DWORD_TABLES_FNV1A)
            throw RuntimeException("decimal128_tables.bin ACTUAL actualDwordFnv1a mismatch")

        val actualByteFnv1a = fnv1aHash(byteTables)
        if (actualByteFnv1a != EXPECTED_BYTE_TABLES_FNV1A)
            throw RuntimeException("decimal128_tables.bin ACTUAL headerByteFnv1a mismatch")

        return dwordTables to byteTables
    }
}
