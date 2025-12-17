package com.example.kingasec

enum class RiskLevel(val displayName: String, val colorResId: Int, val bgColorResId: Int) {
    HIGH("HIGH", R.color.risk_high, R.color.risk_high_bg),
    MEDIUM("MEDIUM", R.color.risk_medium, R.color.risk_medium_bg),
    LOW("LOW", R.color.risk_low, R.color.risk_low_bg)
}