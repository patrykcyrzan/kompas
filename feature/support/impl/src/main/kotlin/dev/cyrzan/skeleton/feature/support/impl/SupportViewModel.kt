package dev.cyrzan.skeleton.feature.support.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.cyrzan.skeleton.core.data.repository.SupportRepository
import dev.cyrzan.skeleton.core.data.repository.SupportRequest
import dev.cyrzan.skeleton.core.model.OrderId
import dev.cyrzan.skeleton.feature.support.api.SupportRequestNavKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The whole 3-step form on ONE screen (one back stack entry): the current step is part of the state,
 * not of the back stack.
 *
 * - Input and the step live in SavedStateHandle, so they survive process death on any step.
 * - Cleared with the entry when the form is left (close, back from step 1, or done).
 */
@HiltViewModel(assistedFactory = SupportViewModel.Factory::class)
internal class SupportViewModel @AssistedInject constructor(
    @Assisted navKey: SupportRequestNavKey,
    private val savedStateHandle: SavedStateHandle,
    private val supportRepository: SupportRepository,
) : ViewModel() {

    private val orderId: OrderId? = navKey.orderId
    private val sending = MutableStateFlow(false)

    val state: StateFlow<SupportFormState> = combine(
        savedStateHandle.getStateFlow(KEY_STEP, SupportStep.Topic.name),
        savedStateHandle.getStateFlow(KEY_TOPIC, (if (orderId != null) SupportTopic.Order else SupportTopic.Other).name),
        savedStateHandle.getStateFlow(KEY_DESCRIPTION, ""),
        savedStateHandle.getStateFlow(KEY_EMAIL, ""),
        combine(savedStateHandle.getStateFlow<String?>(KEY_TICKET, null), sending, ::Pair),
    ) { step, topic, description, email, (ticket, sending) ->
        SupportFormState(SupportStep.valueOf(step), SupportTopic.valueOf(topic), orderId, description, email, sending, ticket)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SupportFormState(orderId = orderId))

    fun next() {
        val form = state.value
        if (form.canGoNext) savedStateHandle[KEY_STEP] = form.step.next().name
    }

    /** One step back. `false` on the first step – the screen leaves the form. */
    fun previous(): Boolean {
        val form = state.value
        if (form.step == SupportStep.Topic || form.isSent) return false
        savedStateHandle[KEY_STEP] = form.step.previous().name
        return true
    }

    fun onTopicChange(topic: SupportTopic) {
        savedStateHandle[KEY_TOPIC] = topic.name
    }

    fun onDescriptionChange(description: String) {
        savedStateHandle[KEY_DESCRIPTION] = description.take(SupportFormState.MAX_DESCRIPTION)
    }

    fun onEmailChange(email: String) {
        savedStateHandle[KEY_EMAIL] = email.trim()
    }

    fun submit() {
        val form = state.value
        if (!form.canSubmit) return
        sending.value = true
        viewModelScope.launch {
            try {
                val ticket = supportRepository.submit(SupportRequest(form.topic.name, form.orderId, form.description.trim(), form.email))
                savedStateHandle[KEY_TICKET] = ticket
            } finally {
                sending.value = false
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: SupportRequestNavKey): SupportViewModel
    }

    private companion object {
        const val KEY_STEP = "step"
        const val KEY_TOPIC = "topic"
        const val KEY_DESCRIPTION = "description"
        const val KEY_EMAIL = "email"
        const val KEY_TICKET = "ticket"
    }
}

internal enum class SupportTopic { Order, Payment, Account, Other }

internal enum class SupportStep {
    Topic, Details, Summary;

    val number: Int get() = ordinal + 1

    fun next(): SupportStep = entries.getOrElse(ordinal + 1) { this }

    fun previous(): SupportStep = entries.getOrElse(ordinal - 1) { this }
}

/** The whole form; each step validates its own part. Pure – unit-tested. */
internal data class SupportFormState(
    val step: SupportStep = SupportStep.Topic,
    val topic: SupportTopic = SupportTopic.Other,
    val orderId: OrderId? = null,
    val description: String = "",
    val email: String = "",
    val sending: Boolean = false,
    val ticket: String? = null,
) {
    val detailsValid: Boolean
        get() = description.trim().length >= MIN_DESCRIPTION && EMAIL.matches(email)

    val isSent: Boolean get() = ticket != null

    val canGoNext: Boolean
        get() = when (step) {
            SupportStep.Topic -> true
            SupportStep.Details -> detailsValid
            SupportStep.Summary -> false
        }

    val canSubmit: Boolean get() = detailsValid && !sending && !isSent

    companion object {
        const val MIN_DESCRIPTION = 10
        const val MAX_DESCRIPTION = 1_000
        private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
