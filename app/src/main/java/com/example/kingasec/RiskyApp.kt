package com.example.kingasec

data class RiskyApp(
    val appName: String,
    val packageName: String,
    val reason: String,
    val riskLevel: RiskLevel,
    val iconResId: Int,
    val permissions: List<String> = emptyList(),
    val privacyRiskScore: Float = 0f
)
