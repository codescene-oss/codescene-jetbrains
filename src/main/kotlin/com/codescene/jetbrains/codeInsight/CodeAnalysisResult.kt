package com.codescene.jetbrains.codeInsight

//TODO: remove after API integration - just for dev purposes

val codeAnalysisResult1 = CodeAnalysisResult(
    score = 8.89,
    fileLevelCodeSmells = listOf(
        CodeSmell(
            category = "Bumpy Road Ahead",
            range = CodeSmellRange(startLine = 1, startColumn = 1, endLine = 1, endColumn = 1),
            details = "LoC = 94 lines"
        ),
        CodeSmell(
            category = "Large Method",
            range = CodeSmellRange(startLine = 1, startColumn = 1, endLine = 1, endColumn = 1),
            details = "LoC = 95 lines"
        )
    ),
    functionLevelCodeSmells = listOf(
        FunctionCodeSmell(
            function = "computeTangents",
            range = CodeSmellRange(startLine = 55, startColumn = 2, endLine = 212, endColumn = 17),
            codeSmells = listOf(
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(startLine = 55, startColumn = 2, endLine = 55, endColumn = 17),
                    details = "bumps = 2"
                ),
                CodeSmell(
                    category = "Complex Method",
                    range = CodeSmellRange(startLine = 55, startColumn = 2, endLine = 55, endColumn = 17),
                    details = "cc = 14"
                ),
                CodeSmell(
                    category = "Large Method",
                    range = CodeSmellRange(startLine = 55, startColumn = 2, endLine = 55, endColumn = 17),
                    details = "LoC = 95 lines"
                )
            )
        )
    ),
    expressionLevelCodeSmells = listOf(
        CodeSmell(
            category = "Bumpy Road Ahead",
            range = CodeSmellRange(startLine = 63, startColumn = 3, endLine = 65, endColumn = 39),
            details = "3 complex conditional expressions"
        )
    ),
    rawScore = "..."
)

val codeAnalysisResult = CodeAnalysisResult(
    score = 8.89,
    fileLevelCodeSmells = listOf(
        CodeSmell(
            category = "Lines of Code in a Single File",
            range = CodeSmellRange(startLine = 1, startColumn = 1, endLine = 1, endColumn = 1),
            details = ""
        ),
        CodeSmell(
            category = "Overall Code Complexity",
            range = CodeSmellRange(startLine = 1, startColumn = 1, endLine = 1, endColumn = 1),
            details = ""
        )
    ),
    functionLevelCodeSmells = listOf(
        FunctionCodeSmell(
            function = "updateForwardRef",
            range = CodeSmellRange(
                startLine = 394,
                startColumn = 10,
                endLine = 473,
                endColumn = 26
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Complex Method",
                    range = CodeSmellRange(
                        startLine = 394,
                        startColumn = 10,
                        endLine = 394,
                        endColumn = 26
                    ),
                    details = "cc = 12"
                ),
                CodeSmell(
                    category = "Excess Number of Function Arguments",
                    range = CodeSmellRange(
                        startLine = 394,
                        startColumn = 10,
                        endLine = 394,
                        endColumn = 26
                    ),
                    details = "Arguments = 5"
                )
            )
        ),
        FunctionCodeSmell(
            function = "updateContextProvider",
            range = CodeSmellRange(
                startLine = 3439,
                startColumn = 10,
                endLine = 3499,
                endColumn = 31
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(
                        startLine = 3439,
                        startColumn = 10,
                        endLine = 3439,
                        endColumn = 31
                    ),
                    details = "bumps = 2"
                ),
                CodeSmell(
                    category = "Deep, Nested Complexity",
                    range = CodeSmellRange(
                        startLine = 3439,
                        startColumn = 10,
                        endLine = 3439,
                        endColumn = 31
                    ),
                    details = "Nesting depth = 4 conditionals"
                ),
                CodeSmell(
                    category = "Complex Method",
                    range = CodeSmellRange(
                        startLine = 3439,
                        startColumn = 10,
                        endLine = 3439,
                        endColumn = 31
                    ),
                    details = "cc = 10"
                )
            )
        ),
        FunctionCodeSmell(
            function = "mountLazyComponent",
            range = CodeSmellRange(
                startLine = 1679,
                startColumn = 10,
                endLine = 1770,
                endColumn = 28
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(
                        startLine = 1679,
                        startColumn = 10,
                        endLine = 1679,
                        endColumn = 28
                    ),
                    details = "bumps = 2"
                ),
                CodeSmell(
                    category = "Complex Method",
                    range = CodeSmellRange(
                        startLine = 1679,
                        startColumn = 10,
                        endLine = 1679,
                        endColumn = 28
                    ),
                    details = "cc = 13"
                ),
                CodeSmell(
                    category = "Large Method",
                    range = CodeSmellRange(
                        startLine = 1679,
                        startColumn = 10,
                        endLine = 1679,
                        endColumn = 28
                    ),
                    details = "LoC = 86 lines"
                )
            )
        ),
        FunctionCodeSmell(
            function = "beginWork",
            range = CodeSmellRange(
                startLine = 3946,
                startColumn = 10,
                endLine = 4216,
                endColumn = 19
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(
                        startLine = 3946,
                        startColumn = 10,
                        endLine = 3946,
                        endColumn = 19
                    ),
                    details = "bumps = 5"
                ),
                CodeSmell(
                    category = "Complex Method",
                    range = CodeSmellRange(
                        startLine = 3946,
                        startColumn = 10,
                        endLine = 3946,
                        endColumn = 19
                    ),
                    details = "cc = 53"
                ),
                CodeSmell(
                    category = "Large Method",
                    range = CodeSmellRange(
                        startLine = 3946,
                        startColumn = 10,
                        endLine = 3946,
                        endColumn = 19
                    ),
                    details = "LoC = 233 lines"
                )
            )
        ),
        FunctionCodeSmell(
            function = "updateOffscreenComponent",
            range = CodeSmellRange(
                startLine = 634,
                startColumn = 10,
                endLine = 820,
                endColumn = 34
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(
                        startLine = 634,
                        startColumn = 10,
                        endLine = 634,
                        endColumn = 34
                    ),
                    details = "bumps = 6"
                ),
                CodeSmell(
                    category = "Deep, Nested Complexity",
                    range = CodeSmellRange(
                        startLine = 634,
                        startColumn = 10,
                        endLine = 634,
                        endColumn = 34
                    ),
                    details = "Nesting depth = 4 conditionals"
                ),
                CodeSmell(
                    category = "Complex Method",
                    range = CodeSmellRange(
                        startLine = 634,
                        startColumn = 10,
                        endLine = 634,
                        endColumn = 34
                    ),
                    details = "cc = 26"
                ),
                CodeSmell(
                    category = "Large Method",
                    range = CodeSmellRange(
                        startLine = 634,
                        startColumn = 10,
                        endLine = 634,
                        endColumn = 34
                    ),
                    details = "LoC = 124 lines"
                )
            )
        ),
        FunctionCodeSmell(
            function = "propagateSuspenseContextChange",
            range = CodeSmellRange(
                startLine = 3039,
                startColumn = 10,
                endLine = 3081,
                endColumn = 40
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(
                        startLine = 3039,
                        startColumn = 10,
                        endLine = 3039,
                        endColumn = 40
                    ),
                    details = "bumps = 2"
                ),
                CodeSmell(
                    category = "Complex Method",
                    range = CodeSmellRange(
                        startLine = 3039,
                        startColumn = 10,
                        endLine = 3039,
                        endColumn = 40
                    ),
                    details = "cc = 10"
                )
            )
        ),
        FunctionCodeSmell(
            function = "mountHostRootWithoutHydrating",
            range = CodeSmellRange(
                startLine = 1499,
                startColumn = 10,
                endLine = 1515,
                endColumn = 39
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Excess Number of Function Arguments",
                    range = CodeSmellRange(
                        startLine = 1499,
                        startColumn = 10,
                        endLine = 1499,
                        endColumn = 39
                    ),
                    details = "Arguments = 5"
                )
            )
        ),
        FunctionCodeSmell(
            function = "validateFunctionComponentInDev",
            range = CodeSmellRange(
                startLine = 1999,
                startColumn = 10,
                endLine = 2070,
                endColumn = 40
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(
                        startLine = 1999,
                        startColumn = 10,
                        endLine = 1999,
                        endColumn = 40
                    ),
                    details = "bumps = 5"
                ),
                CodeSmell(
                    category = "Complex Method",
                    range = CodeSmellRange(
                        startLine = 1999,
                        startColumn = 10,
                        endLine = 1999,
                        endColumn = 40
                    ),
                    details = "cc = 22"
                )
            )
        ),
        FunctionCodeSmell(
            function = "remountFiber",
            range = CodeSmellRange(
                startLine = 3635,
                startColumn = 10,
                endLine = 3704,
                endColumn = 22
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(
                        startLine = 3635,
                        startColumn = 10,
                        endLine = 3635,
                        endColumn = 22
                    ),
                    details = "bumps = 3"
                ),
                CodeSmell(
                    category = "Deep, Nested Complexity",
                    range = CodeSmellRange(
                        startLine = 3635,
                        startColumn = 10,
                        endLine = 3635,
                        endColumn = 22
                    ),
                    details = "Nesting depth = 4 conditionals"
                ),
                CodeSmell(
                    category = "Complex Method",
                    range = CodeSmellRange(
                        startLine = 3635,
                        startColumn = 10,
                        endLine = 3635,
                        endColumn = 22
                    ),
                    details = "cc = 13"
                )
            )
        ),
        FunctionCodeSmell(
            function = "updateTracingMarkerComponent",
            range = CodeSmellRange(
                startLine = 936,
                startColumn = 10,
                endLine = 984,
                endColumn = 38
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(
                        startLine = 936,
                        startColumn = 10,
                        endLine = 936,
                        endColumn = 38
                    ),
                    details = "bumps = 2"
                )
            )
        ),
        FunctionCodeSmell(
            function = "initSuspenseListRenderState",
            range = CodeSmellRange(
                startLine = 3251,
                startColumn = 10,
                endLine = 3278,
                endColumn = 37
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Excess Number of Function Arguments",
                    range = CodeSmellRange(
                        startLine = 3251,
                        startColumn = 10,
                        endLine = 3251,
                        endColumn = 37
                    ),
                    details = "Arguments = 5"
                )
            )
        ),
        FunctionCodeSmell(
            function = "updateHostRoot",
            range = CodeSmellRange(
                startLine = 1370,
                startColumn = 10,
                endLine = 1497,
                endColumn = 24
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(
                        startLine = 1370,
                        startColumn = 10,
                        endLine = 1370,
                        endColumn = 24
                    ),
                    details = "bumps = 4"
                ),
                CodeSmell(
                    category = "Complex Method",
                    range = CodeSmellRange(
                        startLine = 1370,
                        startColumn = 10,
                        endLine = 1370,
                        endColumn = 24
                    ),
                    details = "cc = 12"
                ),
                CodeSmell(
                    category = "Large Method",
                    range = CodeSmellRange(
                        startLine = 1370,
                        startColumn = 10,
                        endLine = 1370,
                        endColumn = 24
                    ),
                    details = "LoC = 94 lines"
                )
            )
        ),
        FunctionCodeSmell(
            function = "updateSuspenseListComponent",
            range = CodeSmellRange(
                startLine = 3287,
                startColumn = 10,
                endLine = 3410,
                endColumn = 37
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(
                        startLine = 3287,
                        startColumn = 10,
                        endLine = 3287,
                        endColumn = 37
                    ),
                    details = "bumps = 3"
                ),
                CodeSmell(
                    category = "Deep, Nested Complexity",
                    range = CodeSmellRange(
                        startLine = 3287,
                        startColumn = 10,
                        endLine = 3287,
                        endColumn = 37
                    ),
                    details = "Nesting depth = 4 conditionals"
                ),
                CodeSmell(
                    category = "Complex Method",
                    range = CodeSmellRange(
                        startLine = 3287,
                        startColumn = 10,
                        endLine = 3287,
                        endColumn = 37
                    ),
                    details = "cc = 12"
                ),
                CodeSmell(
                    category = "Large Method",
                    range = CodeSmellRange(
                        startLine = 3287,
                        startColumn = 10,
                        endLine = 3287,
                        endColumn = 37
                    ),
                    details = "LoC = 101 lines"
                )
            )
        ),
        FunctionCodeSmell(
            function = "finishClassComponent",
            range = CodeSmellRange(
                startLine = 1258,
                startColumn = 10,
                endLine = 1353,
                endColumn = 30
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(
                        startLine = 1258,
                        startColumn = 10,
                        endLine = 1258,
                        endColumn = 30
                    ),
                    details = "bumps = 4"
                ),
                CodeSmell(
                    category = "Complex Method",
                    range = CodeSmellRange(
                        startLine = 1258,
                        startColumn = 10,
                        endLine = 1258,
                        endColumn = 30
                    ),
                    details = "cc = 15"
                ),
                CodeSmell(
                    category = "Large Method",
                    range = CodeSmellRange(
                        startLine = 1258,
                        startColumn = 10,
                        endLine = 1258,
                        endColumn = 30
                    ),
                    details = "LoC = 70 lines"
                ),
                CodeSmell(
                    category = "Excess Number of Function Arguments",
                    range = CodeSmellRange(
                        startLine = 1258,
                        startColumn = 10,
                        endLine = 1258,
                        endColumn = 30
                    ),
                    details = "Arguments = 6"
                )
            )
        ),

        FunctionCodeSmell(
            function = "updateSimpleMemoComponent",
            range = CodeSmellRange(
                startLine = 561,
                startColumn = 10,
                endLine = 632,
                endColumn = 35
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Excess Number of Function Arguments",
                    range = CodeSmellRange(
                        startLine = 561,
                        startColumn = 10,
                        endLine = 561,
                        endColumn = 35
                    ),
                    details = "Arguments = 5"
                )
            )
        ),

        FunctionCodeSmell(
            function = "updateMemoComponent",
            range = CodeSmellRange(
                startLine = 475,
                startColumn = 10,
                endLine = 475,
                endColumn = 29
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Brain Method",
                    range = CodeSmellRange(
                        startLine = 475,
                        startColumn = 10,
                        endLine = 475,
                        endColumn = 29
                    ),
                    details = ""
                ),
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(
                        startLine = 475,
                        startColumn = 10,
                        endLine = 475,
                        endColumn = 29
                    ),
                    details = "bumps = 3"
                ),
                CodeSmell(
                    category = "Deep, Nested Complexity",
                    range = CodeSmellRange(
                        startLine = 475,
                        startColumn = 10,
                        endLine = 475,
                        endColumn = 29
                    ),
                    details = "Nesting depth = 4 conditionals"
                ),
                CodeSmell(
                    category = "Large Method",
                    range = CodeSmellRange(
                        startLine = 475,
                        startColumn = 10,
                        endLine = 475,
                        endColumn = 29
                    ),
                    details = "LoC = 77 lines"
                ),
                CodeSmell(
                    category = "Excess Number of Function Arguments",
                    range = CodeSmellRange(
                        startLine = 475,
                        startColumn = 10,
                        endLine = 475,
                        endColumn = 29
                    ),
                    details = "Arguments = 5"
                )
            )
        ),

        FunctionCodeSmell(
            function = "updateHostComponent",
            range = CodeSmellRange(
                startLine = 1517,
                startColumn = 10,
                endLine = 1605,
                endColumn = 29
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(
                        startLine = 1517,
                        startColumn = 10,
                        endLine = 1517,
                        endColumn = 29
                    ),
                    details = "bumps = 2"
                ),
                CodeSmell(
                    category = "Deep, Nested Complexity",
                    range = CodeSmellRange(
                        startLine = 1517,
                        startColumn = 10,
                        endLine = 1517,
                        endColumn = 29
                    ),
                    details = "Nesting depth = 6 conditionals"
                ),
                CodeSmell(
                    category = "Complex Method",
                    range = CodeSmellRange(
                        startLine = 1517,
                        startColumn = 10,
                        endLine = 1517,
                        endColumn = 29
                    ),
                    details = "cc = 14"
                )
            )
        ),

        FunctionCodeSmell(
            function = "updateFunctionComponent",
            range = CodeSmellRange(
                startLine = 1049,
                startColumn = 10,
                endLine = 1109,
                endColumn = 33
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Complex Method",
                    range = CodeSmellRange(
                        startLine = 1049,
                        startColumn = 10,
                        endLine = 1049,
                        endColumn = 33
                    ),
                    details = "cc = 9"
                ),
                CodeSmell(
                    category = "Excess Number of Function Arguments",
                    range = CodeSmellRange(
                        startLine = 1049,
                        startColumn = 10,
                        endLine = 1049,
                        endColumn = 33
                    ),
                    details = "Arguments = 5"
                )
            )
        ),

        FunctionCodeSmell(
            function = "replayFunctionComponent",
            range = CodeSmellRange(
                startLine = 1111,
                startColumn = 17,
                endLine = 1152,
                endColumn = 40
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Excess Number of Function Arguments",
                    range = CodeSmellRange(
                        startLine = 1111,
                        startColumn = 17,
                        endLine = 1111,
                        endColumn = 40
                    ),
                    details = "Arguments = 6"
                )
            )
        ),

        FunctionCodeSmell(
            function = "updateSuspenseComponent",
            range = CodeSmellRange(
                startLine = 2167,
                startColumn = 10,
                endLine = 2433,
                endColumn = 33
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(
                        startLine = 2167,
                        startColumn = 10,
                        endLine = 2167,
                        endColumn = 33
                    ),
                    details = "bumps = 6"
                ),
                CodeSmell(
                    category = "Deep, Nested Complexity",
                    range = CodeSmellRange(
                        startLine = 2167,
                        startColumn = 10,
                        endLine = 2167,
                        endColumn = 33
                    ),
                    details = "Nesting depth = 5 conditionals"
                ),
                CodeSmell(
                    category = "Complex Method",
                    range = CodeSmellRange(
                        startLine = 2167,
                        startColumn = 10,
                        endLine = 2167,
                        endColumn = 33
                    ),
                    details = "cc = 25"
                ),
                CodeSmell(
                    category = "Large Method",
                    range = CodeSmellRange(
                        startLine = 2167,
                        startColumn = 10,
                        endLine = 2167,
                        endColumn = 33
                    ),
                    details = "LoC = 199 lines"
                )
            )
        ),
        FunctionCodeSmell(
            function = "attemptEarlyBailoutIfNoScheduledUpdate",
            range = CodeSmellRange(
                startLine = 3727,
                startColumn = 10,
                endLine = 3944,
                endColumn = 48
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(
                        startLine = 3727,
                        startColumn = 10,
                        endLine = 3727,
                        endColumn = 48
                    ),
                    details = "bumps = 6"
                ),
                CodeSmell(
                    category = "Deep, Nested Complexity",
                    range = CodeSmellRange(
                        startLine = 3727,
                        startColumn = 10,
                        endLine = 3727,
                        endColumn = 48
                    ),
                    details = "Nesting depth = 4 conditionals"
                ),
                CodeSmell(
                    category = "Complex Method",
                    range = CodeSmellRange(
                        startLine = 3727,
                        startColumn = 10,
                        endLine = 3727,
                        endColumn = 48
                    ),
                    details = "cc = 34"
                ),
                CodeSmell(
                    category = "Large Method",
                    range = CodeSmellRange(
                        startLine = 3727,
                        startColumn = 10,
                        endLine = 3727,
                        endColumn = 48
                    ),
                    details = "LoC = 150 lines"
                )
            )
        ),
        FunctionCodeSmell(
            function = "validateSuspenseListChildren",
            range = CodeSmellRange(
                startLine = 3207,
                startColumn = 10,
                endLine = 3249,
                endColumn = 38
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(
                        startLine = 3207,
                        startColumn = 10,
                        endLine = 3207,
                        endColumn = 38
                    ),
                    details = "bumps = 2"
                ),
                CodeSmell(
                    category = "Deep, Nested Complexity",
                    range = CodeSmellRange(
                        startLine = 3207,
                        startColumn = 10,
                        endLine = 3207,
                        endColumn = 38
                    ),
                    details = "Nesting depth = 7 conditionals"
                ),
                CodeSmell(
                    category = "Complex Method",
                    range = CodeSmellRange(
                        startLine = 3207,
                        startColumn = 10,
                        endLine = 3207,
                        endColumn = 38
                    ),
                    details = "cc = 14"
                )
            )
        ),
        FunctionCodeSmell(
            function = "markRef",
            range = CodeSmellRange(
                startLine = 1028,
                startColumn = 10,
                endLine = 1047,
                endColumn = 17
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(
                        startLine = 1028,
                        startColumn = 10,
                        endLine = 1028,
                        endColumn = 17
                    ),
                    details = "bumps = 2"
                ),
                CodeSmell(
                    category = "Complex Method",
                    range = CodeSmellRange(
                        startLine = 1028,
                        startColumn = 10,
                        endLine = 1028,
                        endColumn = 17
                    ),
                    details = "cc = 9"
                )
            )
        ),
        FunctionCodeSmell(
            function = "updateClassComponent",
            range = CodeSmellRange(
                startLine = 1154,
                startColumn = 10,
                endLine = 1256,
                endColumn = 30
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(
                        startLine = 1154,
                        startColumn = 10,
                        endLine = 1154,
                        endColumn = 30
                    ),
                    details = "bumps = 2"
                ),
                CodeSmell(
                    category = "Complex Method",
                    range = CodeSmellRange(
                        startLine = 1154,
                        startColumn = 10,
                        endLine = 1154,
                        endColumn = 30
                    ),
                    details = "cc = 12"
                ),
                CodeSmell(
                    category = "Large Method",
                    range = CodeSmellRange(
                        startLine = 1154,
                        startColumn = 10,
                        endLine = 1154,
                        endColumn = 30
                    ),
                    details = "LoC = 90 lines"
                ),
                CodeSmell(
                    category = "Excess Number of Function Arguments",
                    range = CodeSmellRange(
                        startLine = 1154,
                        startColumn = 10,
                        endLine = 1154,
                        endColumn = 30
                    ),
                    details = "Arguments = 5"
                )
            )
        ),
        FunctionCodeSmell(
            function = "validateRevealOrder",
            range = CodeSmellRange(
                startLine = 3106,
                startColumn = 10,
                endLine = 3156,
                endColumn = 29
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Deep, Nested Complexity",
                    range = CodeSmellRange(
                        startLine = 3106,
                        startColumn = 10,
                        endLine = 3106,
                        endColumn = 29
                    ),
                    details = "Nesting depth = 4 conditionals"
                ),
                CodeSmell(
                    category = "Complex Method",
                    range = CodeSmellRange(
                        startLine = 3106,
                        startColumn = 10,
                        endLine = 3106,
                        endColumn = 29
                    ),
                    details = "cc = 13"
                )
            )
        ),
        FunctionCodeSmell(
            function = "updateCacheComponent",
            range = CodeSmellRange(
                startLine = 867,
                startColumn = 10,
                endLine = 933,
                endColumn = 30
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(
                        startLine = 867,
                        startColumn = 10,
                        endLine = 867,
                        endColumn = 30
                    ),
                    details = "bumps = 3"
                )
            )
        ),
        FunctionCodeSmell(
            function = "updateDehydratedSuspenseComponent",
            range = CodeSmellRange(
                startLine = 2781,
                startColumn = 10,
                endLine = 2781,
                endColumn = 43
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Brain Method",
                    range = CodeSmellRange(
                        startLine = 2781,
                        startColumn = 10,
                        endLine = 2781,
                        endColumn = 43
                    ),
                    details = ""
                ),
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(
                        startLine = 2781,
                        startColumn = 10,
                        endLine = 2781,
                        endColumn = 43
                    ),
                    details = "bumps = 5"
                ),
                CodeSmell(
                    category = "Deep, Nested Complexity",
                    range = CodeSmellRange(
                        startLine = 2781,
                        startColumn = 10,
                        endLine = 2781,
                        endColumn = 43
                    ),
                    details = "Nesting depth = 4 conditionals"
                ),
                CodeSmell(
                    category = "Large Method",
                    range = CodeSmellRange(
                        startLine = 2781,
                        startColumn = 10,
                        endLine = 2781,
                        endColumn = 43
                    ),
                    details = "LoC = 154 lines"
                ),
                CodeSmell(
                    category = "Excess Number of Function Arguments",
                    range = CodeSmellRange(
                        startLine = 2781,
                        startColumn = 10,
                        endLine = 2781,
                        endColumn = 43
                    ),
                    details = "Arguments = 8"
                )
            )
        ),
        FunctionCodeSmell(
            function = "updateContextConsumer",
            range = CodeSmellRange(
                startLine = 3501,
                startColumn = 10,
                endLine = 3554,
                endColumn = 31
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Bumpy Road Ahead",
                    range = CodeSmellRange(
                        startLine = 3501,
                        startColumn = 10,
                        endLine = 3501,
                        endColumn = 31
                    ),
                    details = "bumps = 2"
                ),
                CodeSmell(
                    category = "Complex Method",
                    range = CodeSmellRange(
                        startLine = 3501,
                        startColumn = 10,
                        endLine = 3501,
                        endColumn = 31
                    ),
                    details = "cc = 9"
                )
            )
        ),
        FunctionCodeSmell(
            function = "mountIncompleteClassComponent",
            range = CodeSmellRange(
                startLine = 1772,
                startColumn = 10,
                endLine = 1809,
                endColumn = 39
            ),
            codeSmells = listOf(
                CodeSmell(
                    category = "Excess Number of Function Arguments",
                    range = CodeSmellRange(
                        startLine = 1772,
                        startColumn = 10,
                        endLine = 1772,
                        endColumn = 39
                    ),
                    details = "Arguments = 5"
                )
            )
        )
    ),
    expressionLevelCodeSmells = listOf(
        CodeSmell(
            category = "Complex Conditional",
            range = CodeSmellRange(startLine = 3108, startColumn = 5, endLine = 3112, endColumn = 36),
            details = "4 complex conditional expressions"
        ),
        CodeSmell(
            category = "Complex Conditional",
            range = CodeSmellRange(startLine = 3212, startColumn = 5, endLine = 3215, endColumn = 27),
            details = "4 complex conditional expressions"
        ),
        CodeSmell(
            category = "Complex Conditional",
            range = CodeSmellRange(startLine = 484, startColumn = 5, endLine = 486, endColumn = 36),
            details = "2 complex conditional expressions"
        ),
        CodeSmell(
            category = "Complex Conditional",
            range = CodeSmellRange(startLine = 649, startColumn = 3, endLine = 652, endColumn = 61),
            details = "3 complex conditional expressions"
        ),
        CodeSmell(
            category = "Complex Conditional",
            range = CodeSmellRange(startLine = 3973, startColumn = 5, endLine = 3975, endColumn = 35),
            details = "2 complex conditional expressions"
        ),
        CodeSmell(
            category = "Complex Conditional",
            range = CodeSmellRange(startLine = 1754, startColumn = 5, endLine = 1756, endColumn = 39),
            details = "2 complex conditional expressions"
        ),
        CodeSmell(
            category = "Complex Conditional",
            range = CodeSmellRange(startLine = 573, startColumn = 5, endLine = 575, endColumn = 44),
            details = "2 complex conditional expressions"
        )
    ),

    rawScore = "..."
)

data class CodeSmellRange(val startLine: Int, val startColumn: Int, val endLine: Int, val endColumn: Int)

data class CodeSmell(
    val category: String,
    val range: CodeSmellRange,
    val details: String
)

data class FunctionCodeSmell(
    val function: String,
    val range: CodeSmellRange,
    val codeSmells: List<CodeSmell>
)

data class CodeAnalysisResult(
    val score: Double,
    val fileLevelCodeSmells: List<CodeSmell>,
    val functionLevelCodeSmells: List<FunctionCodeSmell>,
    val expressionLevelCodeSmells: List<CodeSmell>,
    val rawScore: String
)