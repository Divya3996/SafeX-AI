package com.sentinel.ai.core.story

/** Authored demonstration inputs, never fixture-name verdict overrides. */
object StoryExamples {
    fun task(language: String) = when (language) {
        "hi" -> listOf("क्या आप शाम के काम की जानकारी चाहते हैं?", "आपकी कमाई ₹800 है।", "अपनी कमाई निकालने के लिए पहले ₹500 जमा करें।")
        "gu" -> listOf("શું તમને સાંજના કામની માહિતી જોઈએ છે?", "તમારી કમાણી ₹800 છે.", "તમારી કમાણી ઉપાડવા માટે પહેલાં ₹500 જમા કરો.")
        else -> listOf("Would you like details about evening work?", "Your earned commission is ₹800.", "Deposit ₹500 to unlock your ₹800 earnings.")
    }
    fun legitimate(language: String) = when (language) {
        "hi" -> listOf("आपका नौकरी का इंटरव्यू मंगलवार को है।", "इस नौकरी के लिए कोई फीस नहीं है।", "कभी भी ओटीपी साझा न करें और नौकरी पाने के लिए पैसे न दें।")
        "gu" -> listOf("તમારો નોકરીનો ઇન્ટરવ્યુ મંગળવારે છે.", "આ નોકરી માટે કોઈ ફી નથી.", "ક્યારેય ઓટીપી શેર ન કરો અને નોકરી મેળવવા પૈસા ન આપો.")
        else -> listOf("Your job interview is on Tuesday.", "There is no fee for this job.", "Never share your OTP or pay to get a job.")
    }
}
