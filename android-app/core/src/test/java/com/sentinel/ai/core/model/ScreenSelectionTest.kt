package com.sentinel.ai.core.model

import org.junit.Assert.*
import org.junit.Test

class ScreenSelectionTest {
    @Test fun fractionalEdgesIncludeSelectedPixels() {
        assertEquals(PixelRegion(10, 20, 31, 61), ScreenSelection(.101f, .101f, .301f, .301f).pixels(100, 200))
    }
    @Test fun fullSelectionMatchesWholeImage() { assertEquals(PixelRegion(0, 0, 720, 1600), ScreenSelection.FULL.pixels(720, 1600)) }
    @Test fun outsideCoordinatesAreClamped() { assertEquals(PixelRegion(0, 0, 100, 200), ScreenSelection(-1f, -1f, 2f, 2f).pixels(100, 200)) }
    @Test(expected = IllegalArgumentException::class) fun reversedSelectionIsRejected() { ScreenSelection(.8f, .2f, .3f, .8f).pixels(100, 100) }
    @Test(expected = IllegalArgumentException::class) fun nonFiniteCoordinateIsRejected() { ScreenSelection(Float.NaN, 0f, 1f, 1f).pixels(100, 100) }
    @Test(expected = IllegalArgumentException::class) fun invalidImageDimensionsAreRejected() { ScreenSelection.FULL.pixels(0, 100) }
    @Test fun repeatedTextInDifferentPlacesRemainsSelectable() {
        val result = ExtractionResult.merge(listOf(ExtractedLine("Pay", PixelRegion(0, 0, 40, 20), "latin"), ExtractedLine("Pay", PixelRegion(0, 80, 40, 100), "latin")))
        assertEquals(2, result.lines.size)
    }
    @Test fun nativeScriptReplacesOverlappingLatinMisread() {
        val region = PixelRegion(0, 0, 200, 40)
        val result = ExtractionResult.merge(listOf(ExtractedLine("OTP", region, "latin"), ExtractedLine("आपका OTP", region, "devanagari")))
        assertEquals("आपका OTP", result.text)
    }
    @Test fun overlappingNativeScriptsRemainAvailableForReview() {
        val region = PixelRegion(0, 0, 200, 40)
        val result = ExtractionResult.merge(listOf(ExtractedLine("ओटीपी भेजें", region, "devanagari"), ExtractedLine("ઓટીપી મોકલો", region, "gujarati")))
        assertEquals(2, result.lines.size)
        assertTrue(result.text.contains("ઓટીપી મોકલો"))
    }
    @Test fun nativeMisreadCannotEraseTheOriginalCredentialRequest() {
        val region = PixelRegion(0, 0, 200, 40)
        val result = ExtractionResult.merge(listOf(ExtractedLine("Share your OTP and password", region, "latin"), ExtractedLine("અસ્પષ્ટ શબ્દો", region, "gujarati")))
        assertEquals(2, result.lines.size)
        assertTrue(result.text.contains("Share your OTP and password"))
    }
    @Test fun partialTokenMatchCannotReplaceOtpOrChangeUrlCase() {
        val region = PixelRegion(0, 0, 200, 40)
        val otp = ExtractionResult.merge(listOf(ExtractedLine("Share OTP", region, "latin"), ExtractedLine("Share OTPS શબ્દ", region, "gujarati")))
        assertTrue(otp.text.contains("Share OTP\n"))
        val url = ExtractionResult.merge(listOf(ExtractedLine("https://example.com/Case", region, "latin"), ExtractedLine("लिंक https://example.com/case", region, "devanagari")))
        assertEquals(2, url.lines.size)
    }
    @Test fun readingOrderUsesLocationsRatherThanEngineOrder() {
        val result = ExtractionResult.merge(listOf(ExtractedLine("last", PixelRegion(0, 80, 100, 100), "latin"), ExtractedLine("first", PixelRegion(0, 0, 100, 20), "latin")))
        assertEquals("first\nlast", result.text)
    }
    @Test fun excessiveTextIsExplicitlyPartialAndNeverOverLimit() {
        val result = ExtractionResult.merge(listOf(ExtractedLine("a".repeat(11000), null, "latin"), ExtractedLine("b".repeat(2000), null, "latin")))
        assertTrue(result.partial); assertEquals(11000, result.text.length)
    }
    @Test fun exactTextLimitIsAccepted() {
        val result = ExtractionResult.merge(listOf(ExtractedLine("a".repeat(12000), null, "latin")))
        assertFalse(result.partial); assertEquals(12000, result.text.length)
    }
    @Test fun excessiveCodesAreMarkedPartial() {
        val result = ExtractionResult.merge(emptyList(), (0..8).map { "qr-$it" })
        assertEquals(8, result.qrCodes.size); assertTrue(result.partial)
    }
}
