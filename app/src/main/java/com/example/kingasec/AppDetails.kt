package com.example.kingasec

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class AppDetailsActivity : AppCompatActivity() {

    // Views
    private lateinit var toolbar: Toolbar
    private lateinit var imageViewAppIconLarge: ImageView
    private lateinit var textViewAppNameLarge: TextView
    private lateinit var textViewPackageName: TextView
    private lateinit var textViewRiskLevelLarge: TextView
    private lateinit var cardViewRiskBadgeLarge: CardView
    private lateinit var textViewRiskDescription: TextView
    private lateinit var recyclerViewHighRiskPermissions: RecyclerView
    private lateinit var recyclerViewMediumRiskPermissions: RecyclerView
    private lateinit var recyclerViewSafePermissions: RecyclerView
    private lateinit var recyclerViewRecommendations: RecyclerView
    private lateinit var buttonOpenAppSettings: MaterialButton
    private lateinit var buttonUninstallApp: MaterialButton
    private lateinit var textViewInstallDate: TextView
    private lateinit var textViewLastUpdated: TextView
    private lateinit var textViewAppSize: TextView
    private lateinit var textViewAppVersion: TextView

    // Data
    private lateinit var appInfo: AppInfo
    private lateinit var permissionsAdapter: PermissionsAdapter // Changed
    private lateinit var recommendationsAdapter: RecommendationsAdapter // Changed

    data class AppInfo(
        val appName: String,
        val packageName: String,
        val riskLevel: RiskLevel,
        val iconResId: Int,
        val description: String,
        val installDate: String,
        val lastUpdated: String,
        val appSize: String,
        val version: String
    )

    data class Permission(
        val name: String,
        val description: String,
        val iconResId: Int,
        val riskLevel: RiskLevel
    )

    data class Recommendation(
        val title: String,
        val description: String,
        val priority: Priority,
        val iconResId: Int
    )

    enum class RiskLevel(val displayName: String, val colorResId: Int, val bgColorResId: Int) {
        HIGH("HIGH RISK", android.R.color.holo_red_dark, android.R.color.white),
        MEDIUM("MEDIUM RISK", android.R.color.holo_orange_dark, android.R.color.white),
        LOW("LOW RISK", android.R.color.holo_green_dark, android.R.color.white)
    }

    enum class Priority(val displayName: String, val colorResId: Int) {
        HIGH("HIGH", android.R.color.holo_red_dark),
        MEDIUM("MEDIUM", android.R.color.holo_orange_dark),
        LOW("LOW", android.R.color.holo_green_dark)
    }

    companion object {
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_RISK_LEVEL = "risk_level"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.app_details)

        initViews()
        setupToolbar()
        loadAppData()
        setupRecyclerViews()
        setupClickListeners()
        updateUI()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        imageViewAppIconLarge = findViewById(R.id.imageView_app_icon_large)
        textViewAppNameLarge = findViewById(R.id.textView_app_name_large)
        textViewPackageName = findViewById(R.id.textView_package_name)
        textViewRiskLevelLarge = findViewById(R.id.textView_risk_level_large)
        cardViewRiskBadgeLarge = findViewById(R.id.cardView_risk_badge_large)
        textViewRiskDescription = findViewById(R.id.textView_risk_description)
        recyclerViewHighRiskPermissions = findViewById(R.id.recyclerView_high_risk_permissions)
        recyclerViewMediumRiskPermissions = findViewById(R.id.recyclerView_medium_risk_permissions)
        recyclerViewSafePermissions = findViewById(R.id.recyclerView_safe_permissions)
        recyclerViewRecommendations = findViewById(R.id.recyclerView_recommendations)
        buttonOpenAppSettings = findViewById(R.id.button_open_app_settings)
        buttonUninstallApp = findViewById(R.id.button_uninstall_app)
        textViewInstallDate = findViewById(R.id.textView_install_date)
        textViewLastUpdated = findViewById(R.id.textView_last_updated)
        textViewAppSize = findViewById(R.id.textView_app_size)
        textViewAppVersion = findViewById(R.id.textView_app_version)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun loadAppData() {
        // Get data from intent
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "Unknown App"
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: "com.unknown.app"
        val riskLevelName = intent.getStringExtra(EXTRA_RISK_LEVEL) ?: "HIGH"

        val riskLevel = when (riskLevelName) {
            "HIGH" -> RiskLevel.HIGH
            "MEDIUM" -> RiskLevel.MEDIUM
            "LOW" -> RiskLevel.LOW
            else -> RiskLevel.HIGH
        }

        // Create app info (in real app, this would come from your database/analysis)
        appInfo = AppInfo(
            appName = appName,
            packageName = packageName,
            riskLevel = riskLevel,
            iconResId = android.R.drawable.ic_dialog_alert,
            description = when (riskLevel) {
                RiskLevel.HIGH -> "This app accesses sensitive data extensively and may pose privacy risks. Some permissions appear unnecessary for core functionality."
                RiskLevel.MEDIUM -> "This app requests several permissions that could impact privacy, but most appear justified for its functionality."
                RiskLevel.LOW -> "This app uses standard permissions appropriately and poses minimal privacy risks."
            },
            installDate = "Dec 15, 2024",
            lastUpdated = "Jan 10, 2025",
            appSize = "45.2 MB",
            version = "3.2.1"
        )

        // Load real app data if possible
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val appInfoPm = packageManager.getApplicationInfo(packageName, 0) // Renamed to avoid conflict with class member

            // Update with real data
            this.appInfo = this.appInfo.copy(
                version = packageInfo.versionName ?: "Unknown",
                installDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(Date(packageInfo.firstInstallTime)),
                lastUpdated = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(Date(packageInfo.lastUpdateTime))
                // Consider adding app size from packageInfo if available/needed
            )
        } catch (e: PackageManager.NameNotFoundException) {
            // App not found, use dummy data
        }
    }

    private fun setupRecyclerViews() {
        // Sample permissions data
        val highRiskPermissions = listOf(
            Permission("Access Contacts", "Read your contacts and call history", android.R.drawable.ic_menu_call, RiskLevel.HIGH),
            Permission("Read SMS", "Read your text messages", android.R.drawable.ic_dialog_email, RiskLevel.HIGH),
            Permission("Access Location", "Access your precise location", android.R.drawable.ic_dialog_map, RiskLevel.HIGH)
        )

        val mediumRiskPermissions = listOf(
            Permission("Camera", "Take pictures and record videos", android.R.drawable.ic_menu_camera, RiskLevel.MEDIUM),
            Permission("Microphone", "Record audio", android.R.drawable.ic_btn_speak_now, RiskLevel.MEDIUM)
        )

        val safePermissions = listOf(
            Permission("Internet", "Access network connections", android.R.drawable.ic_dialog_info, RiskLevel.LOW),
            Permission("Vibrate", "Control device vibration", android.R.drawable.ic_dialog_info, RiskLevel.LOW)
        )

        // Sample recommendations
        val recommendations = listOf(
            Recommendation(
                "Disable SMS Permission",
                "SMS access is not required for this loan app's core functionality",
                Priority.HIGH,
                android.R.drawable.ic_dialog_alert
            ),
            Recommendation(
                "Review Contact Access",
                "Consider if contact access is necessary for your use case",
                Priority.MEDIUM,
                android.R.drawable.ic_dialog_info
            ),
            Recommendation(
                "Enable App Permissions Review",
                "Regularly review and audit app permissions",
                Priority.LOW,
                android.R.drawable.ic_dialog_info
            )
        )

        // Setup adapters
        permissionsAdapter = PermissionsAdapter(highRiskPermissions) // Changed
        recyclerViewHighRiskPermissions.apply {
            layoutManager = LinearLayoutManager(this@AppDetailsActivity)
            adapter = permissionsAdapter
            isNestedScrollingEnabled = false
        }

        recyclerViewMediumRiskPermissions.apply {
            layoutManager = LinearLayoutManager(this@AppDetailsActivity)
            adapter = PermissionsAdapter(mediumRiskPermissions) // Changed
            isNestedScrollingEnabled = false
        }

        recyclerViewSafePermissions.apply {
            layoutManager = LinearLayoutManager(this@AppDetailsActivity)
            adapter = PermissionsAdapter(safePermissions) // Changed
            isNestedScrollingEnabled = false
        }

        recommendationsAdapter = RecommendationsAdapter(recommendations) // Changed
        recyclerViewRecommendations.apply {
            layoutManager = LinearLayoutManager(this@AppDetailsActivity)
            adapter = recommendationsAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        buttonOpenAppSettings.setOnClickListener {
            openAppSettings()
        }

        buttonUninstallApp.setOnClickListener {
            showUninstallDialog()
        }
    }

    private fun updateUI() {
        textViewAppNameLarge.text = appInfo.appName
        textViewPackageName.text = appInfo.packageName
        textViewRiskLevelLarge.text = appInfo.riskLevel.displayName
        textViewRiskDescription.text = appInfo.description
        textViewInstallDate.text = appInfo.installDate
        textViewLastUpdated.text = appInfo.lastUpdated
        textViewAppSize.text = appInfo.appSize
        textViewAppVersion.text = appInfo.version

        imageViewAppIconLarge.setImageResource(appInfo.iconResId)

        // Set risk level colors
        when (appInfo.riskLevel) {
            RiskLevel.HIGH -> {
                cardViewRiskBadgeLarge.setCardBackgroundColor(getColor(android.R.color.holo_red_light))
                textViewRiskLevelLarge.setTextColor(getColor(android.R.color.holo_red_dark))
            }
            RiskLevel.MEDIUM -> {
                cardViewRiskBadgeLarge.setCardBackgroundColor(getColor(android.R.color.holo_orange_light))
                textViewRiskLevelLarge.setTextColor(getColor(android.R.color.holo_orange_dark))
            }
            RiskLevel.LOW -> {
                cardViewRiskBadgeLarge.setCardBackgroundColor(getColor(android.R.color.holo_green_light))
                textViewRiskLevelLarge.setTextColor(getColor(android.R.color.holo_green_dark))
            }
        }
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${appInfo.packageName}")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open app settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showUninstallDialog() {
        AlertDialog.Builder(this)
            .setTitle("Uninstall App")
            .setMessage("Are you sure you want to uninstall ${appInfo.appName}? This action cannot be undone.")
            .setPositiveButton("Uninstall") { _, _ ->
                uninstallApp()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun uninstallApp() {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:${appInfo.packageName}")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to uninstall app", Toast.LENGTH_SHORT).show()
        }
    }
}

// Permissions Adapter
class PermissionsAdapter(private val permissions: List<AppDetailsActivity.Permission>) :
    RecyclerView.Adapter<PermissionsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewPermissionIcon: ImageView = itemView.findViewById(R.id.imageView_permission_icon)
        val textViewPermissionName: TextView = itemView.findViewById(R.id.textView_permission_name)
        val textViewPermissionDescription: TextView = itemView.findViewById(R.id.textView_permission_description)
        val viewRiskIndicator: View = itemView.findViewById(R.id.view_risk_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.permission_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val permission = permissions[position]

        holder.textViewPermissionName.text = permission.name
        holder.textViewPermissionDescription.text = permission.description
        holder.imageViewPermissionIcon.setImageResource(permission.iconResId)

        val riskColor = when (permission.riskLevel) {
            AppDetailsActivity.RiskLevel.HIGH -> android.R.color.holo_red_dark
            AppDetailsActivity.RiskLevel.MEDIUM -> android.R.color.holo_orange_dark
            AppDetailsActivity.RiskLevel.LOW -> android.R.color.holo_green_dark
        }
        holder.viewRiskIndicator.setBackgroundColor(
            holder.itemView.context.getColor(riskColor)
        )
    }

    override fun getItemCount() = permissions.size
}

// Recommendations Adapter
class RecommendationsAdapter(private val recommendations: List<AppDetailsActivity.Recommendation>) :
    RecyclerView.Adapter<RecommendationsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewRecommendationIcon: ImageView = itemView.findViewById(R.id.imageView_recommendation_icon)
        val textViewRecommendationTitle: TextView = itemView.findViewById(R.id.textView_recommendation_title)
        val textViewRecommendationDescription: TextView = itemView.findViewById(R.id.textView_recommendation_description)
        val textViewPriority: TextView = itemView.findViewById(R.id.textView_priority)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recommendations, parent, false) // Ensure this layout name is correct
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recommendation = recommendations[position]

        holder.textViewRecommendationTitle.text = recommendation.title
        holder.textViewRecommendationDescription.text = recommendation.description
        holder.textViewPriority.text = recommendation.priority.displayName
        holder.imageViewRecommendationIcon.setImageResource(recommendation.iconResId)

        val priorityColor = when (recommendation.priority) {
            AppDetailsActivity.Priority.HIGH -> android.R.color.holo_red_dark
            AppDetailsActivity.Priority.MEDIUM -> android.R.color.holo_orange_dark
            AppDetailsActivity.Priority.LOW -> android.R.color.holo_green_dark
        }
        holder.textViewPriority.setTextColor(
            holder.itemView.context.getColor(priorityColor)
        )
    }

    override fun getItemCount() = recommendations.size
}
