package com.ivazplus.calleragent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Settings screen - the Android equivalent of SettingsDialog in
 * desktop_client.py. Save writes to SharedPreferences, then asks for the
 * runtime permissions needed to read the incoming caller's number, then
 * starts the always-on foreground service that actually listens for calls.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var hostEdit: EditText
    private lateinit var portEdit: EditText
    private lateinit var secureCheck: CheckBox
    private lateinit var keyEdit: EditText
    private lateinit var operatorEdit: EditText
    private lateinit var statusLabel: TextView

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            startRelayService()
        } else {
            statusLabel.setTextColor(0xFFC62828.toInt())
            statusLabel.text =
                "بدون مجوزهای «تماس‌ها» و «گزارش تماس»، برنامه نمی‌تواند شماره تماس‌گیرنده را ببیند. " +
                "دوباره «ذخیره و شروع» را بزنید و این‌بار مجوزها را تایید کنید."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hostEdit = findViewById(R.id.hostEdit)
        portEdit = findViewById(R.id.portEdit)
        secureCheck = findViewById(R.id.secureCheck)
        keyEdit = findViewById(R.id.keyEdit)
        operatorEdit = findViewById(R.id.operatorEdit)
        statusLabel = findViewById(R.id.statusLabel)

        val prefs = Prefs.get(this)
        hostEdit.setText(prefs.getString("host", ""))
        portEdit.setText(prefs.getInt("port", 8000).toString())
        secureCheck.isChecked = prefs.getBoolean("secure", false)
        keyEdit.setText(prefs.getString("api_key", ""))
        operatorEdit.setText(prefs.getString("operator", ""))

        findViewById<Button>(R.id.saveButton).setOnClickListener { saveAndStart() }

        if (Prefs.isConfigured(this)) {
            statusLabel.setTextColor(0xFF2E7D32.toInt())
            statusLabel.text = "قبلاً تنظیم شده. برای اعمال تغییرات دوباره «ذخیره و شروع» را بزنید."
        }
    }

    private fun saveAndStart() {
        val host = hostEdit.text.toString().trim()
        val port = portEdit.text.toString().trim().toIntOrNull() ?: 8000
        val key = keyEdit.text.toString().trim()

        if (host.isEmpty() || key.isEmpty()) {
            statusLabel.setTextColor(0xFFC62828.toInt())
            statusLabel.text = "آدرس سرور و کلید API الزامی است."
            return
        }

        Prefs.get(this).edit()
            .putString("host", host)
            .putInt("port", port)
            .putBoolean("secure", secureCheck.isChecked)
            .putString("api_key", key)
            .putString("operator", operatorEdit.text.toString().trim())
            .apply()

        requestNeededPermissions()
    }

    private fun requestNeededPermissions() {
        val needed = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            startRelayService()
        } else {
            requestPermissionsLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startRelayService() {
        val intent = Intent(this, CallRelayService::class.java)
        ContextCompat.startForegroundService(this, intent)
        statusLabel.setTextColor(0xFF2E7D32.toInt())
        statusLabel.text = "در حال گوش دادن به تماس‌های ورودی ✔"
    }
}
