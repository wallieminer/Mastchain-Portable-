/*
 *     MastChain Mobile – MastChainStatsFragment
 *     Copyright (C)  2025 MastChain integration additions
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.jvdegithub.aiscatcher.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.jvdegithub.aiscatcher.AisCatcherJava;
import com.jvdegithub.aiscatcher.AisService;
import com.jvdegithub.aiscatcher.DeviceManager;
import com.jvdegithub.aiscatcher.HttpOutputManager;
import com.jvdegithub.aiscatcher.LocationHelper;
import com.jvdegithub.aiscatcher.R;
import com.jvdegithub.aiscatcher.Settings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * MastChain Dashboard fragment showing station status, uptime, reception,
 * GPS/H3 hex, coverage, upload stats, bond info, and toggles.
 *
 * Color theme:
 *   bg-deep: #0A0E14, bg-panel: #121821, bg-elev: #1A2230
 *   orange: #F26A1B, orange-dk: #C9540F, cyan: #00C2D1
 *   green: #29C46A, yellow: #E5B53A, red: #E5484D
 *   text: #E6EAF0, text-dim: #8A94A6, line: #232C3A
 */
public class MastChainStatsFragment extends Fragment {

    private static final String TAG = "MastChain-Dashboard";
    private static final long REFRESH_INTERVAL_MS = 5_000;  // 5 seconds
    private static final String DASHBOARD_URL = "https://app.mastchain.io/app/dashboard/880";

    // MastChain rules
    private static final double TMAST_PER_HEX = 0.223;
    private static final int HEX_ELIGIBLE_THRESHOLD = 500;
    private static final int DAY_SLOTS = 288;  // 5-min slots per day
    private static final double UPTIME_THRESHOLD = 0.80;

    /* ── Views ─────────────────────────────────────────────── */
    // Header
    private View onlineDot;
    private TextView headerStationId;
    private TextView headerStatusLabel;

    // Uptime
    private TextView uptimePercent;
    private LinearLayout uptimeBarContainer;

    // Live Reception
    private TextView rxTotal;
    private TextView rxVessels;
    private TextView rxChA;
    private TextView rxChB;
    private TextView rxMsgMin;

    // Location & Hex
    private TextView locLatLon;
    private TextView locAccuracy;
    private TextView locFixTime;
    private TextView locH3Hex;

    // Hex Coverage
    private TextView hexList;
    private TextView hexTmastTotal;

    // Upload
    private TextView uploadSent;
    private TextView uploadFailed;
    private TextView uploadLast;
    private TextView uploadEndpoint;

    // Toggles
    private SwitchCompat communityShareToggle;
    private SwitchCompat mastchainFeedToggle;

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = this::refreshStats;

    /* ── Uptime tracking ───────────────────────────────────── */
    private long sessionStartTime = 0;
    private int onlineSlotCount = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mastchain_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Header
        onlineDot = view.findViewById(R.id.online_dot);
        headerStationId = view.findViewById(R.id.header_station_id);
        headerStatusLabel = view.findViewById(R.id.header_status_label);

        // Uptime
        uptimePercent = view.findViewById(R.id.uptime_percent);
        uptimeBarContainer = view.findViewById(R.id.uptime_bar_container);

        // Live Reception
        rxTotal = view.findViewById(R.id.rx_total);
        rxVessels = view.findViewById(R.id.rx_vessels);
        rxChA = view.findViewById(R.id.rx_ch_a);
        rxChB = view.findViewById(R.id.rx_ch_b);
        rxMsgMin = view.findViewById(R.id.rx_msg_min);

        // Location & Hex
        locLatLon = view.findViewById(R.id.loc_latlon);
        locAccuracy = view.findViewById(R.id.loc_accuracy);
        locFixTime = view.findViewById(R.id.loc_fix_time);
        locH3Hex = view.findViewById(R.id.loc_h3_hex);

        // Hex Coverage
        hexList = view.findViewById(R.id.hex_list);
        hexTmastTotal = view.findViewById(R.id.hex_tmast_total);

        // Upload
        uploadSent = view.findViewById(R.id.upload_sent);
        uploadFailed = view.findViewById(R.id.upload_failed);
        uploadLast = view.findViewById(R.id.upload_last);
        uploadEndpoint = view.findViewById(R.id.upload_endpoint);

        // Toggles
        communityShareToggle = view.findViewById(R.id.community_share_toggle);
        mastchainFeedToggle = view.findViewById(R.id.mastchain_feed_toggle);

        // Make online dot circular
        if (onlineDot != null) {
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(0xFFE5484D); // red = offline
            onlineDot.setBackground(dot);
        }

        // Load toggle state
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        communityShareToggle.setChecked(prefs.getBoolean("sSHARING", false));
        mastchainFeedToggle.setChecked(prefs.getBoolean("hENABLE", false));

        // Toggle listeners
        communityShareToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("sSHARING", isChecked).apply();
            refreshStats();
        });

        mastchainFeedToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("hENABLE", isChecked).apply();
            if (isChecked) {
                HttpOutputManager.getInstance().start(requireContext());
            } else {
                HttpOutputManager.getInstance().stop();
            }
            refreshStats();
        });

        // Open Dashboard button
        MaterialButton openDashboardBtn = view.findViewById(R.id.open_dashboard_button);
        openDashboardBtn.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(DASHBOARD_URL));
            startActivity(browserIntent);
        });

        // Reset Stats button
        MaterialButton resetStatsBtn = view.findViewById(R.id.reset_stats_button);
        resetStatsBtn.setOnClickListener(v -> {
            HttpOutputManager.resetStats();
            sessionStartTime = System.currentTimeMillis();
            onlineSlotCount = 0;
            refreshStats();
        });

        sessionStartTime = System.currentTimeMillis();
        refreshStats();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStats();
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    private void refreshStats() {
        if (getView() == null || !isAdded()) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        /* ── Header ────────────────────────────────────────── */
        boolean feedEnabled = prefs.getBoolean("hENABLE", false);
        boolean serviceRunning = AisService.isRunning(requireContext());
        boolean isOnline = feedEnabled && serviceRunning;

        // Online dot
        if (onlineDot != null) {
            GradientDrawable dot = (GradientDrawable) onlineDot.getBackground();
            dot.setColor(isOnline ? 0xFF29C46A : 0xFFE5484D); // green or red
        }

        // Station ID
        String stationId = prefs.getString(HttpOutputManager.PREF_STATIONID, "—");
        if (headerStationId != null) {
            headerStationId.setText("Station " + stationId);
        }

        // Status label
        if (headerStatusLabel != null) {
            String statusText;
            int statusColor;
            if (isOnline) {
                statusText = "ONLINE";
                statusColor = 0xFF29C46A;
            } else if (feedEnabled) {
                statusText = "STANDBY";
                statusColor = 0xFFE5B53A;
            } else {
                statusText = "OFFLINE";
                statusColor = 0xFFE5484D;
            }
            headerStatusLabel.setText(statusText);
            headerStatusLabel.setTextColor(statusColor);
        }

        /* ── Uptime ────────────────────────────────────────── */
        updateUptime(isOnline);

        /* ── Live Reception ────────────────────────────────── */
        updateReception();

        /* ── Location & Hex ────────────────────────────────── */
        updateLocation();

        /* ── Hex Coverage ──────────────────────────────────── */
        updateHexCoverage();

        /* ── Upload Stats ──────────────────────────────────── */
        updateUploadStats(prefs);

        /* ── Toggles (sync state) ──────────────────────────── */
        if (communityShareToggle != null) {
            communityShareToggle.setOnCheckedChangeListener(null);
            communityShareToggle.setChecked(prefs.getBoolean("sSHARING", false));
            communityShareToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("sSHARING", isChecked).apply();
                refreshStats();
            });
        }
        if (mastchainFeedToggle != null) {
            mastchainFeedToggle.setOnCheckedChangeListener(null);
            mastchainFeedToggle.setChecked(prefs.getBoolean("hENABLE", false));
            mastchainFeedToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("hENABLE", isChecked).apply();
                if (isChecked) {
                    HttpOutputManager.getInstance().start(requireContext());
                } else {
                    HttpOutputManager.getInstance().stop();
                }
                refreshStats();
            });
        }

        // Schedule next refresh
        refreshHandler.removeCallbacks(refreshRunnable);
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
    }

    /* ════════════════════════════════════════════════════════════ *
     *  Uptime tracking                                             *
     *  Day has 288 slots of 5 min. We estimate uptime from        *
     *  session time and online status.                             *
     * ════════════════════════════════════════════════════════════ */

    private void updateUptime(boolean isOnline) {
        if (uptimePercent == null) return;

        long now = System.currentTimeMillis();
        long dayStart = getDayStart(now);
        long elapsedToday = now - dayStart;
        long sessionElapsed = now - sessionStartTime;

        // Count online slots: estimate based on current session
        // If online now, assume continuous uptime since session start
        int totalSlotsToday = (int) (elapsedToday / (5 * 60 * 1000));
        int onlineSlots;
        if (isOnline) {
            onlineSlots = (int) Math.min(sessionElapsed / (5 * 60 * 1000), totalSlotsToday);
            onlineSlots = Math.max(1, onlineSlots);  // At least 1 if currently online
        } else {
            onlineSlots = onlineSlotCount;
        }

        if (isOnline) onlineSlotCount = onlineSlots;

        double uptimePct = totalSlotsToday > 0 ? (100.0 * onlineSlots / totalSlotsToday) : 0;
        uptimePct = Math.min(100.0, uptimePct);

        uptimePercent.setText(String.format(Locale.getDefault(), "%.0f", uptimePct));

        // Color based on threshold
        if (uptimePct >= UPTIME_THRESHOLD * 100) {
            uptimePercent.setTextColor(0xFF29C46A); // green
        } else if (uptimePct >= 50) {
            uptimePercent.setTextColor(0xFFE5B53A); // yellow
        } else {
            uptimePercent.setTextColor(0xFFE5484D); // red
        }

        // Build uptime bar (288 colored blocks)
        buildUptimeBar(onlineSlots, totalSlotsToday);
    }

    private void buildUptimeBar(int onlineSlots, int totalSlots) {
        if (uptimeBarContainer == null) return;
        uptimeBarContainer.removeAllViews();

        int displaySlots = Math.max(1, Math.min(DAY_SLOTS, totalSlots));
        int displayOnline = Math.min(onlineSlots, displaySlots);

        // We display a simplified bar with fewer blocks for performance
        int blockCount = 48; // 48 blocks * 6 slots each = 288
        int slotsPerBlock = DAY_SLOTS / blockCount;

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        params.setMargins(0, 0, 1, 0);

        for (int i = 0; i < blockCount; i++) {
            TextView block = new TextView(requireContext());
            block.setLayoutParams(params);
            int slotStart = i * slotsPerBlock;
            boolean isSlotOnline = slotStart < displayOnline;
            block.setBackgroundColor(isSlotOnline ? 0xFF29C46A : 0xFFE5484D);
            block.setText("");
            uptimeBarContainer.addView(block);
        }
    }

    private long getDayStart(long now) {
        // Start of today in local timezone
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(now);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /* ════════════════════════════════════════════════════════════ *
     *  Live Reception (from AisCatcherJava.Statistics)             *
     * ════════════════════════════════════════════════════════════ */

    private void updateReception() {
        try {
            int total = AisCatcherJava.Statistics.getTotal();
            int chA = AisCatcherJava.Statistics.getChA();
            int chB = AisCatcherJava.Statistics.getChB();

            if (rxTotal != null) rxTotal.setText(formatNumber(total));
            if (rxChA != null) rxChA.setText(formatNumber(chA));
            if (rxChB != null) rxChB.setText(formatNumber(chB));

            // Estimate vessels from message types (rough: unique MMSIs ~ total msgs * 0.1)
            int msg123 = AisCatcherJava.Statistics.getMsg123();
            int vessels = Math.max(1, msg123 / 3); // rough estimate
            if (rxVessels != null) rxVessels.setText(formatNumber(vessels));

            // msgs/min: estimate from total messages and session time
            long sessionMs = System.currentTimeMillis() - sessionStartTime;
            int minutes = (int) Math.max(1, sessionMs / 60000);
            int msgsPerMin = total / minutes;
            if (rxMsgMin != null) rxMsgMin.setText(String.valueOf(msgsPerMin));
        } catch (Exception e) {
            // Statistics may not be available if decoder not running
        }
    }

    /* ════════════════════════════════════════════════════════════ *
     *  Location & H3 Hex (from LocationHelper)                     *
     * ════════════════════════════════════════════════════════════ */

    private void updateLocation() {
        double lat = LocationHelper.sLatitude();
        double lon = LocationHelper.sLongitude();
        float acc = LocationHelper.sAccuracy();
        long fixTime = LocationHelper.sLastFixTime();
        String h3 = LocationHelper.sH3Hex();

        if (locLatLon != null) {
            if (Double.isNaN(lat) || Double.isNaN(lon)) {
                locLatLon.setText("Waiting for GPS…");
                locLatLon.setTextColor(0xFF8A94A6);
            } else {
                locLatLon.setText(String.format(Locale.getDefault(), "%.5f°, %.5f°", lat, lon));
                locLatLon.setTextColor(0xFFE6EAF0);
            }
        }

        if (locAccuracy != null) {
            if (Float.isNaN(acc)) {
                locAccuracy.setText("");
            } else {
                locAccuracy.setText(String.format(Locale.getDefault(), "±%.0f m accuracy", acc));
            }
        }

        if (locFixTime != null) {
            if (fixTime == 0) {
                locFixTime.setText("No fix yet");
            } else {
                long elapsed = System.currentTimeMillis() - fixTime;
                if (elapsed < 60_000) {
                    locFixTime.setText(String.format(Locale.getDefault(), "Fix %ds ago", elapsed / 1000));
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                    locFixTime.setText("Fix at " + sdf.format(new Date(fixTime)));
                }
            }
        }

        if (locH3Hex != null) {
            if (h3 == null || h3.isEmpty()) {
                locH3Hex.setText("—");
            } else {
                // Display shorter hex for readability (last 12 chars)
                locH3Hex.setText(h3.length() > 12 ? "…" + h3.substring(h3.length() - 12) : h3);
            }
        }
    }

    /* ════════════════════════════════════════════════════════════ *
     *  Hex Coverage                                                *
     *  Shows current hex with local tracking. Real balance is      *
     *  on the server dashboard.                                    *
     * ════════════════════════════════════════════════════════════ */

    private void updateHexCoverage() {
        String h3 = LocationHelper.sH3Hex();

        if (hexList != null) {
            if (h3 == null || h3.isEmpty()) {
                hexList.setText("No GPS fix — hex coverage requires location");
                hexList.setTextColor(0xFF8A94A6);
            } else {
                // Display the current hex with local status
                // We can't know server-side station count, so show what we know
                String hexDisplay = h3.length() > 15 ? h3.substring(0, 15) + "…" : h3;
                hexList.setText("▸ " + hexDisplay + "\n  Local position hex (res-5)\n\n  Visit dashboard for full coverage data\n  and station overlap details.");
                hexList.setTextColor(0xFFE6EAF0);
            }
        }

        // tMAST estimate: just show placeholder since we can't know station overlap
        if (hexTmastTotal != null) {
            boolean hasFix = LocationHelper.sHasFix();
            hexTmastTotal.setText(hasFix ? "see dashboard" : "0.000");
        }
    }

    /* ════════════════════════════════════════════════════════════ *
     *  Upload Stats (from HttpOutputManager)                      *
     * ════════════════════════════════════════════════════════════ */

    private void updateUploadStats(SharedPreferences prefs) {
        if (uploadSent != null) {
            uploadSent.setText(formatNumber(HttpOutputManager.getMessagesSent()));
        }
        if (uploadFailed != null) {
            uploadFailed.setText(formatNumber(HttpOutputManager.getMessagesFailed()));
        }

        long lastUpload = HttpOutputManager.getLastUploadTime();
        if (uploadLast != null) {
            if (lastUpload == 0) {
                uploadLast.setText("—");
            } else {
                long elapsed = System.currentTimeMillis() - lastUpload;
                if (elapsed < 60_000) {
                    uploadLast.setText(elapsed / 1000 + "s ago");
                } else if (elapsed < 3600_000) {
                    uploadLast.setText(elapsed / 60_000 + "m ago");
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    uploadLast.setText(sdf.format(new Date(lastUpload)));
                }
            }
        }

        String url = prefs.getString(HttpOutputManager.PREF_URL, "https://api.mastchain.io/api/upload");
        if (uploadEndpoint != null) {
            // Show just the host for brevity
            try {
                java.net.URI uri = new java.net.URI(url);
                uploadEndpoint.setText(uri.getHost());
            } catch (Exception e) {
                uploadEndpoint.setText(url);
            }
        }
    }

    /* ════════════════════════════════════════════════════════════ *
     *  Utility                                                     *
     * ════════════════════════════════════════════════════════════ */

    private String formatNumber(int n) {
        if (n >= 1_000_000) return String.format(Locale.getDefault(), "%.1fM", n / 1_000_000.0);
        if (n >= 1_000) return String.format(Locale.getDefault(), "%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }
}