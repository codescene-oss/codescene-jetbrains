package com.codescene.jetbrains.components.webview.util

import com.codescene.jetbrains.util.Constants

val docNameMap: Map<String, String> = mapOf(
    "docs_issues_brain_class" to Constants.BRAIN_CLASS,
    "docs_issues_brain_method" to Constants.BRAIN_METHOD,
    "docs_issues_bumpy_road_ahead" to Constants.BUMPY_ROAD_AHEAD,
    "docs_issues_complex_conditional" to Constants.COMPLEX_CONDITIONAL,
    "docs_issues_complex_method" to Constants.COMPLEX_METHOD,
    "docs_issues_constructor_over_injection" to Constants.CONSTRUCTOR_OVER_INJECTION,
    "docs_issues_duplicated_assertion_blocks" to Constants.DUPLICATED_ASSERTION_BLOCKS,
    "docs_issues_code_duplication" to Constants.CODE_DUPLICATION,
    "docs_issues_file_size_issue" to Constants.FILE_SIZE_ISSUE,
    "docs_issues_excess_number_of_function_arguments" to Constants.EXCESS_NUMBER_OF_FUNCTION_ARGUMENTS,
    "docs_issues_number_of_functions_in_a_single_module" to Constants.NUMBER_OF_FUNCTIONS_IN_A_SINGLE_MODULE,
    "docs_issues_global_conditionals" to Constants.GLOBAL_CONDITIONALS,
    "docs_issues_deep_global_nested_complexity" to Constants.DEEP_GLOBAL_NESTED_COMPLEXITY,
    "docs_issues_high_degree_of_code_duplication" to Constants.HIGH_DEGREE_OF_CODE_DUPLICATION,
    "docs_issues_large_assertion_blocks" to Constants.LARGE_ASSERTION_BLOCKS,
    "docs_issues_large_embedded_code_block" to Constants.LARGE_EMBEDDED_CODE_BLOCK,
    "docs_issues_large_method" to Constants.LARGE_METHOD,
    "docs_issues_lines_of_code_in_a_single_file" to Constants.LINES_OF_CODE_IN_A_SINGLE_FILE,
    "docs_issues_lines_of_declarations_in_a_single_file" to Constants.LINES_OF_DECLARATION_IN_A_SINGLE_FILE,
    "docs_issues_low_cohesion" to Constants.LOW_COHESION,
    "docs_issues_missing_arguments_abstractions" to Constants.MISSING_ARGUMENTS_ABSTRACTIONS,
    "docs_issues_modularity_issue" to Constants.MODULARITY_ISSUE,
    "docs_issues_deep_nested_complexity" to Constants.DEEP_NESTED_COMPLEXITY,
    "docs_issues_overall_code_complexity" to Constants.OVERALL_CODE_COMPLEXITY,
    "docs_issues_potentially_low_cohesion" to Constants.POTENTIALLY_LOW_COHESION,
    "docs_issues_primitive_obsession" to Constants.PRIMITIVE_OBSESSION,
    "docs_issues_string_heavy_function_arguments" to Constants.STRING_HEAVY_FUNCTION_ARGUMENTS,
)

val nameDocMap: Map<String, String> = docNameMap.entries.associate { (k, v) -> v to k }