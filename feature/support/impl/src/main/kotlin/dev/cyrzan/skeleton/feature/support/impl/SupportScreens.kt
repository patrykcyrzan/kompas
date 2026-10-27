package dev.cyrzan.skeleton.feature.support.impl

import androidx.activity.compose.BackHandler
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
import dev.cyrzan.skeleton.feature.support.api.SupportRequestNavKey

private val STEPS = SupportStep.entries.size

/**
 * The whole form is one screen: the steps are state of [SupportViewModel], so "back" moves between
 * them here, and only leaves the screen from the first step.
 */
@Composable
internal fun SupportRoute(
    navKey: SupportRequestNavKey,
    onClose: () -> Unit,
    viewModel: SupportViewModel = hiltViewModel<SupportViewModel, SupportViewModel.Factory> { factory -> factory.create(navKey) },
) {
    val form by viewModel.state.collectAsStateWithLifecycle()
    // Steps 2 and 3: back = previous step. After sending, back closes the form like "Done".
    BackHandler(enabled = form.step != SupportStep.Topic) {
        if (!viewModel.previous()) onClose()
    }
    when (form.step) {
        SupportStep.Topic -> TopicStep(form, onTopicChange = viewModel::onTopicChange, onNext = viewModel::next, onClose = onClose)
        SupportStep.Details -> DetailsStep(
            form,
            onDescriptionChange = viewModel::onDescriptionChange,
            onEmailChange = viewModel::onEmailChange,
            onNext = viewModel::next,
            onBack = { viewModel.previous() },
        )
        SupportStep.Summary -> SummaryStep(form, onSubmit = viewModel::submit, onBack = { viewModel.previous() }, onDone = onClose)
    }
}

@Composable
private fun TopicStep(form: SupportFormState, onTopicChange: (SupportTopic) -> Unit, onNext: () -> Unit, onClose: () -> Unit) {
    SupportStepScaffold(step = form.step, title = "What is it about?", onUp = onClose, upIsClose = true, action = "Next", actionEnabled = form.canGoNext, onAction = onNext) {
        form.orderId?.let { Text("About order #$it", style = MaterialTheme.typography.bodyLarge) }
        SupportTopic.entries.forEach { topic ->
            ListItem(
                headlineContent = { Text(topic.name) },
                leadingContent = { RadioButton(selected = form.topic == topic, onClick = null) },
                modifier = Modifier.selectable(selected = form.topic == topic, onClick = { onTopicChange(topic) }),
            )
        }
    }
}

@Composable
private fun DetailsStep(
    form: SupportFormState,
    onDescriptionChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    SupportStepScaffold(step = form.step, title = "Describe the problem", onUp = onBack, action = "Next", actionEnabled = form.canGoNext, onAction = onNext) {
        OutlinedTextField(
            value = form.description,
            onValueChange = onDescriptionChange,
            label = { Text("Description (min. ${SupportFormState.MIN_DESCRIPTION} characters)") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.email,
            onValueChange = onEmailChange,
            label = { Text("E-mail for the reply") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SummaryStep(form: SupportFormState, onSubmit: () -> Unit, onBack: () -> Unit, onDone: () -> Unit) {
    val ticket = form.ticket
    SupportStepScaffold(
        step = form.step,
        title = if (ticket == null) "Check and send" else "Sent",
        onUp = if (ticket == null) onBack else onDone,
        upIsClose = ticket != null,
        action = if (ticket == null) "Send" else "Done",
        actionEnabled = if (ticket == null) form.canSubmit else true,
        busy = form.sending,
        onAction = if (ticket == null) onSubmit else onDone,
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
private fun SupportStepScaffold(
    step: SupportStep,
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
                    title = { Text("Contact support · ${step.number}/$STEPS") },
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
                LinearProgressIndicator(progress = { step.number / STEPS.toFloat() }, modifier = Modifier.fillMaxWidth())
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
