package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.ZoneCheck
import com.peakdevcol.project.hotelmanagement.domain.model.check
import com.peakdevcol.project.hotelmanagement.domain.model.distanceMeters
import com.peakdevcol.project.hotelmanagement.domain.model.parseCoordinate
import com.peakdevcol.project.hotelmanagement.domain.model.parseCoordinatePair
import com.peakdevcol.project.hotelmanagement.fakes.hotelSettings
import com.peakdevcol.project.hotelmanagement.fakes.readingNorthOfHotel
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** RF-ASI-10. */
class HotelZoneTest {
    private val zone = hotelSettings.zone!!

    @Test
    fun aDegreeOfLatitudeIsAbout111Kilometers() {
        val meters = distanceMeters(4.0, -74.0, 5.0, -74.0)

        assertTrue(abs(meters - 111_195) < 10, "was $meters")
    }

    @Test
    fun insideTheRadiusKeepsTheDistanceAndTheAccuracy() {
        val check = zone.check(readingNorthOfHotel(degrees = 0.001, accuracyMeters = 20.4))

        assertEquals(ZoneCheck.Inside(distanceMeters = 111, accuracyMeters = 20), check)
    }

    @Test
    fun outsideTheRadiusSaysHowFar() {
        val check = assertIs<ZoneCheck.Outside>(zone.check(readingNorthOfHotel(degrees = 0.0045)))

        assertEquals(500, check.distanceMeters)
    }

    @Test
    fun aReadingWorseThanTheMinimumAccuracyIsNotTrustedEvenAtTheHotel() {
        assertEquals(ZoneCheck.Imprecise(accuracyMeters = 140), zone.check(readingNorthOfHotel(accuracyMeters = 140.0)))
    }

    @Test
    fun aSimulatedLocationIsRejectedEvenInsideTheZone() {
        assertEquals(ZoneCheck.Simulated, zone.check(readingNorthOfHotel(simulated = true)))
    }

    @Test
    fun thereIsNoZoneUntilTheHotelHasBothCoordinates() {
        assertNull(hotelSettings.copy(hotelLongitude = null).zone)
    }

    @Test
    fun coordinatesAreTypedWithADotOrAComma() {
        assertEquals(4.60971, parseCoordinate(" 4,60971 "))
        assertEquals(-74.08175, parseCoordinate("-74.08175"))
        assertNull(parseCoordinate("norte"))
    }

    @Test
    fun bothCoordinatesArePastedTogetherFromAMap() {
        assertEquals(4.60971 to -74.08175, parseCoordinatePair("4.60971, -74.08175"))
        assertNull(parseCoordinatePair("4,60971"))
    }
}
