package no.statnett.parquet

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.border.MatteBorder
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

class ParquetToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout())
        val tableModel = DefaultTableModel()
        val table = JBTable(tableModel)

        // 1. Table Setup & Sorter
        table.autoResizeMode = JTable.AUTO_RESIZE_OFF
        val sorter = TableRowSorter(tableModel)
        table.rowSorter = sorter

        table.setDefaultRenderer(Object::class.java, object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(t: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
                val c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column)
                val text = value?.toString() ?: ""
                horizontalAlignment = if (text.toDoubleOrNull() != null) SwingConstants.RIGHT else SwingConstants.LEFT

                val num = text.toDoubleOrNull()
                if (num != null && num < 0) {
                    c.foreground = JBColor.RED
                } else if (!isSelected) {
                    c.foreground = t?.foreground
                }
                return c
            }
        })

        // 2. Query History Logic (ComboBox)
        val queryHistoryModel = DefaultComboBoxModel<String>()
        val sqlCombo = JComboBox(queryHistoryModel)
        sqlCombo.isEditable = true
        sqlCombo.font = Font("Monospaced", Font.PLAIN, 13)
        queryHistoryModel.addElement("SELECT * FROM 'path/to/file.parquet' LIMIT 100")

        // 3. UI Styling - Unified Statnett Blue
        val statnettBlue = Color(52, 116, 186)

        // All buttons now use statnettBlue
        val browseBtn = createStyledButton("Select Parquet file or folder", statnettBlue)
        val runBtn = createStyledButton("Run SQL Query", statnettBlue)
        val clearFilterBtn = createStyledButton("Clear", statnettBlue)

        val filterField = JTextField(15)
        val statusLabel = JLabel("Rows: 0")
        statusLabel.font = statusLabel.font.deriveFont(Font.BOLD)

        // 4. Execution & Selection Logic
        runBtn.addActionListener {
            val query = sqlCombo.editor.item.toString()
            runBtn.isEnabled = false

            ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Executing SQL Query") {
                override fun run(indicator: ProgressIndicator) {
                    try {
                        val (columns, data) = ParquetSqlService().runQuery(query)
                        SwingUtilities.invokeLater {
                            tableModel.setDataVector(data.map { it.toTypedArray() }.toTypedArray(), columns.toTypedArray())

                            if (queryHistoryModel.getIndexOf(query) == -1) {
                                queryHistoryModel.insertElementAt(query, 0)
                            }

                            filterField.text = ""
                            applyFilter(filterField, sorter, statusLabel, tableModel)
                            resizeColumns(table)
                            runBtn.isEnabled = true
                        }
                    } catch (e: Exception) {
                        SwingUtilities.invokeLater {
                            JOptionPane.showMessageDialog(panel, "Error: ${e.message}")
                            runBtn.isEnabled = true
                        }
                    }
                }
            })
        }

        browseBtn.addActionListener {
            val descriptor = FileChooserDescriptor(true, true, false, false, false, false)
                .withTitle("Select Parquet")
            FileChooser.chooseFile(descriptor, project, null)?.let { file ->
                val path = if (file.isDirectory) "${file.path}/*.parquet" else file.path
                val newQuery = "SELECT * FROM '$path' LIMIT 100"
                sqlCombo.editor.item = newQuery
            }
        }

        filterField.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) = applyFilter(filterField, sorter, statusLabel, tableModel)
        })

        clearFilterBtn.addActionListener {
            filterField.text = ""
            applyFilter(filterField, sorter, statusLabel, tableModel)
        }

        // 5. Layout Construction
        val topPanel = JPanel(BorderLayout())
        topPanel.border = MatteBorder(0, 0, 3, 0, statnettBlue)

        topPanel.add(browseBtn, BorderLayout.WEST)
        topPanel.add(sqlCombo, BorderLayout.CENTER)

        val controlBar = JPanel(BorderLayout())
        controlBar.border = JBUI.Borders.empty(5, 10)

        val leftFlow = JPanel(FlowLayout(FlowLayout.LEFT, 8, 5))
        leftFlow.add(JLabel("Filter:"))
        leftFlow.add(filterField)
        leftFlow.add(clearFilterBtn)

        val rightFlow = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 5))
        rightFlow.border = MatteBorder(0, 2, 0, 0, JBColor.LIGHT_GRAY)
        rightFlow.add(runBtn)
        rightFlow.add(statusLabel)

        controlBar.add(leftFlow, BorderLayout.WEST)
        controlBar.add(rightFlow, BorderLayout.EAST)

        val centerPanel = JPanel(BorderLayout())
        centerPanel.add(controlBar, BorderLayout.NORTH)
        centerPanel.add(JBScrollPane(table), BorderLayout.CENTER)

        panel.add(topPanel, BorderLayout.NORTH)
        panel.add(centerPanel, BorderLayout.CENTER)

        toolWindow.contentManager.addContent(
            toolWindow.contentManager.factory.createContent(panel, "", false)
        )
    }

    private fun applyFilter(field: JTextField, sorter: TableRowSorter<DefaultTableModel>, label: JLabel, model: DefaultTableModel) {
        val text = field.text
        sorter.rowFilter = if (text.isEmpty()) null else RowFilter.regexFilter("(?i)$text")
        label.text = if (text.isEmpty()) "Rows: ${model.rowCount}" else "Filtered: ${sorter.viewRowCount} of ${model.rowCount}"
    }

    private fun createStyledButton(text: String, color: Color): JButton {
        return JButton(text).apply {
            background = color
            foreground = Color.WHITE
            isContentAreaFilled = true
            isOpaque = true
            putClientProperty("JButton.buttonType", "roundRect")
            border = BorderFactory.createEmptyBorder(4, 10, 4, 10)
        }
    }

    private fun resizeColumns(table: JTable) {
        for (column in 0 until table.columnCount) {
            var width = 100
            val headerComp = table.tableHeader.defaultRenderer.getTableCellRendererComponent(table, table.getColumnName(column), false, false, 0, column)
            width = maxOf(width, headerComp.preferredSize.width + 30)
            for (row in 0 until minOf(table.rowCount, 50)) {
                val comp = table.prepareRenderer(table.getCellRenderer(row, column), row, column)
                width = maxOf(width, comp.preferredSize.width + 30)
            }
            table.columnModel.getColumn(column).preferredWidth = width
        }
    }
}