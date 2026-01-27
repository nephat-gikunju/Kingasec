package com.example.kingasec

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kingasec.databinding.ItemRiskyAppBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class AllAppsActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerViewAllApps: RecyclerView
    private lateinit var chipGroupFilter: ChipGroup
    private lateinit var chipAll: Chip
    private lateinit var chipHigh: Chip
    private lateinit var chipMedium: Chip
    private lateinit var chipLow: Chip
    private lateinit var textViewStats: TextView

    private lateinit var allAppsAdapter: AllAppsAdapter
    private var allApps: List<AppData> = emptyList()
    private var filteredApps: List<AppData> = emptyList()

    data class AppData(
        val appName: String,
        val packageName: String,
        val reason: String,
        val riskLevel: RiskLevel,
        val iconResId: Int,
        val permissions: List<String>,
        val privacyRiskScore: Float
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_apps)

        initViews()
        setupToolbar()
        loadAppsData()
        setupRecyclerView()
        setupFilterChips()
        updateStats()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerViewAllApps = findViewById(R.id.recyclerView_all_apps)
        chipGroupFilter = findViewById(R.id.chipGroup_filter)
        chipAll = findViewById(R.id.chip_all)
        chipHigh = findViewById(R.id.chip_high)
        chipMedium = findViewById(R.id.chip_medium)
        chipLow = findViewById(R.id.chip_low)
        textViewStats = findViewById(R.id.textView_stats)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "All Scanned Apps"

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun loadAppsData() {
        // Load from ScanResultsManager and sort the data
        val results = ScanResultsManager.getScanResults()

        allApps = results.map { result ->
            AppData(
                appName = result.appName,
                packageName = result.packageName,
                reason = result.reason,
                riskLevel = when (result.riskLevel) {
                    "HIGH" -> RiskLevel.HIGH
                    "MEDIUM" -> RiskLevel.MEDIUM
                    "LOW" -> RiskLevel.LOW
                    else -> RiskLevel.LOW
                },
                iconResId = result.iconResId,
                permissions = result.permissions,
                privacyRiskScore = result.privacyRiskScore
            )
        }.sortedWith(
            compareBy<AppData> { it.riskLevel.ordinal }
                .thenByDescending { it.privacyRiskScore }
        )

        filteredApps = allApps
    }

    private fun setupRecyclerView() {
        allAppsAdapter = AllAppsAdapter(filteredApps) { app ->
            openAppDetails(app)
        }

        recyclerViewAllApps.apply {
            layoutManager = LinearLayoutManager(this@AllAppsActivity)
            adapter = allAppsAdapter
        }
    }

    private fun setupFilterChips() {
        chipAll.setOnClickListener {
            filterApps(null)
        }

        chipHigh.setOnClickListener {
            filterApps(RiskLevel.HIGH)
        }

        chipMedium.setOnClickListener {
            filterApps(RiskLevel.MEDIUM)
        }

        chipLow.setOnClickListener {
            filterApps(RiskLevel.LOW)
        }

        // Set default selection
        chipAll.isChecked = true
    }

    private fun filterApps(riskLevel: RiskLevel?) {
        filteredApps = if (riskLevel == null) {
            allApps
        } else {
            allApps.filter { it.riskLevel == riskLevel }
        }

        allAppsAdapter.updateApps(filteredApps)
        updateStats()
    }

    private fun updateStats() {
        val highCount = allApps.count { it.riskLevel == RiskLevel.HIGH }
        val mediumCount = allApps.count { it.riskLevel == RiskLevel.MEDIUM }
        val lowCount = allApps.count { it.riskLevel == RiskLevel.LOW }

        textViewStats.text = "Total: ${allApps.size} apps | " +
                "High Risk: $highCount | " +
                "Medium: $mediumCount | " +
                "Low: $lowCount"

        // Update chip badges
        chipHigh.text = "High Risk ($highCount)"
        chipMedium.text = "Medium ($mediumCount)"
        chipLow.text = "Low ($lowCount)"
        chipAll.text = "All (${allApps.size})"
    }

    private fun openAppDetails(app: AppData) {
        val intent = Intent(this, AppDetailsActivity::class.java).apply {
            putExtra(AppDetailsActivity.EXTRA_APP_NAME, app.appName)
            putExtra(AppDetailsActivity.EXTRA_PACKAGE_NAME, app.packageName)
            putExtra(AppDetailsActivity.EXTRA_RISK_LEVEL, app.riskLevel.name)
        }
        startActivity(intent)
    }
}

class AllAppsAdapter(
    private var apps: List<AllAppsActivity.AppData>,
    private val onItemClick: (AllAppsActivity.AppData) -> Unit
) : RecyclerView.Adapter<AllAppsAdapter.ViewHolder>() {

    class ViewHolder(private val binding: ItemRiskyAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AllAppsActivity.AppData, onItemClick: (AllAppsActivity.AppData) -> Unit) {
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

    fun updateApps(newApps: List<AllAppsActivity.AppData>) {
        apps = newApps
        notifyDataSetChanged()
    }
}