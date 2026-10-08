package com.sentinel.ai.ui.screens.help

import android.content.Intent
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sentinel.ai.ui.i18n.LocalizedText as Text
import com.sentinel.ai.ui.screens.scanner.ReviewPanel

enum class IncidentType(val label: String, val steps: List<String>) {
    CLICKED("I opened a link", listOf(
        "Close the page. Do not enter information, download files or grant permissions.",
        "If you only opened the page, that alone does not prove your device is compromised. Check for unexpected downloads and permission changes.",
        "If you shared information, installed an app or paid, choose that situation below for additional steps.")),
    PASSWORD("I shared a password", listOf(
        "From a device you trust, open the service's official app or type its website address yourself. Change the exposed password.",
        "Change that password anywhere else you reused it. Review active sessions and sign out unknown devices.",
        "Enable multi-factor authentication and check recovery email, phone number and recent account activity.")),
    OTP("I shared an OTP or PIN", listOf(
        "Contact the affected bank or service immediately through its official app or a number you already trust.",
        "Ask about securing the account and stopping unauthorized transactions. Change an exposed PIN through the official service.",
        "Check recent transactions. If money was lost, use the payment incident steps and report promptly.")),
    INSTALLED("I installed an app", listOf(
        "Stop using the suspicious app. If someone is remotely controlling your phone, disconnect it from the internet.",
        "Use a different trusted device to contact your bank and secure affected accounts.",
        "Review and revoke the app's accessibility and device administrator access, then uninstall it. Run Google Play Protect and install security updates.",
        "Preserve the app name and relevant evidence. SafeX AI cannot confirm or remove a device infection.")),
    PAID("I sent money", listOf(
        "Contact your bank or payment provider immediately using official contact details. Report the transaction and ask what actions are available.",
        "In India, call 1930 for financial cyber fraud and submit a complaint at cybercrime.gov.in.",
        "Keep the transaction reference, date, amount and relevant messages. Never include passwords, PINs or OTPs in shared evidence.",
        "Do not pay anyone who promises to recover your money. Reporting does not guarantee recovery."))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IncidentHelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var chosen by rememberSaveable { mutableStateOf(IncidentType.CLICKED.name) }
    val incident = IncidentType.valueOf(chosen)
    var completed by rememberSaveable { mutableStateOf(listOf<String>()) }
    fun launch(intent: Intent) {
        if (runCatching { context.startActivity(intent) }.isFailure)
            Toast.makeText(context, com.sentinel.ai.core.i18n.I18n.translate(context, "No app is available for this action."), Toast.LENGTH_LONG).show()
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Take the next safe step", style = MaterialTheme.typography.headlineLarge)
        Text("Choose what happened. These checklists work offline; calls and official websites open only when you choose them.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            IncidentType.entries.forEach { type -> FilterChip(chosen == type.name, { chosen = type.name }, label = { Text(type.label) }) }
        }
        ReviewPanel(incident.label, Icons.Default.HealthAndSafety) {
            incident.steps.forEachIndexed { index, step ->
                val key = "${incident.name}:$index"
                Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.toggleable(key in completed, role = Role.Checkbox,
                    onValueChange = { completed = if (it) completed + key else completed - key }), verticalAlignment = androidx.compose.ui.Alignment.Top) {
                    Checkbox(key in completed, null)
                    Text(step, modifier = Modifier.weight(1f).padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        ReviewPanel("Official help in India", Icons.Default.SupportAgent) {
            Text("1930 • financial cyber fraud helpline", style = MaterialTheme.typography.titleSmall)
            Button(onClick = { launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"))) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Open dialer • 1930") }
            OutlinedButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cybercrime.gov.in/"))
                val handlers = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).filter { it.activityInfo.packageName != context.packageName }
                if (handlers.isEmpty()) launch(intent.setPackage("com.android.chrome"))
                else {
                    val options = handlers.map { Intent(intent).setComponent(ComponentName(it.activityInfo.packageName, it.activityInfo.name)) }
                    launch(Intent.createChooser(options.first(), com.sentinel.ai.core.i18n.I18n.translate(context, "Open official reporting website"))
                        .putExtra(Intent.EXTRA_INITIAL_INTENTS, options.drop(1).toTypedArray()))
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Open official reporting website") }
            Text("SafeX AI does not submit reports or call anyone automatically. Website access needs an internet connection in your browser.", style = MaterialTheme.typography.bodySmall)
            Text("Sources: National Cybercrime Reporting Portal, NPCI and Google Account Help.", style = MaterialTheme.typography.labelSmall)
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
