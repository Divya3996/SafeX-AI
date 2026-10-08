package com.sentinel.ai.protection.intent.heuristic

data class LinkHeuristicConfig(
    val suspiciousTlds: Set<String> = setOf(
        "live", "click", "top", "xyz", "online", "info", "vip", "fit", "gq", 
        "cf", "tk", "ml", "ga", "work", "club", "buzz", "support", "security", 
        "update", "verify", "download", "bid", "loan", "men", "win", "stream"
    ),
    
    val brandOfficialDomains: Map<String, List<String>> = mapOf(
        "google" to listOf("google.com", "google.co.in", "youtube.com", "gmail.com"),
        "paypal" to listOf("paypal.com", "paypal.me"),
        "amazon" to listOf("amazon.com", "amazon.in", "amazon.co.jp", "amazon.de"),
        "apple" to listOf("apple.com", "icloud.com"),
        "instagram" to listOf("instagram.com"),
        "facebook" to listOf("facebook.com", "fb.com"),
        "whatsapp" to listOf("whatsapp.com"),
        "telegram" to listOf("telegram.org", "t.me"),
        "microsoft" to listOf("microsoft.com", "microsoftonline.com", "office.com", "live.com", "outlook.com"),
        "netflix" to listOf("netflix.com"),
        "sbi" to listOf("sbi.co.in", "onlinesbi.sbi"),
        "hdfc" to listOf("hdfcbank.com"),
        "icici" to listOf("icicibank.com"),
        "paytm" to listOf("paytm.com"),
        "phonepe" to listOf("phonepe.com")
    ),
    
    val socialEngineeringKeywords: Set<String> = setOf(
        "banking", "payment", "verification", "verify", "login", "password", 
        "otp", "aadhaar", "pan", "wallet", "upi", "reward", "lottery", 
        "ipl-ticket", "ipl", "ticket", "gift", "urgent", "account", "secure", 
        "signin", "free-gift", "cashback", "refund", "claim", "win-money"
    ),

    val trackingParameters: Set<String> = setOf(
        "fbclid", "gclid", "msclkid"
    ),

    val trackingParameterPrefixes: Set<String> = setOf(
        "utm_"
    ),

    val redirectParameters: Set<String> = setOf(
        "redirect", "redirect_url", "redirect_uri", "return", "return_url", "next", "continue",
        "target", "destination", "dest", "goto", "out", "link", "url", "to"
    ),

    val brandExtraWords: Set<String> = setOf(
        "login", "verify", "secure", "account", "support", "update", "gift", "reward", "wallet", "bank",
        "kyc", "refund", "signin", "auth", "recovery", "help", "verification", "otp", "payment"
    ),

    val lookAlikeReplacements: Map<Char, Set<Char>> = mapOf(
        'o' to setOf('0'),
        'i' to setOf('1', 'l'),
        'l' to setOf('1', 'i'),
        'e' to setOf('3'),
        'a' to setOf('4'),
        's' to setOf('5'),
        'g' to setOf('9')
    ),

    val weights: Map<String, Float> = mapOf(
        "suspicious_tld" to 15f,
        "ip_address" to 25f,
        "excessive_subdomains" to 10f,
        "random_hostname" to 10f,
        "repeated_hyphens" to 10f,
        "excessive_digits" to 10f,
        "punycode" to 15f,
        "excessive_length" to 5f,
        "deep_nesting" to 5f,
        "long_filename" to 5f,
        "excessive_query" to 10f,
        "encoded_chars" to 10f,
        "multiple_fragments" to 15f,
        "suspicious_redirect" to 15f,
        "insecure_http" to 30f,
        "non_standard_port" to 10f,
        "userinfo_deception" to 30f,
        "embedded_url" to 30f,
        "tracking_parameters" to 0f,
        "social_engineering" to 20f,
        "brand_impersonation" to 30f,
        "brand_lookalike" to 30f
    ),

    val subdomainThreshold: Int = 3,
    val urlLengthThreshold: Int = 150,
    val nestingThreshold: Int = 4,
    val filenameLengthThreshold: Int = 30,
    val queryParamsThreshold: Int = 5,
    val digitRatioThreshold: Double = 0.3,
    val randomHostnameEntropyThreshold: Double = 3.8
)
