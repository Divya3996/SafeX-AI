package com.sentinel.ai.ui.screens.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sentinel.ai.core.model.*
import com.sentinel.ai.ui.i18n.LocalizedText as Text

@Composable
internal fun LinkIdentityCard(inspection: LinkInspection) {
    ReviewPanel("Understand this destination", Icons.Default.Language) {
        Fact("Website domain", inspection.registrableDomain)
        if (inspection.host != inspection.registrableDomain) Fact("Full host", inspection.host)
        Text("The website domain identifies the destination. Brand names elsewhere in a link do not prove ownership.", style = MaterialTheme.typography.bodySmall)
        if (inspection.unicodeHost != inspection.host) {
            Fact("International spelling", inspection.unicodeHost)
            Text("International characters can resemble another brand. Compare both spellings carefully.", style = MaterialTheme.typography.bodySmall)
        }
        Text(if (inspection.usesHttps) "HTTPS encrypts the connection; it does not prove that a website is legitimate." else "This link uses HTTP. Information entered there may not be protected in transit.", style = MaterialTheme.typography.bodySmall)
        if (inspection.hasUserInfo) Text("Text before @ is login information, not the destination website.", color = MaterialTheme.colorScheme.error)
        if (inspection.isNumericAddress) Text("The destination is a numeric network address, not a named website.")
        if (inspection.embeddedHosts.isNotEmpty()) {
            Text("Other destinations written inside this link", style = MaterialTheme.typography.titleSmall)
            inspection.embeddedHosts.distinct().forEach { Fact("Embedded host", it) }
            Text("These are visible instructions only. SafeX AI has not followed redirects or opened the page.", style = MaterialTheme.typography.bodySmall)
        }
        inspection.signals.take(4).forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
internal fun PaymentReviewCard(review: PaymentQrReview, onApply: (ContextAnswers) -> Unit, saving: Boolean) {
    var compare by rememberSaveable { mutableStateOf(false) }
    var expectedAddress by rememberSaveable { mutableStateOf("") }
    var expectedAmount by rememberSaveable { mutableStateOf("") }
    val recipientMismatch = review.recipientMismatch(expectedAddress)
    val amountMismatch = review.amountMismatch(expectedAmount)
    ReviewPanel("Review this UPI payment", Icons.Default.Payments) {
        Fact("Payment address", review.address ?: "Not readable", localizeValue = review.address == null)
        Fact("Name supplied by QR • unverified", review.suppliedName ?: "Not supplied", localizeValue = review.suppliedName == null)
        Fact("Requested amount", review.amount ?: "Not fixed in QR", localizeValue = review.amount == null)
        Fact("Currency", review.currency ?: "Not readable", localizeValue = review.currency == null)
        Text("SafeX AI cannot verify the bank account or recipient identity offline. Confirm the bank-verified name and final amount in your trusted payment app.", style = MaterialTheme.typography.bodySmall)
        Text("Scanning a payment QR is not necessary to receive money. Never enter a UPI PIN to receive a payment.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        review.issues.forEach { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        TextButton(onClick = { compare = !compare }) { Text(if (compare) "Hide comparison" else "Compare expected details") }
        if (compare) {
            OutlinedTextField(expectedAddress, { expectedAddress = it.take(256) }, label = { Text("Expected UPI address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(expectedAmount, { expectedAmount = it.take(24) }, label = { Text("Expected amount • optional") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
            if (recipientMismatch) Text("The QR payment address does not match the recipient you expected.", color = MaterialTheme.colorScheme.error)
            if (amountMismatch) Text("The QR payment amount is missing or does not match the amount you expected.", color = MaterialTheme.colorScheme.error)
            if ((expectedAddress.isNotBlank() || expectedAmount.isNotBlank()) && !recipientMismatch && !amountMismatch)
                Text("Matches the details you entered. Recipient identity is still unverified.", style = MaterialTheme.typography.bodySmall)
            if (recipientMismatch || amountMismatch) Button(onClick = {
                onApply(ContextAnswers(recipientMismatch = recipientMismatch, amountMismatch = amountMismatch))
            }, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("Add mismatch to this result") }
        }
    }
}

@Composable
internal fun ContextReviewCard(result: ScanResult, onApply: (ContextAnswers) -> Unit, saving: Boolean) {
    var expanded by rememberSaveable(result.id) { mutableStateOf(false) }
    val saver = androidx.compose.runtime.saveable.listSaver<ContextAnswers, Boolean>(
        save = { listOf(it.unexpected, it.asksForSecret, it.asksForPayment, it.asksToInstall, it.pressuresYou, it.recipientMismatch, it.amountMismatch) },
        restore = { ContextAnswers(it[0], it[1], it[2], it[3], it[4], it[5], it[6]) })
    var answers by rememberSaveable(result.id, result.contextReview, stateSaver = saver) { mutableStateOf(result.contextReview ?: ContextAnswers()) }
    ReviewPanel("Check the situation", Icons.AutoMirrored.Filled.FactCheck) {
        Text("A normal-looking link can still be used in a scam. Your answers can add caution; they never remove existing warnings.", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide questions" else "Add context") }
        if (expanded) {
            Question("Were you not expecting this message or request?", answers.unexpected) { answers = answers.copy(unexpected = it) }
            Question("Does it ask you to share an OTP, PIN or password?", answers.asksForSecret) { answers = answers.copy(asksForSecret = it) }
            Question("Does it ask you to send money or pay a fee?", answers.asksForPayment) { answers = answers.copy(asksForPayment = it) }
            Question("Does it ask you to install an app or allow remote access?", answers.asksToInstall) { answers = answers.copy(asksToInstall = it) }
            Question("Are you being pressured to act immediately?", answers.pressuresYou) { answers = answers.copy(pressuresYou = it) }
            Text("Select the statements that apply. Leave others unchecked.", style = MaterialTheme.typography.bodySmall)
            Button(onClick = { onApply(answers) }, enabled = !saving && answers != ContextAnswers(), modifier = Modifier.fillMaxWidth()) {
                Text(if (saving) "Saving review…" else "Update risk review")
            }
        }
    }
}

@Composable
private fun Question(text: String, selected: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.toggleable(selected, role = Role.Checkbox, onValueChange = onChange), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Checkbox(selected, null)
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun ReviewPanel(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            content()
        }
    }
}

@Composable
private fun Fact(label: String, value: String, localizeValue: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelectionContainer { Text(value, localize = localizeValue, style = MaterialTheme.typography.bodyLarge) }
    }
}
