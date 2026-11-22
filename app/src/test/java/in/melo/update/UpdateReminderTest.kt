package `in`.melo.update

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class UpdateReminderTest {
    private val today = LocalDate.of(2026, 11, 22)

    @Test fun firstOfferAndNewerReleaseAreImmediate() {
        assertTrue(UpdateReminder.isDue("v1.2.4", null, null, today))
        assertTrue(UpdateReminder.isDue("v1.2.5", "v1.2.4", today.toString(), today))
    }

    @Test fun sameReleaseStaysDeferredForTheCurrentLocalDate() {
        assertFalse(UpdateReminder.isDue("v1.2.4", "v1.2.4", today.toString(), today))
        assertFalse(UpdateReminder.isDue("1.2.4", "v1.2.4", today.toString(), today))
        assertTrue(UpdateReminder.isDue("v1.2.4", "1.2.4", today.minusDays(1).toString(), today))
    }

    @Test fun invalidSavedVersionOrDateDoesNotBlockReminders() {
        assertTrue(UpdateReminder.isDue("v1.2.4", "invalid", today.toString(), today))
        assertTrue(UpdateReminder.isDue("v1.2.4", "v1.2.4", "invalid", today))
        assertTrue(UpdateReminder.isDue("v1.2.4", "v1.2.4", null, today))
    }
}
