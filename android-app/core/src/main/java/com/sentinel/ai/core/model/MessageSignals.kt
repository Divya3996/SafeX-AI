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
            (Regex("\\b(never|do not|don't)\\s+(share|send|reveal|disclose|give|tell|provide|submit|confirm|scan|pay|transfer|install|enter|click|tap|open|visit|follow|reply|call)\\b").containsMatchIn(it) || it.trimStart().startsWith("beware of") ||
                Regex("""(कभी.*नहीं|कभी.*(न बत|न दें|न भेज|साझा न|शेयर न)|मत (बत|भेज|दे|शेयर|साझा|स्कैन|स्केन|कर|भुगतान|क्लिक|खोल)|(?:^|\s)न\s+(बत|दें|भेज|करें|करे|शेयर|डाल|दर्ज|भर)|साझा न|शेयर न|क्लिक न|बताएं नहीं|भेजें नहीं|ઓટીપી.*(શેર ન|આપશો નહીં|જણાવશો નહીં)|ક્યારેય.*(ન કરો|ન આપ|ન મોકલ|ન જણાવ|ન શેર)|(?:^|\s)(?:ન|ના)\s+(મોકલ|જણાવ|આપ|નાખ|દાખલ|શેર|ખોલ|ક્લિક|ભર|ચુકવ)|શેર (ન|ના) કરો|આપશો નહીં|મોકલશો નહીં|જણાવશો નહીં|ખોલશો નહીં|(સ્કેન|સ્કૅન|ચુકવણી|ઇન્સ્ટોલ|ક્લિક).*ન કરો|સાવધાન રહો|सावधान रहें)""").containsMatchIn(it)) &&
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
        val credentials = matches("\\b(otp|password|pin|verification code|security code|bank details|card number|card details|cvv|passcode)\\b|ओटीपी|पासवर्ड|पिन|गोपनीय कोड|सुरक्षा कोड|सत्यापन कोड|बैंक विवरण|कार्ड नंबर|सीवीवी|ઓટીપી|પાસવર્ડ|પિન|ગુપ્ત કોડ|સુરક્ષા કોડ|ચકાસણી કોડ|બેંકની વિગતો|કાર્ડ નંબર|સીવીવી")
        val request = matches("\\b(share|send|tell|provide|enter|reveal|confirm|submit|give)\\b|भेज|बताओ|बताएं|बताइए|दें|दीजिए|डालें|दर्ज|साझा|शेयर|पुष्टि|મોકલ|જણાવ|આપો|દાખલ|શેર|પુષ્ટિ|ભરો|bhejo|batao")
        val money = matches("\\b(pay|transfer|deposit|fee|payment|upi|refund|wallet|bank|rupees|rs|fine|toll|bill)\\b|₹|पैसे|भुगतान|रुपये|शुल्क|जमा|रिफंड|यूपीआई|बैंक|बिल|जुर्माना|પૈસા|પૈસો|ચુકવણી|રૂપિયા|ફી|જમા|રિફંડ|યુપીઆઈ|યુપી[આઇઈ]+|બેંક|બિલ|દંડ")
        val financialAction = matches("\\b(pay|transfer|deposit|withdraw|buy|purchase)\\b|जमा करें|भुगतान करें|भरें|पैसे भेज|चुकाएं|ચુકવો|જમા કરો|ભરો|પૈસા મોકલ")
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
        if (money && financialAction && matches("\\b(guaranteed|risk[- ]free|double your money)\\b|मुनाफे की गारंटी|निश्चित मुनाफा|નફાની ગેરંટી") &&
            matches("\\b(profit|returns|investment|crypto)\\b|मुनाफा|मुनाफे|निवेश|क्रिप्टो|નફો|નફાની|રોકાણ|ક્રિપ્ટો")) {
            signal(35f, "Promises guaranteed returns while requesting a transfer")
            if (category == "General") category = "Investment / crypto request"
            advice = "Verify investment claims and the recipient independently before transferring money."
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
        // Eligibility for the action-context model path; research vocabulary warnings use a separate higher threshold.
        val webAction = matches("\\b(click|tap|visit|open|follow|login|log in|sign in|update|validate|activate|claim|redeem)\\b|क्लिक|टैप|लिंक खोल|लॉगिन|लॉग इन|अपडेट|सत्यापन|દબાવો|ક્લિક|લિંક ખોલ|લોગિન|અપડેટ|ચકાસણી")
        val callback = matches("\\b(call|reply|contact|text us)\\b|कॉल|जवाब दें|संपर्क करें|કોલ|જવાબ આપો|સંપર્ક કરો")
        val giftCard = matches("\\bgift cards?\\b|redemption codes?|गिफ्ट कार्ड|રિડેમ્પશન કોડ|ગિફ્ટ કાર્ડ")
        val giftAction = matches("\\b(buy|purchase|send|share|give)\\b|खरीद|भेज|साझा|खरीदो|ખરીદ|મોકલ|શેર")
        val modelWarningEligible = ((credentials || money || prize || jobs || authority) && (request || urgency || webAction || callback || financialAction)) ||
            (webAction && urls(active).isNotEmpty()) || (giftCard && giftAction)
        return Result(score.coerceIn(0f, 100f), category, reasons.distinct(), advice, active, modelWarningEligible)
    }
}
