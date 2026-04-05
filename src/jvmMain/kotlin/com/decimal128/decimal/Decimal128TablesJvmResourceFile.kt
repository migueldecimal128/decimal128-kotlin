package com.decimal128.decimal

import java.io.DataInputStream

@JvmField
internal actual val DWORD_TABLES: LongArray = LongArray(DWORD_TABLES_SIZE_POW_2)

@JvmField
internal actual val POW10: LongArray = DWORD_TABLES

@JvmField
internal actual val BYTE_TABLES: ByteArray = ByteArray(BYTE_TABLES_SIZE_POW_2)


internal actual fun loadDecimal128ConstantTables() {
    loadResourceTable()
}

private var resourceLoadAttempted = false
private var resourceLoadSuccessful = false

fun loadResourceTable() {
    if (resourceLoadAttempted)
        return
    resourceLoadAttempted = true
    val stream = object {}.javaClass.getResourceAsStream(RESOURCE_TABLE_PATHNAME)
    if (stream == null) {
        println("resource not found:$RESOURCE_TABLE_PATHNAME")
        return
    }

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

        // read dword table
        for (i in 0..<dwordTablesSize)
            DWORD_TABLES[i] = dis.readLong()

        // read byte table
        for (i in 0..<byteTablesSize)
            BYTE_TABLES[i] = dis.readByte()

        // verify checksums
        val actualDwordFnv1a = fnv1aHash(DWORD_TABLES)
        if (actualDwordFnv1a != EXPECTED_DWORD_TABLES_FNV1A)
            throw RuntimeException("decimal128_tables.bin ACTUAL actualDwordFnv1a mismatch")

        val actualByteFnv1a = fnv1aHash(BYTE_TABLES)
        if (actualByteFnv1a != EXPECTED_BYTE_TABLES_FNV1A)
            throw RuntimeException("decimal128_tables.bin ACTUAL headerByteFnv1a mismatch")

        resourceLoadSuccessful = true
    }
}
