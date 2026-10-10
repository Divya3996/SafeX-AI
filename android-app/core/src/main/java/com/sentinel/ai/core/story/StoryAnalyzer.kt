package com.sentinel.ai.core.story

import com.sentinel.ai.core.model.PaymentQrParser
import com.sentinel.ai.core.model.ProtectionDecision
import com.sentinel.ai.core.model.RiskLevel
import java.text.Normalizer
import java.util.Locale

/** Bounded, evidence-grounded sequence rules. Concern is not a fraud probability. */
object StoryAnalyzer {
    const val VERSION = 1
    private fun rx(value: String) = Regex(value, RegexOption.IGNORE_CASE)
    private val work = rx("\\b(job|work|hiring|recruit|task|commission|earnings)\\b|नौकरी|काम|कमाई|कमीशन|टास्क|નોકરી|કામ|કમાણી|કમિશન|ટાસ્ક")
    private val earnings = rx("\\b(earned|earnings|commission|salary|balance)\\b|कमाई|कमीशन|वेतन|कमाए|बैलेंस|કમાણી|કમિશન|પગાર|કમાયા|બેલેન્સ")
    private val payment = rx("\\b(pay|deposit|transfer|payment|fee)\\b|send\\s+(money|₹|rs|inr)|भुगतान|जमा|पैसे भेज|राशि भेज|शुल्क|फीस|પૈસા મોક|રકમ મોક|ચૂકવ|ચુકવ|જમા કર|ફી|ટ્રાન્સફર")
    private val release = rx("\\b(unlock|withdraw|release)\\b|(?:get|start|receive|activate|before).{0,45}\\b(job|work|hired|paid)\\b|निकाल|जारी|अनलॉक|नौकरी.{0,30}(पहले|पाने)|काम.{0,30}(शुरू|पहले)|ઉપાડ|અનલોક|રિલીઝ|નોકરી.{0,30}(મેળવ|પહેલ)|કામ.{0,30}(શરૂ|પહેલ)|મુક્ત")
    private val education = rx("\\b(course|tuition|college|university|enrollment|invoice)\\b|पाठ्यक्रम|कॉलेज|विश्वविद्यालय|કોર્સ|કૉલેજ|યુનિવર્સિટી|ટ્યુશન")
    private val authority = rx("\\b(bank|rbi|police|cbi|customs|government)\\b|बैंक|पुलिस|सरकार|आरबीआई|બેંક|પોલીસ|સરકાર|આરબીઆઈ")
    private val threat = rx("\\b(suspend|suspended|freeze|frozen|arrest|arrested)\\b|block.{0,24}account|legal.{0,20}(action|case)|खाता.{0,30}(बंद|फ्रीज|निलंबित)|गिरफ्तार|कानूनी कार्रवाई|ખાત.{0,30}(બંધ|ફ્રીઝ|સ્થગિત)|ધરપકડ|કાનૂની કાર્યવાહી")
    private val secret = rx("\\b(otp|pin|password|passcode|recovery phrase|seed phrase)\\b|ओटीपी|पासवर्ड|પિન|ઓટીપી|પાસવર્ડ")
    private val disclose = rx("\\b(share|tell|send|provide|reveal|enter|give|confirm)\\b|साझा|बताएं|बताओ|भेज|दें|જણાવો|આપો|મોકલો|શેર")
    private val support = rx("\\b(support|helpdesk|technical|virus|infected)\\b|help desk|tech support|सपोर्ट|सहायता|तकनीकी|વાયરસ|સપોર્ટ|સહાયતા|ટેક્નિકલ")
    private val remote = rx("anydesk|teamviewer|quicksupport|rustdesk|remote.{0,12}(access|control)|screen.{0,12}shar|रिमोट|स्क्रीन.{0,12}शेयर|રિમોટ|સ્ક્રીન.{0,12}શેર")
    private val install = rx("\\b(install|download|open|enable|grant|connect)\\b|इंस्टॉल|डाउनलोड|खोल|अनुमति|ઇન્સ્ટોલ|ડાઉનલોડ|ખોલો|મંજૂરી|જોડો")
    private val protective = rx("(?:never|do not|don't|dont|avoid|no need to|will not|won't)\\s+(?:ever\\s+)?(?:pay|deposit|transfer|send|share|give|tell|provide|reveal|install|download|enter)|(?:scammers?|fraudsters?)\\s+(?:may\\s+|will\\s+|often\\s+)?(?:ask|request|demand)|(?:scam|fraud|awareness|fictional|synthetic|training)\\s+(?:example|warning|awareness|scenario)|(?:पैसे|राशि|ओटीपी|पासवर्ड).{0,20}(?:न भेज|न दें|न बताएं|साझा न|मत भेज|मत दें)|(?:भुगतान|जमा|साझा|इंस्टॉल).{0,10}(?:न करें|मत करें)|कभी.{0,35}(?:न दें|न भेज|न करें|न बताएं)|नहीं मांग|(?:પૈસા|રકમ|ઓટીપી|પાસવર્ડ).{0,20}(?:ન મોક|ન આપ|ન જણાવ|શેર ન)|(?:ચૂકવ|ચુકવ|જમા|શેર|ઇન્સ્ટોલ).{0,12}(?:ન કરો|કરશો નહીં)|ક્યારેય.{0,35}(?:ન આપ|ન મોક|ન કર|ન જણાવ)|નહીં માંગ")
    private val zeroWidth = Regex("[\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF]")
    fun fingerprint(text: String) = Normalizer.normalize(text, Normalizer.Form.NFKC)
        .replace(zeroWidth, "").lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()
    // URL spelling and invisible characters can affect a verdict. Preserve them
    // when rejecting duplicate input; normalize more broadly only for sequence counts.
    fun duplicateKey(text: String) = text.replace(Regex("\\s+"), " ").trim()

    fun signals(item: StoryItem): List<StorySignal> {
        val signals = mutableListOf<StorySignal>()
        if (rx("^(scam example|scam awareness|fraud example|training example|fictional scenario|quoted example|example\\s*:|उदाहरण\\s*:|जागरूकता|ઉદાહરણ\\s*:)").containsMatchIn(fingerprint(item.text))) return emptyList()
        // Offsets refer to original reviewed text, never to normalized/reconstructed evidence.
        Regex("[^\\n.!?।]+[.!?।]?").findAll(item.text.take(StoryLimits.ITEM_CHARS)).forEach { sentence ->
            val text = fingerprint(sentence.value)
            if (protective.containsMatchIn(text)) return@forEach
            fun add(kind: StorySignalKind, yes: Boolean) { if (yes) signals += StorySignal(item.id, kind, sentence.range.first, sentence.range.last + 1) }
            add(StorySignalKind.WORK, work.containsMatchIn(text))
            add(StorySignalKind.EARNINGS, earnings.containsMatchIn(text))
            add(StorySignalKind.PAYMENT, payment.containsMatchIn(text))
            add(StorySignalKind.FEE_RELEASE, payment.containsMatchIn(text) && release.containsMatchIn(text) && !education.containsMatchIn(text))
            add(StorySignalKind.AUTHORITY, authority.containsMatchIn(text))
            add(StorySignalKind.THREAT, threat.containsMatchIn(text))
            add(StorySignalKind.SECRET, secret.containsMatchIn(text) && disclose.containsMatchIn(text))
            add(StorySignalKind.SUPPORT, support.containsMatchIn(text))
            add(StorySignalKind.REMOTE, remote.containsMatchIn(text) && install.containsMatchIn(text))
        }
        return signals.distinctBy { it.kind }.take(9)
    }

    fun analyze(case: StoryCase): StoryAnalysis {
        val items = case.items.distinctBy { fingerprint(it.text) }.take(StoryLimits.ITEMS)
        val order = items.mapIndexed { i, it -> it.id to i }.toMap()
        val all = items.flatMap(::signals)
        fun first(kind: StorySignalKind, from: Int = 0) = all.firstOrNull { it.kind == kind && order.getValue(it.itemId) >= from }
        val findings = mutableListOf<StoryFinding>()
        val workSignal = first(StorySignalKind.WORK) ?: first(StorySignalKind.EARNINGS)
        if (workSignal != null) {
            val later = first(StorySignalKind.FEE_RELEASE, order.getValue(workSignal.itemId) + 1)
            if (later != null) findings += StoryFinding("task_fee", "Payment to unlock promised work or earnings",
                "An earlier item describes work or earnings. A later item requires payment to unlock them. Together, these match an advance-fee or task-scam pattern.",
                "Pause the payment. Verify the employer through contact details you obtained independently.", StoryConcern.HIGH,
                listOf(workSignal, later), "This pattern can be followed by another fee request. More payments do not verify the promised earnings.")
        }
        val authoritySignal = first(StorySignalKind.AUTHORITY)
        if (authoritySignal != null) {
            val at = order.getValue(authoritySignal.itemId)
            val threatSignal = first(StorySignalKind.THREAT, at)
            val demand = threatSignal?.let { pressure -> all.firstOrNull {
                it.kind in setOf(StorySignalKind.SECRET, StorySignalKind.PAYMENT, StorySignalKind.REMOTE) &&
                    order.getValue(it.itemId) > at && order.getValue(it.itemId) >= order.getValue(pressure.itemId)
            } }
            if (threatSignal != null && demand != null && order.getValue(threatSignal.itemId) <= order.getValue(demand.itemId))
                findings += StoryFinding("authority_pressure", "Authority claim followed by pressure and a demand",
                    "The case combines a claimed bank or government identity, a threat, and a later request for money, a secret or remote access. The claimed identity is not verified.",
                    "Stop and contact the institution through its official app or an independently obtained number.", StoryConcern.HIGH,
                    listOf(authoritySignal, threatSignal, demand), "Requests for secrecy or another transfer can follow this pattern. Verify outside this conversation.")
        }
        val supportSignal = first(StorySignalKind.SUPPORT)
        if (supportSignal != null) {
            val remoteSignal = first(StorySignalKind.REMOTE, order.getValue(supportSignal.itemId) + 1)
            val demand = remoteSignal?.let { remoteEvidence ->
                first(StorySignalKind.SECRET, order.getValue(remoteEvidence.itemId))
                    ?: first(StorySignalKind.PAYMENT, order.getValue(remoteEvidence.itemId))
            }
            if (remoteSignal != null && demand != null) findings += StoryFinding("support_remote", "Support claim followed by remote access and a demand",
                "An earlier item claims to provide support. Later evidence asks for remote access and money or a secret. This combination needs independent verification.",
                "Do not grant more access. Use official support; if access was granted, open Help after a scam.", if (demand.kind == StorySignalKind.SECRET) StoryConcern.HIGH else StoryConcern.REVIEW,
                listOf(supportSignal, remoteSignal, demand), "A request to open banking or share an OTP can follow this pattern. Do not treat remote access as identity verification.")
        }
        val payments = items.mapNotNull { item -> PaymentQrParser.find(item.text).firstOrNull()?.address?.let { item to it.lowercase(Locale.ROOT) } }
        if (payments.map { it.second }.distinct().size > 1) findings += StoryFinding("recipient_changed", "Different payment addresses in this case",
            "The submitted items contain different UPI addresses. This can be legitimate; confirm which recipient you intended before paying. QR labels do not verify ownership.",
            "Compare the payment address with details from a trusted source.", StoryConcern.REVIEW,
            payments.distinctBy { it.second }.take(2).map { StorySignal(it.first.id, StorySignalKind.PAYMENT, 0, it.first.text.length) })
        case.items.take(StoryLimits.ITEMS).forEach { item ->
            val result = item.result
            if (result != null && result.decision != ProtectionDecision.ALLOW) {
                val high = result.riskLevel in setOf(RiskLevel.RED, RiskLevel.CRITICAL) || result.decision == ProtectionDecision.BLOCK
                findings += StoryFinding("item_${item.id}", "An individual check found a warning",
                    "This item already raised a warning in the existing detector. Its result remains visible even when no story pattern matches.",
                    "Read the individual reasons before acting.", if (high) StoryConcern.HIGH else StoryConcern.REVIEW,
                    listOf(StorySignal(item.id, StorySignalKind.THREAT, 0, item.text.length)))
            }
        }
        return StoryAnalysis(findings.maxByOrNull { it.concern.ordinal }?.concern ?: StoryConcern.CONTEXT,
            findings, case.items.take(StoryLimits.ITEMS).any { it.analysisFailed || it.result?.coverageDetails?.assessment in setOf(com.sentinel.ai.core.model.AssessmentCoverage.LIMITED, com.sentinel.ai.core.model.AssessmentCoverage.UNSUPPORTED) })
    }
}
