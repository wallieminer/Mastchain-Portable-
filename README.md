# 🚢 MastChain Portable

**AIS-catcher Android app with MastChain integration** — Track ships and earn tokens!

Built by [WallieMiner](https://github.com/wallieminer) based on [AIS-catcher](https://github.com/jvde-github/AIS-catcher-for-Android) by jvde-github.

---

## 📱 What is MastChain Portable?

MastChain Portable is a modified version of the AIS-catcher Android app that **uploads AIS data directly to the MastChain network**. With an RTL-SDR dongle and an Android phone, you can track ships and **earn MAST tokens**!

### 🔥 What We Changed

| Feature | Original AIS-catcher | MastChain Portable |
|---------|---------------------|-------------------|
| **MastChain Upload** | ❌ No HTTP output | ✅ `HttpOutputManager` (335 lines) |
| **MastChain Feed Settings** | ❌ No UI | ✅ URL, User, Password, Protocol, Interval |
| **MastChain Dashboard** | ❌ No dashboard | ✅ Stats, status, community toggle |
| **SSL Certificate Fix** | ❌ Crashes on GrapheneOS | ✅ `onReceivedSslError` bypass |
| **Mixed Content** | ❌ Blocks HTTP+HTTPS | ✅ `MIXED_CONTENT_ALWAYS_ALLOW` |
| **Network Security** | ❌ Blocks MastChain API | ✅ `api.mastchain.io` trusted |
| **Station Support** | ❌ No station ID | ✅ Station ID (WallieM3) |
| **Community Share Toggle** | ❌ Settings only | ✅ Dashboard toggle |

### 🛠️ Technical Details

We built **10 versions** to fix the black map on GrapheneOS/Vanadium:

1. **V1**: Original + MastChain HTTP upload → ✅ Upload works, map black
2. **V2**: + `network_security_config` → map black
3. **V3**: + `onReceivedSslError` + mixed content → ✅ **MAP WORKS!** 🎉
4. **V4**: + global SSL bypass in MainActivity → map black
5. **V5**: + HTTPS tile proxy → map black (Chromium internal SSL)
6. **V6**: + proxy ALL HTTPS → map black
7. **V7**: + buffered proxy → build error
8. **V8**: + JavaScript HTTPS→HTTP injection → map black
9. **V9**: original WebView without fixes → map black
10. **V3-fix**: original WebView + `onReceivedSslError` + mixed content → ✅ **FINAL VERSION** 🏆

**The fix**: Simply `handler.proceed()` in `onReceivedSslError()` + `setMixedContentMode(MIXED_CONTENT_ALWAYS_ALLOW)`. That's it. 10 versions for 2 lines of code. 😂

---

## 📲 Download

Download the latest APK from the [Releases](https://github.com/wallieminer/Mastchain-Portable-/releases) page.

## 🔧 Setup

1. Install the APK on your Android phone
2. Connect an **RTL-SDR dongle** via USB OTG
3. Accept the USB permission popup
4. Go to **Settings → MastChain Feed**
5. Fill in:
   - **URL**: `https://api.mastchain.io/api/upload`
   - **User**: `your@email.com`
   - **Password**: `your MastChain token`
   - **Protocol**: AISCATCHER
   - **Interval**: 60
6. Start the app and watch ships appear! 🚢

## 🏗️ Building from Source

```bash
git clone https://github.com/wallieminer/Mastchain-Portable-.git
cd Mastchain-Portable-
./gradlew assembleDebug
```

The APK will be in `app/build/outputs/apk/debug/app-debug.apk`.

**Note**: You need the Android SDK and NDK to compile the native C++ code.

## 📊 MastChain Dashboard

After setup, you'll see your station on the [MastChain Dashboard](https://app.mastchain.io/):

- ✅ Station online (green dots)
- ✅ AIS messages received
- ✅ Range circle visible
- ✅ Ships on the map

## 🔧 Hardware

- **RTL-SDR dongle** (e.g. RTL-SDR Blog V4)
- **AIS antenna** (162 MHz marine band)
- **Android phone** with USB OTG support
- **USB OTG adapter**

---

## 💻 Code Architecture

### New Files Added

| File | Lines | Purpose |
|------|-------|---------|
| `HttpOutputManager.java` | 335 | HTTP POST engine for MastChain API |
| `MastChainStatsFragment.java` | 200 | Dashboard UI with stats & toggles |
| `fragment_mastchain_stats.xml` | 280 | Dashboard layout (Material Design) |
| `network_security_config.xml` | 18 | Trust MastChain API + map tiles |

### Modified Files

| File | Changes |
|------|---------|
| `MainActivity.java` | Added MastChain tab navigation + HttpOutput lifecycle |
| `Settings.java` | Added HTTP output preferences (hENABLE, hURL, hUSERNAME, hPASSWORD, hSTATIONID, hINTERVAL, hPROTOCOL) |
| `WebViewMapFragment.java` | Added `onReceivedSslError` bypass + `MIXED_CONTENT_ALWAYS_ALLOW` |
| `preferences.xml` | Added MastChain Feed preferences category |
| `bottom_menu.xml` | Added MastChain tab icon |
| `AndroidManifest.xml` | Added `networkSecurityConfig` reference |

---

## 🔌 HttpOutputManager — API Reference

The `HttpOutputManager` is the core of the MastChain integration. It handles all HTTP communication with the MastChain API.

### Supported Protocols

| Protocol | Format | Description |
|----------|--------|-------------|
| **NMEA** | Plain text | One NMEA sentence per line |
| **AISCATCHER** | JSON | Full envelope with station metadata + message array |
| **MINIMAL** | JSON | Same as AISCATCHER but with sparse fields |

### AISCATCHER JSON Format

```json
{
  "protocol": "jsonaiscatcher",
  "encodetime": "1719654321",
  "stationid": "WallieM3",
  "receiver": {
    "description": "AIS-catcher Android",
    "version": "1.09"
  },
  "msgs": [
    "!AIVDM,1,1,,A,15N4cJ`005Jrek0H@9n`DW5608EP,0*13",
    "!AIVDM,1,1,,B,15N4cJ`005Jrek0H@9n`DW5608EP,0*13"
  ]
}
```

### Configuration (SharedPreferences)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `hENABLE` | boolean | `false` | Master on/off switch |
| `hURL` | string | `https://api.mastchain.io/api/upload` | Target HTTP endpoint |
| `hUSERNAME` | string | `wallieminer@protonmail.com` | Basic auth username |
| `hPASSWORD` | string | *(token)* | Basic auth password |
| `hSTATIONID` | string | `WallieM3` | Station identifier |
| `hINTERVAL` | string | `5` | Post interval in seconds |
| `hPROTOCOL` | string | `NMEA` | Protocol: NMEA, AISCATCHER, or MINIMAL |

### Key Methods

```java
// Singleton access
HttpOutputManager getInstance()

// Lifecycle
void start(Context context)    // Read settings + start scheduler
void stop()                     // Stop scheduler + flush remaining

// Data input (called from NMEA callback thread)
void enqueueNmea(String nmea)  // Thread-safe enqueue

// Stats
static int getMessagesSent()
static int getMessagesFailed()
static long getLastUploadTime()
static boolean wasLastUploadSuccess()
static void resetStats()
```

### SSL Handling

The `HttpOutputManager` uses a custom `X509TrustManager` that trusts all certificates. This is required for:

1. **MastChain API** — Self-signed or expired certificates
2. **GrapheneOS/Vanium** — Stricter certificate validation
3. **Local networks** — Corporate/university WiFi with MITM proxies

```java
SSLContext sc = SSLContext.getInstance("TLS");
sc.init(null, new TrustManager[]{new X509TrustManager() {
    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
}}, new SecureRandom());
```

---

## 🗺️ Map Fix (GrapheneOS / Vanadium)

The map showed a black screen on GrapheneOS and Vanadium browsers. After **10 iterations**, the fix was just 2 lines:

### WebViewMapFragment.java

```java
// Line 80: Allow mixed content (HTTP tiles on HTTPS page)
webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

// Lines 84-86: Trust all SSL certificates (self-signed map tiles)
@Override
public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
    handler.proceed();  // ← This is the fix. 1 line. 10 versions. 🤦
}
```

### network_security_config.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certs src="system" />
            <certs src="user" />
        </trust-anchors>
    </base-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">api.mastchain.io</domain>
        <domain includeSubdomains="true">tile.openstreetmap.org</domain>
        <domain includeSubdomains="true">tiles.openseamap.org</domain>
        <!-- ... more tile domains -->
    </domain-config>
</network-security-config>
```

---

## 📊 MastChain Dashboard (MastChainStatsFragment)

The dashboard shows real-time station status and upload statistics.

### Features

- **Status indicator** — Green (online), Yellow (standby), Red (offline)
- **Station ID** — Shows configured station name
- **Data source** — SDR station or community feed
- **Message stats** — Sent count, failed count, last upload time
- **Community Share toggle** — Enable/disable aiscatcher.org sharing
- **MastChain Feed toggle** — Enable/disable HTTP output
- **Open Dashboard button** — Opens app.mastchain.io in browser
- **Reset Stats button** — Clear all upload counters

### Auto-refresh

The dashboard refreshes every **10 seconds** when visible. Stats are read from `HttpOutputManager` singleton.

---

## 📷 Screenshots

*Coming soon — need to take screenshots on a real device*

---

## 👥 Contributors

- **[WallieMiner](https://github.com/wallieminer)** — MastChain integration, HTTP output, SSL fixes, dashboard UI, build & testing
- **[jvde-github](https://github.com/jvde-github)** — Original AIS-catcher app
- **[Avi](https://github.com/avipars)** — Contributing to AIS-catcher
- **SwitchBot** — AI assistant that wrote the code 😄

## 🙏 Credits

- [AIS-catcher](https://github.com/jvde-github/AIS-catcher-for-Android) — Original app
- [MastChain](https://mastchain.io/) — Decentralized AIS network

## 📜 License

Based on AIS-catcher, see original [license](https://github.com/jvde-github/AIS-catcher-for-Android/blob/main/LICENSE).

---

*Built with ❤️ and 10 versions of trial-and-error on a Nintendo Switch (SwitchClaw) and Steam Deck (SteamClaw)*