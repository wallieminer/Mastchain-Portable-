package com.jvdegithub.aiscatcher.ui.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.net.http.SslError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.jvdegithub.aiscatcher.MainActivity;
import com.jvdegithub.aiscatcher.R;
import com.jvdegithub.aiscatcher.tools.LogBook;

import java.io.IOException;
import java.io.InputStream;

public class WebViewMapFragment extends Fragment {

    private WebView webView;
    private LogBook logbook;
    private SharedPreferences sharedPreferences;
    private Context context;
    private Handler retryHandler = new Handler(Looper.getMainLooper());
    private int retryCount = 0;
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 2000;

    public static WebViewMapFragment newInstance() {
        return new WebViewMapFragment();
    }

    private boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        return networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private SharedPreferences getSharedPreferences() {
        if (sharedPreferences == null && context != null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
        return sharedPreferences;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        context = null;
    }

    private String getMapHtml() {
        boolean isDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        String bgColor = isDark ? "#0a0e14" : "#ffffff";

        return "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "<link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />" +
            "<script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>" +
            "<style>body{margin:0;padding:0;background:" + bgColor + ";} #map{width:100vw;height:100vh;}</style></head>" +
            "<body><div id=\"map\"></div>" +
            "<script>" +
            "var map = L.map('map').setView([52.4, 5.3], 10);" +
            "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {" +
            "attribution: '&copy; OpenStreetMap contributors', maxZoom: 19}).addTo(map);" +
            "L.marker([52.4, 5.3]).addTo(map).bindPopup('MastChain Station').openPopup();" +
            "</script></body></html>";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        logbook = LogBook.getInstance();

        View rootView = inflater.inflate(R.layout.fragment_map, container, false);
        webView = rootView.findViewById(R.id.webmap);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                if (url.startsWith("https://cdn.jsdelivr.net/") || url.startsWith("https://unpkg.com/")) {
                    String prefix = url.startsWith("https://cdn.jsdelivr.net/") ? "https://cdn.jsdelivr.net/" : "https://unpkg.com/";
                    String remainingPath = "webassets/cdn/" + url.substring(prefix.length());

                    try {
                        if (context == null) return null;
                        InputStream inputStream = context.getAssets().open(remainingPath);
                        String contentType;
                        if (remainingPath.endsWith(".css")) contentType = "text/css";
                        else if (remainingPath.endsWith(".svg")) contentType = "image/svg+xml";
                        else if (remainingPath.endsWith(".png")) contentType = "image/png";
                        else if (remainingPath.endsWith(".js")) contentType = "text/plain";
                        else return null;
                        return new WebResourceResponse(contentType, "UTF-8", inputStream);
                    } catch (IOException e) {
                        logbook.addLog("Cannot load " + remainingPath);
                    }
                }
                return null;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                webView.setVisibility(View.INVISIBLE);
                if (getSharedPreferences() != null) {
                    String localStorageContent = getSharedPreferences().getString("localStorageContent", null);
                    if (localStorageContent != null) {
                        webView.evaluateJavascript("localStorage.setItem('settings', " + localStorageContent + ");", null);
                    }
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                webView.setVisibility(View.VISIBLE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                logbook.addLog(String.format("W(%d): %s", consoleMessage.lineNumber(), consoleMessage.message()));
                return true;
            }
        });

        // Try local AIS-catcher server first, fallback to online map
        if (MainActivity.port > 0) {
            String url = "http://localhost:" + MainActivity.port + "?welcome=false&android=true";
            if ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
                url += "&dark_mode=true";
            else
                url += "&dark_mode=false";
            webView.loadUrl(url);
            logbook.addLog("Opening: " + url);

            // Fallback to online map if local server doesn't respond
            retryHandler.postDelayed(() -> {
                if (webView.getProgress() < 100 && retryCount < MAX_RETRIES) {
                    retryCount++;
                    logbook.addLog("Local server not responding, trying online map...");
                    loadOnlineMap();
                }
            }, 5000);
        } else {
            loadOnlineMap();
        }

        return rootView;
    }

    private void loadOnlineMap() {
        if (webView != null && context != null) {
            webView.loadDataWithBaseURL("https://openstreetmap.org", getMapHtml(), "text/html", "UTF-8", null);
            logbook.addLog("Loading online map (no local server)");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        retryHandler.removeCallbacksAndMessages(null);
        if (webView != null) {
            webView.evaluateJavascript("localStorage.getItem('settings');", value -> {
                if (value != null && !value.equals("null")) {
                    getSharedPreferences().edit().putString("localStorageContent", value).apply();
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        retryHandler.removeCallbacksAndMessages(null);
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        logbook.addLog("View is destroyed.");
    }
}
