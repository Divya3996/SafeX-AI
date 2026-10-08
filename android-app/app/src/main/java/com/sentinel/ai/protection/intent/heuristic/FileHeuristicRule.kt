package com.sentinel.ai.protection.intent.heuristic

interface FileHeuristicRule {
    val id: String
    val name: String
    fun evaluate(filename: String, config: FileHeuristicConfig): RuleResult
}
