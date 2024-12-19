package com.codescene.jetbrains.components.codehealth.slider

import com.codescene.jetbrains.util.Constants.GREEN
import com.codescene.jetbrains.util.Constants.ORANGE
import com.codescene.jetbrains.util.Constants.RED
import com.intellij.ui.JBColor
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.LinearGradientPaint
import java.awt.RenderingHints
import javax.swing.JSlider
import javax.swing.plaf.basic.BasicSliderUI
import kotlin.math.roundToInt

class CustomSlider(value: Double) : JSlider() {
    init {
        isOpaque = false
        isEnabled = false
        isFocusable = false

        // JSlider works with integers only. To support floating-point values, we need to scale the range.
        minimum = 0
        maximum = 1000

        setSliderPosition(value)

        setUI(object : BasicSliderUI(this) {
            override fun paintThumb(g: Graphics) {}
            override fun paintTrack(g: Graphics) {}
            override fun paintFocus(g: Graphics) {}
        })
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        addLabels(g2)
        addGradient(g2)
        addThumb(g2)
    }

    private fun setSliderPosition(position: Double) {
        val mappedPosition = if (position <= 1.0) minimum + 25.0 else position * 100

        val clampedPosition = mappedPosition.coerceIn(minimum.toDouble(), maximum.toDouble())

        value = clampedPosition.roundToInt()
    }

    private fun addGradient(g2: Graphics2D) {
        val trackHeight = 8
        val yPosition = height / 2 - trackHeight / 2

        val fractions = floatArrayOf(0.0f, 0.399f, 0.4f, 0.899f, 0.9f, 1.0f)
        val colors = arrayOf(RED, RED, ORANGE, ORANGE, GREEN, GREEN)

        g2.paint = LinearGradientPaint(0f, 0f, width.toFloat(), 0f, fractions, colors)
        g2.fillRoundRect(0, yPosition, width, trackHeight, 10, 10)
    }

    private fun addThumb(g2: Graphics2D) {
        val thumbWidth = 8
        val thumbHeight = 20
        val valuePosition = xPositionForValue(value)

        val x = valuePosition - thumbWidth / 2
        val y = height / 2 - thumbHeight / 2

        g2.color = JBColor.DARK_GRAY
        g2.fillRoundRect(x, y, thumbWidth, thumbHeight, 10, 10)

        g2.color = JBColor.WHITE
        g2.drawRoundRect(x, y, thumbWidth, thumbHeight, 10, 10)
    }

    private fun addLabels(g2: Graphics2D) {
        g2.font = g2.font.deriveFont(12f)
        val labelPosition = height / 2 + 25

        val leftLabel = "1"
        val leftX = 0
        val rightLabel = "10"
        val rightX = width - g2.fontMetrics.stringWidth(rightLabel)

        g2.color = RED
        g2.drawString(leftLabel, leftX, labelPosition)

        g2.color = GREEN
        g2.drawString(rightLabel, rightX, labelPosition)
    }

    private fun xPositionForValue(value: Int): Int {
        val range = maximum - minimum
        val percent = (value - minimum).toFloat() / range

        return (percent * (width - 8)).roundToInt()
    }
}
