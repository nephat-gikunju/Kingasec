package com.example.kingasec

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service to scan installed applications and extract permission data
 */
class AppScannerService(private val context: Context) {

    data class ScannedApp(
        val appName: String,
        val packageName: String,
        val category: String,
        val dangerousPermissions: List<String>,
        val allPermissions: List<String>,
        val permissionsCount: Int,
        val icon: Drawable?,
        val installDate: Long,
        val lastUpdateDate: Long,
        val versionName: String,
        val isSystemApp: Boolean
    )

    // Dangerous permissions according to Android documentation
    private val dangerousPermissionGroups = setOf(
        // Location
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",

        // Camera
        "android.permission.CAMERA",

        // Microphone
        "android.permission.RECORD_AUDIO",

        // Contacts
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.GET_ACCOUNTS",

        // Phone
        "android.permission.READ_PHONE_STATE",
        "android.permission.READ_PHONE_NUMBERS",
        "android.permission.CALL_PHONE",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.ADD_VOICEMAIL",
        "android.permission.USE_SIP",
        "android.permission.PROCESS_OUTGOING_CALLS",
        "android.permission.ANSWER_PHONE_CALLS",

        // SMS
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_WAP_PUSH",
        "android.permission.RECEIVE_MMS",

        // Storage
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.ACCESS_MEDIA_LOCATION",

        // Calendar
        "android.permission.READ_CALENDAR",
        "android.permission.WRITE_CALENDAR",

        // Body Sensors
        "android.permission.BODY_SENSORS",

        // Activity Recognition
        "android.permission.ACTIVITY_RECOGNITION"
    )

    /**
     * Scan all installed applications (excluding system apps by default)
     */
    suspend fun scanAllApps(includeSystemApps: Boolean = false): List<ScannedApp> =
        withContext(Dispatchers.IO) {
            val packageManager = context.packageManager
            val installedApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }

            installedApps.mapNotNull { packageInfo ->
                try {
                    // Get applicationInfo and return null if it's missing
                    val appInfo: ApplicationInfo = packageInfo.applicationInfo ?: return@mapNotNull null

                    // Skip system apps if requested
                    if (!includeSystemApps && isSystemApp(appInfo)) {
                        return@mapNotNull null
                    }

                    scanApp(packageInfo, appInfo, packageManager)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }

    /**
     * Scan a specific app by package name
     */
    suspend fun scanAppByPackage(packageName: String): ScannedApp? =
        withContext(Dispatchers.IO) {
            try {
                val packageManager = context.packageManager
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(
                        packageName,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                }

                val appInfo: ApplicationInfo = packageInfo.applicationInfo ?: return@withContext null
                scanApp(packageInfo, appInfo, packageManager)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    private fun scanApp(
        packageInfo: PackageInfo,
        appInfo: ApplicationInfo,
        packageManager: PackageManager
    ): ScannedApp {
        // Get app name
        val appName = packageManager.getApplicationLabel(appInfo).toString()

        // Get all permissions
        val allPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

        // Filter dangerous permissions
        val dangerousPermissions = allPermissions.filter { permission ->
            dangerousPermissionGroups.contains(permission)
        }

        // Get app category
        val category = getAppCategory(appInfo)

        // Get app icon
        val icon = try {
            packageManager.getApplicationIcon(appInfo)
        } catch (e: Exception) {
            null
        }

        return ScannedApp(
            appName = appName,
            packageName = packageInfo.packageName,
            category = category,
            dangerousPermissions = dangerousPermissions,
            allPermissions = allPermissions,
            permissionsCount = allPermissions.size,
            icon = icon,
            installDate = packageInfo.firstInstallTime,
            lastUpdateDate = packageInfo.lastUpdateTime,
            versionName = packageInfo.versionName ?: "Unknown",
            isSystemApp = isSystemApp(appInfo)
        )
    }

    /**
     * Get app category
     */
    private fun getAppCategory(appInfo: ApplicationInfo): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_GAME -> "Game"
                ApplicationInfo.CATEGORY_AUDIO -> "Audio"
                ApplicationInfo.CATEGORY_VIDEO -> "Video"
                ApplicationInfo.CATEGORY_IMAGE -> "Image"
                ApplicationInfo.CATEGORY_SOCIAL -> "Social"
                ApplicationInfo.CATEGORY_NEWS -> "News"
                ApplicationInfo.CATEGORY_MAPS -> "Maps"
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
                else -> "Other"
            }
        } else {
            // For older Android versions, try to guess from package name
            when {
                appInfo.packageName.contains("game", ignoreCase = true) -> "Game"
                appInfo.packageName.contains("social", ignoreCase = true) -> "Social"
                appInfo.packageName.contains("bank", ignoreCase = true) -> "Finance"
                appInfo.packageName.contains("music", ignoreCase = true) -> "Audio"
                else -> "Other"
            }
        }
    }

    /**
     * Check if app is a system app
     */
    private fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    }

    /**
     * Get human-readable permission name
     */
    fun getPermissionDisplayName(permission: String): String {
        return permission
            .removePrefix("android.permission.")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.lowercase().replaceFirstChar { char -> char.uppercase() } }
    }

    /**
     * Get permission description
     */
    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            "android.permission.ACCESS_FINE_LOCATION" -> "Access your precise location"
            "android.permission.ACCESS_COARSE_LOCATION" -> "Access your approximate location"
            "android.permission.CAMERA" -> "Take pictures and record videos"
            "android.permission.RECORD_AUDIO" -> "Record audio"
            "android.permission.READ_CONTACTS" -> "Read your contacts"
            "android.permission.WRITE_CONTACTS" -> "Modify your contacts"
            "android.permission.READ_PHONE_STATE" -> "Read phone state and identity"
            "android.permission.READ_SMS" -> "Read your text messages"
            "android.permission.SEND_SMS" -> "Send SMS messages"
            "android.permission.READ_CALL_LOG" -> "Read call log"
            "android.permission.WRITE_CALL_LOG" -> "Write call log"
            "android.permission.READ_CALENDAR" -> "Read calendar events"
            "android.permission.WRITE_CALENDAR" -> "Add or modify calendar events"
            "android.permission.READ_EXTERNAL_STORAGE" -> "Read files from storage"
            "android.permission.WRITE_EXTERNAL_STORAGE" -> "Modify or delete files"
            "android.permission.BODY_SENSORS" -> "Access body sensors like heart rate"
            "android.permission.ACTIVITY_RECOGNITION" -> "Recognize your physical activity"
            else -> "Permission: ${getPermissionDisplayName(permission)}"
        }
    }

    /**
     * Check if permission is granted
     */
    fun isPermissionGranted(packageName: String, permission: String): Boolean {
        return try {
            context.packageManager.checkPermission(permission, packageName) ==
                    PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }
}