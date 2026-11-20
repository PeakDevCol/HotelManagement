package com.peakdevcol.project.hotelmanagement.domain.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** RF-ASI-10: where marking from a phone is accepted. It is the hotel, never an employee. */
data class HotelZone(
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    /** The worst accuracy of the GPS that is still trusted. */
    val minAccuracyMeters: Int,
)

/**
 * Where the phone is at the moment of marking. It is never stored: only the distance to the
 * hotel and the accuracy are kept (RF-ASI-10, S10).
 */
data class LocationReading(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    /** V5: the location comes from an app that fakes it, which Android and iOS can tell. */
    val isSimulated: Boolean,
)

/** What the zone says about a reading. */
sealed interface ZoneCheck {
    data class Inside(val distanceMeters: Int, val accuracyMeters: Int) : ZoneCheck
    data class Outside(val distanceMeters: Int) : ZoneCheck
    data class Imprecise(val accuracyMeters: Int) : ZoneCheck
    data object Simulated : ZoneCheck
}

fun HotelZone.check(reading: LocationReading): ZoneCheck {
    if (reading.isSimulated) return ZoneCheck.Simulated
    val accuracy = reading.accuracyMeters.roundToInt()
    if (accuracy > minAccuracyMeters) return ZoneCheck.Imprecise(accuracy)
    val distance = distanceMeters(latitude, longitude, reading.latitude, reading.longitude).roundToInt()
    return if (distance <= radiusMeters) ZoneCheck.Inside(distance, accuracy) else ZoneCheck.Outside(distance)
}

private const val EARTH_RADIUS_METERS = 6_371_000.0

/** The distance between two points of the Earth, by the haversine formula. */
fun distanceMeters(fromLatitude: Double, fromLongitude: Double, toLatitude: Double, toLongitude: Double): Double {
    val fromLatitudeRadians = fromLatitude.toRadians()
    val toLatitudeRadians = toLatitude.toRadians()
    val latitudeDelta = (toLatitude - fromLatitude).toRadians()
    val longitudeDelta = (toLongitude - fromLongitude).toRadians()
    val haversine = sin(latitudeDelta / 2).pow(2) + cos(fromLatitudeRadians) * cos(toLatitudeRadians) * sin(longitudeDelta / 2).pow(2)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(haversine))
}

private fun Double.toRadians() = this * kotlin.math.PI / 180

/** A latitude or a longitude as it is typed, with a dot or a comma for the decimals. */
fun parseCoordinate(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

// Maps write the decimals with a dot. A comma alone, as in "4,60971", is a decimal typed by hand.
private val COORDINATE_PAIR = Regex("""^\s*(-?\d+\.\d+)\s*,\s*(-?\d+\.\d+)\s*$""")

/** The latitude and the longitude copied together from a map, like `4.60971, -74.08175`. Null for anything else. */
fun parseCoordinatePair(text: String): Pair<Double, Double>? {
    val match = COORDINATE_PAIR.matchEntire(text) ?: return null
    return match.groupValues[1].toDouble() to match.groupValues[2].toDouble()
}
