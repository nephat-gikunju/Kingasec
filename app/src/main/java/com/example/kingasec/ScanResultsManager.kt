package com.example.kingasec

/**
 * Singleton to share scan results between activities
 * In production, consider using ViewModel with LiveData or a database
 */
object ScanResultsManager {

    data class ScannedAppResult(
        val appName: String,
        val packageName: String,
        val reason: String,
        val riskLevel: String, // HIGH, MEDIUM, LOW
        val iconResId: Int,
        val permissions: List<String>,
        val privacyRiskScore: Float
    )

    private var scanResults: List<ScannedAppResult> = emptyList()

    fun setScanResults(results: List<ScannedAppResult>) {
        scanResults = results
    }

    fun getScanResults(): List<ScannedAppResult> {
        return scanResults
    }

    fun clearResults() {
        scanResults = emptyList()
    }

    fun getResultsCount(): Int {
        return scanResults.size
    }

    fun getHighRiskCount(): Int {
        return scanResults.count { it.riskLevel == "HIGH" }
    }

    fun getMediumRiskCount(): Int {
        return scanResults.count { it.riskLevel == "MEDIUM" }
    }

    fun getLowRiskCount(): Int {
        return scanResults.count { it.riskLevel == "LOW" }
    }
}