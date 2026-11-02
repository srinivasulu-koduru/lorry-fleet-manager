package com.lorryowner.backend.service;

import com.lorryowner.backend.dto.tracking.*;
import com.lorryowner.backend.entity.*;
import com.lorryowner.backend.repository.LorryRepository;
import com.lorryowner.backend.repository.TripLocationRepository;
import com.lorryowner.backend.repository.TripRepository;
import com.lorryowner.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class LiveTrackingService {

    private static final Logger log = LoggerFactory.getLogger(LiveTrackingService.class);
    private static final long LIVE_THRESHOLD_SECONDS = 90; // GPS is considered LIVE if updated within 90s
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a")
            .withZone(ZoneId.of("Asia/Kolkata"));

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final LorryRepository lorryRepository;
    private final TripLocationRepository tripLocationRepository;

    public LiveTrackingService(
            UserRepository userRepository,
            TripRepository tripRepository,
            LorryRepository lorryRepository,
            TripLocationRepository tripLocationRepository
    ) {
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.lorryRepository = lorryRepository;
        this.tripLocationRepository = tripLocationRepository;
    }

    /**
     * Submits a live GPS coordinate update from the authenticated driver's phone.
     */
    @Transactional
    public LiveTrackingLorryDto submitDriverLocation(Long tripId, SubmitLocationRequest request, String driverUsername) {
        User driver = getAuthenticatedUser(driverUsername);
        if (driver.getRole() != Role.DRIVER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only authenticated drivers can submit live GPS coordinates.");
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found with ID: " + tripId));

        if (!trip.getDriver().getId().equals(driver.getId())) {
            log.warn("Access violation: Driver '{}' (ID={}) attempted to submit location for trip ID={} belonging to driver ID={}",
                    driver.getUsername(), driver.getId(), trip.getId(), trip.getDriver().getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not assigned to this trip.");
        }

        if (trip.getTripStatus() == TripStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot submit location for a completed trip.");
        }
        if (trip.getTripStatus() == TripStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot submit location for a cancelled trip.");
        }
        if (trip.getTripStatus() != TripStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trip is not active. Current status: " + trip.getTripStatus());
        }

        BigDecimal lat = BigDecimal.valueOf(request.latitude()).setScale(7, RoundingMode.HALF_UP);
        BigDecimal lon = BigDecimal.valueOf(request.longitude()).setScale(7, RoundingMode.HALF_UP);
        BigDecimal speed = request.speedKmh() != null ? BigDecimal.valueOf(request.speedKmh()).setScale(2, RoundingMode.HALF_UP) : null;
        BigDecimal heading = request.heading() != null ? BigDecimal.valueOf(request.heading()).setScale(2, RoundingMode.HALF_UP) : null;
        BigDecimal accuracy = request.accuracyMeters() != null ? BigDecimal.valueOf(request.accuracyMeters()).setScale(2, RoundingMode.HALF_UP) : null;
        Instant clientTime = request.recordedAt() != null ? request.recordedAt() : Instant.now();

        TripLocation location = new TripLocation(
                trip,
                trip.getLorry(),
                driver,
                lat,
                lon,
                speed,
                heading,
                accuracy,
                clientTime
        );

        TripLocation saved = tripLocationRepository.save(location);
        log.debug("Saved GPS location for trip {} ({}, {}), speed: {} km/h",
                trip.getTripReference(), lat, lon, speed);

        return mapToLorryDto(trip, saved);
    }

    /**
     * Retrieves all currently trackable running lorries (with IN_PROGRESS trips) for an owner.
     */
    @Transactional(readOnly = true)
    public List<LiveTrackingLorryDto> getRunningLorriesForOwner(String ownerUsername) {
        User owner = getAuthenticatedUser(ownerUsername);
        if (owner.getRole() != Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only fleet owners can access fleet live tracking.");
        }

        List<Trip> activeTrips = tripRepository.findByOwnerAndTripStatus(owner, TripStatus.IN_PROGRESS);

        List<LiveTrackingLorryDto> result = new ArrayList<>();
        for (Trip trip : activeTrips) {
            TripLocation latestLocation = tripLocationRepository.findFirstByTripOrderByIdDesc(trip).orElse(null);
            result.add(mapToLorryDto(trip, latestLocation));
        }

        return result;
    }

    /**
     * Retrieves detailed live tracking information including breadcrumb trail for an owner's trip.
     */
    @Transactional(readOnly = true)
    public LiveTrackingDetailsResponse getTripLiveTrackingDetails(Long tripId, String ownerUsername) {
        User owner = getAuthenticatedUser(ownerUsername);
        if (owner.getRole() != Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only fleet owners can access fleet live tracking.");
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found with ID: " + tripId));

        if (!trip.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this trip.");
        }

        TripLocation latestLocation = tripLocationRepository.findFirstByTripOrderByIdDesc(trip).orElse(null);
        LiveTrackingLorryDto summary = mapToLorryDto(trip, latestLocation);

        List<TripLocation> history = tripLocationRepository.findTop50ByTripOrderByIdAsc(trip);
        List<TripLocationBreadcrumbDto> breadcrumbs = history.stream()
                .map(loc -> new TripLocationBreadcrumbDto(
                        loc.getLatitudeAsDouble(),
                        loc.getLongitudeAsDouble(),
                        loc.getSpeedKmhAsDouble(),
                        loc.getServerTimestamp()
                ))
                .toList();

        double[] originCoords = resolveCoordinatesForCity(trip.getOriginLocation());
        double[] destCoords = resolveCoordinatesForCity(trip.getDestinationLocation());

        return new LiveTrackingDetailsResponse(
                summary.lorryId(),
                summary.registrationNumber(),
                summary.makeModel(),
                summary.tripId(),
                summary.tripReference(),
                summary.driverName(),
                summary.driverPhone(),
                summary.originLocation(),
                summary.destinationLocation(),
                summary.currentLocationDescription(),
                summary.latitude(),
                summary.longitude(),
                summary.currentSpeedKmh(),
                summary.heading(),
                summary.accuracyMeters(),
                summary.distanceTravelledKm(),
                summary.remainingDistanceKm(),
                summary.estimatedDrivingTime(),
                summary.estimatedArrival(),
                summary.gpsStatus(),
                summary.lastGpsUpdate(),
                summary.isLive(),
                summary.hasGpsFix(),
                originCoords[0],
                originCoords[1],
                destCoords[0],
                destCoords[1],
                breadcrumbs
        );
    }

    /**
     * Retrieves detailed live tracking information for a specific lorry.
     */
    @Transactional(readOnly = true)
    public LiveTrackingDetailsResponse getLorryLiveTrackingDetails(Long lorryId, String ownerUsername) {
        User owner = getAuthenticatedUser(ownerUsername);
        if (owner.getRole() != Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only fleet owners can access fleet live tracking.");
        }

        Lorry lorry = lorryRepository.findById(lorryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lorry not found with ID: " + lorryId));

        if (lorry.getOwner() == null || !lorry.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this lorry.");
        }

        Optional<Trip> activeTripOpt = tripRepository.findFirstByLorryAndTripStatusOrderByTripDateDescStartTimeDesc(lorry, TripStatus.IN_PROGRESS);
        if (activeTripOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No active in-progress trip found for lorry: " + lorry.getRegistrationNumber());
        }

        return getTripLiveTrackingDetails(activeTripOpt.get().getId(), ownerUsername);
    }

    private LiveTrackingLorryDto mapToLorryDto(Trip trip, TripLocation location) {
        Lorry lorry = trip.getLorry();
        User driver = trip.getDriver();

        boolean hasGpsFix = (location != null && location.getLatitude() != null && location.getLongitude() != null);
        boolean isLive = false;
        String gpsStatus = "WAITING_FOR_LOCATION";
        Instant lastUpdate = null;

        Double lat = null;
        Double lon = null;
        Double speedKmh = null;
        Double heading = null;
        Double accuracy = null;

        if (hasGpsFix) {
            lat = location.getLatitudeAsDouble();
            lon = location.getLongitudeAsDouble();
            speedKmh = location.getSpeedKmhAsDouble();
            heading = location.getHeading() != null ? location.getHeading().doubleValue() : null;
            accuracy = location.getAccuracyMeters() != null ? location.getAccuracyMeters().doubleValue() : null;
            lastUpdate = location.getServerTimestamp();

            long ageSeconds = Math.abs(Duration.between(lastUpdate, Instant.now()).getSeconds());
            if (ageSeconds <= LIVE_THRESHOLD_SECONDS) {
                gpsStatus = "LIVE";
                isLive = true;
            } else {
                gpsStatus = "STALE";
                isLive = false;
            }
        }

        // Distance & ETA calculation
        double totalDistanceKm = trip.getDistanceKm() != null ? trip.getDistanceKm().doubleValue() : 430.0;
        double travelledKm;
        double remainingKm;

        if (hasGpsFix) {
            double[] originCoords = resolveCoordinatesForCity(trip.getOriginLocation());
            double haversineFromOrigin = calculateHaversineKm(originCoords[0], originCoords[1], lat, lon);
            // Road factor is typically ~1.15 to 1.25x straight-line distance
            travelledKm = Math.min(totalDistanceKm, Math.max(0.0, Math.round(haversineFromOrigin * 1.20 * 10.0) / 10.0));
            remainingKm = Math.max(0.0, Math.round((totalDistanceKm - travelledKm) * 10.0) / 10.0);
        } else {
            travelledKm = 0.0;
            remainingKm = totalDistanceKm;
        }

        // Speed & ETA estimation
        double effectiveSpeed = (speedKmh != null && speedKmh > 10.0) ? speedKmh : 50.0; // standard commercial lorry speed
        long remainingMinutes = Math.round((remainingKm / effectiveSpeed) * 60.0);
        long hours = remainingMinutes / 60;
        long mins = remainingMinutes % 60;
        String estimatedDrivingTime = String.format("%dh %02dm", hours, mins);

        Instant etaInstant = Instant.now().plus(Duration.ofMinutes(remainingMinutes));
        String estimatedArrival = TIME_FORMATTER.format(etaInstant);

        // Location description
        String locationDesc = resolveLocationDescription(lat, lon, trip.getOriginLocation(), trip.getDestinationLocation());

        return new LiveTrackingLorryDto(
                lorry.getId(),
                lorry.getRegistrationNumber(),
                lorry.getMakeModel(),
                trip.getId(),
                trip.getTripReference(),
                driver != null ? driver.getFullName() : "Assigned Driver",
                driver != null ? driver.getPhoneNumber() : null,
                trip.getOriginLocation(),
                trip.getDestinationLocation(),
                locationDesc,
                lat,
                lon,
                speedKmh,
                heading,
                accuracy,
                travelledKm,
                remainingKm,
                estimatedDrivingTime,
                estimatedArrival,
                gpsStatus,
                lastUpdate,
                isLive,
                hasGpsFix
        );
    }

    private String resolveLocationDescription(Double lat, Double lon, String origin, String dest) {
        if (lat == null || lon == null) {
            return "Waiting for driver location";
        }
        // Check proximity to notable corridors / hubs
        if (lat >= 15.6 && lat <= 16.0 && lon >= 77.8 && lon <= 78.3) {
            return "Near Kurnool (NH 44)";
        }
        if (lat >= 17.1 && lat <= 17.6 && lon >= 78.2 && lon <= 78.7) {
            return "Hyderabad Outskirts (ORR / NH 44)";
        }
        if (lat >= 14.3 && lat <= 14.6 && lon >= 79.8 && lon <= 80.2) {
            return "Near Nellore (NH 16)";
        }
        if (lat >= 16.4 && lat <= 16.7 && lon >= 80.5 && lon <= 80.8) {
            return "Vijayawada Bypass (NH 16)";
        }
        return String.format("Lat: %.4f, Lon: %.4f", lat, lon);
    }

    private double[] resolveCoordinatesForCity(String locationName) {
        if (locationName == null || locationName.trim().isBlank()) return new double[]{17.3850, 78.4867};
        String lower = locationName.toLowerCase().trim();
        if (lower.contains("anantapur") || lower.contains("ananthapur") || lower.contains("atp")) return new double[]{14.6819, 77.6006};
        if (lower.contains("tadipatri") || lower.contains("tadipati") || lower.contains("tadpatri")) return new double[]{14.9123, 78.0125};
        if (lower.contains("kadapa") || lower.contains("cudappah") || lower.contains("cuddapah")) return new double[]{14.4674, 78.8241};
        if (lower.contains("tirupati") || lower.contains("tirupathi")) return new double[]{13.6288, 79.4192};
        if (lower.contains("nellore") || lower.contains("nellur")) return new double[]{14.4426, 79.9865};
        if (lower.contains("kurnool") || lower.contains("kurnul")) return new double[]{15.8281, 78.0373};
        if (lower.contains("nandyal")) return new double[]{15.4886, 78.4836};
        if (lower.contains("proddatur")) return new double[]{14.7526, 78.5522};
        if (lower.contains("gooty") || lower.contains("gutti")) return new double[]{15.1147, 77.6366};
        if (lower.contains("dhone")) return new double[]{15.4200, 77.8700};
        if (lower.contains("hindupur")) return new double[]{13.8289, 77.4919};
        if (lower.contains("dharmavaram")) return new double[]{14.4137, 77.7126};
        if (lower.contains("vijayawada") || lower.contains("bezawada")) return new double[]{16.5062, 80.6480};
        if (lower.contains("guntur")) return new double[]{16.3067, 80.4365};
        if (lower.contains("ongole")) return new double[]{15.5057, 80.0499};
        if (lower.contains("chittoor") || lower.contains("chittur")) return new double[]{13.2172, 79.1003};
        if (lower.contains("visakhapatnam") || lower.contains("visakapatnam") || lower.contains("vizag")) return new double[]{17.6868, 83.2185};
        if (lower.contains("rajahmundry") || lower.contains("rajamundry")) return new double[]{17.0005, 81.8040};
        if (lower.contains("kakinada")) return new double[]{16.9891, 82.2475};
        if (lower.contains("hyderabad") || lower.contains("secunderabad")) return new double[]{17.3850, 78.4867};
        if (lower.contains("warangal")) return new double[]{17.9784, 79.5941};
        if (lower.contains("nizamabad")) return new double[]{18.6725, 78.0941};
        if (lower.contains("karimnagar")) return new double[]{18.4386, 79.1288};
        if (lower.contains("khammam")) return new double[]{17.2473, 80.1514};
        if (lower.contains("mahbubnagar") || lower.contains("mahabubnagar")) return new double[]{16.7488, 77.9856};
        if (lower.contains("nalgonda")) return new double[]{17.0575, 79.2684};
        if (lower.contains("raichur")) return new double[]{16.2076, 77.3463};
        if (lower.contains("bellary") || lower.contains("ballari")) return new double[]{15.1394, 76.9214};
        if (lower.contains("hubli") || lower.contains("hubballi")) return new double[]{15.3647, 75.1240};
        if (lower.contains("bengaluru") || lower.contains("bangalore")) return new double[]{12.9716, 77.5946};
        if (lower.contains("chennai") || lower.contains("madras")) return new double[]{13.0827, 80.2707};
        if (lower.contains("pune")) return new double[]{18.5204, 73.8567};
        if (lower.contains("mumbai") || lower.contains("bombay")) return new double[]{19.0760, 72.8777};
        if (lower.contains("delhi")) return new double[]{28.6139, 77.2090};
        return new double[]{17.3850, 78.4867}; // Default Andhra/Telangana hub
    }

    private double calculateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return R * c;
    }

    private User getAuthenticatedUser(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated.");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found with username: " + username));
    }
}
