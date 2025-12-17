package com.example.kingasec

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kingasec.databinding.ActivityMainBinding
import com.example.kingasec.databinding.ItemRiskyAppBinding
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var riskyAppsAdapter: RiskyAppsAdapter
    private lateinit var appScanner: AppScannerService
    private lateinit var mlProcessor: MLDataProcessor

    // Cached scan results
    private var scannedApps: List<RiskyApp> = emptyList()
    private var isScanning = false

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
        val mediumRiskApps = scannedApps.filter { it.riskLevel == RiskLevel.MEDIUM }

        animateCounterText(binding.textViewHighRiskCount, highRiskApps.size)
        animateCounterText(binding.textViewFlaggedCount, highRiskApps.size + mediumRiskApps.size)

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
                val scannedData = appScanner.scanAllApps(includeSystemApps = false)
                showToast("Scanned ${scannedData.size} apps. Analyzing permissions...")

                // Asynchronously process all apps
                scannedApps = scannedData.map { app ->
                    async { processScannedApp(app) }
                }.awaitAll()

                // Sort by risk level (HIGH > MEDIUM > LOW)
                scannedApps = scannedApps.sortedWith(
                    compareBy<RiskyApp> { it.riskLevel.ordinal }
                        .thenByDescending { it.privacyRiskScore }
                )

                riskyAppsAdapter.updateApps(scannedApps)
                updateDashboard()

                ScanResultsManager.setScanResults(
                    scannedApps.map { app ->
                        ScanResultsManager.ScannedAppResult(
                            appName = app.appName,
                            packageName = app.packageName,
                            reason = app.reason,
                            riskLevel = app.riskLevel.name,
                            iconResId = app.iconResId,
                            permissions = app.permissions,
                            privacyRiskScore = app.privacyRiskScore
                        )
                    }
                )

                showToast("Scan complete! ${scannedApps.size} apps analyzed.")

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


    private suspend fun processScannedApp(app: AppScannerService.ScannedApp): RiskyApp {
        // Master exclusion list to immediately classify trusted apps as LOW risk.
        if (mlProcessor.isAppExcluded(app)) {
            return RiskyApp(
                appName = app.appName,
                packageName = app.packageName,
                reason = "This is a trusted application. Its permissions are appropriate for its functions.",
                riskLevel = RiskLevel.LOW,
                iconResId = R.drawable.ic_app_placeholder,
                permissions = app.dangerousPermissions.map { it.removePrefix("android.permission.") },
                privacyRiskScore = 1.0f
            )
        }

        // If not excluded, proceed with the normal analysis pipeline.
        val riskScore = mlProcessor.predictRiskScore(app)
        val riskLevel = mlProcessor.scoreToRiskLevel(riskScore)

        var reason = mlProcessor.generateRiskDescription(app, riskScore)

        // For high and medium risk apps, get a more detailed reason from the cloud.
        if (riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.MEDIUM) {
            val enhancedReason = ContextChecker.getRiskAnalysis(
                appName = app.appName,
                appCategory = app.category,
                permissions = app.dangerousPermissions
            )
            if (enhancedReason != null) {
                reason = enhancedReason
            }
        }

        return RiskyApp(
            appName = app.appName,
            packageName = app.packageName,
            reason = reason,
            riskLevel = riskLevel,
            iconResId = R.drawable.ic_app_placeholder,
            permissions = app.dangerousPermissions.map { it.removePrefix("android.permission.") },
            privacyRiskScore = riskScore
        )
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
        if (scannedApps.isEmpty()) {
            showToast("No apps scanned yet. Please run a scan first.")
            return
        }

        val intent = Intent(this, AllAppsActivity::class.java)
        startActivity(intent)
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun animateCardEntrance() {
        val cards = listOf(binding.cardViewScore, binding.buttonScan, binding.recyclerViewRiskyApps)
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
        val animator = ValueAnimator.ofInt(textView.text.toString().toIntOrNull() ?: 0, targetValue)
        animator.duration = 1000
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            textView.text = animation.animatedValue.toString()
        }
        animator.start()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

class RiskyAppsAdapter(
    private var apps: List<RiskyApp>,
    private val onItemClick: (RiskyApp) -> Unit
) : RecyclerView.Adapter<RiskyAppsAdapter.ViewHolder>() {

    class ViewHolder(private val binding: ItemRiskyAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: RiskyApp, onItemClick: (RiskyApp) -> Unit) {
            binding.textViewAppName.text = app.appName
            binding.textViewRiskReason.text = app.reason
            binding.textViewRiskLevel.text = app.riskLevel.displayName
            binding.imageViewAppIcon.setImageResource(app.iconResId)

            val context = binding.root.context
            binding.textViewRiskLevel.setTextColor(ContextCompat.getColor(context, app.riskLevel.colorResId))
            binding.cardViewRiskBadge.setCardBackgroundColor(ContextCompat.getColor(context, app.riskLevel.bgColorResId))

            binding.root.setOnClickListener { onItemClick(app) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRiskyAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(apps[position], onItemClick)
    }

    override fun getItemCount() = apps.size

    fun updateApps(newApps: List<RiskyApp>) {
        apps = newApps
        notifyDataSetChanged()
    }
}