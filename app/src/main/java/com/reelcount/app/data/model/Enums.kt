package com.reelcount.app.data.model

enum class ScrollClassification {
    REEL,
    FEED,
    UNKNOWN
}

enum class TargetApp(val packageName: String) {
    INSTAGRAM("com.instagram.android"),
    YOUTUBE("com.google.android.youtube"),
    UNKNOWN("");

    companion object {
        fun fromPackage(pkg: String?): TargetApp =
            entries.firstOrNull { it.packageName == pkg } ?: UNKNOWN
    }
}
