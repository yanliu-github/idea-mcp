package com.ly.ideamcp.ui

import com.intellij.openapi.options.Configurable
import com.ly.ideamcp.config.PluginSettings
import javax.swing.*
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets

/**
 * 插件配置界面
 * Settings > Tools > IDEA MCP Server
 */
class PluginConfigurable : Configurable {
    private var settingsPanel: JPanel? = null
    private val settings = PluginSettings.getInstance()

    // UI 组件
    private lateinit var portField: JTextField
    private lateinit var hostField: JTextField
    private lateinit var autoStartCheckbox: JCheckBox
    private lateinit var enableAuthCheckbox: JCheckBox
    private lateinit var authTokenField: JPasswordField
    private lateinit var enableCorsCheckbox: JCheckBox
    private lateinit var corsOriginsField: JTextField
    private lateinit var allowedIpsField: JTextField
    private lateinit var verboseLoggingCheckbox: JCheckBox
    private lateinit var requestTimeoutField: JTextField

    override fun getDisplayName(): String {
        return "IDEA MCP Server"
    }

    override fun createComponent(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(5, 5, 5, 5)
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.HORIZONTAL
        }

        var row = 0

        // Port
        addLabel(panel, gbc, row, "Port:")
        portField = JTextField(settings.port.toString(), 10)
        addComponent(panel, gbc, row++, portField)

        // Host
        addLabel(panel, gbc, row, "Host:")
        hostField = JTextField(settings.host, 20)
        addComponent(panel, gbc, row++, hostField)

        // Auto Start
        autoStartCheckbox = JCheckBox("Auto-start server on project open", settings.autoStart)
        gbc.gridx = 0
        gbc.gridy = row++
        gbc.gridwidth = 2
        panel.add(autoStartCheckbox, gbc)
        gbc.gridwidth = 1

        // Separator
        addSeparator(panel, gbc, row++)

        // Enable Auth
        enableAuthCheckbox = JCheckBox("Enable authentication", settings.enableAuth)
        gbc.gridx = 0
        gbc.gridy = row++
        gbc.gridwidth = 2
        panel.add(enableAuthCheckbox, gbc)
        gbc.gridwidth = 1

        // Auth Token
        addLabel(panel, gbc, row, "Bearer Token:")
        authTokenField = JPasswordField(settings.authToken, 30)
        addComponent(panel, gbc, row++, authTokenField)

        // Separator
        addSeparator(panel, gbc, row++)

        // Enable CORS
        enableCorsCheckbox = JCheckBox("Enable CORS", settings.enableCors)
        gbc.gridx = 0
        gbc.gridy = row++
        gbc.gridwidth = 2
        panel.add(enableCorsCheckbox, gbc)
        gbc.gridwidth = 1

        // CORS Origins
        addLabel(panel, gbc, row, "CORS Allowed Origins:")
        corsOriginsField = JTextField(settings.corsAllowedOrigins, 30)
        addComponent(panel, gbc, row++, corsOriginsField)

        // Separator
        addSeparator(panel, gbc, row++)

        // Allowed IPs
        addLabel(panel, gbc, row, "Allowed IPs (comma-separated):")
        allowedIpsField = JTextField(settings.allowedIps, 30)
        addComponent(panel, gbc, row++, allowedIpsField)

        // Request Timeout
        addLabel(panel, gbc, row, "Request Timeout (seconds):")
        requestTimeoutField = JTextField(settings.requestTimeout.toString(), 10)
        addComponent(panel, gbc, row++, requestTimeoutField)

        // Verbose Logging
        verboseLoggingCheckbox = JCheckBox("Enable verbose logging", settings.verboseLogging)
        gbc.gridx = 0
        gbc.gridy = row++
        gbc.gridwidth = 2
        panel.add(verboseLoggingCheckbox, gbc)

        // Fill remaining space
        gbc.gridy = row
        gbc.weighty = 1.0
        panel.add(JPanel(), gbc)

        settingsPanel = panel
        return panel
    }

    private fun addLabel(panel: JPanel, gbc: GridBagConstraints, row: Int, text: String) {
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel(text), gbc)
    }

    private fun addComponent(panel: JPanel, gbc: GridBagConstraints, row: Int, component: JComponent) {
        gbc.gridx = 1
        gbc.gridy = row
        gbc.weightx = 1.0
        panel.add(component, gbc)
    }

    private fun addSeparator(panel: JPanel, gbc: GridBagConstraints, row: Int) {
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 2
        panel.add(JSeparator(), gbc)
        gbc.gridwidth = 1
    }

    override fun isModified(): Boolean {
        return portField.text.toIntOrNull() != settings.port ||
                hostField.text != settings.host ||
                autoStartCheckbox.isSelected != settings.autoStart ||
                enableAuthCheckbox.isSelected != settings.enableAuth ||
                String(authTokenField.password) != settings.authToken ||
                enableCorsCheckbox.isSelected != settings.enableCors ||
                corsOriginsField.text != settings.corsAllowedOrigins ||
                allowedIpsField.text != settings.allowedIps ||
                requestTimeoutField.text.toIntOrNull() != settings.requestTimeout ||
                verboseLoggingCheckbox.isSelected != settings.verboseLogging
    }

    override fun apply() {
        settings.port = portField.text.toIntOrNull() ?: 58888
        settings.host = hostField.text
        settings.autoStart = autoStartCheckbox.isSelected
        settings.enableAuth = enableAuthCheckbox.isSelected
        settings.authToken = String(authTokenField.password)
        settings.enableCors = enableCorsCheckbox.isSelected
        settings.corsAllowedOrigins = corsOriginsField.text
        settings.allowedIps = allowedIpsField.text
        settings.requestTimeout = requestTimeoutField.text.toIntOrNull() ?: 30
        settings.verboseLogging = verboseLoggingCheckbox.isSelected
    }

    override fun reset() {
        portField.text = settings.port.toString()
        hostField.text = settings.host
        autoStartCheckbox.isSelected = settings.autoStart
        enableAuthCheckbox.isSelected = settings.enableAuth
        authTokenField.text = settings.authToken
        enableCorsCheckbox.isSelected = settings.enableCors
        corsOriginsField.text = settings.corsAllowedOrigins
        allowedIpsField.text = settings.allowedIps
        requestTimeoutField.text = settings.requestTimeout.toString()
        verboseLoggingCheckbox.isSelected = settings.verboseLogging
    }

    override fun disposeUIResources() {
        settingsPanel = null
    }
}
