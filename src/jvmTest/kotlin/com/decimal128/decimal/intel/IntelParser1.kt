package com.decimal128.decimal.intel

object IntelParser1 {

    fun parseAllCases(fileText: String): List<IntelCase1> {
        val allCases = ArrayList<IntelCase1>()
        for (line in fileText.lineSequence()) {
            // trim comments and empty lines
            if (line.substringBefore("--").trim().isEmpty())
                    continue
            val intelCase = IntelCase1.parseIntelCase(line)
            allCases.add(intelCase)
        }
        return allCases
    }

    fun parseCases(cases: Array<String>): List<IntelCase1> {
        val casesList = ArrayList<IntelCase1>()
        cases.forEach {
            val intelCase = IntelCase1.parseIntelCase(it)
            casesList.add(intelCase)
        }
        return casesList
    }

    private val fileCaseCache = HashMap<String, List<IntelCase1>>()

    fun parseTestsInFile(fileName: String = "/intel/readtest.in"): List<IntelCase1> {
        val v = fileCaseCache[fileName]
        if (v != null)
            return v
        val fileText = IntelParser1::class.java.getResource(fileName)!!.readText()
        val allCases = parseAllCases(fileText)
        fileCaseCache[fileName] = allCases
        return allCases
    }

}
