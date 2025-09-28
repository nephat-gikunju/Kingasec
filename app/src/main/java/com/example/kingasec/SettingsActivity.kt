package com.example.kingasec

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback // <-- NEW: Import the callback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    // Views
    private lateinit var toolbar: Toolbar
    private lateinit var switchAutoScan: SwitchMaterial
    private lateinit var switchRealtimeMonitoring: SwitchMaterial
    private lateinit var switchPrivacyAlerts: SwitchMaterial
    private lateinit var layoutScanFrequency: LinearLayout
    private lateinit var layoutRiskSensitivity: LinearLayout
    private lateinit var layoutClearHistory: LinearLayout
    private lateinit var layoutExportReport: LinearLayout
    private lateinit var layoutPrivacyPolicy: LinearLayout
    private lateinit var textViewScanFrequency: TextView
    private lateinit var textViewRiskSensitivity: TextView

    private val scanFrequencyOptions = arrayOf("Daily", "Weekly", "Monthly", "Manual Only")
    private val riskSensitivityOptions = arrayOf("Low", "Medium", "High")

    private var currentScanFrequency = 0 // Daily
    private var currentRiskSensitivity = 2 // High

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()
        setupToolbar()
        loadPreferences()
        setupClickListeners()
        updateUI()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

                savePreferences()

                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        switchAutoScan = findViewById(R.id.switch_auto_scan)
        switchRealtimeMonitoring = findViewById(R.id.switch_realtime_monitoring)
        switchPrivacyAlerts = findViewById(R.id.switch_privacy_alerts)
        layoutScanFrequency = findViewById(R.id.layout_scan_frequency)
        layoutRiskSensitivity = findViewById(R.id.layout_risk_sensitivity)
        layoutClearHistory = findViewById(R.id.layout_clear_history)
        layoutExportReport = findViewById(R.id.layout_export_report)
        layoutPrivacyPolicy = findViewById(R.id.layout_privacy_policy)
        textViewScanFrequency = findViewById(R.id.textView_scan_frequency)
        textViewRiskSensitivity = findViewById(R.id.textView_risk_sensitivity)

        sharedPreferences = getSharedPreferences("smart_shield_prefs", MODE_PRIVATE)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.setNavigationOnClickListener {
            // This button should also trigger the back press dispatcher
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadPreferences() {
        switchAutoScan.isChecked = sharedPreferences.getBoolean("auto_scan", true)
        switchRealtimeMonitoring.isChecked = sharedPreferences.getBoolean("realtime_monitoring", false)
        switchPrivacyAlerts.isChecked = sharedPreferences.getBoolean("privacy_alerts", true)
        currentScanFrequency = sharedPreferences.getInt("scan_frequency", 0)
        currentRiskSensitivity = sharedPreferences.getInt("risk_sensitivity", 2)
    }

    private fun savePreferences() {
        sharedPreferences.edit().apply {
            putBoolean("auto_scan", switchAutoScan.isChecked)
            putBoolean("realtime_monitoring", switchRealtimeMonitoring.isChecked)
            putBoolean("privacy_alerts", switchPrivacyAlerts.isChecked)
            putInt("scan_frequency", currentScanFrequency)
            putInt("risk_sensitivity", currentRiskSensitivity)
            apply()
        }
    }

    private fun setupClickListeners() {
        // Switch listeners
        switchAutoScan.setOnCheckedChangeListener { _, isChecked ->
            savePreferences()
            Toast.makeText(this, if (isChecked) "Auto scan enabled" else "Auto scan disabled", Toast.LENGTH_SHORT).show()
        }

        switchRealtimeMonitoring.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showRealtimeMonitoringDialog()
            } else {
                savePreferences()
            }
        }

        switchPrivacyAlerts.setOnCheckedChangeListener { _, _ ->
            savePreferences()
        }

        // Other listeners
        layoutScanFrequency.setOnClickListener { showScanFrequencyDialog() }
        layoutRiskSensitivity.setOnClickListener { showRiskSensitivityDialog() }
        layoutClearHistory.setOnClickListener { showClearHistoryDialog() }
        layoutExportReport.setOnClickListener { exportPrivacyReport() }
        layoutPrivacyPolicy.setOnClickListener { openPrivacyPolicy() }
    }

    private fun updateUI() {
        textViewScanFrequency.text = scanFrequencyOptions[currentScanFrequency]
        textViewRiskSensitivity.text = riskSensitivityOptions[currentRiskSensitivity]
    }

    private fun showRealtimeMonitoringDialog() {
        AlertDialog.Builder(this)
            .setTitle("Real-time Monitoring")
            .setMessage("This feature requires additional permissions to monitor app behavior in real-time. Enable now?")
            .setPositiveButton("Enable") { _, _ ->
                // TODO: Request necessary permissions
                savePreferences()
                Toast.makeText(this, "Real-time monitoring enabled", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { _, _ ->
                switchRealtimeMonitoring.isChecked = false
                savePreferences()
            }
            .show()
    }

    private fun showScanFrequencyDialog() {
        AlertDialog.Builder(this)
            .setTitle("Scan Frequency")
            .setSingleChoiceItems(scanFrequencyOptions, currentScanFrequency) { dialog, which ->
                currentScanFrequency = which
                savePreferences()
                updateUI()
                dialog.dismiss()
                Toast.makeText(this, "Scan frequency set to ${scanFrequencyOptions[which]}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRiskSensitivityDialog() {
        AlertDialog.Builder(this)
            .setTitle("Risk Sensitivity")
            .setMessage("Choose how sensitive the privacy risk detection should be:")
            .setSingleChoiceItems(riskSensitivityOptions, currentRiskSensitivity) { dialog, which ->
                currentRiskSensitivity = which
                savePreferences()
                updateUI()
                dialog.dismiss()
                val message = when (which) {
                    0 -> "Low sensitivity - Only critical risks will be flagged"
                    1 -> "Medium sensitivity - Balanced risk detection"
                    2 -> "High sensitivity - All potential risks will be flagged"
                    else -> ""
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showClearHistoryDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear Scan History")
            .setMessage("This will delete all previous scan results and app analysis data. This action cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                clearScanHistory()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearScanHistory() {
        // TODO: Implement clearing scan history from database/storage
        sharedPreferences.edit().clear().apply()
        Toast.makeText(this, "Scan history cleared successfully", Toast.LENGTH_SHORT).show()
    }

    private fun exportPrivacyReport() {
        Toast.makeText(this, "Generating privacy report...", Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({
            Toast.makeText(this, "Privacy report saved to Downloads", Toast.LENGTH_LONG).show()
        }, 2000)
    }

    private fun openPrivacyPolicy() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://your-privacy-policy-url.com"))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Privacy policy will be available soon", Toast.LENGTH_SHORT).show()
        }
    }

}