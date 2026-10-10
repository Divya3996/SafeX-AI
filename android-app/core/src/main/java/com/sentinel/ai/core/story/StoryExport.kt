package com.sentinel.ai.core.story

/** Deliberately excludes raw input, addresses, amounts, titles, detector details and case IDs. */
object StoryExport {
    fun lines(case: StoryCase, analysis: StoryAnalysis): List<String> = buildList {
        add("SafeX AI situation summary")
        add("User-prepared guidance, not a submitted complaint or verified identity.")
        if (case.synthetic) add("Synthetic example")
        add(analysis.concern.label)
        add("Evidence count: ${case.items.size}")
        analysis.findings.filterNot { it.id.startsWith("item_") }.forEach {
            add(it.title); add(it.explanation); add(it.action)
            add("Supporting items: " + it.evidence.mapNotNull { evidence -> case.items.indexOfFirst { item -> item.id == evidence.itemId }.takeIf { index -> index >= 0 }?.plus(1) }.distinct().joinToString(", "))
        }
        add("Raw messages, screenshots, links, payment details and secrets are excluded.")
        add("A low-concern result is not a safety guarantee. Verify through a trusted independent channel.")
    }
}
