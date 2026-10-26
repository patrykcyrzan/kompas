package dev.cyrzan.skeleton.feature.support.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.cyrzan.skeleton.core.navigation.rememberSharedViewModelScope
import dev.cyrzan.skeleton.feature.support.api.SupportRequestNavKey

private const val STEPS = 3

/** The form's ViewModel – the same instance on every step. */
@Composable
internal fun supportViewModel(): SupportViewModel {
    val scope = rememberSharedViewModelScope<SupportRequestNavKey>()
    return hiltViewModel<SupportViewModel, SupportViewModel.Factory>(scope.owner) { factory -> factory.create(scope.firstKey) }
}

@Composable
internal fun SupportTopicRoute(onNext: () -> Unit, onClose: () -> Unit, viewModel: SupportViewModel = supportViewModel()) {
    val form by viewModel.state.collectAsStateWithLifecycle()
    SupportStep(step = 1, title = "What is it about?", onUp = onClose, upIsClose = true, action = "Next", actionEnabled = true, onAction = onNext) {
        form.orderId?.let { Text("About order #$it", style = MaterialTheme.typography.bodyLarge) }
        SupportTopic.entries.forEach { topic ->
            ListItem(
                headlineContent = { Text(topic.name) },
                leadingContent = { RadioButton(selected = form.topic == topic, onClick = null) },
                modifier = Modifier.selectable(selected = form.topic == topic, onClick = { viewModel.onTopicChange(topic) }),
            )
        }
    }
}

@Composable
internal fun SupportDetailsRoute(onNext: () -> Unit, onBack: () -> Unit, viewModel: SupportViewModel = supportViewModel()) {
    val form by viewModel.state.collectAsStateWithLifecycle()
    SupportStep(step = 2, title = "Describe the problem", onUp = onBack, action = "Next", actionEnabled = form.detailsValid, onAction = onNext) {
        OutlinedTextField(
            value = form.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text("Description (min. ${SupportFormState.MIN_DESCRIPTION} characters)") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("E-mail for the reply") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun SupportSummaryRoute(onDone: () -> Unit, onBack: () -> Unit, viewModel: SupportViewModel = supportViewModel()) {
    val form by viewModel.state.collectAsStateWithLifecycle()
    val ticket = form.ticket
    SupportStep(
        step = 3,
        title = if (ticket == null) "Check and send" else "Sent",
        onUp = if (ticket == null) onBack else onDone,
        upIsClose = ticket != null,
        action = if (ticket == null) "Send" else "Done",
        actionEnabled = if (ticket == null) form.canSubmit else true,
        busy = form.sending,
        onAction = if (ticket == null) viewModel::submit else onDone,
    ) {
        if (ticket != null) {
            Text("Thank you! Your ticket number is $ticket.", style = MaterialTheme.typography.titleMedium)
        }
        SummaryRow("Topic", form.topic.name)
        form.orderId?.let { SummaryRow("Order", "#$it") }
        SummaryRow("Description", form.description.trim())
        SummaryRow("E-mail", form.email)
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    ListItem(overlineContent = { Text(label) }, headlineContent = { Text(value) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupportStep(
    step: Int,
    title: String,
    onUp: () -> Unit,
    action: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
    upIsClose: Boolean = false,
    busy: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Contact support · $step/$STEPS") },
                    navigationIcon = {
                        IconButton(onClick = onUp) {
                            if (upIsClose) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            } else {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                )
                LinearProgressIndicator(progress = { step / STEPS.toFloat() }, modifier = Modifier.fillMaxWidth())
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            content()
            Spacer(Modifier.weight(1f))
            Button(onClick = onAction, enabled = actionEnabled && !busy, modifier = Modifier.fillMaxWidth()) {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(action)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
