/*
 *     AIS-catcher for Android
 *     Copyright (C)  2022-2023 jvde.github@gmail.com.
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

package com.jvdegithub.aiscatcher;

import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HostnameVerifier;
import java.security.cert.X509Certificate;
import java.security.SecureRandom;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages HTTP output of AIS data to a remote endpoint (e.g. MastChain).
 *
 * <p>This runs alongside the existing Community Hub sharing. When enabled,
 * NMEA sentences received from the native AIS-catcher library are buffered
 * and POSTed to the configured URL at the configured interval.</p>
 *
 * <h3>Supported protocols</h3>
 * <ul>
 *   <li><b>NMEA</b> – plain NMEA sentences, one per line</li>
 *   <li><b>AISCATCHER</b> – JSON envelope with station metadata and message array</li>
 *   <li><b>MINIMAL</b> – same envelope as AISCATCHER but with sparse JSON fields</li>
 * </ul>
 *
 * <h3>Configuration (SharedPreferences)</h3>
 * <ul>
 *   <li>{@code hENABLE} – master on/off switch (boolean)</li>
 *   <li>{@code hURL} – target HTTP endpoint URL (string)</li>
 *   <li>{@code hUSERNAME} – Basic-auth username (string)</li>
 *   <li>{@code hPASSWORD} – Basic-auth password (string)</li>
 *   <li>{@code hSTATIONID} – station identifier (string)</li>
 *   <li>{@code hINTERVAL} – post interval in seconds (integer string)</li>
 *   <li>{@code hPROTOCOL} – "NMEA", "AISCATCHER", or "MINIMAL" (string)</li>
 * </ul>
 */
public class HttpOutputManager {

    private static final String TAG = "AIS-HTTP";

    /* ── Preference keys ──────────────────────────────────────────── */
    public static final String PREF_ENABLE     = "hENABLE";
    public static final String PREF_URL        = "hURL";
    public static final String PREF_USERNAME    = "hUSERNAME";
    public static final String PREF_PASSWORD    = "hPASSWORD";
    public static final String PREF_STATIONID   = "hSTATIONID";
    public static final String PREF_INTERVAL    = "hINTERVAL";
    public static final String PREF_PROTOCOL    = "hPROTOCOL";

    /* ── Defaults ─────────────────────────────────────────────────── */
    private static final String DEFAULT_URL       = "https://api.mastchain.io/api/upload";
    private static final String DEFAULT_USERNAME   = "wallieminer@protonmail.com";
    private static final String DEFAULT_PASSWORD   = "6mlgmE9UhB5iAa4mOyFdCaZmiWG5t39K5yOC0/H92Hk=";
    private static final String DEFAULT_STATIONID = "WallieM3";
    private static final String DEFAULT_INTERVAL   = "5";
    private static final String DEFAULT_PROTOCOL   = "NMEA";

    /* ── Instance state ───────────────────────────────────────────── */
    private final ConcurrentLinkedQueue<String> nmeaQueue = new ConcurrentLinkedQueue<>();
    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    /* ── Config snapshot (read once at start) ─────────────────────── */
    private String url;
    private String authHeader;          // "Basic …"
    private String stationId;
    private int    intervalSec;
    private String protocol;            // "NMEA", "AISCATCHER", "MINIMAL"
    private String model;
    private String modelSetting;

    /* ── Singleton ─────────────────────────────────────────────────── */
    private static final HttpOutputManager INSTANCE = new HttpOutputManager();

    public static HttpOutputManager getInstance() {
        return INSTANCE;
    }

    private HttpOutputManager() { }

    /* ================================================================ *
     *  Public API                                                      *
     * ================================================================ */

    /**
     * Read settings from SharedPreferences and start the periodic POST
     * scheduler.  Calling start() while already running is safe (no-op).
     */
    public synchronized void start(android.content.Context context) {
        if (running) return;
        readSettings(context);
        if (!isEnabled(context)) {
            Log.i(TAG, "HTTP output disabled – not starting");
            return;
        }
        running = true;
        nmeaQueue.clear();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AIS-HTTP-Poster");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::postBatch, intervalSec, intervalSec, TimeUnit.SECONDS);
        Log.i(TAG, "HTTP output started → " + url + " every " + intervalSec + "s [" + protocol + "]");
    }

    /**
     * Stop the scheduler and flush any remaining messages.
     */
    public synchronized void stop() {
        if (!running) return;
        running = false;
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
            scheduler = null;
        }
        // One last flush
        postBatch();
        nmeaQueue.clear();
        Log.i(TAG, "HTTP output stopped");
    }

    /**
     * Enqueue a NMEA sentence received from the native library.
     * Thread-safe; called from the NMEA callback thread.
     */
    public void enqueueNmea(String nmea) {
        if (!running) return;
        if (nmea != null && !nmea.isEmpty()) {
            nmeaQueue.offer(nmea);
        }
    }

    /** Whether the feature is enabled in preferences. */
    public static boolean isEnabled(android.content.Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_ENABLE, false);
    }

    /** Apply defaults for all HTTP output preferences (only on first install). */
    public static void setDefaults(android.content.Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putBoolean(PREF_ENABLE, false);
        if (!prefs.contains(PREF_URL))        ed.putString(PREF_URL, DEFAULT_URL);
        if (!prefs.contains(PREF_USERNAME))   ed.putString(PREF_USERNAME, DEFAULT_USERNAME);
        if (!prefs.contains(PREF_PASSWORD))   ed.putString(PREF_PASSWORD, DEFAULT_PASSWORD);
        if (!prefs.contains(PREF_STATIONID))  ed.putString(PREF_STATIONID, DEFAULT_STATIONID);
        if (!prefs.contains(PREF_INTERVAL))   ed.putString(PREF_INTERVAL, DEFAULT_INTERVAL);
        if (!prefs.contains(PREF_PROTOCOL))  ed.putString(PREF_PROTOCOL, DEFAULT_PROTOCOL);
        ed.apply();
    }

    /* ================================================================ *
     *  Internals                                                       *
     * ================================================================ */

    private void readSettings(android.content.Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        url        = prefs.getString(PREF_URL, DEFAULT_URL);
        String user = prefs.getString(PREF_USERNAME, DEFAULT_USERNAME);
        String pass = prefs.getString(PREF_PASSWORD, DEFAULT_PASSWORD);
        stationId  = prefs.getString(PREF_STATIONID, DEFAULT_STATIONID);
        protocol   = prefs.getString(PREF_PROTOCOL, DEFAULT_PROTOCOL);
        try {
            intervalSec = Integer.parseInt(prefs.getString(PREF_INTERVAL, DEFAULT_INTERVAL));
            if (intervalSec < 1) intervalSec = 1;
        } catch (NumberFormatException e) {
            intervalSec = 5;
        }

        // Build Basic auth header once
        String credentials = user + ":" + pass;
        authHeader = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

        // Model info from native
        try {
            model = AisCatcherJava.getLibraryVersion();
        } catch (Exception e) {
            model = "AIS-catcher Android";
        }
        modelSetting = "";
    }

    /**
     * Drain the queue, build a request body according to the selected
     * protocol, and POST it to the configured endpoint.
     */
    private void postBatch() {
        List<String> batch = new ArrayList<>();
        while (!nmeaQueue.isEmpty()) {
            String s = nmeaQueue.poll();
            if (s != null) batch.add(s);
        }

        // Always post (heartbeat) for AISCATCHER/MINIMAL; for NMEA skip empty
        String body = buildBody(batch);
        if (body == null || body.isEmpty()) {
            if (protocol.equals("NMEA")) return; // nothing to send
        }

        HttpURLConnection conn = null;
        try {
            URL endpoint = new URL(url);
            conn = (HttpURLConnection) endpoint.openConnection();

            // Trust all SSL certificates for MastChain compatibility
            if (conn instanceof HttpsURLConnection) {
                try {
                    SSLContext sc = SSLContext.getInstance("TLS");
                    sc.init(null, new TrustManager[]{new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }}, new SecureRandom());
                    ((HttpsURLConnection) conn).setSSLSocketFactory(sc.getSocketFactory());
                    ((HttpsURLConnection) conn).setHostnameVerifier((hostname, session) -> true);
                } catch (Exception e) {
                    Log.w(TAG, "SSL trust setup failed: " + e.getMessage());
                }
            }
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", protocol.equals("NMEA")
                    ? "text/plain; charset=utf-8"
                    : "application/json; charset=utf-8");
            if (authHeader != null) {
                conn.setRequestProperty("Authorization", authHeader);
            }
            conn.setRequestProperty("User-Agent", "AIS-catcher-Android/1.0");
            conn.setInstanceFollowRedirects(true);

            // Write body
            byte[] bodyBytes = body.getBytes("UTF-8");
            conn.setFixedLengthStreamingMode(bodyBytes.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                Log.d(TAG, "POST OK (" + responseCode + ") – " + batch.size() + " msgs sent");
            } else {
                // Read error body for diagnostics
                String errorBody = "";
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    errorBody = sb.toString();
                } catch (Exception ignored) { }
                Log.w(TAG, "POST failed (" + responseCode + "): " + errorBody);
            }
        } catch (Exception e) {
            Log.e(TAG, "HTTP POST error: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Build the POST body based on the selected protocol.
     *
     * <ul>
     *   <li><b>NMEA</b> – plain NMEA sentences separated by newlines</li>
     *   <li><b>AISCATCHER</b> – full JSON envelope with station metadata</li>
     *   <li><b>MINIMAL</b> – same as AISCATCHER but each msg uses sparse dict</li>
     * </ul>
     */
    private String buildBody(List<String> msgs) {
        if (protocol.equals("NMEA")) {
            if (msgs.isEmpty()) return null;
            StringBuilder sb = new StringBuilder();
            for (String m : msgs) {
                sb.append(m);
                if (!m.endsWith("\n")) sb.append('\n');
            }
            return sb.toString();
        }

        // AISCATCHER / MINIMAL – JSON envelope
        // Matches the CLI's HTTPStreamer::post() AISCATCHER protocol format
        long now = System.currentTimeMillis() / 1000;
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"protocol\":\"").append(protocol.equals("MINIMAL") ? "jsonaiscatcher" : "jsonaiscatcher").append("\",");
        sb.append("\"encodetime\":\"").append(now).append("\",");
        sb.append("\"stationid\":\"").append(escapeJson(stationId)).append("\",");

        sb.append("\"receiver\":{");
        sb.append("\"description\":\"AIS-catcher Android\",");
        sb.append("\"version\":\"1.09\"");
        sb.append("},");

        sb.append("\"msgs\":[");
        for (int i = 0; i < msgs.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(msgs.get(i));
        }
        sb.append(']');
        sb.append('}');
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}