package com.sentinel.ai.core.model

import org.junit.Assert.*
import org.junit.Test

class MessageSignalsTest {
    @Test fun `detects no link credential theft`() {
        val result = MessageSignals.analyze("Bank support: urgent! Share your OTP and password immediately or your account will be blocked.")
        assertEquals(100f, result.score, 0f)
        assertEquals("Credential theft", result.category)
    }
    @Test fun `advice does not erase a separate attack sentence`() {
        assertTrue(MessageSignals.analyze("Never share your OTP. But send your OTP to me immediately.").score >= 70)
    }
    @Test fun `benign controls remain low risk`() {
        listOf("Never share your OTP or PIN with anyone.", "Your payment was successful.",
            "Beware of anyone asking you to send an OTP for a refund", "Your meeting is urgent please join the office call").forEach {
            assertTrue(it, MessageSignals.analyze(it).score < 30)
        }
    }
    @Test fun `supports basic hindi and hinglish rules`() {
        assertTrue(MessageSignals.analyze("बैंक खाता बंद होगा तुरंत अपना ओटीपी भेजो").score >= 70)
        assertTrue(MessageSignals.analyze("Bank verification send OTP jaldi account block ho jayega").score >= 70)
    }
    @Test fun `normalizes hidden characters and limits distinct links`() {
        assertTrue(MessageSignals.analyze("Urgent share your O\u200BTP password").score >= 70)
        val urls = MessageSignals.urls((1..20).joinToString(" ") { "https://$it.example/path" })
        assertEquals(8, urls.size)
        assertEquals(listOf("https://example.com"), MessageSignals.urls("See https://example.com)."))
    }
    @Test fun `block cannot open and non links cannot open`() {
        val result = ScanResult("a", "test", riskLevel = RiskLevel.CRITICAL, riskScore = 100f, explanation = "risk", timestamp = 0, target = "https://example.com")
        assertFalse(DecisionPolicy.canOpen(result))
        assertFalse(DecisionPolicy.canOpen(result.copy(riskLevel = RiskLevel.GREEN, decision = ProtectionDecision.ALLOW, contentType = "message")))
    }
    @Test fun `native hindi and gujarati credential scams and safety advice are distinct`() {
        listOf("बैंक सहायता तुरंत अपना ओटीपी और पासवर्ड भेजें", "બેંક સહાય હમણાં તમારો ઓટીપી અને પાસવર્ડ મોકલો",
            "કેવાયસી માટે તમારો પિન અને સુરક્ષા કોડ આપો").forEach { text ->
            val result = MessageSignals.analyze(text)
            assertTrue(text, result.score >= 70f)
            assertEquals("Credential theft", result.category)
        }
        listOf("अपना ओटीपी या पासवर्ड किसी को न बताएं।", "તમારો ઓટીપી કે પાસવર્ડ ક્યારેય કોઈને આપશો નહીં.",
            "बैंक कभी आपका पिन नहीं माँगता", "બેંક તમારો પિન માગતી નથી સુરક્ષા કોડ શેર ન કરો.").forEach { text ->
            assertEquals(text, 0f, MessageSignals.analyze(text).score, 0f)
        }
    }
    @Test fun `native reward employment and payment traps are detected`() {
        listOf("आप लॉटरी जीते हैं इनाम पाने के लिए पहले शुल्क जमा करें", "તમે લોટરી જીત્યા છો ઇનામ મેળવવા માટે પહેલા ફી ચૂકવો",
            "नौकरी पक्की है पहले पंजीकरण फीस भुगतान करें", "નોકરીની ગેરંટી છે પહેલા રજિસ્ટ્રેશન ફી જમા કરો",
            "रिफंड प्राप्त करने के लिए क्यूआर स्कैन करें और यूपीआई पिन डालें", "રિફંડ મેળવવા માટે ક્યુઆર સ્કેન કરો અને યુપીઆઈ પિન દાખલ કરો"
        ).forEach { text -> assertTrue(text, MessageSignals.analyze(text).score >= 55f) }
    }
    @Test fun `native advice does not erase separate attack sentence`() {
        assertTrue(MessageSignals.analyze("अपना ओटीपी किसी को न बताएं। लेकिन अभी मुझे अपना ओटीपी भेजें।").score >= 70f)
        assertTrue(MessageSignals.analyze("ઓટીપી ક્યારેય શેર ન કરો. પરંતુ હમણાં તમારો ઓટીપી મને મોકલો.").score >= 70f)
    }
    @Test fun `secrecy clause cannot suppress coercion`() {
        listOf("Police digital arrest transfer payment now and do not tell anyone",
            "पुलिस डिजिटल गिरफ्तारी से बचने के लिए पैसे भेजें और किसी को न बताएं",
            "પોલીસ ડિજિટલ અરેસ્ટથી બચવા હમણાં પૈસા મોકલો અને કોઈને જણાવશો નહીં"
        ).forEach { assertTrue(it, MessageSignals.analyze(it).score >= 70f) }
    }
    @Test fun `refund safety advice is not a payment request in any language`() {
        for (text in listOf("Do not scan a QR code to receive a refund.", "रिफंड पाने के लिए क्यूआर स्कैन मत करें।", "રિફંડ મેળવવા માટે ક્યુઆર સ્કેન ન કરો."))
            assertEquals(text, 0f, MessageSignals.analyze(text).score, 0f)
    }
    @Test fun `financial secrecy is not filtered as protective advice`() {
        for (text in listOf("Send money immediately do not tell anyone.", "पुलिस ने कहा तुरंत पैसे भेजो किसी को मत बताओ।", "પોલીસ કહે છે તરત પૈસા મોકલો કોઈને કહેશો નહીં."))
            assertTrue(text, MessageSignals.analyze(text).score >= 30f)
    }
    @Test fun `prototype classifier needs sensitive action context across languages`() {
        for (text in listOf("Read example.com/help for public information.", "सार्वजनिक जानकारी के लिए example.com/help देखें।", "જાહેર માહિતી માટે example.com/help જુઓ.", "Never share your bank PIN.")) {
            assertFalse(text, MessageSignals.analyze(text).modelWarningEligible)
        }
        for (text in listOf("Urgent bank notice", "तुरंत बैंक सूचना", "તાત્કાલિક બેંક સૂચના")) {
            assertTrue(text, MessageSignals.analyze(text).modelWarningEligible)
        }
    }
    @Test fun `phishing web actions and gift codes are eligible without warning by rules alone`() {
        for (text in listOf("Update your delivery address at https://example.com/form", "यह लिंक खोलें https://example.com/update", "આ લિંક ખોલો https://example.com/update", "Buy gift cards and send the redemption codes")) {
            assertTrue(text, MessageSignals.analyze(text).modelWarningEligible)
        }
        assertEquals(0f, MessageSignals.analyze("Visit https://example.com/docs").score, 0f)
    }
    @Test fun `protective clicking advice remains ineligible`() {
        for (text in listOf("Do not click https://example.com/update", "यह लिंक मत खोलें https://example.com/update", "આ લિંક પર ક્લિક ન કરો https://example.com/update")) {
            assertFalse(text, MessageSignals.analyze(text).modelWarningEligible)
        }
        assertTrue(MessageSignals.analyze("Do not click unknown links. But update your bank details at https://example.com/update now.").modelWarningEligible)
    }
    @Test fun `submit and native pin negation remain protective`() {
        for (text in listOf("Never submit a password on an unexpected page.", "रिफंड पाने के लिए यूपीआई पिन न डालें।", "પાસવર્ડ કોઈને ન મોકલો।", "કોલરને ઓટીપી ન જણાવો।", "રિફંડ મેળવવા યુપીઆઈ પિન ન નાખો।", "કાર્ડનો સુરક્ષા કોડ ન આપો।")) {
            assertEquals(text, 0f, MessageSignals.analyze(text).score, 0f)
            assertFalse(text, MessageSignals.analyze(text).modelWarningEligible)
        }
    }
    @Test fun `native verification and card code requests are sensitive`() {
        for (text in listOf("सत्यापन कोड मुझे भेजें", "ચકાસણી કોડ મને મોકલો", "कार्ड नंबर और सीवीवी डालें", "કાર્ડ નંબર અને સીવીવી દાખલ કરો")) {
            assertTrue(text, MessageSignals.analyze(text).score >= 70f)
        }
    }
    @Test fun `investment guarantee needs a financial action`() {
        assertTrue(MessageSignals.analyze("Guaranteed daily profit transfer funds to my crypto wallet now").score >= 30f)
        assertEquals(0f, MessageSignals.analyze("Investments carry risk. Read about guaranteed returns and crypto fraud.").score, 0f)
    }
}
