package com.sentinel.ai.core.model

import org.junit.Assert.*
import org.junit.Test

class PaymentQrParserTest {
    @Test fun readsAddressAndUnverifiedNameWithoutLaunchingAnything() {
        val qr = PaymentQrParser.parse("upi://pay?pa=shop%40bank&pn=Local%20Shop&am=125.50&cu=INR")
        assertEquals("shop@bank", qr.address)
        assertEquals("Local Shop", qr.suppliedName)
        assertEquals("125.50", qr.amount)
        assertEquals("INR", qr.currency)
        assertTrue(qr.isInspectable)
    }
    @Test fun supportsHindiAndGujaratiNames() {
        assertEquals("दुकान", PaymentQrParser.parse("upi://pay?pa=shop@bank&pn=दुकान").suppliedName)
        assertEquals("દુકાન", PaymentQrParser.parse("upi://pay?pa=shop@bank&pn=દુકાન").suppliedName)
    }
    @Test fun amountMayBeUnspecifiedButCannotMatchAnExpectedAmount() {
        val qr = PaymentQrParser.parse("upi://pay?pa=shop@bank")
        assertNull(qr.amount)
        assertTrue(qr.amountMismatch("50"))
        assertFalse(qr.amountMismatch(""))
    }
    @Test fun comparesDecimalAmountsWithoutFloatingPointRounding() {
        val qr = PaymentQrParser.parse("upi://pay?pa=shop@bank&am=125.50")
        assertFalse(qr.amountMismatch("125.5"))
        assertTrue(qr.amountMismatch("125.51"))
        assertTrue(qr.amountMismatch("bad"))
        assertTrue(qr.amountMismatch("0"))
        assertFalse(qr.recipientMismatch(" SHOP@BANK "))
        assertTrue(qr.recipientMismatch("attacker@bank"))
    }
    @Test fun refusesAmbiguousDuplicateParametersEvenIfTheyHaveTheSameValue() {
        listOf("pa=shop@bank&pa=shop@bank", "pa=shop@bank&PA=attacker@bank", "pa=shop@bank&am=1&am=900", "pa=shop@bank&p%61=other@bank").forEach {
            assertFalse(it, PaymentQrParser.parse("upi://pay?$it").isInspectable)
        }
    }
    @Test fun refusesHandoffsCredentialsFragmentsAndUnexpectedHosts() {
        listOf("intent://pay?pa=shop@bank", "upi://collect?pa=shop@bank", "upi://shop@pay?pa=shop@bank", "upi://pay:80?pa=shop@bank", "upi://pay/path?pa=shop@bank", "upi://pay?pa=shop@bank#hidden", "upi://pay?pa=shop@bank&pn=%ZZ").forEach {
            assertFalse(it, PaymentQrParser.parse(it).isInspectable)
        }
    }
    @Test fun rejectsHiddenControlsAndMisleadingBidiNames() {
        listOf("%0A", "%00", "%E2%80%AE", "%E2%80%8B").forEach {
            assertFalse(PaymentQrParser.parse("upi://pay?pa=shop@bank&pn=$it").isInspectable)
        }
    }
    @Test fun refusesNonPositiveExponentialAndOversizedAmounts() {
        listOf("0", "-1", "1e6", "NaN", "1.999", "99999999999999999999").forEach {
            val qr = PaymentQrParser.parse("upi://pay?pa=shop@bank&am=$it")
            assertFalse(it, qr.isInspectable)
            assertNull(qr.amount)
        }
    }
    @Test fun rejectsMissingOrMalformedAddressAndUnsupportedCurrency() {
        listOf("pn=Bank", "pa=bad", "pa=bad%20name@bank", "pa=shop@bank&cu=USD").forEach {
            assertFalse(it, PaymentQrParser.parse("upi://pay?$it").isInspectable)
        }
    }
    @Test fun findsMultiplePaymentInstructionsInsideExtractedContent() {
        val reviews = PaymentQrParser.find("QR codes:\nupi://pay?pa=one@bank&am=10\nupi://pay?pa=two@bank&am=20")
        assertEquals(listOf("one@bank", "two@bank"), reviews.map { it.address })
        assertEquals(8, PaymentQrParser.find((1..12).joinToString("\n") { "upi://pay?pa=shop$it@bank" }).size)
    }
    @Test fun boundedInputDoesNotProduceAUsablePayment() {
        assertFalse(PaymentQrParser.parse("upi://pay?pa=shop@bank&pn=" + "a".repeat(9000)).isInspectable)
    }
}
