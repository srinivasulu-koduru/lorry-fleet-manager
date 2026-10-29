package com.lorryowner.app.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.lorryowner.app.MainActivity
import com.lorryowner.app.network.ApiConfig
import com.lorryowner.app.security.SessionManager
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/**
 * Foreground service providing battery-conscious, high-reliability live GPS tracking
 * from the authenticated driver's phone during an active trip.
 */
class DriverTripTrackingService : Service(), LocationListener {

    companion object {
        private const val TAG = "TripTrackingService"
        const val ACTION_START_TRACKING = "com.lorryowner.app.action.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.lorryowner.app.action.STOP_TRACKING"
        const val EXTRA_TRIP_ID = "extra_trip_id"
        const val EXTRA_TRIP_REF = "extra_trip_ref"

        private const val NOTIFICATION_CHANNEL_ID = "bhp_trip_tracking_channel"
        private const val NOTIFICATION_ID = 2001

        fun startTracking(context: Context, tripId: Long, tripReference: String) {
            val intent = Intent(context, DriverTripTrackingService::class.java).apply {
                action = ACTION_START_TRACKING
                putExtra(EXTRA_TRIP_ID, tripId)
                putExtra(EXTRA_TRIP_REF, tripReference)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopTracking(context: Context) {
            val intent = Intent(context, DriverTripTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var sessionManager: SessionManager? = null
    private var currentTripId: Long? = null
    private var currentTripRef: String? = null

    private var locationManager: LocationManager? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var fusedLocationCallback: LocationCallback? = null

    private var lastSubmittedTime: Long = 0
    private var lastSubmittedLat: Double = 0.0
    private var lastSubmittedLon: Double = 0.0

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(applicationContext)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        } catch (e: Exception) {
            Log.w(TAG, "FusedLocationProviderClient init error, falling back to LocationManager: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_TRACKING) {
            Log.i(TAG, "Received stop tracking intent.")
            stopTrackingInternal()
            return START_NOT_STICKY
        }

        val tripId = intent?.getLongExtra(EXTRA_TRIP_ID, -1L) ?: -1L
        val tripRef = intent?.getStringExtra(EXTRA_TRIP_REF) ?: "Active Trip"

        if (tripId <= 0) {
            Log.w(TAG, "Invalid trip ID ($tripId), stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        currentTripId = tripId
        currentTripRef = tripRef

        startForegroundNotification(tripRef)
        startLocationUpdates()

        return START_STICKY
    }

    private fun startForegroundNotification(tripRef: String) {
        createNotificationChannel()

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("BHP Logistics • Trip Live Tracking")
            .setContentText("Actively transmitting live GPS location for Trip #$tripRef")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "BHP Logistics Live Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent status while driver phone shares live trip GPS coordinates"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        // 1. Try FusedLocationProviderClient first
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 12_000L)
                .setMinUpdateIntervalMillis(8_000L)
                .setMinUpdateDistanceMeters(10.0f)
                .build()

            fusedLocationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    handleNewLocation(location)
                }
            }

            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                fusedLocationCallback!!,
                Looper.getMainLooper()
            )
            Log.d(TAG, "Requested location updates via FusedLocationProviderClient")
        } catch (e: Exception) {
            Log.w(TAG, "FusedLocationProviderClient failed, using LocationManager: ${e.message}")
        }

        // 2. Also register LocationManager GPS_PROVIDER as reliable fallback
        try {
            locationManager?.let { lm ->
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        12_000L,
                        10.0f,
                        this,
                        Looper.getMainLooper()
                    )
                }
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        15_000L,
                        15.0f,
                        this,
                        Looper.getMainLooper()
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "LocationManager provider registration error: ${e.message}")
        }
    }

    override fun onLocationChanged(location: Location) {
        handleNewLocation(location)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {
        Log.d(TAG, "Provider enabled: $provider")
    }
    override fun onProviderDisabled(provider: String) {
        Log.w(TAG, "Provider disabled: $provider")
    }

    private fun handleNewLocation(location: Location) {
        val now = System.currentTimeMillis()
        // Throttle updates: minimum 5 seconds between consecutive HTTP uploads unless moved > 25 meters
        val timeDiff = now - lastSubmittedTime
        val distMoved = FloatArray(1)
        Location.distanceBetween(lastSubmittedLat, lastSubmittedLon, location.latitude, location.longitude, distMoved)

        if (timeDiff < 5_000L && distMoved[0] < 25.0f) {
            return
        }

        lastSubmittedTime = now
        lastSubmittedLat = location.latitude
        lastSubmittedLon = location.longitude

        val tripId = currentTripId ?: return
        val speedKmh = if (location.hasSpeed()) (location.speed * 3.6).coerceAtLeast(0.0) else null
        val heading = if (location.hasBearing()) location.bearing.toDouble() else null
        val accuracy = if (location.hasAccuracy()) location.accuracy.toDouble() else null

        serviceScope.launch {
            uploadLocationToBackend(tripId, location.latitude, location.longitude, speedKmh, heading, accuracy)
        }
    }

    private suspend fun uploadLocationToBackend(
        tripId: Long,
        lat: Double,
        lon: Double,
        speedKmh: Double?,
        heading: Double?,
        accuracy: Double?
    ) {
        val token = sessionManager?.getAccessToken() ?: return
        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)

        val payload = JSONObject().apply {
            put("latitude", lat)
            put("longitude", lon)
            if (speedKmh != null) put("speedKmh", speedKmh)
            if (heading != null) put("heading", heading)
            if (accuracy != null) put("accuracyMeters", accuracy)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                put("recordedAt", Instant.now().toString())
            }
        }.toString()

        for (baseUrl in candidateUrls) {
            try {
                val fullUrl = "$baseUrl/api/driver/trips/$tripId/location"
                val conn = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                }

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(payload)
                    writer.flush()
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    Log.d(TAG, "Live GPS location uploaded successfully to $fullUrl (lat=$lat, lon=$lon, speed=$speedKmh)")
                    conn.disconnect()
                    return
                } else {
                    Log.w(TAG, "Backend returned HTTP $code for location update")
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Log.d(TAG, "Network attempt to $baseUrl failed: ${e.message}")
            }
        }
    }

    private fun stopTrackingInternal() {
        try {
            fusedLocationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
            locationManager?.removeUpdates(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing location updates: ${e.message}")
        }
        serviceScope.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        stopTrackingInternal()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
