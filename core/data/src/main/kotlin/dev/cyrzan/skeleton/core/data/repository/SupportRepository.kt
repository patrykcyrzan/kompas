package dev.cyrzan.skeleton.core.data.repository

import dev.cyrzan.skeleton.core.model.OrderId

data class SupportRequest(
    val topic: String,
    val orderId: OrderId?,
    val description: String,
    val email: String,
)

interface SupportRepository {
    /** Sends the request; returns the ticket number. */
    suspend fun submit(request: SupportRequest): String
}
