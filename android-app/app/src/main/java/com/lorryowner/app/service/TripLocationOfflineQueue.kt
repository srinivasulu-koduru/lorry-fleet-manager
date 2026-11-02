package com.lorryowner.app.service

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Thread-safe, bounded persistent offline queue for genuine GPS fixes.
 * Stores up to [MAX_QUEUE_SIZE] locations in app-private internal storage
 * during network dead zones, and flushes them in chronological order when
 * connectivity returns.
 */
class TripLocationOfflineQueue(private val context: Context) {

    companion object {
        private const val TAG = "TripOfflineQueue"
        private const val FILE_NAME = "bhp_offline_gps_queue.json"
        const val MAX_QUEUE_SIZE = 50 // Bounded to ~10-12 minutes of fixes
    }

    private val queueLock = Any()
    private val queueFile: File get() = File(context.filesDir, FILE_NAME)

    data class QueuedLocation(
        val tripId: Long,
        val latitude: Double,
        val longitude: Double,
        val speedKmh: Double?,
        val heading: Double?,
        val accuracyMeters: Double?,
        val recordedAt: String
    ) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("tripId", tripId)
            put("latitude", latitude)
            put("longitude", longitude)
            if (speedKmh != null) put("speedKmh", speedKmh)
            if (heading != null) put("heading", heading)
            if (accuracyMeters != null) put("accuracyMeters", accuracyMeters)
            put("recordedAt", recordedAt)
        }

        fun toHttpPayload(): String = JSONObject().apply {
            put("latitude", latitude)
            put("longitude", longitude)
            if (speedKmh != null) put("speedKmh", speedKmh)
            if (heading != null) put("heading", heading)
            if (accuracyMeters != null) put("accuracyMeters", accuracyMeters)
            if (recordedAt.isNotBlank()) put("recordedAt", recordedAt)
        }.toString()

        companion object {
            fun fromJson(json: JSONObject): QueuedLocation? {
                return try {
                    QueuedLocation(
                        tripId = json.getLong("tripId"),
                        latitude = json.getDouble("latitude"),
                        longitude = json.getDouble("longitude"),
                        speedKmh = if (json.has("speedKmh") && !json.isNull("speedKmh")) json.getDouble("speedKmh") else null,
                        heading = if (json.has("heading") && !json.isNull("heading")) json.getDouble("heading") else null,
                        accuracyMeters = if (json.has("accuracyMeters") && !json.isNull("accuracyMeters")) json.getDouble("accuracyMeters") else null,
                        recordedAt = json.optString("recordedAt", "")
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    fun enqueue(item: QueuedLocation) {
        synchronized(queueLock) {
            val list = loadFromDisk().toMutableList()
            // Bound queue: drop oldest fix if capacity exceeded
            while (list.size >= MAX_QUEUE_SIZE) {
                val dropped = list.removeAt(0)
                Log.d(TAG, "Queue capacity reached. Dropped oldest fix recordedAt=${dropped.recordedAt}")
            }
            list.add(item)
            saveToDisk(list)
            Log.d(TAG, "Enqueued offline location for trip ${item.tripId}. Total queued: ${list.size}")
        }
    }

    fun peekOldest(maxCount: Int = 10): List<QueuedLocation> {
        synchronized(queueLock) {
            val list = loadFromDisk()
            return list.take(maxCount)
        }
    }

    fun removeOldest(count: Int) {
        synchronized(queueLock) {
            val list = loadFromDisk().toMutableList()
            val removeCount = count.coerceAtMost(list.size)
            repeat(removeCount) {
                if (list.isNotEmpty()) list.removeAt(0)
            }
            saveToDisk(list)
            Log.d(TAG, "Removed $removeCount uploaded fixes. Remaining: ${list.size}")
        }
    }

    fun clear() {
        synchronized(queueLock) {
            saveToDisk(emptyList())
            Log.d(TAG, "Cleared offline queue.")
        }
    }

    fun size(): Int {
        synchronized(queueLock) {
            return loadFromDisk().size
        }
    }

    private fun loadFromDisk(): List<QueuedLocation> {
        if (!queueFile.exists()) return emptyList()
        return try {
            val text = queueFile.readText()
            if (text.isBlank()) return emptyList()
            val array = JSONArray(text)
            val result = ArrayList<QueuedLocation>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                QueuedLocation.fromJson(obj)?.let { result.add(it) }
            }
            result
        } catch (e: Exception) {
            Log.w(TAG, "Error reading offline queue: ${e.message}")
            emptyList()
        }
    }

    private fun saveToDisk(list: List<QueuedLocation>) {
        try {
            val array = JSONArray()
            for (item in list) {
                array.put(item.toJson())
            }
            queueFile.writeText(array.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Error writing offline queue: ${e.message}")
        }
    }
}
