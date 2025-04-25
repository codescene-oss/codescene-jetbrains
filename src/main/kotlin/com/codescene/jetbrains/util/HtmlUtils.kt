package com.codescene.jetbrains.util

/**
 * Method to append single collapsable part to the current HTML content.
 */
fun appendSubpart(content: StringBuilder, htmlPart: HtmlPart) {
    content.append("\n")
        .append(
        """
            <div>
                <details open>
                    <summary>${htmlPart.title}</summary>
            """.trimIndent()
    )
        .append(htmlPart.body)
        .append("\n</details></div>\n\n")
}

