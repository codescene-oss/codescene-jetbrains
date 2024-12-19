package com.codescene.jetbrains.components.codehealth.detail

import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_DECREASE
import com.codescene.jetbrains.components.codehealth.slider.CustomSlider
import com.codescene.jetbrains.util.Constants.ORANGE
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.ui.JBColor
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import javax.swing.*

data class SubHeaderParams(
    val first: String,
    val firstIcon: Icon,
    val second: String,
    val secondIcon: Icon
)

val lorem =
    "<html>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque elementum ex sed nunc dictum, in tempus odio posuere. Nulla lacinia tincidunt ligula non sollicitudin. Fusce sed urna fermentum, iaculis leo id, fringilla risus. Proin tempor est sed tortor convallis finibus. Maecenas dignissim rutrum lorem in ullamcorper. Ut porttitor porttitor ipsum a volutpat. Cras congue tincidunt nulla a ornare. Donec accumsan tortor in pellentesque interdum. Nam at consequat sem..</html>"

class CodeHealthDetailsPanel {

    fun getContent() = JBScrollPane(JPanel().apply {
        layout = GridBagLayout()
        border = JBUI.Borders.empty(0, 10, 10, 10)

        val constraint = getGridBagConstraints()

        addHeader("<html><h2>Code Health Score</h2></html>", constraint)

        val params = SubHeaderParams(
            first = "JavaScript",
            firstIcon = FileTypeManager.getInstance().getFileTypeByExtension("js").icon,
            second = "Code Health Declining",
            secondIcon = CODE_HEALTH_DECREASE
        )

        addSubHeader(params, constraint)
        addSeparator(constraint)
        addCodeHealthHeader(constraint)

        addHealthDecline("Declined from 6.0 (-15.33%)", constraint)
        addSlider(3.42, constraint)
        addBody("<html><h3>Why is this important</h3></html>", lorem, constraint)
        addLink(
            "Learn more about Code Health Analysis",
            "https://codescene.com/product/code-health#:~:text=Code%20Health%20is%20an%20aggregated,negative%20outcomes%20for%20your%20project",
            constraint
        )
    }).apply {
        border = null
        verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
    }

    private fun getGridBagConstraints() = GridBagConstraints().apply {
        fill = GridBagConstraints.HORIZONTAL
        insets = JBUI.emptyInsets()
        weightx = 0.0
        weighty = 0.0
    }

    private fun JPanel.addHeader(text: String, constraint: GridBagConstraints) {
        constraint.gridy = 0
        constraint.gridx = 0
        constraint.gridwidth = 3

        add(JLabel(text), constraint)
    }

    private fun JPanel.addSeparator(constraint: GridBagConstraints) {
        constraint.gridy = 2
        constraint.gridx = 0
        constraint.gridwidth = 3
        constraint.ipady = 5

        add(JSeparator(), constraint)
        constraint.ipady = 0
    }

    private fun JPanel.addSubHeader(params: SubHeaderParams, constraint: GridBagConstraints) {
        constraint.gridy = 1
        constraint.gridx = 0
        constraint.gridwidth = 1
        constraint.ipadx = 15
        constraint.ipady = 15

        add(JLabel(params.first).apply { icon = params.firstIcon }, constraint)
        constraint.ipadx = 0

        constraint.gridx = 1
        add(JLabel(params.second).apply { icon = params.secondIcon }, constraint)
        constraint.ipady = 0
    }

    private fun JPanel.addCodeHealthHeader(constraint: GridBagConstraints) {
        constraint.gridy = 3
        constraint.gridx = 0

        add(JPanel().apply {
            layout = FlowLayout(FlowLayout.LEFT)
            add(JLabel("<html><h1>5.08</h1></html>"), constraint)

            add(Box.createHorizontalStrut(5))

            add(JButton("Problematic").apply {
                foreground = ORANGE
                isContentAreaFilled = false
                isFocusPainted = false
                border = RoundedLineBorder(JBColor.ORANGE, 12)
                isOpaque = false
            })
        }, constraint)
    }

    private fun JPanel.addHealthDecline(text: String, constraint: GridBagConstraints) {
        constraint.gridy = 4
        constraint.gridx = 0
        constraint.gridwidth = 3

        add(JLabel(text).apply {
            foreground = JBColor.GRAY
        }, constraint)
    }

    private fun JPanel.addSlider(value: Double, constraint: GridBagConstraints) {
        constraint.gridy = 5
        constraint.gridx = 0
        constraint.ipady = 30
        constraint.weightx = 1.0
        constraint.gridwidth = 3

        add(CustomSlider(value), constraint)
    }

    private fun JPanel.addBody(title: String, body: String, constraint: GridBagConstraints) {
        constraint.ipady = 20
        constraint.gridy = 6
        constraint.gridx = 0
        constraint.gridwidth = 3
        constraint.weightx = 0.0

        add(JLabel(title), constraint)

        constraint.ipady = 0
        constraint.gridy = 7
        constraint.gridx = 0
        constraint.gridwidth = 3
        constraint.weightx = 1.0

        add(JLabel(body), constraint)
    }

    private fun JPanel.addLink(info: String, url: String, constraint: GridBagConstraints) {
        constraint.gridy = 8
        constraint.gridx = 0
        constraint.gridwidth = 3
        constraint.ipady = 15

        val linkLabel =
            JLabel("<html><a href='$url'>$info</a></html>").apply {
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            }

        linkLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                Desktop.getDesktop().browse(URI.create(url))
            }
        })

        add(linkLabel, constraint)
    }

}
