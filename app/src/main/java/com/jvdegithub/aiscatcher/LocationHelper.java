/*
 *     MastChain Mobile – Location Helper
 *     Copyright (C)  2025 MastChain integration additions
 *
 *     FusedLocationProviderClient with GPS_PROVIDER fallback.
 *     H3 resolution-5 hex calculation.
 *     Runtime permission handling including "denied forever".
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 */

package com.jvdegithub.aiscatcher;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Manages GPS location for MastChain station position.
 *
 * <p>Uses FusedLocationProviderClient (Google Play Services) as primary,
 * with GPS_PROVIDER fallback when Play Services is unavailable.</p>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>High-accuracy location updates every 5-15 seconds</li>
 *   <li>H3 resolution-5 hex ID calculation</li>
 *   <li>Runtime permission handling with rationale dialog</li>
 *   <li>"Denied forever" detection with link to app settings</li>
 *   <li>Location services check with prompt to enable</li>
 *   <li>Thread-safe access to current location data</li>
 * </ul>
 */
public class LocationHelper {

    private static final String TAG = "MastChain-GPS";

    /* ── Preference keys ──────────────────────────────────────────── */
    public static final String PREF_GPS_ENABLED = "gpsENABLE";

    /* ── Update intervals ─────────────────────────────────────────── */
    private static final long UPDATE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(10);
    private static final long FASTEST_INTERVAL_MS = TimeUnit.SECONDS.toMillis(3);

    /* ── Permission request code ──────────────────────────────────── */
    public static final int PERMISSION_REQUEST_CODE = 1001;
    public static final int LOCATION_SETTINGS_REQUEST_CODE = 1002;

    /* ── Instance state ───────────────────────────────────────────── */
    private final Activity activity;
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /* ── FusedLocation (primary) ──────────────────────────────────── */
    private FusedLocationProviderClient fusedClient;
    private LocationCallback fusedCallback;
    private boolean fusedAvailable = false;

    /* ── GPS Provider fallback ────────────────────────────────────── */
    private LocationManager locationManager;
    private final LocationListener gpsListener = new LocationListener() {
        @Override public void onLocationChanged(@NonNull Location location) { onNewLocation(location); }
        @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override public void onProviderEnabled(@NonNull String provider) {}
        @Override public void onProviderDisabled(@NonNull String provider) { promptEnableLocation(); }
    };

    /* ── Current location state ────────────────────────────────────── */
    private volatile double latitude = Double.NaN;
    private volatile double longitude = Double.NaN;
    private volatile float accuracy = Float.NaN;
    private volatile long lastFixTime = 0;
    private volatile String h3HexId = "";
    private volatile boolean hasFix = false;
    private volatile String gpsStatus = "GPS off";

    private boolean isUpdating = false;

    /* ── H3 resolution for hex calculation ─────────────────────────── */
    private static final int H3_RESOLUTION = 5;

    /* ── Singleton ────────────────────────────────────────────────── */
    private static LocationHelper instance;

    public static synchronized LocationHelper getInstance(Activity activity) {
        if (instance == null) {
            instance = new LocationHelper(activity);
        }
        return instance;
    }

    public static synchronized LocationHelper getInstance() {
        return instance;
    }

    private LocationHelper(Activity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        trySetupFusedLocation();
    }

    /* ================================================================ *
     *  Public API                                                      *
     * ================================================================ */

    /**
     * Start location updates. Requests permissions if needed.
     */
    public void requestLocationUpdates() {
        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }
        if (!isLocationEnabled()) {
            promptEnableLocation();
            return;
        }
        startUpdates();
    }

    /**
     * Stop all location updates.
     */
    public void removeLocationUpdates() {
        stopUpdates();
    }

    /**
     * Check and request permissions if needed, then start updates.
     * Call this from onResume() or when the MastChain tab is shown.
     */
    public void ensureLocationRunning() {
        if (!hasLocationPermission()) {
            gpsStatus = "No permission";
            return;
        }
        if (!isLocationEnabled()) {
            gpsStatus = "GPS disabled";
            return;
        }
        if (!isUpdating) {
            startUpdates();
        }
    }

    /* ── Getters (thread-safe) ─────────────────────────────────────── */

    public boolean hasFix() { return hasFix; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public float getAccuracy() { return accuracy; }
    public long getLastFixTime() { return lastFixTime; }
    public String getH3HexId() { return h3HexId; }
    public String getGpsStatus() { return gpsStatus; }

    public String getLatitudeString() {
        return hasFix ? String.format("%.6f", latitude) : "—";
    }

    public String getLongitudeString() {
        return hasFix ? String.format("%.6f", longitude) : "—";
    }

    public String getAccuracyString() {
        return hasFix && !Float.isNaN(accuracy) ? String.format("%.0f m", accuracy) : "—";
    }

    public String getTimeSinceFix() {
        if (lastFixTime == 0) return "—";
        long elapsed = System.currentTimeMillis() - lastFixTime;
        if (elapsed < 60_000) return (elapsed / 1000) + "s ago";
        if (elapsed < 3_600_000) return (elapsed / 60_000) + "m ago";
        return (elapsed / 3_600_000) + "h ago";
    }

    /* ── Static convenience methods for easy access from fragments ─── */

    /** Get latitude from singleton, NaN if no instance or no fix. */
    public static double sLatitude() {
        LocationHelper h = getInstance();
        return h != null ? h.getLatitude() : Double.NaN;
    }

    /** Get longitude from singleton, NaN if no instance or no fix. */
    public static double sLongitude() {
        LocationHelper h = getInstance();
        return h != null ? h.getLongitude() : Double.NaN;
    }

    /** Get accuracy from singleton, NaN if no instance or no fix. */
    public static float sAccuracy() {
        LocationHelper h = getInstance();
        return h != null ? h.getAccuracy() : Float.NaN;
    }

    /** Get last fix time from singleton, 0 if no instance. */
    public static long sLastFixTime() {
        LocationHelper h = getInstance();
        return h != null ? h.getLastFixTime() : 0;
    }

    /** Get H3 hex ID from singleton, empty string if no instance. */
    public static String sH3Hex() {
        LocationHelper h = getInstance();
        return h != null ? h.getH3HexId() : "";
    }

    /** Check if singleton has a GPS fix. */
    public static boolean sHasFix() {
        LocationHelper h = getInstance();
        return h != null && h.hasFix();
    }

    /* ================================================================ *
     *  Permissions                                                      *
     * ================================================================ */

    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Show rationale dialog
            new AlertDialog.Builder(activity)
                    .setTitle("Location Access Needed")
                    .setMessage("MastChain needs your GPS location to assign AIS messages to the correct hex cell and calculate your station's coverage area.")
                    .setPositiveButton("Grant", (dialog, which) ->
                            ActivityCompat.requestPermissions(activity,
                                    new String[]{
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                    },
                                    PERMISSION_REQUEST_CODE))
                    .setNegativeButton("Deny", null)
                    .show();
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Handle permission result. Call from Activity.onRequestPermissionsResult().
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                            @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                gpsStatus = "GPS starting…";
                requestLocationUpdates();
            } else {
                // Check if "denied forever"
                if (!ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // "Don't ask again" checked → open settings
                    gpsStatus = "GPS denied forever";
                    new AlertDialog.Builder(activity)
                            .setTitle("Location Permission Required")
                            .setMessage("MastChain needs location access to report your station position. Please enable it in app settings.")
                            .setPositiveButton("Open Settings", (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.fromParts("package", context.getPackageName(), null));
                                activity.startActivity(intent);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    gpsStatus = "GPS denied";
                }
            }
        }
    }

    /* ================================================================ *
     *  Location Services Check                                          *
     * ================================================================ */

    private boolean isLocationEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return lm != null && lm.isLocationEnabled();
        } else {
            try {
                LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                return lm != null && (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                        || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
            } catch (Exception e) {
                return false;
            }
        }
    }

    private void promptEnableLocation() {
        new AlertDialog.Builder(activity)
                .setTitle("Enable Location")
                .setMessage("GPS is currently disabled. MastChain needs location to report your station position.")
                .setPositiveButton("Enable", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    activity.startActivityForResult(intent, LOCATION_SETTINGS_REQUEST_CODE);
                })
                .setNegativeButton("Cancel", (dialog, which) -> gpsStatus = "GPS disabled")
                .show();
    }

    /**
     * Handle result from location settings activity.
     */
    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode == LOCATION_SETTINGS_REQUEST_CODE) {
            if (isLocationEnabled()) {
                requestLocationUpdates();
            } else {
                gpsStatus = "GPS disabled";
            }
        }
    }

    /* ================================================================ *
     *  FusedLocationProviderClient (primary)                            *
     * ================================================================ */

    private void trySetupFusedLocation() {
        try {
            fusedClient = LocationServices.getFusedLocationProviderClient(activity);
            fusedCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    Location loc = locationResult.getLastLocation();
                    if (loc != null) {
                        onNewLocation(loc);
                    }
                }
            };
            fusedAvailable = true;
            Log.i(TAG, "FusedLocationProviderClient available");
        } catch (Exception e) {
            Log.w(TAG, "FusedLocationProviderClient not available, falling back to GPS_PROVIDER: " + e.getMessage());
            fusedAvailable = false;
        }
    }

    private void startFusedUpdates() {
        if (!fusedAvailable || fusedClient == null) return;
        try {
            LocationRequest request = new LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
                    .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
                    .build();

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                // Get last known location first for quick fix
                fusedClient.getLastLocation()
                        .addOnSuccessListener(loc -> {
                            if (loc != null) onNewLocation(loc);
                        });

                fusedClient.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper());
                Log.i(TAG, "FusedLocation updates started (interval=" + UPDATE_INTERVAL_MS + "ms)");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException starting fused updates: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error starting fused updates: " + e.getMessage());
            fusedAvailable = false;
            startGpsFallback();
        }
    }

    private void stopFusedUpdates() {
        if (fusedClient != null && fusedCallback != null) {
            try {
                fusedClient.removeLocationUpdates(fusedCallback);
            } catch (Exception e) {
                Log.w(TAG, "Error stopping fused updates: " + e.getMessage());
            }
        }
    }

    /* ================================================================ *
     *  GPS_PROVIDER Fallback                                            *
     * ================================================================ */

    private void startGpsFallback() {
        if (locationManager == null) return;
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        UPDATE_INTERVAL_MS,
                        0, // min distance
                        gpsListener,
                        Looper.getMainLooper());
                Log.i(TAG, "GPS_PROVIDER fallback started");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException starting GPS fallback: " + e.getMessage());
        }
    }

    private void stopGpsFallback() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(gpsListener);
            } catch (Exception e) {
                Log.w(TAG, "Error stopping GPS fallback: " + e.getMessage());
            }
        }
    }

    /* ================================================================ *
     *  Start / Stop                                                     *
     * ================================================================ */

    private void startUpdates() {
        if (isUpdating) return;
        isUpdating = true;
        gpsStatus = "GPS searching…";

        if (fusedAvailable) {
            startFusedUpdates();
        } else {
            startGpsFallback();
        }
    }

    private void stopUpdates() {
        if (!isUpdating) return;
        isUpdating = false;
        stopFusedUpdates();
        stopGpsFallback();
    }

    /* ================================================================ *
     *  Location Update Handler                                          *
     * ================================================================ */

    private void onNewLocation(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        accuracy = location.getAccuracy();
        lastFixTime = System.currentTimeMillis();
        hasFix = true;
        gpsStatus = "GPS fix OK";

        // Calculate H3 hex
        h3HexId = calculateH3Hex(latitude, longitude);

        // Update native AIS decoder with position
        AisCatcherJava.setLatLon((float) latitude, (float) longitude);

        Log.d(TAG, String.format("GPS fix: %.6f, %.6f (±%.0fm) hex=%s",
                latitude, longitude, accuracy, h3HexId));
    }

    /* ================================================================ *
     *  H3 Hex Calculation (Resolution 5)                                *
     * ================================================================ */

    /**
     * Calculate H3 hex ID at resolution 5 from lat/lon.
     * Uses Uber H3 Java library if available, otherwise falls back
     * to a simple geohash-based cell identifier.
     */
    private String calculateH3Hex(double lat, double lon) {
        try {
            // Try Uber H3 Java library
            Class<?> h3Clazz = Class.forName("com.uber.h3core.H3Core");
            Method newInstance = h3Clazz.getMethod("newInstance");
            Object h3 = newInstance.invoke(null);
            Method latLonToCell = h3Clazz.getMethod("latLngToCell", double.class, double.class, int.class);
            long cellAddr = (long) latLonToCell.invoke(h3, lat, lon, H3_RESOLUTION);
            Method formatCell = h3Clazz.getMethod("h3ToString", long.class);
            return (String) formatCell.invoke(h3, cellAddr);
        } catch (Exception e) {
            // Fallback: simple cell-based hex ID
            return calculateSimpleHexId(lat, lon);
        }
    }

    /**
     * Simple fallback hex ID when H3 library is not available.
     * Creates a grid cell identifier at ~resolution 5 accuracy (~4.5 km edge length).
     */
    private String calculateSimpleHexId(double lat, double lon) {
        // Resolution 5 ≈ 4.5 km edge length
        // Grid cell size: ~0.04 degrees latitude, ~0.04/0.8 = 0.05 degrees longitude
        // (accounting for cosine of latitude)
        double cosLat = Math.cos(Math.toRadians(lat));
        double latStep = 0.04;
        double lonStep = cosLat > 0.1 ? latStep / cosLat : 0.4;

        int latIdx = (int) Math.floor(lat / latStep);
        int lonIdx = (int) Math.floor(lon / lonStep);

        // Offset to make indices positive
        int latOffset = latIdx + 90 / (int) latStep; // roughly center at equator
        int lonOffset = lonIdx + 180 / (int) lonStep;

        return String.format("%d-%d", latOffset, lonOffset);
    }
}