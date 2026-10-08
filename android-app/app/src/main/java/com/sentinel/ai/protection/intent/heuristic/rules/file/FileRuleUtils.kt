package com.sentinel.ai.protection.intent.heuristic.rules.file

internal fun String.cleanFilename(): String {
    return substringAfterLast('/').substringAfterLast('\\').substringBefore('?').lowercase()
}

internal fun String.extensionParts(): List<String> {
    return cleanFilename().split('.').filter { it.isNotBlank() }
}

internal fun String.extensionOrEmpty(): String {
    return extensionParts().lastOrNull().orEmpty()
}
