package com.sentinel.ai.core.model

import java.math.BigDecimal
import java.net.URI
import java.net.URLDecoder
import java.util.Locale

/** Reads payment instructions only. QR labels and address syntax never prove recipient identity. */
data class PaymentQrReview(
    val address: String?,
    val suppliedName: String?,
    val amount: String?,
    val currency: String?,
    val issues: List<String>
) {
    val isInspectable: Boolean get() = address != null && issues.isEmpty()

    fun recipientMismatch(expected: String): Boolean = expected.isNotBlank() &&
        (address == null || !address.equals(expected.trim(), ignoreCase = true))

    fun amountMismatch(expected: String): Boolean {
        if (expected.isBlank()) return false
        val requested = amount?.toBigDecimalOrNull() ?: return true
        val entered = expected.trim().toBigDecimalOrNull() ?: return true
        return entered <= BigDecimal.ZERO || requested.compareTo(entered) != 0
    }
}

object PaymentQrParser {
    private val candidate = Regex("(?i)upi://[^\\s<>\"']+")
    private val address = Regex("[A-Za-z0-9._-]{1,128}@[A-Za-z0-9.-]{1,64}")
    private val amount = Regex("[0-9]{1,12}(?:\\.[0-9]{1,2})?")
    private const val MALFORMED = "Payment QR could not be read reliably. Do not use these details to make a payment."

    fun find(content: String): List<PaymentQrReview> = candidate.findAll(content.take(12000))
        .map { parse(it.value) }.distinct().take(8).toList()

    fun parse(raw: String): PaymentQrReview {
        if (raw.length > 8192) return invalid()
        val uri = runCatching { URI(raw) }.getOrNull() ?: return invalid()
        if (!uri.scheme.equals("upi", true) || !uri.host.equals("pay", true) ||
            uri.rawUserInfo != null || uri.port != -1 || !uri.rawPath.isNullOrEmpty() || uri.rawFragment != null) return invalid()
        val values = linkedMapOf<String, String>()
        val issues = mutableListOf<String>()
        for (part in uri.rawQuery.orEmpty().split('&').filter { it.isNotEmpty() }) {
            val name = decode(part.substringBefore('='))?.lowercase(Locale.ROOT) ?: return invalid()
            val value = decode(part.substringAfter('=', "")) ?: return invalid()
            if (value.any { it.isISOControl() || it.code in 0x200B..0x200F || it.code in 0x202A..0x202E || it.code in 0x2066..0x2069 || it.code == 0xFEFF }) return invalid()
            if (values.containsKey(name)) return invalid()
            values[name] = value
        }
        val pa = values["pa"]?.takeIf { address.matches(it) }
        if (pa == null) issues += "A readable UPI address is missing. This payment format is not supported for recipient review."
        val rawAmount = values["am"]
        val validAmount = rawAmount?.takeIf { amount.matches(it) && it.toBigDecimal() > BigDecimal.ZERO }
        if (rawAmount != null && validAmount == null) issues += "The QR contains an invalid payment amount."
        val currency = values["cu"]?.uppercase(Locale.ROOT) ?: "INR"
        if (currency != "INR") issues += "The payment currency is unsupported. Verify it independently."
        return PaymentQrReview(pa, values["pn"]?.takeIf { it.isNotBlank() }?.take(160), validAmount, currency, issues)
    }
    private fun invalid() = PaymentQrReview(null, null, null, null, listOf(MALFORMED))
    private fun decode(value: String): String? = runCatching { URLDecoder.decode(value, "UTF-8").also { require('\uFFFD' !in it) } }.getOrNull()
}
