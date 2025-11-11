package com.example.kingasec

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Processes scanned app data for ML model prediction
 */
class MLDataProcessor(private val context: Context) {

    data class AppMLData(
        val appName: String,
        val packageName: String,
        val category: String,
        val dangerousPermissionsString: String, // Comma-separated list
        val permissionsCount: Int,
        val privacyRiskScore: Float = 0f, // Will be predicted by ML model
        val excessivePermissionsFlag: Int = 0 // Will be calculated
    )

    /**
     * Convert scanned app to ML-ready format
     */
    fun prepareForML(scannedApp: AppScannerService.ScannedApp): AppMLData {
        // Join dangerous permissions into a comma-separated string
        val dangerousPermissionsString = scannedApp.dangerousPermissions
            .joinToString(", ") { it.removePrefix("android.permission.") }

        // Calculate excessive permissions flag (simple heuristic)
        // You can adjust this logic based on your training data
        val excessivePermissionsFlag = calculateExcessivePermissions(
            scannedApp.category,
            scannedApp.dangerousPermissions.size,
            scannedApp.permissionsCount
        )

        return AppMLData(
            appName = scannedApp.appName,
            packageName = scannedApp.packageName,
            category = scannedApp.category,
            dangerousPermissionsString = dangerousPermissionsString,
            permissionsCount = scannedApp.permissionsCount,
            excessivePermissionsFlag = excessivePermissionsFlag
        )
    }

    /**
     * Calculate if app has excessive permissions
     * This is a heuristic - adjust based on your ML model training
     */
    private fun calculateExcessivePermissions(
        category: String,
        dangerousPermCount: Int,
        totalPermCount: Int
    ): Int {
        // Define expected permission ranges by category
        val expectedDangerousPerms = when (category) {
            "Game" -> 2
            "Social" -> 5
            "Finance" -> 3
            "Productivity" -> 3
            "Audio", "Video" -> 4
            "Maps" -> 4
            else -> 3
        }

        // Flag if dangerous permissions exceed expected by 50% or more
        return if (dangerousPermCount > expectedDangerousPerms * 1.5) 1 else 0
    }

    /**
     * Batch process multiple apps
     */
    suspend fun prepareMultipleAppsForML(
        scannedApps: List<AppScannerService.ScannedApp>
    ): List<AppMLData> = withContext(Dispatchers.Default) {
        scannedApps.map { prepareForML(it) }
    }

    /**
     * Create feature vector for TensorFlow Lite model
     * This matches your training data structure
     */
    fun createFeatureVector(appMLData: AppMLData, allCategories: Set<String>): FloatArray {
        // This will need to match exactly with your trained model's input
        // For now, creating a basic structure

        val features = mutableListOf<Float>()

        // 1. Numerical features (scaled)
        features.add(appMLData.permissionsCount.toFloat())

        // 2. One-hot encode category
        allCategories.sorted().forEach { category ->
            features.add(if (appMLData.category == category) 1f else 0f)
        }

        // 3. One-hot encode dangerous permissions
        // You'll need to match this with your training data's dangerous permissions
        val knownDangerousPermissions = listOf(
            "ACCESS_FINE_LOCATION",
            "READ_CONTACTS",
            "SEND_SMS",
            "READ_SMS",
            "CAMERA",
            "RECORD_AUDIO",
            "READ_PHONE_STATE",
            "READ_CALL_LOG",
            "WRITE_CALL_LOG"
        )

        knownDangerousPermissions.forEach { perm ->
            features.add(
                if (appMLData.dangerousPermissionsString.contains(perm)) 1f else 0f
            )
        }

        return features.toFloatArray()
    }

    /**
     * Convert privacy risk score to risk level
     */
    fun scoreToRiskLevel(score: Float): MainActivity.RiskLevel {
        return when {
            score >= 7.0f -> MainActivity.RiskLevel.HIGH
            score >= 4.0f -> MainActivity.RiskLevel.MEDIUM
            else -> MainActivity.RiskLevel.LOW
        }
    }

    /**
     * Generate risk description based on permissions
     */
    fun generateRiskDescription(
        scannedApp: AppScannerService.ScannedApp,
        riskScore: Float
    ): String {
        val dangerousCount = scannedApp.dangerousPermissions.size

        return when {
            riskScore >= 7.0f -> {
                "This app accesses ${dangerousCount} sensitive permissions including " +
                        "${scannedApp.dangerousPermissions.take(2).joinToString(", ") {
                            it.removePrefix("android.permission.").replace("_", " ")
                        }}. Some permissions may be excessive for its functionality."
            }
            riskScore >= 4.0f -> {
                "This app requests ${dangerousCount} sensitive permissions. " +
                        "Most appear justified for its ${scannedApp.category} functionality."
            }
            else -> {
                "This app uses standard permissions appropriately and poses minimal privacy risks."
            }
        }
    }

    /**
     * Get all unique categories from scanned apps
     */
    fun extractCategories(apps: List<AppScannerService.ScannedApp>): Set<String> {
        return apps.map { it.category }.toSet()
    }

    /**
     * Get all unique dangerous permissions from scanned apps
     */
    fun extractDangerousPermissions(apps: List<AppScannerService.ScannedApp>): Set<String> {
        return apps.flatMap { it.dangerousPermissions }.toSet()
    }
}