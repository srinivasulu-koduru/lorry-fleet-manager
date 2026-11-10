package com.lorryowner.app.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.lorryowner.app.MainActivity
import com.lorryowner.app.network.ApiConfig
import com.lorryowner.app.security.SessionManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/**
 * Foreground service providing battery-conscious, high-reliability live GPS tracking
 * from the authenticated driver's phone during an active IN_PROGRESS trip.
 *
 * Continues tracking when the driver switches apps or the phone screen is locked.
 * Features:
 * - Scoped Partial WakeLock with 15s timeout to ensure HTTP uploads complete during screen lock.
 * - Persistent bounded offline queue to buffer GPS fixes during cellular dead zones.
 * - Automatic termination if the backend reports the trip was cancelled, completed, or unassigned.
 */
class DriverTripTrackingService : Service(), LocationListener {

    companion object {
        private const val TAG = "TripTrackingService"
        const val ACTION_START_TRACKING = "com.lorryowner.app.action.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.lorryowner.app.action.STOP_TRACKING"
        const val ACTION_RESTART_TRACKING = "com.lorryowner.app.action.RESTART_TRACKING"
        const val EXTRA_TRIP_ID = "extra_trip_id"
        const val EXTRA_TRIP_REF = "extra_trip_ref"

        private const val NOTIFICATION_CHANNEL_ID = "bhp_trip_tracking_channel"
        private const val NOTIFICATION_ID = 2001

        private val _isTrackingActive = MutableStateFlow(false)
        val isTrackingActive: StateFlow<Boolean> = _isTrackingActive.asStateFlow()

        private val _currentTrackingTripId = MutableStateFlow<Long?>(null)
        val currentTrackingTripId: StateFlow<Long?> = _currentTrackingTripId.asStateFlow()

        private val _latestLocation = MutableStateFlow<Location?>(null)
        val latestLocation: StateFlow<Location?> = _latestLocation.asStateFlow()

        private val _recentLocations = MutableStateFlow<List<Location>>(emptyList())
        val recentLocations: StateFlow<List<Location>> = _recentLocations.asStateFlow()

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

        fun restartTracking(context: Context, tripId: Long, tripReference: String) {
            val intent = Intent(context, DriverTripTrackingService::class.java).apply {
                action = ACTION_RESTART_TRACKING
                putExtra(EXTRA_TRIP_ID, tripId)
                putExtra(EXTRA_TRIP_REF, tripReference)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var sessionManager: SessionManager? = null
    private var offlineQueue: TripLocationOfflineQueue? = null
    private var currentTripId: Long? = null
    private var currentTripRef: String? = null

    private var locationManager: LocationManager? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var fusedLocationCallback: LocationCallback? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val gpsFilter = StationaryGpsFilter()
    private val speedCalculator = GpsSpeedCalculator()

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(applicationContext)
        offlineQueue = TripLocationOfflineQueue(applicationContext)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BhpLogistics:DriverTripTrackingWakeLock")?.apply {
                setReferenceCounted(false)
            }
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock initialization failed: ${e.message}")
        }

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

        // Verify location permissions before starting foreground service (Android 14 requirement)
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            Log.e(TAG, "Cannot start foreground service: Location permission is not granted.")
            stopSelf()
            return START_NOT_STICKY
        }

        val isRestart = action == ACTION_RESTART_TRACKING
        if (isRestart) {
            Log.i(TAG, "Driver requested manual GPS tracking restart for trip #$tripId ($tripRef)")
        }

        // Clean up prior listeners before attaching new ones
        try {
            fusedLocationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
            locationManager?.removeUpdates(this)
        } catch (e: Exception) {
            Log.d(TAG, "Cleanup prior listeners on (re)start: ${e.message}")
        }

        currentTripId = tripId
        currentTripRef = tripRef
        gpsFilter.reset()
        _isTrackingActive.value = true
        _currentTrackingTripId.value = tripId

        startForegroundNotification(tripRef)
        startLocationUpdates()

        // Attempt immediate lastLocation query for fast coordinator refresh
        try {
            fusedLocationClient?.lastLocation?.addOnSuccessListener { loc ->
                if (loc != null) {
                    Log.d(TAG, "Fast GPS refresh fix acquired upon (re)start: ${loc.latitude}, ${loc.longitude}")
                    handleNewLocation(loc)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Immediate location check error: ${e.message}")
        }

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
        // 1. Try FusedLocationProviderClient first (highest accuracy & battery balance)
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
        Log.d(TAG, "Location provider enabled: $provider")
    }
    override fun onProviderDisabled(provider: String) {
        Log.w(TAG, "Location provider disabled: $provider")
    }

    private fun handleNewLocation(location: Location) {
        val filterResult = gpsFilter.filter(location)
        when (filterResult) {
            is GpsFilterResult.DiscardUnreliable -> {
                Log.d(TAG, "Discarded GPS fix: ${filterResult.reason} (accuracy=${filterResult.accuracyMeters}m)")
                return
            }
            is GpsFilterResult.SuppressStationaryJitter -> {
                speedCalculator.calculateSpeedKmh(location, isStationaryAnchor = true)
                // Keep UI lorry marker anchored to the stable stop coordinate with speed clamped to 0
                val stableLocation = Location(location).apply {
                    latitude = filterResult.anchoredLat
                    longitude = filterResult.anchoredLon
                    speed = 0.0f
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        speedAccuracyMetersPerSecond = 0.0f
                    }
                }
                _latestLocation.value = stableLocation

                if (!filterResult.shouldEmitHeartbeat) {
                    Log.d(TAG, "Suppressed stationary GPS jitter: dist=${filterResult.displacementFromAnchor.toInt()}m, speed=0.0 km/h")
                    return
                }

                // Heartbeat interval reached: transmit stable anchor coordinates to keep server updated without breadcrumb zigzags
                Log.d(TAG, "Emitting stationary heartbeat for Trip #$currentTripId at anchor (${filterResult.anchoredLat}, ${filterResult.anchoredLon})")
                transmitLocation(stableLocation, speedKmh = 0.0)
            }
            is GpsFilterResult.AcceptGenuineMovement -> {
                val accurateSpeedKmh = speedCalculator.calculateSpeedKmh(location, isStationaryAnchor = false)
                val enrichedLocation = Location(location).apply {
                    speed = accurateSpeedKmh / 3.6f
                }
                _latestLocation.value = enrichedLocation
                val currentHistory = _recentLocations.value
                _recentLocations.value = (currentHistory + enrichedLocation).takeLast(40)

                Log.d(TAG, "Accepted genuine movement: dist=${filterResult.displacement.toInt()}m, speed=${accurateSpeedKmh} km/h, acc=${location.accuracy}m")
                transmitLocation(enrichedLocation, speedKmh = accurateSpeedKmh.toDouble())
            }
        }
    }

    private fun transmitLocation(location: Location, speedKmh: Double?) {
        val tripId = currentTripId ?: return
        val heading = if (location.hasBearing()) location.bearing.toDouble() else null
        val accuracy = if (location.hasAccuracy()) location.accuracy.toDouble() else null
        val recordedAt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Instant.now().toString() else ""

        // Acquire WakeLock with 15-second safety timeout so CPU doesn't sleep mid-upload
        try {
            wakeLock?.acquire(15_000L)
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock acquire failed: ${e.message}")
        }

        serviceScope.launch {
            val queuedPoint = TripLocationOfflineQueue.QueuedLocation(
                tripId = tripId,
                latitude = location.latitude,
                longitude = location.longitude,
                speedKmh = speedKmh,
                heading = heading,
                accuracyMeters = accuracy,
                recordedAt = recordedAt
            )

            val uploadResult = uploadSingleLocation(tripId, queuedPoint.toHttpPayload())
            when (uploadResult) {
                UploadResult.SUCCESS -> {
                    // Flush any pending points queued during dead zones
                    flushOfflineQueue(tripId)
                }
                UploadResult.NETWORK_ERROR -> {
                    // Cellular dead zone: buffer in offline queue
                    offlineQueue?.enqueue(queuedPoint)
                }
                UploadResult.TRIP_INACTIVE -> {
                    // Trip was completed or cancelled remotely by owner
                    Log.i(TAG, "Trip #$tripId is no longer active. Automatically stopping tracking service.")
                    offlineQueue?.clear()
                    stopTrackingInternal()
                }
                UploadResult.UNAUTHORIZED -> {
                    Log.w(TAG, "Driver session unauthorized or unassigned. Stopping tracking service.")
                    offlineQueue?.clear()
                    stopTrackingInternal()
                }
            }
        }
    }

    private enum class UploadResult {
        SUCCESS,
        NETWORK_ERROR,
        TRIP_INACTIVE,
        UNAUTHORIZED
    }

    private fun uploadSingleLocation(tripId: Long, payload: String): UploadResult {
        val token = sessionManager?.getAccessToken()
        if (token.isNullOrBlank()) {
            return UploadResult.UNAUTHORIZED
        }

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var networkFailed = false

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
                conn.disconnect()

                when (code) {
                    in 200..299 -> {
                        ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                        return UploadResult.SUCCESS
                    }
                    400, 404 -> {
                        // Bad Request: "Cannot submit location for a cancelled/completed trip" or Not Found
                        return UploadResult.TRIP_INACTIVE
                    }
                    403 -> {
                        // Forbidden: Driver is not assigned to this trip
                        return UploadResult.UNAUTHORIZED
                    }
                    401 -> {
                        return UploadResult.UNAUTHORIZED
                    }
                    else -> {
                        Log.w(TAG, "Server returned HTTP $code from $fullUrl")
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Network attempt to $baseUrl failed: ${e.message}")
                networkFailed = true
            }
        }

        return if (networkFailed) UploadResult.NETWORK_ERROR else UploadResult.NETWORK_ERROR
    }

    private suspend fun flushOfflineQueue(tripId: Long) {
        val queue = offlineQueue ?: return
        val pendingCount = queue.size()
        if (pendingCount == 0) return

        Log.d(TAG, "Flushing $pendingCount offline queued fixes for trip #$tripId...")
        val batch = queue.peekOldest(10)
        var uploadedCount = 0

        for (point in batch) {
            val result = uploadSingleLocation(tripId, point.toHttpPayload())
            if (result == UploadResult.SUCCESS) {
                uploadedCount++
            } else if (result == UploadResult.TRIP_INACTIVE || result == UploadResult.UNAUTHORIZED) {
                queue.clear()
                stopTrackingInternal()
                return
            } else {
                // Network failed mid-flush; stop flush and retry later
                break
            }
        }

        if (uploadedCount > 0) {
            queue.removeOldest(uploadedCount)
        }
    }

    private fun stopTrackingInternal() {
        gpsFilter.reset()
        speedCalculator.reset()
        _isTrackingActive.value = false
        _currentTrackingTripId.value = null
        _latestLocation.value = null
        _recentLocations.value = emptyList()

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "Released WakeLock.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock: ${e.message}")
        }

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
        Log.i(TAG, "DriverTripTrackingService stopped successfully.")
    }

    override fun onDestroy() {
        stopTrackingInternal()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
