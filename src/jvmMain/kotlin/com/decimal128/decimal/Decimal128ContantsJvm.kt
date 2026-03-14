package com.decimal128.decimal

// FIXME - for some reason I cannot have both DWORD_TABLES and the alias
//  POW10 as expect/actual
//  Some unit tests fail, but if I run the tests individually then they run fine.
/*
@JvmField
actual val DWORD_TABLES: LongArray = LongArray(1024)
*/
@JvmField
actual val POW10: LongArray = DWORD_TABLES

