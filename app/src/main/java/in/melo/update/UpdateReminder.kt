package `in`.melo.update

import java.time.LocalDate

/** A release is hidden only for the current local calendar day when the user requests it. */
internal object UpdateReminder {
    fun isDue(tag: String, hiddenTag: String?, hiddenDate: String?, today: LocalDate = LocalDate.now()): Boolean {
        val release = Version.parse(tag)
        val hidden = hiddenTag?.let(Version::parse)
        if (release == null || hidden == null || release.compareTo(hidden) != 0) return true
        return hiddenDate != today.toString()
    }
}
