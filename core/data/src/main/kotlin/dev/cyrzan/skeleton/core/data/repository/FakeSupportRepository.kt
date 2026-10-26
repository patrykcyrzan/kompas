package dev.cyrzan.skeleton.core.data.repository

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/** Pretends to talk to the backend. */
internal class FakeSupportRepository @Inject constructor() : SupportRepository {

    private val nextTicket = AtomicInteger(4100)

    override suspend fun submit(request: SupportRequest): String {
        delay(1_500)
        return "T-${nextTicket.incrementAndGet()}"
    }
}
