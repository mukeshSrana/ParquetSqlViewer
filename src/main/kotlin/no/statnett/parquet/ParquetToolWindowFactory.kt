package no.statnett.parquet

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
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

    private val statnettBlue = Color(52, 116, 186)
    private val tableModel = DefaultTableModel()
    private val table = JBTable(tableModel)
    private val queryHistoryModel = DefaultComboBoxModel<String>()
    private val statusLabel = JLabel("Rows: 0")

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val mainPanel = JPanel(BorderLayout())

        // 1. Initialize Table Logic
        val sorter = setupTableAndSorter()

        // 2. Create Components
        val sqlCombo = createQueryComboBox()
        val filterField = createFilterField(sorter)

        val browseBtn = createBrowseButton(project, sqlCombo)
        val runBtn = createRunButton(project, sqlCombo, sorter, filterField)
        val clearBtn = createClearButton(filterField, sorter)

        // 3. Assemble Layout
        val topPanel = assembleTopPanel(browseBtn, sqlCombo)
        val controlBar = assembleControlBar(filterField, clearBtn, runBtn)

        val centerPanel = JPanel(BorderLayout())
        centerPanel.add(controlBar, BorderLayout.NORTH)
        centerPanel.add(JBScrollPane(table), BorderLayout.CENTER)

        mainPanel.add(topPanel, BorderLayout.NORTH)
        mainPanel.add(centerPanel, BorderLayout.CENTER)

        toolWindow.contentManager.addContent(
            toolWindow.contentManager.factory.createContent(mainPanel, "", false)
        )
    }

    // --- Component Creation Functions ---
    private fun setupTableAndSorter(): TableRowSorter<DefaultTableModel> {
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
        return sorter
    }

    private fun createQueryComboBox() = ComboBox(queryHistoryModel).apply {
        isEditable = true
        font = Font("Monospaced", Font.PLAIN, 13)
        queryHistoryModel.addElement("SELECT * FROM 'path/to/file.parquet' LIMIT 100")
    }

    private fun createFilterField(sorter: TableRowSorter<DefaultTableModel>) = JTextField(15).apply {
        addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) = applyFilter(this@apply, sorter)
        })
    }

    private fun createBrowseButton(project: Project, sqlCombo: ComboBox<String>) =
        createStyledButton("Select Parquet file or folder", statnettBlue).apply {
            addActionListener {
                val descriptor = FileChooserDescriptor(true, true, false, false, false, false).withTitle("Select Parquet")
                FileChooser.chooseFile(descriptor, project, null)?.let { file ->
                    val path = if (file.isDirectory) "${file.path}/*.parquet" else file.path
                    val newQuery = "SELECT * FROM '$path' LIMIT 100"

                    // FIX: Set both editor text and selected item to keep state in sync
                    sqlCombo.editor.item = newQuery
                    sqlCombo.selectedItem = newQuery
                }
            }
        }

    private fun createRunButton(project: Project, sqlCombo: ComboBox<String>, sorter: TableRowSorter<DefaultTableModel>, filterField: JTextField) =
        createStyledButton("Run SQL Query", statnettBlue).apply {
            addActionListener {
                val query = sqlCombo.editor.item.toString()
                this.isEnabled = false
                executeSqlTask(project, query, sorter, filterField, this, sqlCombo)
            }
        }

    private fun createClearButton(filterField: JTextField, sorter: TableRowSorter<DefaultTableModel>) =
        createStyledButton("Clear", statnettBlue).apply {
            addActionListener {
                filterField.text = ""
                applyFilter(filterField, sorter)
            }
        }

    // --- Layout Assembly Functions ---
    private fun assembleTopPanel(browseBtn: JButton, sqlCombo: ComboBox<String>) = JPanel(BorderLayout()).apply {
        border = MatteBorder(0, 0, 3, 0, statnettBlue)
        add(browseBtn, BorderLayout.WEST)
        add(sqlCombo, BorderLayout.CENTER)
    }

    private fun assembleControlBar(filterField: JTextField, clearBtn: JButton, runBtn: JButton): JPanel {
        val bar = JPanel(BorderLayout())
        bar.border = JBUI.Borders.empty(5, 10)

        val left = JPanel(FlowLayout(FlowLayout.LEFT, 8, 5)).apply {
            add(JLabel("Filter:"))
            add(filterField)
            add(clearBtn)
        }

        val right = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 5)).apply {
            border = MatteBorder(0, 2, 0, 0, JBColor.LIGHT_GRAY)
            add(runBtn)
            statusLabel.font = statusLabel.font.deriveFont(Font.BOLD)
            add(statusLabel)
        }

        bar.add(left, BorderLayout.WEST)
        bar.add(right, BorderLayout.EAST)
        return bar
    }

    // --- Logic & Utility Functions ---
    private fun executeSqlTask(project: Project, query: String, sorter: TableRowSorter<DefaultTableModel>, filterField: JTextField, runBtn: JButton, sqlCombo: ComboBox<String>) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Executing SQL Query") {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val (columns, data) = ParquetSqlService.getInstance().runQuery(query)
                    SwingUtilities.invokeLater {
                        tableModel.setDataVector(data.map { it.toTypedArray() }.toTypedArray(), columns.toTypedArray())

                        // FIX: Ensure new queries are added and explicitly selected in history
                        if (queryHistoryModel.getIndexOf(query) == -1) {
                            queryHistoryModel.insertElementAt(query, 0)
                        }
                        sqlCombo.selectedItem = query

                        filterField.text = ""
                        applyFilter(filterField, sorter)
                        resizeColumns()
                        runBtn.isEnabled = true
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        JOptionPane.showMessageDialog(null, "Error: ${e.message}")
                        runBtn.isEnabled = true
                    }
                }
            }
        })
    }

    private fun applyFilter(field: JTextField, sorter: TableRowSorter<DefaultTableModel>) {
        val text = field.text
        sorter.rowFilter = if (text.isEmpty()) null else RowFilter.regexFilter("(?i)$text")
        statusLabel.text = if (text.isEmpty()) "Rows: ${tableModel.rowCount}" else "Filtered: ${sorter.viewRowCount} of ${tableModel.rowCount}"
    }

    private fun createStyledButton(text: String, color: Color) = JButton(text).apply {
        background = color
        foreground = Color.WHITE
        isContentAreaFilled = true
        isOpaque = true
        putClientProperty("JButton.buttonType", "roundRect")
        border = BorderFactory.createEmptyBorder(4, 10, 4, 10)
    }

    private fun resizeColumns() {
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
