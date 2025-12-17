package com.example.kingasec

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Processes scanned app data using a hybrid approach: a TFLite model and a heuristic rules engine.
 */
class MLDataProcessor(private val context: Context) {

    private var interpreter: Interpreter? = null

    private val PERMISSIONS_COUNT_MEAN = 15.0f
    private val PERMISSIONS_COUNT_STD_DEV = 8.0f
    private val RISK_SCORE_MEAN = 4.5f
    private val RISK_SCORE_STD_DEV = 2.5f

    // You must provide the exact list of 70 feature names from 'feature_names.pkl'.
    private val FEATURE_NAMES: List<String> = listOf()

    // Master exclusion list for trusted apps.
    private val exclusionListPackages = setOf("com.whatsapp", "com.facebook.katana", "com.safaricom.mpesa")
    private val exclusionListKeywords = setOf("whatsapp", "facebook", "mpesa")

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val assetManager = context.assets
            val modelFileDescriptor = assetManager.openFd("privacy_model.tflite")
            val inputStream = FileInputStream(modelFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = modelFileDescriptor.startOffset
            val declaredLength = modelFileDescriptor.declaredLength
            val modelByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            interpreter = Interpreter(modelByteBuffer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Public function to check if an app is on the master exclusion list.
     */
    fun isAppExcluded(app: AppScannerService.ScannedApp): Boolean {
        return app.packageName in exclusionListPackages || exclusionListKeywords.any { app.appName.contains(it, ignoreCase = true) }
    }

    fun predictRiskScore(scannedApp: AppScannerService.ScannedApp): Float {
        if (isAppExcluded(scannedApp)) {
            return 1.0f // Return a definitive low score.
        }

        // First, apply the hardcoded permission rules. These override the ML model.
        val ruleBasedScore = applyHeuristicRules(scannedApp)
        if (ruleBasedScore != null) {
            return ruleBasedScore
        }

        // If no rules were triggered, then use the ML model.
        if (interpreter == null || FEATURE_NAMES.isEmpty()) {
            return 2.0f // Default low score if model isn't available
        }

        val mlData = prepareForML(scannedApp)
        val featureVector = createFeatureVector(mlData)

        if (featureVector.isEmpty()) {
            return 2.0f
        }

        val inputBuffer = ByteBuffer.allocateDirect(1 * FEATURE_NAMES.size * 4).apply {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().put(featureVector)
        }
        val outputBuffer = ByteBuffer.allocateDirect(1 * 1 * 4).apply { order(ByteOrder.nativeOrder()) }
        interpreter?.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()
        val scaledPrediction = outputBuffer.asFloatBuffer().get()
        val modelScore = (scaledPrediction * RISK_SCORE_STD_DEV) + RISK_SCORE_MEAN

        return modelScore.coerceIn(0f, 10f)
    }

    private fun applyHeuristicRules(app: AppScannerService.ScannedApp): Float? {
        val hasSms = app.dangerousPermissions.any { it.contains("READ_SMS") }
        val hasContacts = app.dangerousPermissions.any { it.contains("READ_CONTACTS") }

        Log.d("KingaSec-Debug", "Rule Check for ${app.appName}: Category='${app.category}', hasSms=$hasSms, hasContacts=$hasContacts")

        // RULE 1: Critical risk - app reads both SMS and Contacts.
        if (hasSms && hasContacts) {
            Log.d("KingaSec-Debug", "${app.appName} triggered HIGH (SMS & Contacts) rule. Score -> 9.0")
            return 9.0f
        }

        // RULE 2: Medium risk - app reads SMS only.
        if (hasSms) {
            Log.d("KingaSec-Debug", "${app.appName} triggered MEDIUM (SMS only) rule. Score -> 6.5")
            return 6.5f
        }

        // RULE 3: Medium risk - app reads Contacts only (and is not a comms app).
        if (hasContacts && app.category != "Communication") {
            Log.d("KingaSec-Debug", "${app.appName} triggered MEDIUM (Contacts only) rule. Score -> 6.0")
            return 6.0f
        }

        // No specific rules triggered.
        return null
    }

    fun generateRiskDescription(scannedApp: AppScannerService.ScannedApp, riskScore: Float): String {
        if (isAppExcluded(scannedApp)) {
            return "This is a trusted application. Its permissions are appropriate for its functions."
        }

        val hasSms = scannedApp.dangerousPermissions.any { it.contains("READ_SMS") }
        val hasContacts = scannedApp.dangerousPermissions.any { it.contains("READ_CONTACTS") }

        // Descriptions should match the new, simpler heuristic rules.
        if (hasSms && hasContacts) {
            return "This app reads both your SMS messages and contact list. This combination is high-risk and grants invasive access to your personal data."
        }
        if (hasSms) {
            return "This app can read your SMS messages, which could include one-time passwords and personal conversations. This is a significant privacy risk for a non-messaging app."
        }
        if (hasContacts && scannedApp.category != "Communication") {
            return "This app can read your entire contact list. This is an unnecessary and significant privacy risk for a non-communication app."
        }

        val dangerousCount = scannedApp.dangerousPermissions.size
        return when (scoreToRiskLevel(riskScore)) {
            RiskLevel.HIGH -> "High risk score (${ "%.1f".format(riskScore) }). Accesses $dangerousCount sensitive permissions and was flagged by the ML model."
            RiskLevel.MEDIUM -> "Medium risk score (${ "%.1f".format(riskScore) }). The ML model suggests reviewing the $dangerousCount sensitive permissions for this ${scannedApp.category} app."
            else -> "Low risk score (${ "%.1f".format(riskScore) }). Appears to use a reasonable number of permissions for its category."
        }
    }

    fun scoreToRiskLevel(score: Float): RiskLevel {
        return when {
            score >= 7.0f -> RiskLevel.HIGH
            score >= 4.0f -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }

    private fun prepareForML(scannedApp: AppScannerService.ScannedApp): AppMLData {
        return AppMLData(
            category = scannedApp.category,
            dangerousPermissionsString = scannedApp.dangerousPermissions.joinToString(",") { it.removePrefix("android.permission.") },
            permissionsCount = scannedApp.permissionsCount
        )
    }

    private fun createFeatureVector(appMLData: AppMLData): FloatArray {
        if (FEATURE_NAMES.isEmpty()) return floatArrayOf()

        val featureMap = mutableMapOf<String, Float>()
        val scaledPermissionsCount = (appMLData.permissionsCount - PERMISSIONS_COUNT_MEAN) / PERMISSIONS_COUNT_STD_DEV
        featureMap["Permissions_Count"] = scaledPermissionsCount

        val categoryFeature = "Category_${appMLData.category}"
        featureMap[categoryFeature] = 1f

        val dangerousPermissions = appMLData.dangerousPermissionsString.split(",").filter { it.isNotBlank() }
        for (perm in dangerousPermissions) {
            val permFeature = "Dangerous_Permissions_${perm}"
            featureMap[permFeature] = 1f
        }

        val finalFeatureVector = FloatArray(FEATURE_NAMES.size)
        for (i in FEATURE_NAMES.indices) {
            finalFeatureVector[i] = featureMap.getOrDefault(FEATURE_NAMES[i], 0f)
        }

        return finalFeatureVector
    }

    private data class AppMLData(
        val category: String,
        val dangerousPermissionsString: String,
        val permissionsCount: Int
    )
}