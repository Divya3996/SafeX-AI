package com.sentinel.ai.protection.intent.heuristic

data class FileHeuristicConfig(
    val dangerousExtensions: Set<String> = setOf(
        "apk", "exe", "bat", "cmd", "sh", "bin", "js", "vbs", "wsf", "scr", "jar", "msi", "com"
    ),
    
    val fakeDocumentExtensions: Set<String> = setOf(
        "pdf", "docx", "xlsx", "pptx", "txt"
    ),
    
    val suspiciousArchiveExtensions: Set<String> = setOf(
        "zip", "rar", "7z", "tar", "gz", "bz2"
    ),
    
    val misleadingKeywords: Set<String> = setOf(
        "invoice", "receipt", "payment", "salary", "bonus", "security", "update", 
        "alert", "private", "confidential", "verification", "statement", "cv", "resume"
    ),

    val weights: Map<String, Float> = mapOf(
        "dangerous_extension" to 40f,
        "double_extension" to 30f,
        "misleading_filename" to 10f,
        "random_filename" to 10f,
        "suspicious_archive" to 5f,
        "fake_document" to 25f
    ),

    val randomFilenameEntropyThreshold: Double = 3.5
)
