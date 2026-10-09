package com.sentinel.ai.core.model

import org.junit.Assert.*
import org.junit.Test

class ContentCandidateExtractorTest {
    @Test fun `bare and unicode domains retain path case and ignore email addresses`() {
        val candidates = ContentCandidateExtractor.extract("See Example.com/Account?Ref=AbC or https://ઉદાહરણ.ભારત/help. Email help@example.com; version 1.5.0")
        assertEquals(listOf("https://Example.com/Account?Ref=AbC", "https://ઉદાહરણ.ભારત/help"), candidates.map { it.inspectionValue })
        assertEquals(setOf(CandidateChange.ASSUMED_HTTPS), candidates.first().changes)
    }
    @Test fun `defanged and invisible spelling always requires user confirmation`() {
        val candidates = ContentCandidateExtractor.extract("hxxps://bank[.]example/LoGin https://pay\u200Bpal.example/Account")
        assertEquals(2, candidates.size)
        assertEquals("https://bank.example/LoGin", candidates[0].inspectionValue)
        assertEquals("hxxps://bank[.]example/LoGin", candidates[0].original)
        assertTrue(candidates.all { it.needsConfirmation })
        assertTrue(ContentCandidateExtractor.extract("(hxxps://example.com)").single().needsConfirmation)
        assertTrue(candidates[1].original.contains('\u200B'))
    }
    @Test fun `wrapped target never replaces original destination`() {
        val candidates = ContentCandidateExtractor.extract("https://example.com/go?url=https%3A%2F%2Fother.example%2FSignIn")
        assertEquals(2, candidates.size)
        assertTrue(candidates[0].inspectionValue.startsWith("https://example.com/"))
        assertEquals("https://other.example/SignIn", candidates[1].inspectionValue)
        assertTrue(CandidateChange.WRAPPED in candidates[1].changes)
        assertTrue(candidates[1].needsConfirmation)
    }
    @Test fun `candidate bounds retain distinct case sensitive paths`() {
        assertEquals(8, ContentCandidateExtractor.extract((1..20).joinToString(" ") { "https://host$it.example/" }).size)
        assertEquals(2, ContentCandidateExtractor.extract("https://example.com/A https://example.com/a").size)
        assertTrue(ContentCandidateExtractor.extract("a@bank.example 1.2.3 file:///etc/private.com").isEmpty())
    }
    @Test fun `multilingual wrappers preserve visible domain`() {
        for (text in listOf("यहाँ example.com/help देखें", "અહીં example.com/help જુઓ", "Open example.com/help"))
            assertEquals("https://example.com/help", ContentCandidateExtractor.extract(text).single().inspectionValue)
    }
    @Test fun `wrapped lines are additional confirmed candidates and never silent replacements`() {
        val candidates = ContentCandidateExtractor.extract("https://example.com/\nAccount")
        assertEquals(listOf("https://example.com/", "https://example.com/Account"), candidates.map { it.inspectionValue })
        assertTrue(CandidateChange.LINE_BREAK in candidates.last().changes)
        assertTrue(candidates.last().needsConfirmation)
        assertEquals("https://ઉદાહરણ.ભારત/help", ContentCandidateExtractor.extract("ઉદાહરણ.ભારત/help").single().inspectionValue)
    }
}
