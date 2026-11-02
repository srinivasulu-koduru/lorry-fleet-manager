package com.lorryowner.app.network

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import java.util.Locale

/**
 * Safe, zero-crash coordinate resolver for trip origins and destinations.
 *
 * Utilizes Android's native [Geocoder] when online/available, and falls back to an
 * authoritative dictionary of South Indian and national logistics corridors and hubs.
 */
object CityCoordinateResolver {

    private const val TAG = "CityCoordinateResolver"

    private val CITY_COORDINATES = mapOf(
        "tadipatri" to LatLng(14.9123, 78.0125),
        "tadipati" to LatLng(14.9123, 78.0125),
        "tadpatri" to LatLng(14.9123, 78.0125),
        "tadipathri" to LatLng(14.9123, 78.0125),
        "hyderabad" to LatLng(17.3850, 78.4867),
        "secunderabad" to LatLng(17.4399, 78.4983),
        "kurnool" to LatLng(15.8281, 78.0373),
        "kurnul" to LatLng(15.8281, 78.0373),
        "anantapur" to LatLng(14.6819, 77.6006),
        "ananthapur" to LatLng(14.6819, 77.6006),
        "anantapuram" to LatLng(14.6819, 77.6006),
        "ananthapuram" to LatLng(14.6819, 77.6006),
        "atp" to LatLng(14.6819, 77.6006),
        "kadapa" to LatLng(14.4674, 78.8241),
        "cudappah" to LatLng(14.4674, 78.8241),
        "cuddapah" to LatLng(14.4674, 78.8241),
        "bengaluru" to LatLng(12.9716, 77.5946),
        "bangalore" to LatLng(12.9716, 77.5946),
        "vijayawada" to LatLng(16.5062, 80.6480),
        "bezawada" to LatLng(16.5062, 80.6480),
        "guntur" to LatLng(16.3067, 80.4365),
        "gunturu" to LatLng(16.3067, 80.4365),
        "nellore" to LatLng(14.4426, 79.9865),
        "nellur" to LatLng(14.4426, 79.9865),
        "tirupati" to LatLng(13.6288, 79.4192),
        "tirupathi" to LatLng(13.6288, 79.4192),
        "visakhapatnam" to LatLng(17.6868, 83.2185),
        "visakapatnam" to LatLng(17.6868, 83.2185),
        "vizag" to LatLng(17.6868, 83.2185),
        "rajahmundry" to LatLng(17.0005, 81.8040),
        "rajamundry" to LatLng(17.0005, 81.8040),
        "rajamahendravaram" to LatLng(17.0005, 81.8040),
        "kakinada" to LatLng(16.9891, 82.2475),
        "ongole" to LatLng(15.5057, 80.0499),
        "ongolu" to LatLng(15.5057, 80.0499),
        "chittoor" to LatLng(13.2172, 79.1003),
        "chittur" to LatLng(13.2172, 79.1003),
        "hindupur" to LatLng(13.8289, 77.4919),
        "hindupuram" to LatLng(13.8289, 77.4919),
        "dharmavaram" to LatLng(14.4137, 77.7126),
        "warangal" to LatLng(17.9784, 79.5941),
        "nizamabad" to LatLng(18.6725, 78.0941),
        "karimnagar" to LatLng(18.4386, 79.1288),
        "mahbubnagar" to LatLng(16.7488, 77.9856),
        "mahabubnagar" to LatLng(16.7488, 77.9856),
        "nalgonda" to LatLng(17.0575, 79.2684),
        "khammam" to LatLng(17.2473, 80.1514),
        "raichur" to LatLng(16.2076, 77.3463),
        "bellary" to LatLng(15.1394, 76.9214),
        "ballari" to LatLng(15.1394, 76.9214),
        "hubli" to LatLng(15.3647, 75.1240),
        "hubballi" to LatLng(15.3647, 75.1240),
        "nandyal" to LatLng(15.4886, 78.4836),
        "proddatur" to LatLng(14.7526, 78.5522),
        "gooty" to LatLng(15.1147, 77.6366),
        "dhone" to LatLng(15.4200, 77.8700),
        "chennai" to LatLng(13.0827, 80.2707),
        "mumbai" to LatLng(19.0760, 72.8777),
        "pune" to LatLng(18.5204, 73.8567),
        "delhi" to LatLng(28.6139, 77.2090),
        "shamshabad" to LatLng(17.2543, 78.4311),
        "shadnagar" to LatLng(17.0673, 78.2073),
        "jadcherla" to LatLng(16.7667, 78.1333)
    )

    /**
     * Resolves location string into LatLng coordinates.
     */
    fun resolveCoordinates(context: Context?, locationName: String?): LatLng {
        if (locationName.isNullOrBlank()) {
            return CITY_COORDINATES["hyderabad"]!!
        }

        val cleaned = locationName.trim()

        // 1. Check local lookup table first for instant response
        val directMatch = findInLookupTable(cleaned)
        if (directMatch != null) {
            return directMatch
        }

        // 2. Try native Android Geocoder if context is provided
        if (context != null) {
            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocationName(cleaned, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        return LatLng(addr.latitude, addr.longitude)
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Geocoder lookup failed for '$cleaned': ${e.message}")
            }
        }

        // 3. Fallback to default central hub
        return CITY_COORDINATES["hyderabad"]!!
    }

    /**
     * Resolves location purely from lookup dictionary (test-safe without Context).
     */
    fun resolveCityCoordinates(locationName: String?): LatLng {
        return resolveCoordinates(null, locationName)
    }

    fun findInLookupTable(locationName: String?): LatLng? {
        if (locationName.isNullOrBlank()) return null
        val lower = locationName.lowercase(Locale.ROOT).trim()
        for ((city, latLng) in CITY_COORDINATES) {
            if (lower.contains(city)) {
                return latLng
            }
        }
        return null
    }
}
