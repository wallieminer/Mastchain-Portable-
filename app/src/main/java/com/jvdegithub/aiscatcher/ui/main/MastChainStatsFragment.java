/*
 *     MastChain Mobile – MastChain Dashboard Fragment
 *     Copyright (C)  2025 MastChain integration additions
 *
 *     Displays station status, uptime, live reception, GPS location,
 *     H3 hex coverage, upload stats, and bond/withdrawal info.
 *
 *     Colors follow the MastChain design system:
 *     --mc-bg-deep:#0A0E14, --mc-bg-panel:#121821, --mc-orange:#F26A1B,
 *     --mc-cyan:#00C2D1, --mc-green:#29C46A, --mc-yellow:#E5B53A,
 *     --mc-red:#E5484D, --mc-text:#E6EAF0, --mc-text-dim:#8A94A6
 */

package com.jvdegithub.aiscatcher.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.jvdegithub.aiscatcher.AisCatcherJava;
import com.jvdegithub.aiscatcher.AisService;
import com.jvdegithub.aiscatcher.DeviceManager;
import com.jvdegithub.aiscatcher.HttpOutputManager;
import com.jvdegithub.aiscatcher.LocationHelper;
import com.jvdegithub.aiscatcher.R;
import com.jvdegithub.aiscatcher.Settings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * MastChain Dashboard with real data from AIS decoder, GPS, and HTTP upload.
 *
 * Sections (top to bottom):
 * 1. Header: station ID + online status
 * 2. Uptime: 288-slot bar, percentage, warning
 * 3. Live Reception: total messages, vessels, channel A/B, msg/min
 * 4. Location & Hex: GPS lat/lon, accuracy, H3 hex ID
 * 5. Hex Coverage: hex list with progress, color, tMAST estimate
 * 6. Upload: sent/failed/last upload, endpoint
 * 7. Bond & Withdrawal: bond status, explanation
 * 8. Buttons: Open Dashboard, Reset Stats
 */
public class MastChainStatsFragment extends Fragment {

    private static final String TAG = "MastChainStats";
    private static final long REFRESH_INTERVAL_MS = 5_000; // 5 seconds
    private static final String STATION_DASHBOARD_URL = "https://app.mastchain.io/app/dashboard/880";

    /* ── MastChain Colors ──────────────────────────────────────────── */
    private static final int COLOR_BG_DEEP = Color.parseColor("#0A0E14");
    private static final int COLOR_BG_PANEL = Color.parseColor("#121821");
    private static final int COLOR_BG_ELEV = Color.parseColor("#1A2230");
    private static final int COLOR_ORANGE = Color.parseColor("#F26A1B");
    private static final int COLOR_ORANGE_DK = Color.parseColor("#C9540F");
    private static final int COLOR_CYAN = Color.parseColor("#00C2D1");
    private static final int COLOR_GREEN = Color.parseColor("#29C46A");
    private static final int COLOR_YELLOW = Color.parseColor("#E5B53A");
    private static final int COLOR_RED = Color.parseColor("#E5484D");
    private static final int COLOR_TEXT = Color.parseColor("#E6EAF0");
    private static final int COLOR_TEXT_DIM = Color.parseColor("#8A94A6");
    private static final int COLOR_LINE = Color.parseColor("#232C3A");

    /* ── H3 / MastChain reward constants ───────────────────────────── */
    private static final double TMAST_PER_HEX = 0.223;
    private static final int HEX_ELIGIBLE_THRESHOLD = 500;

    /* ── Uptime tracking ───────────────────────────────────────────── */
    private static final int SLOTS_PER_DAY = 288; // 5-minute slots in 24 hours
    private static final long SLOT_DURATION_MS = 5 * 60 * 1000L;
    private final boolean[] uptimeSlots = new boolean[SLOTS_PER_DAY];
    private long sessionStartTime = 0;
    private long lastUploadSlotTime = 0;

    /* ── Hex coverage tracking ─────────────────────────────────────── */
    private final Map<String, HexInfo> hexMap = new HashMap<>();
    private long msgCountAtLastRefresh = 0;

    /* ── Views ─────────────────────────────────────────────────────── */
    private View statusIndicator;
    private TextView statusText;
    private TextView stationIdText;
    private TextView uptimePercentage;
    private LinearLayout uptimeBar;
    private TextView uptimeWarning;
    private TextView totalMessagesText;
    private TextView vesselsSeenText;
    private TextView channelABText;
    private TextView msgPerMinText;
    private TextView gpsStatusText;
    private TextView gpsLastFixText;
    private TextView latitudeText;
    private TextView longitudeText;
    private TextView accuracyText;
    private TextView hexIdText;
    private TextView hexListText;
    private TextView estimatedTmastText;
    private TextView messagesSentText;
    private TextView messagesFailedText;
    private TextView lastUploadText;
    private TextView endpointUrlText;
    private TextView bondStatusText;
    private SwitchCompat communityShareToggle;
    private SwitchCompat mastchainFeedToggle;

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = this::refreshStats;

    /* ── Hex info for coverage tracking ────────────────────────────── */
    private static class HexInfo {
        int messageCount;
        long firstSeen;
        long lastSeen;
        int stationCount; // 1 = green, >1 = yellow with N

        HexInfo() {
            messageCount = 0;
            firstSeen = System.currentTimeMillis();
            lastSeen = firstSeen;
            stationCount = 1;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mastchain_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Section 1: Header
        statusIndicator = view.findViewById(R.id.status_indicator);
        statusText = view.findViewById(R.id.status_text);
        stationIdText = view.findViewById(R.id.station_id_text);

        // Section 2: Uptime
        uptimePercentage = view.findViewById(R.id.uptime_percentage);
        uptimeBar = view.findViewById(R.id.uptime_bar);
        uptimeWarning = view.findViewById(R.id.uptime_warning);

        // Section 3: Live Reception
        totalMessagesText = view.findViewById(R.id.total_messages_text);
        vesselsSeenText = view.findViewById(R.id.vessels_seen_text);
        channelABText = view.findViewById(R.id.channel_ab_text);
        msgPerMinText = view.findViewById(R.id.msg_per_min_text);

        // Section 4: Location & Hex
        gpsStatusText = view.findViewById(R.id.gps_status_text);
        gpsLastFixText = view.findViewById(R.id.gps_last_fix_text);
        latitudeText = view.findViewById(R.id.latitude_text);
        longitudeText = view.findViewById(R.id.longitude_text);
        accuracyText = view.findViewById(R.id.accuracy_text);
        hexIdText = view.findViewById(R.id.hex_id_text);

        // Section 5: Hex Coverage
        hexListText = view.findViewById(R.id.hex_list_text);
        estimatedTmastText = view.findViewById(R.id.estimated_tmast_text);

        // Section 6: Upload
        messagesSentText = view.findViewById(R.id.messages_sent_text);
        messagesFailedText = view.findViewById(R.id.messages_failed_text);
        lastUploadText = view.findViewById(R.id.last_upload_text);
        endpointUrlText = view.findViewById(R.id.endpoint_url_text);

        // Section 7: Bond
        bondStatusText = view.findViewById(R.id.bond_status_text);

        // Toggles (from original dashboard)
        communityShareToggle = view.findViewById(R.id.community_share_toggle);
        mastchainFeedToggle = view.findViewById(R.id.mastchain_feed_toggle);

        // Initialize uptime
        sessionStartTime = System.currentTimeMillis();
        markUptimeSlot();

        // Setup status indicator shape
        if (statusIndicator != null) {
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(COLOR_RED);
            statusIndicator.setBackground(dot);
        }

        // Setup toggles
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        if (communityShareToggle != null) {
            communityShareToggle.setChecked(prefs.getBoolean("sSHARING", false));
            communityShareToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("sSHARING", isChecked).apply();
                refreshStats();
            });
        }
        if (mastchainFeedToggle != null) {
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

        // Buttons
        view.findViewById(R.id.open_dashboard_button).setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(STATION_DASHBOARD_URL));
            startActivity(browserIntent);
        });

        view.findViewById(R.id.reset_stats_button).setOnClickListener(v -> {
            HttpOutputManager.resetStats();
            hexMap.clear();
            refreshStats();
        });

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

    /* ================================================================ *
     *  Uptime Tracking                                                  *
     * ================================================================ */

    private void markUptimeSlot() {
        long now = System.currentTimeMillis();
        int slotIndex = getSlotIndex(now);
        if (slotIndex >= 0 && slotIndex < SLOTS_PER_DAY) {
            uptimeSlots[slotIndex] = true;
        }
        lastUploadSlotTime = now;
    }

    private int getSlotIndex(long timeMs) {
        // Slots from midnight local time
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String time = sdf.format(new Date(timeMs));
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        return (hours * 60 + minutes) / 5; // 5-minute slots
    }

    private double calculateUptimePercentage() {
        int onlineSlots = 0;
        int totalSlotsToday = getSlotIndex(System.currentTimeMillis()) + 1;
        if (totalSlotsToday <= 0) totalSlotsToday = 1;

        for (int i = 0; i < totalSlotsToday && i < SLOTS_PER_DAY; i++) {
            if (uptimeSlots[i]) onlineSlots++;
        }
        return (double) onlineSlots / totalSlotsToday * 100.0;
    }

    private void buildUptimeBar() {
        if (uptimeBar == null) return;
        uptimeBar.removeAllViews();

        int totalSlotsToday = getSlotIndex(System.currentTimeMillis()) + 1;
        int segmentSize = Math.max(1, totalSlotsToday / 48); // 48 segments visible

        for (int i = 0; i < totalSlotsToday && i < SLOTS_PER_DAY; i += segmentSize) {
            View segment = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
            params.setMargins(0, 0, 1, 0);
            segment.setLayoutParams(params);

            // Check if any slot in this segment is online
            boolean online = false;
            for (int j = i; j < i + segmentSize && j < SLOTS_PER_DAY; j++) {
                if (uptimeSlots[j]) { online = true; break; }
            }

            segment.setBackgroundColor(online ? COLOR_GREEN : COLOR_RED);
            uptimeBar.addView(segment);
        }
    }

    /* ================================================================ *
     *  Refresh Stats                                                    *
     * ================================================================ */

    private void refreshStats() {
        if (getView() == null || !isAdded()) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // ── Section 1: Header ─────────────────────────────────────────
        boolean feedEnabled = prefs.getBoolean("hENABLE", false);
        boolean serviceRunning = AisService.isRunning(requireContext());
        boolean isOnline = feedEnabled && serviceRunning;

        if (statusIndicator != null) {
            GradientDrawable dot = (GradientDrawable) statusIndicator.getBackground();
            dot.setColor(isOnline ? COLOR_GREEN : COLOR_RED);
        }
        if (statusText != null) {
            statusText.setText(isOnline ? "Online" : (feedEnabled ? "Standby" : "Offline"));
            statusText.setTextColor(isOnline ? COLOR_GREEN : (feedEnabled ? COLOR_YELLOW : COLOR_RED));
        }
        String stationId = prefs.getString(HttpOutputManager.PREF_STATIONID, "—");
        if (stationIdText != null) {
            stationIdText.setText(stationId);
        }

        // Mark uptime if online
        if (feedEnabled && HttpOutputManager.wasLastUploadSuccess()) {
            markUptimeSlot();
        }

        // ── Section 2: Uptime ──────────────────────────────────────────
        double uptimePct = calculateUptimePercentage();
        if (uptimePercentage != null) {
            uptimePercentage.setText(String.format(Locale.getDefault(), "%.1f%%", uptimePct));
            uptimePercentage.setTextColor(uptimePct >= 80 ? COLOR_GREEN : COLOR_RED);
        }
        buildUptimeBar();

        if (uptimeWarning != null) {
            if (uptimePct < 80 && uptimePct > 0) {
                uptimeWarning.setVisibility(View.VISIBLE);
                uptimeWarning.setText(String.format(Locale.getDefault(),
                        "⚠ Uptime below 80%% (%.1f%%). Push every minute to stay green.", uptimePct));
            } else {
                uptimeWarning.setVisibility(View.GONE);
            }
        }

        // ── Section 3: Live Reception ──────────────────────────────────
        int total = AisCatcherJava.Statistics.getTotal();
        int chA = AisCatcherJava.Statistics.getChA();
        int chB = AisCatcherJava.Statistics.getChB();
        int msg123 = AisCatcherJava.Statistics.getMsg123();
        int msg5 = AisCatcherJava.Statistics.getMsg5();
        int msg1819 = AisCatcherJava.Statistics.getMsg1819();
        int msg24 = AisCatcherJava.Statistics.getMsg24();

        if (totalMessagesText != null) totalMessagesText.setText(String.valueOf(total));
        if (channelABText != null) channelABText.setText(chA + " / " + chB);

        // Vessels seen (approximation: messages 5 + 24 + 1819 are ship-specific)
        int vesselsEst = msg5 + msg24;
        if (vesselsSeenText != null) vesselsSeenText.setText(String.valueOf(vesselsEst));

        // Messages per minute
        long uptime = System.currentTimeMillis() - sessionStartTime;
        if (uptime > 60000) {
            double msgMin = (double) total / (uptime / 60000.0);
            if (msgPerMinText != null) msgPerMinText.setText(String.format(Locale.getDefault(), "%.1f", msgMin));
        } else {
            if (msgPerMinText != null) msgPerMinText.setText("—");
        }

        // ── Section 4: Location & Hex ────────────────────────────────
        LocationHelper locHelper = LocationHelper.getInstance();
        if (locHelper != null) {
            if (gpsStatusText != null) {
                gpsStatusText.setText(locHelper.getGpsStatus());
                boolean hasFix = locHelper.hasFix();
                gpsStatusText.setTextColor(hasFix ? COLOR_GREEN : COLOR_RED);
            }
            if (gpsLastFixText != null) gpsLastFixText.setText(locHelper.getTimeSinceFix());
            if (latitudeText != null) latitudeText.setText(locHelper.getLatitudeString());
            if (longitudeText != null) longitudeText.setText(locHelper.getLongitudeString());
            if (accuracyText != null) accuracyText.setText(locHelper.getAccuracyString());

            String hex = locHelper.getH3HexId();
            if (hexIdText != null) hexIdText.setText(hex.isEmpty() ? "—" : hex);

            // Track hex coverage
            if (locHelper.hasFix() && !hex.isEmpty()) {
                HexInfo info = hexMap.get(hex);
                if (info == null) {
                    info = new HexInfo();
                    hexMap.put(hex, info);
                }
                info.messageCount += (total - msgCountAtLastRefresh);
                info.lastSeen = System.currentTimeMillis();
            }
        } else {
            if (gpsStatusText != null) {
                gpsStatusText.setText("GPS unavailable");
                gpsStatusText.setTextColor(COLOR_RED);
            }
        }
        msgCountAtLastRefresh = total;

        // ── Section 5: Hex Coverage ───────────────────────────────────
        StringBuilder hexListBuilder = new StringBuilder();
        double totalTmast = 0;

        for (Map.Entry<String, HexInfo> entry : hexMap.entrySet()) {
            String hexId = entry.getKey();
            HexInfo info = entry.getValue();
            int count = info.messageCount;
            boolean eligible = count >= HEX_ELIGIBLE_THRESHOLD;

            String colorEmoji;
            double tmast;
            if (!eligible) {
                colorEmoji = "⬜";
                tmast = 0;
            } else if (info.stationCount <= 2) {
                colorEmoji = "🟢";
                tmast = TMAST_PER_HEX / info.stationCount;
            } else {
                colorEmoji = "🟡";
                tmast = TMAST_PER_HEX / info.stationCount;
            }

            hexListBuilder.append(String.format(Locale.getDefault(),
                    "%s %s  %d/%d  %.3f tMAST\n",
                    colorEmoji, hexId, count, HEX_ELIGIBLE_THRESHOLD, tmast));
            totalTmast += tmast;
        }

        if (hexListText != null) {
            hexListText.setText(hexMap.isEmpty() ? "No hex data yet (GPS fixing…)" : hexListBuilder.toString().trim());
        }
        if (estimatedTmastText != null) {
            estimatedTmastText.setText(String.format(Locale.getDefault(), "%.3f", totalTmast));
        }

        // ── Section 6: Upload ─────────────────────────────────────────
        if (messagesSentText != null) messagesSentText.setText(String.valueOf(HttpOutputManager.getMessagesSent()));
        if (messagesFailedText != null) messagesFailedText.setText(String.valueOf(HttpOutputManager.getMessagesFailed()));

        long lastUpload = HttpOutputManager.getLastUploadTime();
        if (lastUploadText != null) {
            if (lastUpload == 0) {
                lastUploadText.setText("—");
            } else {
                long elapsed = System.currentTimeMillis() - lastUpload;
                if (elapsed < 60_000) lastUploadText.setText(elapsed / 1000 + "s ago");
                else if (elapsed < 3_600_000) lastUploadText.setText(elapsed / 60_000 + "m ago");
                else {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    lastUploadText.setText(sdf.format(new Date(lastUpload)));
                }
            }
        }

        String url = prefs.getString(HttpOutputManager.PREF_URL, "https://api.mastchain.io/api/upload");
        if (endpointUrlText != null) endpointUrlText.setText(url);

        // ── Section 7: Bond ────────────────────────────────────────────
        // tMAST estimation from hex coverage
        if (bondStatusText != null) {
            if (totalTmast >= 10) {
                bondStatusText.setText(String.format(Locale.getDefault(), "✅ %.1f tMAST (bond met!)", totalTmast));
                bondStatusText.setTextColor(COLOR_GREEN);
            } else {
                bondStatusText.setText(String.format(Locale.getDefault(), "%.1f / 10 tMAST needed", totalTmast));
                bondStatusText.setTextColor(COLOR_YELLOW);
            }
        }

        // Schedule next refresh
        refreshHandler.removeCallbacks(refreshRunnable);
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
    }
}