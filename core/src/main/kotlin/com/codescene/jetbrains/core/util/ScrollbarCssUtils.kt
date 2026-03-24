package com.codescene.jetbrains.core.util

fun parseScrollbarHex(css: String): String {
    val regex = """::-webkit-scrollbar-thumb\s*\{[^}]*background-color:\s*([^;]+);""".toRegex()
    val match = regex.find(css) ?: return ""

    val rgba = match.groupValues[1].trim()
    val rgbaRegex = """rgba?\((\d+),\s*(\d+),\s*(\d+),?\s*([\d.]+)?\)""".toRegex()
    val rgbaMatch = rgbaRegex.find(rgba) ?: return ""

    val r = rgbaMatch.groupValues[1].toInt().coerceIn(0, 255)
    val g = rgbaMatch.groupValues[2].toInt().coerceIn(0, 255)
    val b = rgbaMatch.groupValues[3].toInt().coerceIn(0, 255)
    val a = (rgbaMatch.groupValues.getOrNull(4)?.toFloatOrNull() ?: 1f).coerceIn(0f, 1f)

    return String.format("%02X%02X%02X%02X", r, g, b, (a * 255).toInt())
}
