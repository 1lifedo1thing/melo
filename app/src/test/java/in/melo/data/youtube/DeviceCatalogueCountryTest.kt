package `in`.melo.data.youtube

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceCatalogueCountryTest {
    @Test fun networkCountryTakesPriorityOverSimTimezoneAndLanguage() {
        assertEquals("IN", deviceCatalogueCountry(listOf("in", "gb", "US", "FR")))
    }

    @Test fun wifiOnlyDeviceUsesTheFirstAvailableCountry() {
        assertEquals("IN", deviceCatalogueCountry(listOf(null, "", "IN", "US")))
        assertEquals("FR", deviceCatalogueCountry(listOf(null, null, "001", " fr ")))
    }

    @Test fun invalidOrAbsentValuesUseTheExistingDiscoveryFallback() {
        assertEquals("IN", deviceCatalogueCountry(listOf(null, "", "001", "U1", "IND")))
        assertEquals("IN", deviceCatalogueCountry(emptyList()))
    }
}
