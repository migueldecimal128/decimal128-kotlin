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

    fun parseCases(cases: Array<String>): List<IntelCase> {
        val casesList = ArrayList<IntelCase>()
        cases.forEach {
            val intelCase = IntelCase.parseIntelCase(it)
            casesList.add(intelCase)
        }
        return casesList
    }

    private val fileCaseCache = HashMap<String, List<IntelCase>>()

    fun parseTestsInFile(fileName: String = "/intel/readtest.in"): List<IntelCase> {
        val v = fileCaseCache[fileName]
        if (v != null)
            return v
        val fileText = IntelParser::class.java.getResource(fileName)!!.readText()
        val allCases = parseAllCases(fileText)
        fileCaseCache[fileName] = allCases
        return allCases
    }

}
