package com.example.kingasec

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kingasec.databinding.ActivityMainBinding
import com.example.kingasec.databinding.ItemRiskyAppBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var riskyAppsAdapter: RiskyAppsAdapter

    // Enhanced data models
    data class RiskyApp(
        val appName: String,
        val reason: String,
        val riskLevel: RiskLevel,
        val iconResId: Int,
        val permissions: List<String> = emptyList()
    )

    enum class RiskLevel(val displayName: String, val colorResId: Int, val bgColorResId: Int) {
        HIGH("HIGH", R.color.risk_high, R.color.risk_high_bg),
        MEDIUM("MEDIUM", R.color.risk_medium, R.color.risk_medium_bg),
        LOW("LOW", R.color.risk_low, R.color.risk_low_bg)
    }

    // Enhanced dummy data
    private val dummyRiskyApps = listOf(
        RiskyApp(
            appName = "Tala",
            reason = "Accesses contacts, SMS, and location data extensively",
            riskLevel = RiskLevel.HIGH,
            iconResId = R.drawable.ic_app_placeholder,
            permissions = listOf("CONTACTS", "SMS", "LOCATION")
        ),
        RiskyApp(
            appName = "Zenka",
            reason = "Unnecessary SMS permissions for loan app",
            riskLevel = RiskLevel.HIGH,
            iconResId = R.drawable.ic_app_placeholder,
            permissions = listOf("SMS", "CONTACTS")
        ),
        RiskyApp(
            appName = "Truecaller",
            reason = "High permissions but justified for caller ID",
            riskLevel = RiskLevel.MEDIUM,
            iconResId = R.drawable.ic_app_placeholder,
            permissions = listOf("CONTACTS", "PHONE", "SMS")
        ),
        RiskyApp(
            appName = "WhatsApp",
            reason = "Standard permissions for messaging app",
            riskLevel = RiskLevel.LOW,
            iconResId = R.drawable.ic_app_placeholder,
            permissions = listOf("CONTACTS", "CAMERA", "MICROPHONE")
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        updateDashboard()
        setupClickListeners()

        // Add entrance animations
        animateCardEntrance()
    }

    private fun setupUI() {
        // Make status bar transparent or matching
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary_dark)
    }

    private fun setupRecyclerView() {
        riskyAppsAdapter = RiskyAppsAdapter(dummyRiskyApps) { app ->
            showAppDetails(app)
        }

        binding.recyclerViewRiskyApps.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = riskyAppsAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun updateDashboard() {
        val highRiskApps = dummyRiskyApps.filter { it.riskLevel == RiskLevel.HIGH }
        val flaggedAppsCount = dummyRiskyApps.size

        animateCounterText(binding.textViewHighRiskCount, highRiskApps.size)
        animateCounterText(binding.textViewFlaggedCount, flaggedAppsCount)

        updateRiskLevelDisplay(highRiskApps.size)
    }

    private fun updateRiskLevelDisplay(highRiskCount: Int) {
        when {
            highRiskCount >= 3 -> {
                binding.textViewRiskLevel.text = "HIGH"
                binding.textViewScoreValue.text = "High Risk Detected"
                binding.cardViewRiskCircle.setCardBackgroundColor(
                    ContextCompat.getColor(this, R.color.risk_high_bg)
                )
                binding.imageViewRiskIcon.setImageResource(R.drawable.ic_warning_red)
            }
            highRiskCount >= 1 -> {
                binding.textViewRiskLevel.text = "MEDIUM"
                binding.textViewScoreValue.text = "Medium Risk Level"
                binding.cardViewRiskCircle.setCardBackgroundColor(
                    ContextCompat.getColor(this, R.color.risk_medium_bg)
                )
                binding.imageViewRiskIcon.setImageResource(R.drawable.ic_warning_orange)
            }
            else -> {
                binding.textViewRiskLevel.text = "LOW"
                binding.textViewScoreValue.text = "Good Privacy Status"
                binding.cardViewRiskCircle.setCardBackgroundColor(
                    ContextCompat.getColor(this, R.color.risk_low_bg)
                )
                binding.imageViewRiskIcon.setImageResource(R.drawable.ic_shield_check)
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonScan.setOnClickListener {
            startScan()
        }

        binding.buttonOptimize.setOnClickListener {
            optimizePrivacySettings()
        }

        binding.textViewViewAll.setOnClickListener {
            showAllApps()
        }

        binding.imageViewSettings.setOnClickListener {
            openSettings()
        }
    }

    private fun startScan() {
        binding.buttonScan.isEnabled = false
        binding.buttonScan.text = "SCANNING..."

        binding.buttonScan.postDelayed({
            binding.buttonScan.isEnabled = true
            binding.buttonScan.text = "SCAN NOW"
            updateDashboard()
            showToast("Scan completed - ${dummyRiskyApps.size} apps analyzed")
        }, 2000)
    }

    private fun optimizePrivacySettings() {
        showToast("Privacy optimization feature coming soon!")
    }

    private fun showAppDetails(app: RiskyApp) {
        val intent = Intent(this, AppDetailsActivity::class.java).apply {
            putExtra(AppDetailsActivity.EXTRA_APP_NAME, app.appName)
            putExtra(AppDetailsActivity.EXTRA_PACKAGE_NAME, "com.example.${app.appName.lowercase()}")
            putExtra(AppDetailsActivity.EXTRA_RISK_LEVEL, app.riskLevel.name)
        }
        startActivity(intent)
    }

    private fun showAllApps() {
        showToast("Showing all ${dummyRiskyApps.size} analyzed apps")
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun animateCardEntrance() {
        val cards = listOf(
            binding.cardViewScore,
            binding.buttonScan,
            binding.recyclerViewRiskyApps
        )

        cards.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 100f

            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(index * 150L)
                .setDuration(500)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun animateCounterText(textView: TextView, targetValue: Int) {
        ValueAnimator.ofInt(0, targetValue).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                textView.text = animation.animatedValue.toString()
            }
            start()
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}

class RiskyAppsAdapter(
    private val apps: List<MainActivity.RiskyApp>,
    private val onItemClick: (MainActivity.RiskyApp) -> Unit
) : RecyclerView.Adapter<RiskyAppsAdapter.ViewHolder>() {

    class ViewHolder(private val binding: ItemRiskyAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: MainActivity.RiskyApp, onItemClick: (MainActivity.RiskyApp) -> Unit) {
            binding.textViewAppName.text = app.appName
            binding.textViewRiskReason.text = app.reason
            binding.textViewRiskLevel.text = app.riskLevel.displayName
            binding.imageViewAppIcon.setImageResource(app.iconResId)

            val context = binding.root.context
            binding.textViewRiskLevel.setTextColor(
                ContextCompat.getColor(context, app.riskLevel.colorResId)
            )
            binding.cardViewRiskBadge.setCardBackgroundColor(
                ContextCompat.getColor(context, app.riskLevel.bgColorResId)
            )

            if (app.permissions.isNotEmpty()) {
                binding.linearLayoutPermissions.visibility = View.VISIBLE
            } else {
                binding.linearLayoutPermissions.visibility = View.GONE
            }

            binding.root.setOnClickListener { onItemClick(app) }

            binding.root.alpha = 0.8f
            binding.root.animate().alpha(1f).setDuration(200).start()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRiskyAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(apps[position], onItemClick)
    }

    override fun getItemCount() = apps.size
}

// Placeholder for AppDetailsActivity constants (should be in AppDetailsActivity.kt)
object AppDetails {
    const val EXTRA_APP_NAME = "extra_app_name"
    const val EXTRA_PACKAGE_NAME = "extra_package_name"
    const val EXTRA_RISK_LEVEL = "extra_risk_level"
}