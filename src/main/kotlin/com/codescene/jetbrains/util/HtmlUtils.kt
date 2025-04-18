package com.codescene.jetbrains.util

import com.codescene.data.ace.Confidence

/**
 * Method to append single collapsable part to the current HTML content.
 */
fun appendSubpart(content: StringBuilder, htmlPart: HtmlPart) {
    content.append(
        """
            <div>
                <details open>
                    <summary>${htmlPart.title}</summary>
            """.trimIndent()
    )
        .append(htmlPart.body)
        .append("\n</details></div>\n\n")
}

fun refactoringSummary(content: StringBuilder, confidence: Confidence) {
    val level = confidence.level.value()
    val details = confidence.recommendedAction.details
    val description = confidence.recommendedAction.description

    val levelClass = "level-$level"
    content.append(
        """
        <div class="refactoring-summary $levelClass">
            <div class="refactoring-summary-header $levelClass">$description</div>
            <span>$details</span>
        </div>
    """.trimIndent())
        .append("\n")

}