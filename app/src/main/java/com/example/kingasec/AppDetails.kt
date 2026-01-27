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
import androidx.core.content.ContextCompat
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
    private lateinit var textViewInstallDate: TextView
    private lateinit var textViewLastUpdated: TextView
    private lateinit var textViewAppSize: TextView
    private lateinit var textViewAppVersion: TextView

    // Data
    private lateinit var appInfo: AppInfo
    private lateinit var recommendationsAdapter: RecommendationsAdapter

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
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return

        val scanResult = ScanResultsManager.getScanResults().find { it.packageName == packageName }
        if (scanResult == null) {
            Toast.makeText(this, "App details not found.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val riskLevel = try {
            RiskLevel.valueOf(scanResult.riskLevel)
        } catch (e: IllegalArgumentException) {
            RiskLevel.LOW // Default to LOW if the string is invalid
        }

        appInfo = AppInfo(
            appName = scanResult.appName,
            packageName = scanResult.packageName,
            riskLevel = riskLevel,
            iconResId = scanResult.iconResId,
            description = scanResult.reason,
            installDate = "", // Will be loaded below
            lastUpdated = "", // Will be loaded below
            appSize = "", // Will be loaded below
            version = ""
        )

        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            appInfo = appInfo.copy(
                version = packageInfo.versionName ?: "Unknown",
                installDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(Date(packageInfo.firstInstallTime)),
                lastUpdated = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(Date(packageInfo.lastUpdateTime))
            )
        } catch (e: PackageManager.NameNotFoundException) {
            // App not found, use placeholder data
        }
    }

    private fun setupRecyclerViews() {
        val permissions = try {
            val packageInfo = packageManager.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS)
            packageInfo.requestedPermissions?.toList() ?: emptyList()
        } catch (e: PackageManager.NameNotFoundException) {
            emptyList()
        }

        val allPermissions = permissions.mapNotNull { permissionName ->
            val (desc, risk) = PermissionInfo.getPermissionDetails(permissionName)
            if (desc != "Unknown permission") { // Only add known permissions
                Permission(
                    name = permissionName.substringAfterLast('.'),
                    description = desc,
                    iconResId = PermissionInfo.getPermissionIcon(permissionName),
                    riskLevel = risk
                )
            } else {
                null
            }
        }

        val highRiskPermissions = allPermissions.filter { it.riskLevel == RiskLevel.HIGH }
        val mediumRiskPermissions = allPermissions.filter { it.riskLevel == RiskLevel.MEDIUM }
        val safePermissions = allPermissions.filter { it.riskLevel == RiskLevel.LOW }

        recyclerViewHighRiskPermissions.apply {
            layoutManager = LinearLayoutManager(this@AppDetailsActivity)
            adapter = PermissionsAdapter(highRiskPermissions)
            isNestedScrollingEnabled = false
        }

        recyclerViewMediumRiskPermissions.apply {
            layoutManager = LinearLayoutManager(this@AppDetailsActivity)
            adapter = PermissionsAdapter(mediumRiskPermissions)
            isNestedScrollingEnabled = false
        }

        recyclerViewSafePermissions.apply {
            layoutManager = LinearLayoutManager(this@AppDetailsActivity)
            adapter = PermissionsAdapter(safePermissions)
            isNestedScrollingEnabled = false
        }

        // Sample recommendations (can be made dynamic later)
        val recommendations = listOf(
            Recommendation(
                "Disable Unused Permissions",
                "Regularly review and disable permissions that the app doesn't need for its core functionality.",
                Priority.HIGH,
                android.R.drawable.ic_dialog_alert
            ),
            Recommendation(
                "Check Data Access",
                "Monitor how this app uses your data, especially with high-risk permissions.",
                Priority.MEDIUM,
                android.R.drawable.ic_dialog_info
            )
        )

        recommendationsAdapter = RecommendationsAdapter(recommendations)
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

        try {
            val icon = packageManager.getApplicationIcon(appInfo.packageName)
            imageViewAppIconLarge.setImageDrawable(icon)
        } catch (e: PackageManager.NameNotFoundException) {
            imageViewAppIconLarge.setImageResource(R.drawable.ic_app_placeholder)
        }

        cardViewRiskBadgeLarge.setCardBackgroundColor(ContextCompat.getColor(this, appInfo.riskLevel.bgColorResId))
        textViewRiskLevelLarge.setTextColor(ContextCompat.getColor(this, appInfo.riskLevel.colorResId))
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
}

// Adapter for Permissions
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

        holder.viewRiskIndicator.setBackgroundColor(
            holder.itemView.context.getColor(permission.riskLevel.colorResId)
        )
    }

    override fun getItemCount() = permissions.size
}

// Adapter for Recommendations
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
            .inflate(R.layout.recommendations, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recommendation = recommendations[position]

        holder.textViewRecommendationTitle.text = recommendation.title
        holder.textViewRecommendationDescription.text = recommendation.description
        holder.textViewPriority.text = recommendation.priority.displayName
        holder.imageViewRecommendationIcon.setImageResource(recommendation.iconResId)

        val priorityColor = holder.itemView.context.getColor(recommendation.priority.colorResId)
        holder.textViewPriority.setTextColor(priorityColor)
    }

    override fun getItemCount() = recommendations.size
}

// Helper object to provide details about Android permissions
private object PermissionInfo {
    private val permissionDetails = mapOf(
        "android.permission.READ_CONTACTS" to Pair("Read your contacts and call history", RiskLevel.HIGH),
        "android.permission.READ_SMS" to Pair("Read your text messages", RiskLevel.HIGH),
        "android.permission.ACCESS_FINE_LOCATION" to Pair("Access your precise location", RiskLevel.HIGH),
        "android.permission.CAMERA" to Pair("Take pictures and record videos", RiskLevel.HIGH),
        "android.permission.RECORD_AUDIO" to Pair("Record audio", RiskLevel.HIGH),
        "android.permission.READ_CALL_LOG" to Pair("Read your call log", RiskLevel.HIGH),
        "android.permission.WRITE_EXTERNAL_STORAGE" to Pair("Write to external storage (legacy)", RiskLevel.HIGH),

        "android.permission.ACCESS_COARSE_LOCATION" to Pair("Access your approximate location", RiskLevel.MEDIUM),
        "android.permission.READ_PHONE_STATE" to Pair("Read phone status and identity", RiskLevel.MEDIUM),
        "android.permission.GET_ACCOUNTS" to Pair("Find accounts on the device", RiskLevel.MEDIUM),
        "android.permission.BLUETOOTH_ADMIN" to Pair("Pair with Bluetooth devices", RiskLevel.MEDIUM),

        "android.permission.INTERNET" to Pair("Access network connections", RiskLevel.LOW),
        "android.permission.VIBRATE" to Pair("Control device vibration", RiskLevel.LOW),
        "android.permission.ACCESS_NETWORK_STATE" to Pair("View network connections", RiskLevel.LOW),
        "android.permission.WAKE_LOCK" to Pair("Prevent phone from sleeping", RiskLevel.LOW)
    )

    fun getPermissionDetails(permission: String): Pair<String, RiskLevel> {
        return permissionDetails[permission] ?: Pair("Unknown permission", RiskLevel.LOW)
    }

    fun getPermissionIcon(permission: String): Int {
        return when (getPermissionDetails(permission).second) {
            RiskLevel.HIGH -> android.R.drawable.ic_dialog_alert
            RiskLevel.MEDIUM -> android.R.drawable.ic_dialog_info
            RiskLevel.LOW -> android.R.drawable.ic_dialog_info
        }
    }
}