package com.sentinel.ai.ml

import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicRiskEngine
import com.sentinel.ai.protection.intent.link.UrlNormalizer
import org.junit.Assert.*
import org.junit.Test

class UrlModelCoverageTest {
    @Test fun underrepresentedBenignContextsDoNotReceiveStandaloneModelWarnings() {
        listOf("https://www.paypal.com/signin", "https://हिन्दी.example/", "https://ગુજરાતી.example/",
            "https://example.com/?email=person@example.com").forEach {
            assertFalse(it, UrlModelCoverage.canWarnIndependently(UrlNormalizer.parse(it)))
        }
    }
    @Test fun limitedModelCoverageNeverNeutralizesHeuristicEvidence() {
        listOf("https://раypal.example/login",
            "https://www.paypal.com/?redirect=https://attacker.example/login").forEach {
            assertFalse(it, UrlModelCoverage.canWarnIndependently(UrlNormalizer.parse(it)))
            assertTrue(it, LinkHeuristicRiskEngine().analyze(it).score >= 30f)
        }
    }
    @Test fun unofficialBrandBoundariesAndUserinfoRemainEligible() {
        listOf("https://paypal.com.attacker.example/login", "https://paypal.com@attacker.example/",
            "https://ordinary.example/", "https://fake.firebaseapp.com/", "https://example.com/path#a@b.com").forEach {
            assertTrue(it, UrlModelCoverage.canWarnIndependently(UrlNormalizer.parse(it)))
        }
    }
}
