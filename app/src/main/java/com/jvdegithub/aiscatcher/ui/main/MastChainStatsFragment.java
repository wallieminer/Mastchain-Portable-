/*
 *     AIS-catcher for Android – MastChain Dashboard Fragment
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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.jvdegithub.aiscatcher.AisService;
import com.jvdegithub.aiscatcher.DeviceManager;
import com.jvdegithub.aiscatcher.HttpOutputManager;
import com.jvdegithub.aiscatcher.R;
import com.jvdegithub.aiscatcher.Settings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Dashboard fragment showing MastChain station status, upload statistics,
 * and toggles for Community Share (sSHARING) and MastChain Feed (hENABLE).
 */
public class MastChainStatsFragment extends Fragment {

    private static final String TAG = "MastChainStats";
    private static final long REFRESH_INTERVAL_MS = 10_000; // 10 seconds

    private View statusIndicator;
    private TextView statusText;
    private TextView stationIdText;
    private TextView dataSourceText;
    private TextView messagesSentText;
    private TextView messagesFailedText;
    private TextView lastUploadText;
    private TextView endpointUrlText;
    private SwitchCompat communityShareToggle;
    private SwitchCompat mastchainFeedToggle;

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = this::refreshStats;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mastchain_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        statusIndicator = view.findViewById(R.id.status_indicator);
        statusText = view.findViewById(R.id.status_text);
        stationIdText = view.findViewById(R.id.station_id_text);
        dataSourceText = view.findViewById(R.id.data_source_text);
        messagesSentText = view.findViewById(R.id.messages_sent_text);
        messagesFailedText = view.findViewById(R.id.messages_failed_text);
        lastUploadText = view.findViewById(R.id.last_upload_text);
        endpointUrlText = view.findViewById(R.id.endpoint_url_text);
        communityShareToggle = view.findViewById(R.id.community_share_toggle);
        mastchainFeedToggle = view.findViewById(R.id.mastchain_feed_toggle);

        // Make status indicator circular
        if (statusIndicator != null) {
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(0xFFCC0000);
            statusIndicator.setBackground(dot);
        }

        // Load toggle state from preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        communityShareToggle.setChecked(prefs.getBoolean("sSHARING", false));
        mastchainFeedToggle.setChecked(prefs.getBoolean("hENABLE", false));

        // Community Share toggle listener
        communityShareToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putBoolean("sSHARING", isChecked);
            ed.apply();
            refreshStats();
        });

        // MastChain Feed toggle listener
        mastchainFeedToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putBoolean("hENABLE", isChecked);
            ed.apply();
            // Start or stop HTTP output immediately
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
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://app.mastchain.io/"));
            startActivity(browserIntent);
        });

        // Reset Stats button
        MaterialButton resetStatsBtn = view.findViewById(R.id.reset_stats_button);
        resetStatsBtn.setOnClickListener(v -> {
            HttpOutputManager.resetStats();
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

    private void refreshStats() {
        if (getView() == null || !isAdded()) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // Station status – based on whether MastChain feed (hENABLE) is active
        boolean feedEnabled = prefs.getBoolean("hENABLE", false);
        boolean serviceRunning = AisService.isRunning(requireContext());
        boolean isOnline = feedEnabled && serviceRunning;

        if (statusIndicator != null) {
            GradientDrawable dot = (GradientDrawable) statusIndicator.getBackground();
            dot.setColor(isOnline ? 0xFF00CC44 : 0xFFCC0000);
        }
        if (statusText != null) {
            statusText.setText(isOnline ? "Online" : (feedEnabled ? "Standby" : "Offline"));
            statusText.setTextColor(isOnline ? 0xFF00CC44 : (feedEnabled ? 0xFFFFAA00 : 0xFFFF4444));
        }

        // Station ID
        String stationId = prefs.getString(HttpOutputManager.PREF_STATIONID, "—");
        if (stationIdText != null) {
            stationIdText.setText("Station: " + stationId);
        }

        // Data source
        String source;
        if (serviceRunning) {
            String deviceType = DeviceManager.getDeviceTypeDescription();
            source = "🟢 Your SDR station (" + deviceType + ")";
        } else if (prefs.getBoolean("sSHARING", false)) {
            source = "🌐 Community feed (aiscatcher.org) — no local SDR";
        } else {
            source = "⚪ No active data source";
        }
        if (dataSourceText != null) {
            dataSourceText.setText(source);
        }

        // Upload stats
        if (messagesSentText != null) {
            messagesSentText.setText(String.valueOf(HttpOutputManager.getMessagesSent()));
        }
        if (messagesFailedText != null) {
            messagesFailedText.setText(String.valueOf(HttpOutputManager.getMessagesFailed()));
        }

        long lastUpload = HttpOutputManager.getLastUploadTime();
        if (lastUploadText != null) {
            if (lastUpload == 0) {
                lastUploadText.setText("—");
            } else {
                long elapsed = System.currentTimeMillis() - lastUpload;
                if (elapsed < 60_000) {
                    lastUploadText.setText(elapsed / 1000 + "s ago");
                } else if (elapsed < 3600_000) {
                    lastUploadText.setText(elapsed / 60_000 + "m ago");
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    lastUploadText.setText(sdf.format(new Date(lastUpload)));
                }
            }
        }

        // Endpoint URL
        String url = prefs.getString(HttpOutputManager.PREF_URL, "https://api.mastchain.io/api/upload");
        if (endpointUrlText != null) {
            endpointUrlText.setText(url);
        }

        // Refresh toggles (in case changed from settings)
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
}