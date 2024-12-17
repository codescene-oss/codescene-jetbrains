package com.codescene.jetbrains.components.codehealth.slider

import com.intellij.ui.JBColor
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JSlider
import javax.swing.plaf.basic.BasicSliderUI
import kotlin.math.roundToInt

class CustomSlider : JSlider() {
    init {
        isOpaque = false
        isEnabled = false
        isFocusable = false

        // JSlider works with integers only. To support floating-point values, we need to scale the range.
        minimum = 100
        maximum = 1000

        setUI(object : BasicSliderUI(this) {
            override fun paintThumb(g: Graphics) {} // Do not paint thumb
            override fun paintTrack(g: Graphics) {} // Do not paint track
            override fun paintFocus(g: Graphics) {} // Remove focus border
        })
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        addGradient(g2)
        addThumb(g2)
        addLabels(g2)
    }

    fun setSliderPosition(position: Double) {
        val clampedPosition = position.coerceIn(minimum.toDouble(), maximum.toDouble())
        value = (clampedPosition * 100).roundToInt()
    }

    private fun addGradient(g2: Graphics2D) {
        val trackHeight = 8

        val gradient = GradientPaint(
            0f, (height / 2).toFloat(), JBColor.RED,
            width.toFloat(), (height / 2).toFloat(), JBColor.GREEN
        )
        g2.paint = gradient
        g2.fillRoundRect(0, height / 2 - trackHeight / 2, width, trackHeight, 10, 10)
    }

    private fun addThumb(g2: Graphics2D) {
        val thumbWidth = 12
        val thumbHeight = 20
        val valuePosition = xPositionForValue(value)

        g2.color = JBColor.WHITE
        g2.fillRoundRect(
            valuePosition - thumbWidth / 2, height / 2 - thumbHeight / 2,
            thumbWidth, thumbHeight, 10, 10
        )

        g2.color = JBColor.DARK_GRAY
        g2.drawRoundRect(
            valuePosition - thumbWidth / 2, height / 2 - thumbHeight / 2,
            thumbWidth, thumbHeight, 10, 10
        )
    }

    private fun addLabels(g2: Graphics2D) {
        g2.color = JBColor.RED
        g2.font = g2.font.deriveFont(12f)

        val leftLabel = "1"
        val leftX = 0
        val labelY = height / 2 + 25

        g2.drawString(leftLabel, leftX, labelY)

        g2.color = JBColor.GREEN

        val rightLabel = "10"
        val rightX = width - g2.fontMetrics.stringWidth(rightLabel)

        g2.drawString(rightLabel, rightX, labelY)
    }

    private fun xPositionForValue(value: Int): Int {
        val range = maximum - minimum
        val percent = (value - minimum).toFloat() / range

        return (percent * (width - 12)).roundToInt()
    }
}