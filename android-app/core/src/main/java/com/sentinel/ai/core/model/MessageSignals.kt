package com.sentinel.ai.core.model

import java.text.Normalizer
import java.util.Locale

/** Deterministic local signals, shared by manual and notification entry points. */
object MessageSignals {
    fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKC)
        .replace(Regex("[\\u200B-\\u200D\\uFEFF]"), "").lowercase(Locale.ROOT)
    fun urls(text: String, limit: Int = 8): List<String> = ContentCandidateExtractor.extract(text, limit).map { it.inspectionValue }

    data class Result(val score: Float, val category: String, val reasons: List<String>, val advice: String, val classifierInput: String = "", val modelWarningEligible: Boolean = false)

    fun analyze(text: String): Result {
        val lower = normalize(text)
        val active = lower.split(Regex("[.!?।॥\\n]+\\s*|\\b(?:but|however|and)\\b|लेकिन|परंतु|मगर| और |પરંતુ| અને ")).filterNot {
            (Regex("\\b(never|do not|don't)\\s+(share|send|reveal|disclose|give|tell|provide|scan|pay|transfer|install|enter)\\b").containsMatchIn(it) || it.trimStart().startsWith("beware of") ||
                Regex("""(कभी.*नहीं|कभी.*(न बत|न दें|न भेज|साझा न|शेयर न)|मत (बत|भेज|दे|शेयर|साझा|स्कैन|स्केन|कर|भुगतान)|(?:^|\s)न\s+(बत|दें|भेज|करें|करे|शेयर)|साझा न|शेयर न|बताएं नहीं|भेजें नहीं|ઓટીપી.*(શેર ન|આપશો નહીં|જણાવશો નહીં)|ક્યારેય.*(ન કરો|ન આપ|ન મોકલ|ન જણાવ|ન શેર)|શેર (ન|ના) કરો|આપશો નહીં|મોકલશો નહીં|જણાવશો નહીં|(સ્કેન|સ્કૅન|ચુકવણી|ઇન્સ્ટોલ).*ન કરો|સાવધાન રહો|सावधान रहें)""").containsMatchIn(it)) &&
                !Regex("\\b(but|except|however|instead)\\b|लेकिन|परंतु|मगर|लेकिन अभी|પરંતુ|પણ હમણાં|પણ હવે").containsMatchIn(it)
                && !(Regex("do not tell (anyone|your family)|किसी को मत बता|કોઈને (કહેશો|જણાવશો) નહીં").containsMatchIn(it) &&
                    Regex("pay|transfer|send|money|bank|police|पैसे|भुगतान|बैंक|पुलिस|પૈસા|ચુકવણી|બેંક|પોલીસ").containsMatchIn(it))
        }.joinToString(". ")
        val reasons = mutableListOf<String>()
        var score = 0f
        var category = "General"
        var advice = "Verify unexpected requests through a trusted channel."
        fun matches(pattern: String) = Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(active)
        fun signal(points: Float, reason: String) { score += points; reasons += reason }
        val urgency = matches("\\b(urgent|immediately|act now|suspended|blocked|within \\d+ (minutes|hours)|last chance)\\b|तुरंत|अभी|जल्दी|बंद हो|निलंबित|अवरुद्ध|तत्काल|છેલ્લી તક|હમણાં|તાત્કાલિક|તરત|તુરંત|બંધ થશે|બંધ થઈ|બ્લોક|jaldi")
        val credentials = matches("\\b(otp|password|pin|verification code|security code|bank details|cvv|passcode)\\b|ओटीपी|पासवर्ड|पिन|गोपनीय कोड|सुरक्षा कोड|बैंक विवरण|ઓટીપી|પાસવર્ડ|પિન|ગુપ્ત કોડ|સુરક્ષા કોડ|બેંકની વિગતો")
        val request = matches("\\b(share|send|tell|provide|enter|reveal|confirm|submit|give)\\b|भेज|बताओ|बताएं|बताइए|दें|दीजिए|डालें|दर्ज|साझा|शेयर|पुष्टि|મોકલ|જણાવ|આપો|દાખલ|શેર|પુષ્ટિ|ભરો|bhejo|batao")
        val money = matches("\\b(pay|transfer|deposit|fee|payment|upi|refund|wallet|bank|rupees|rs)\\b|₹|पैसे|भुगतान|रुपये|शुल्क|जमा|रिफंड|यूपीआई|बैंक|પૈસા|પૈસો|ચુકવણી|રૂપિયા|ફી|જમા|રિફંડ|યુપીઆઈ|યુપી[આઇઈ]+|બેંક")
        val prize = matches("\\b(won|winner|lottery|prize|jackpot|free gift|reward|cashback)\\b|इनाम|लॉटरी|पुरस्कार|जीत गए|जीता|कैशबैक|ઇનામ|લોટરી|પુરસ્કાર|જીત્યા|જીત્યું|કેશબેક")
        val jobs = matches("\\b(job|hiring|salary|work from home|part time|loan|approved loan)\\b|नौकरी|ऋण|कर्ज|लोन|वेतन|નોકરી|લોન|પગાર|ઘરેથી કામ")
        val authority = matches("\\b(bank|police|government|customs|income tax|support team|rbi|sbi|hdfc|icici|aadhaar|kyc)\\b|बैंक|पुलिस|सरकार|कस्टम|आधार|केवाईसी|બેંક|પોલીસ|સરકાર|કસ્ટમ|આધાર|કેવાયસી")
        if (credentials && request) {
            signal(70f, "Requests a password, PIN, one-time code, or sensitive account details")
            category = "Credential theft"
            advice = "Do not share passwords, PINs or one-time codes. Contact the organization through its official app."
        }
        if (urgency) signal(12f, "Uses time pressure or threatens loss of access")
        if (authority && (credentials && request || urgency && urls(text).isNotEmpty())) {
            signal(18f, "Claims trusted authority while requesting sensitive action")
            if (category == "General") category = "Bank / KYC impersonation"
        }
        if (prize && (money || request || urls(text).isNotEmpty())) {
            signal(55f, "Offers an unexpected prize or reward and asks you to take action")
            category = "Prize / reward scam"
            advice = "Do not pay to claim a prize. Verify the offer independently."
        }
        if (jobs && matches("\\b(fee|deposit|pay first|registration|processing|guaranteed|advance)\\b|फीस|शुल्क|पहले.*(पैसे|भुगतान|जमा)|पंजीकरण|गारंटी|પ્રથમ.*(ચૂકવ|ચુકવ|જમા)|પહેલા.*(પૈસા|ચૂકવ|ચુકવ|જમા)|ફી|રજિસ્ટ્રેશન|ગેરંટી")) {
            signal(60f, "Requests an upfront fee for a job or loan")
            category = "Job / loan scam"
            advice = "Verify the company or lender independently before paying an upfront fee."
        }
        if (money && matches("\\b(scan.*(receive|refund)|(receive|refund).*scan|upi pin.*receive|receive.*upi pin)\\b|(स्कैन|स्केन|क्यूआर|यूपीआई पिन).*(पाने|प्राप्त|रिफंड|वापस)|(पाने|प्राप्त|रिफंड|वापस).*(स्कैन|स्केन|क्यूआर|यूपीआई पिन)|(સ્કેન|સ્કૅન|ક્યુઆર|પિન).*(મેળવ|રિફંડ)|(મેળવ|રિફંડ).*(સ્કેન|સ્કૅન|ક્યુઆર|પિન)")) {
            signal(85f, "Asks you to scan or enter a UPI PIN to receive money")
            category = "Payment / UPI scam"
            advice = "A UPI PIN authorizes outgoing payments. Check the payee and amount in your payment app."
        }
        if (matches("\\b(digital arrest|arrest warrant|keep.*secret|do not tell|remote access|anydesk|teamviewer)\\b|डिजिटल अरेस्ट|गिरफ्तारी|किसी को मत बता|डिजिटल गिरफ्तारी|દિજિટલ અરેસ્ટ|ડિજિટલ અરેસ્ટ|ધરપકડ|કોઈને કહેશો નહીં|કોઈને જણાવશો નહીં|એનીડેસ્ક") && (money || authority || request)) {
            signal(75f, "Uses coercion, secrecy, or remote-access requests")
            category = "Impersonation / coercion"
            advice = "Stop the conversation. Verify independently and do not install remote-access software."
        }
        if (money && urgency && request && category == "General") {
            signal(35f, "Pressures you to act on a financial request")
            category = "Payment request"
        }
        if (urls(text).isNotEmpty() && urgency) signal(12f, "Combines a link with pressure to act quickly")
        // The small synthetic classifier needs corroborating sensitive-action context.
        // A language token or ordinary educational link alone must not become a scam warning.
        val modelWarningEligible = (credentials || money || prize || jobs || authority) && (request || urgency)
        return Result(score.coerceIn(0f, 100f), category, reasons.distinct(), advice, active, modelWarningEligible)
    }
}
