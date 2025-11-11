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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kingasec.databinding.ActivityMainBinding
import com.example.kingasec.databinding.ItemRiskyAppBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var riskyAppsAdapter: RiskyAppsAdapter
    private lateinit var appScanner: AppScannerService
    private lateinit var mlProcessor: MLDataProcessor

    // Cached scan results
    private var scannedApps: List<RiskyApp> = emptyList()
    private var isScanning = false

    // Enhanced data models
    data class RiskyApp(
        val appName: String,
        val packageName: String,
        val reason: String,
        val riskLevel: RiskLevel,
        val iconResId: Int,
        val permissions: List<String> = emptyList(),
        val privacyRiskScore: Float = 0f
    )

    enum class RiskLevel(val displayName: String, val colorResId: Int, val bgColorResId: Int) {
        HIGH("HIGH", R.color.risk_high, R.color.risk_high_bg),
        MEDIUM("MEDIUM", R.color.risk_medium, R.color.risk_medium_bg),
        LOW("LOW", R.color.risk_low, R.color.risk_low_bg)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize services
        appScanner = AppScannerService(this)
        mlProcessor = MLDataProcessor(this)

        setupUI()
        setupRecyclerView()
        setupClickListeners()

        // Load cached results or show empty state
        loadCachedResults()

        // Add entrance animations
        animateCardEntrance()
    }

    private fun setupUI() {
        // Make status bar transparent or matching
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary_dark)
    }

    private fun setupRecyclerView() {
        riskyAppsAdapter = RiskyAppsAdapter(scannedApps) { app ->
            showAppDetails(app)
        }

        binding.recyclerViewRiskyApps.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = riskyAppsAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun loadCachedResults() {
        // TODO: Load from database/shared preferences if you want to cache
        // For now, just update UI with empty state
        updateDashboard()
    }

    private fun updateDashboard() {
        val highRiskApps = scannedApps.filter { it.riskLevel == RiskLevel.HIGH }
        val flaggedAppsCount = scannedApps.size

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
            if (!isScanning) {
                startScan()
            }
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
        isScanning = true
        binding.buttonScan.isEnabled = false
        binding.buttonScan.text = "SCANNING..."

        lifecycleScope.launch {
            try {
                // Scan all non-system apps
                val scannedData = appScanner.scanAllApps(includeSystemApps = false)

                showToast("Scanned ${scannedData.size} apps")

                // Process apps and calculate risk scores
                // For now, we'll use a simple heuristic until ML model is integrated
                scannedApps = scannedData.mapNotNull { app ->
                    processScannedApp(app)
                }

                // Sort by risk level (HIGH first)
                scannedApps = scannedApps.sortedWith(
                    compareByDescending<RiskyApp> { it.riskLevel.ordinal }
                        .thenByDescending { it.privacyRiskScore }
                )

                // Update UI
                riskyAppsAdapter.updateApps(scannedApps)
                updateDashboard()

                showToast("Scan completed - ${scannedApps.size} apps analyzed")

            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Scan failed: ${e.message}")
            } finally {
                isScanning = false
                binding.buttonScan.isEnabled = true
                binding.buttonScan.text = "SCAN NOW"
            }
        }
    }

    /**
     * Process a scanned app and calculate risk score
     * TODO: Replace this with actual ML model prediction
     */
    private fun processScannedApp(app: AppScannerService.ScannedApp): RiskyApp? {
        // Calculate a simple risk score based on dangerous permissions
        // This is temporary until ML model is integrated
        val riskScore = calculateRiskScore(app)

        // Only include apps with some risk
        if (riskScore < 2.0f) {
            return null
        }

        val riskLevel = when {
            riskScore >= 7.0f -> RiskLevel.HIGH
            riskScore >= 4.0f -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val reason = mlProcessor.generateRiskDescription(app, riskScore)

        return RiskyApp(
            appName = app.appName,
            packageName = app.packageName,
            reason = reason,
            riskLevel = riskLevel,
            iconResId = R.drawable.ic_app_placeholder, // You can save actual icon if needed
            permissions = app.dangerousPermissions.map {
                it.removePrefix("android.permission.")
            },
            privacyRiskScore = riskScore
        )
    }

    /**
     * Simple risk score calculation (temporary heuristic)
     * TODO: Replace with ML model prediction
     */
    private fun calculateRiskScore(app: AppScannerService.ScannedApp): Float {
        var score = 0f

        // Base score on dangerous permissions count
        score += app.dangerousPermissions.size * 0.8f

        // Add extra weight for particularly sensitive permissions
        val criticalPermissions = listOf(
            "READ_SMS", "SEND_SMS", "READ_CONTACTS",
            "READ_CALL_LOG", "ACCESS_FINE_LOCATION"
        )

        app.dangerousPermissions.forEach { perm ->
            if (criticalPermissions.any { perm.contains(it) }) {
                score += 1.0f
            }
        }

        // Adjust based on category
        when (app.category) {
            "Finance" -> {
                // Finance apps with SMS/Contacts are higher risk
                if (app.dangerousPermissions.any { it.contains("SMS") }) {
                    score += 2.0f
                }
                if (app.dangerousPermissions.any { it.contains("CONTACTS") }) {
                    score += 1.5f
                }
            }
            "Game" -> {
                // Games shouldn't need many permissions
                if (app.dangerousPermissions.size > 2) {
                    score += 2.0f
                }
            }
        }

        // Cap at 10
        return minOf(score, 10f)
    }

    private fun optimizePrivacySettings() {
        showToast("Privacy optimization feature coming soon!")
    }

    private fun showAppDetails(app: RiskyApp) {
        val intent = Intent(this, AppDetailsActivity::class.java).apply {
            putExtra(AppDetailsActivity.EXTRA_APP_NAME, app.appName)
            putExtra(AppDetailsActivity.EXTRA_PACKAGE_NAME, app.packageName)
            putExtra(AppDetailsActivity.EXTRA_RISK_LEVEL, app.riskLevel.name)
        }
        startActivity(intent)
    }

    private fun showAllApps() {
        showToast("Showing all ${scannedApps.size} analyzed apps")
        // TODO: Navigate to a full list view
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
    private var apps: List<MainActivity.RiskyApp>,
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

    fun updateApps(newApps: List<MainActivity.RiskyApp>) {
        apps = newApps
        notifyDataSetChanged()
    }
}