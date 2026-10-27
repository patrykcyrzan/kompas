package dev.cyrzan.skeleton.feature.support.impl

import org.junit.Assert.assertEquals
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

    @Test
    fun `next step only when the current one is valid`() {
        assertTrue(SupportFormState(step = SupportStep.Topic).canGoNext)
        assertFalse(SupportFormState(step = SupportStep.Details).canGoNext)
        assertTrue(valid.copy(step = SupportStep.Details).canGoNext)
        assertFalse(valid.copy(step = SupportStep.Summary).canGoNext)
    }

    @Test
    fun `steps move one at a time and stop at the ends`() {
        assertEquals(SupportStep.Details, SupportStep.Topic.next())
        assertEquals(SupportStep.Summary, SupportStep.Summary.next())
        assertEquals(SupportStep.Topic, SupportStep.Topic.previous())
        assertEquals(3, SupportStep.Summary.number)
    }
}
