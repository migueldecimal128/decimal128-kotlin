package com.decimal128.decimal.intel

object IntelParser {

    fun parseAllCases(fileText: String): List<IntelCase> {
        val allCases = ArrayList<IntelCase>()
        for (line in fileText.lineSequence()) {
            // trim comments and empty lines
            if (line.substringBefore("--").trim().isEmpty())
                    continue
            val intelCase = IntelCase.parseIntelCase(line)
            allCases.add(intelCase)
        }
        return allCases
    }

    fun parseTestsInFile(fileName: String = "/intel/readtest.in"): List<IntelCase> {
        val fileText = IntelParser::class.java.getResource(fileName)!!.readText()
        val allTests = parseAllCases(fileText)
        return allTests
    }
}
