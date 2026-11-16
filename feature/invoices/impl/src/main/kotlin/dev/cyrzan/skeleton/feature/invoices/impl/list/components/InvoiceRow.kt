package dev.cyrzan.skeleton.feature.invoices.impl.list.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.cyrzan.skeleton.feature.invoices.impl.domain.Invoice
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency

@Composable
internal fun InvoiceRow(
    invoice: Invoice,
    isDownloading: Boolean,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val amount = remember(invoice.amountMinor, invoice.currency) { formatAmount(invoice.amountMinor, invoice.currency) }
    val date = remember(invoice.issuedOn) { invoice.issuedOn.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) }
    ListItem(
        headlineContent = { Text(invoice.number) },
        supportingContent = { Text("$date · $amount") },
        trailingContent = {
            // Same size in both states – the row doesn't jump when the download starts.
            Box(Modifier.size(width = 72.dp, height = 40.dp), contentAlignment = Alignment.Center) {
                if (isDownloading) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = onDownloadClick) { Text("PDF") }
                }
            }
        },
        modifier = modifier,
    )
}

private fun formatAmount(amountMinor: Long, currencyCode: String): String {
    val currency = Currency.getInstance(currencyCode)
    val format = NumberFormat.getCurrencyInstance().apply { this.currency = currency }
    return format.format(BigDecimal.valueOf(amountMinor, currency.defaultFractionDigits.coerceAtLeast(0)))
}
