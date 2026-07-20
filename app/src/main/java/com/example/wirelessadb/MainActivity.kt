package com.example.wirelessadb

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.DataOutputStream
import java.text.MessageFormat

class MainActivity : AppCompatActivity() {

    private val adbPort = 5555
    private var isEnabled = false

    private lateinit var statusText: TextView
    private lateinit var ipText: TextView
    private lateinit var portText: TextView
    private lateinit var commandText: TextView
    private lateinit var toggleButton: Button
    private lateinit var copyButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        ipText = findViewById(R.id.ipText)
        portText = findViewById(R.id.portText)
        commandText = findViewById(R.id.commandText)
        toggleButton = findViewById(R.id.toggleButton)
        copyButton = findViewById(R.id.copyButton)

        portText.text = getString(R.string.port_label) + " " + adbPort
        refreshIp()

        toggleButton.setOnClickListener {
            if (isEnabled) disableAdbTcp() else enableAdbTcp()
        }

        copyButton.setOnClickListener {
            val ip = getDeviceIp()
            if (ip != null) {
                val cmd = "adb connect $ip:$adbPort"
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("adb_command", cmd))
                Toast.makeText(this, getString(R.string.copied), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshIp() {
        val ip = getDeviceIp()
        if (ip == null) {
            ipText.text = getString(R.string.no_wifi)
            commandText.text = ""
        } else {
            ipText.text = getString(R.string.ip_label) + " " + ip
            commandText.text = MessageFormat.format(
                getString(R.string.connect_hint), ip, adbPort.toString()
            )
        }
    }

    private fun getDeviceIp(): String? {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipInt = wifiManager.connectionInfo.ipAddress
        if (ipInt == 0) return null
        return String.format(
            "%d.%d.%d.%d",
            ipInt and 0xff,
            ipInt shr 8 and 0xff,
            ipInt shr 16 and 0xff,
            ipInt shr 24 and 0xff
        )
    }

    /**
     * Runs a shell command as root using su, waits for completion,
     * and returns true if the exit code was 0.
     */
    private fun runAsRoot(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
            os.close()
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun enableAdbTcp() {
        val ok = runAsRoot("setprop service.adb.tcp.port $adbPort && stop adbd && start adbd")
        if (!ok) {
            Toast.makeText(this, getString(R.string.root_denied), Toast.LENGTH_LONG).show()
            return
        }
        isEnabled = true
        statusText.text = getString(R.string.status_enabled)
        statusText.setTextColor(getColor(R.color.green_status))
        toggleButton.text = getString(R.string.btn_disable)
        refreshIp()
    }

    private fun disableAdbTcp() {
        val ok = runAsRoot("setprop service.adb.tcp.port -1 && stop adbd && start adbd")
        if (!ok) {
            Toast.makeText(this, getString(R.string.root_denied), Toast.LENGTH_LONG).show()
            return
        }
        isEnabled = false
        statusText.text = getString(R.string.status_disabled)
        statusText.setTextColor(getColor(R.color.red_status))
        toggleButton.text = getString(R.string.btn_enable)
    }
}
