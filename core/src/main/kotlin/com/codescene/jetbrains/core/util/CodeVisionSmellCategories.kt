package com.codescene.jetbrains.core.util

object CodeVisionSmellCategories {
    val orderedForDisplay: List<String> =
        listOf(
            Constants.BRAIN_CLASS,
            Constants.BRAIN_METHOD,
            Constants.BUMPY_ROAD_AHEAD,
            Constants.CODE_DUPLICATION,
            Constants.COMPLEX_CONDITIONAL,
            Constants.COMPLEX_METHOD,
            Constants.CONSTRUCTOR_OVER_INJECTION,
            Constants.DEEP_GLOBAL_NESTED_COMPLEXITY,
            Constants.DEEP_NESTED_COMPLEXITY,
            Constants.DUPLICATED_ASSERTION_BLOCKS,
            Constants.EXCESS_NUMBER_OF_FUNCTION_ARGUMENTS,
            Constants.FILE_SIZE_ISSUE,
            Constants.GLOBAL_CONDITIONALS,
            Constants.HIGH_DEGREE_OF_CODE_DUPLICATION,
            Constants.LARGE_ASSERTION_BLOCKS,
            Constants.LARGE_EMBEDDED_CODE_BLOCK,
            Constants.LARGE_METHOD,
            Constants.LINES_OF_CODE_IN_A_SINGLE_FILE,
            Constants.LINES_OF_DECLARATION_IN_A_SINGLE_FILE,
            Constants.LOW_COHESION,
            Constants.MISSING_ARGUMENTS_ABSTRACTIONS,
            Constants.MODULARITY_ISSUE,
            Constants.NUMBER_OF_FUNCTIONS_IN_A_SINGLE_MODULE,
            Constants.OVERALL_CODE_COMPLEXITY,
            Constants.POTENTIALLY_LOW_COHESION,
            Constants.PRIMITIVE_OBSESSION,
            Constants.STRING_HEAVY_FUNCTION_ARGUMENTS,
        )
}
