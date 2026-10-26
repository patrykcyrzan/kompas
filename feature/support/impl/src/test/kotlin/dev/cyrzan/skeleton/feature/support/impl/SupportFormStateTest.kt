package dev.cyrzan.skeleton.feature.support.impl

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportFormStateTest {

    private val valid = SupportFormState(description = "Parcel never arrived", email = "jan@example.com")

    @Test
    fun `details need a long enough description and a valid e-mail`() {
        assertTrue(valid.detailsValid)
        assertFalse(valid.copy(description = "too short").detailsValid)
        assertFalse(valid.copy(email = "jan@").detailsValid)
    }

    @Test
    fun `cannot submit twice`() {
        assertTrue(valid.canSubmit)
        assertFalse(valid.copy(sending = true).canSubmit)
        assertFalse(valid.copy(ticket = "T-1").canSubmit)
    }
}
