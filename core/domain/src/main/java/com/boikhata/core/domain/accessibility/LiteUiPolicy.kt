package com.boikhata.core.domain.accessibility

object LiteUiPolicy {
    const val typographyScale = 1.2f
    const val minimumTouchTargetDp = 56
    const val maximumTouchTargetDp = 64

    fun scaledTextSize(sp: Float, enabled: Boolean): Float = if (enabled) sp * typographyScale else sp
    fun touchTargetDp(enabled: Boolean): Int = if (enabled) maximumTouchTargetDp else minimumTouchTargetDp
}

object VoiceSetupSteps {
    val Bengali: List<String> = listOf(
        "ধাপ ১: দোকানের নাম ও প্রাথমিক তথ্য ঠিক করুন।",
        "ধাপ ২: বইয়ের ক্যাটালগে বই এবং দাম যোগ করুন।",
        "ধাপ ৩: বিক্রির সময় কার্টে বই যোগ করে বিল তৈরি করুন।",
        "ধাপ ৪: বাকি ও দেনার হিসাব নিয়মিত লিখুন।",
        "ধাপ ৫: রিপোর্ট দেখুন এবং প্রতি মাসে ডেটার কপি রাখুন।",
    )
}
