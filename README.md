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
| **Station Support** | ❌ No station ID | ✅ Station ID (configurable) |
| **Community Share Toggle** | ❌ Settings only | ✅ Dashboard toggle |
| **Privacy** | ⚠️ Hardcoded credentials | ✅ **Blank defaults — enter your own** |

### 🔒 Privacy Fix (v3.0-community & nav-finish-community)

The original builds contained **hardcoded personal credentials** as default values. The community builds have all credential fields set to **blank**:

| Setting | Before (⚠️ LEAK) | After (✅ Safe) |
|---------|-------------------|-----------------|
| Username | `wallieminer@protonmail.com` | *(blank — enter your own)* |
| Password | *(base64 encoded token)* | *(blank — enter your own)* |
| Station ID | `WallieM3` | *(blank — enter your own)* |
| URL | `https://api.mastchain.io/api/upload` | `https://api.mastchain.io/api/upload` *(unchanged)* |

```xml
<!-- BEFORE (dangerous - personal data in APK) -->
<EditTextPreference android:key="hUSERNAME" android:defaultValue="wallieminer@protonmail.com" />
<EditTextPreference android:key="hPASSWORD" android:defaultValue="6mlgmE9UhB5iAa4mOyFdCaZmiWG5t39K5yOC0/H92Hk=" />
<EditTextPreference android:key="hSTATIONID" android:defaultValue="WallieM3" />

<!-- AFTER (safe - blank defaults, user enters own credentials) -->
<EditTextPreference android:key="hUSERNAME" android:defaultValue="" />
<EditTextPreference android:key="hPASSWORD" android:defaultValue="" />
<EditTextPreference android:key="hSTATIONID" android:defaultValue="" />
```

---

## 📲 Download

Download the latest APK from the [Releases](https://github.com/wallieminer/Mastchain-Portable-/releases) page.

### Available Builds

| Build | File | Description |
|-------|------|-------------|
| **v3.0 Community** | `mastchain-v3.0-community.apk` | Original v3.0 base, credentials removed |
| **Nav Finish Community** | `mastchain-nav-finish-community.apk` | Nav-finish build with UI improvements, credentials removed |

### 🔧 Build Comparison

**🤖 Claude Code Changes (Nav Finish Build):**
- ✅ Improved navigation bar flow
- ✅ Better station dashboard integration
- ✅ UI refinements for mobile screens
- ✅ Updated MastChain stats fragment layout

**🎮 SwitchBot/OpenClaw Changes (Both Community Builds):**
- ❌ Removed hardcoded email → **blank**
- ❌ Removed hardcoded password/token → **blank**
- ❌ Removed hardcoded station ID → **blank**
- ✅ All credential fields default to **empty** — users must enter their own

---

## 🔧 Setup

1. Install the APK on your Android phone
2. Connect an **RTL-SDR dongle** via USB OTG
3. Accept the USB permission popup
4. Go to **Settings → MastChain Feed**
5. Fill in **your own** credentials:
   - **URL**: `https://api.mastchain.io/api/upload`
   - **User**: Your MastChain email
   - **Password**: Your MastChain token
   - **Station ID**: Your station name
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

### Rebuilding from APK (apktool)

If you want to modify the APK directly:

```bash
# Decompile
apktool d mastchain-v3.0.apk -o mastchain-decompiled

# Edit res/xml/preferences.xml — change defaults to blank
# Rebuild
apktool b mastchain-decompiled -o mastchain-community.apk
```

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
| `preferences.xml` | Added MastChain Feed preferences category — **blank defaults** |
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
  "stationid": "YourStationID",
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
| `hUSERNAME` | string | *(blank)* | Your MastChain username |
| `hPASSWORD` | string | *(blank)* | Your MastChain password/token |
| `hSTATIONID` | string | *(blank)* | Your station identifier |
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

| # | Screenshot | Description |
|---|-----------|-------------|
| 1 | `screenshots/screenshot-01-main.jpg` | Main screen |
| 2 | `screenshots/screenshot-02.jpg` | App view |
| 3 | `screenshots/screenshot-03.jpg` | Settings |
| 4 | `screenshots/screenshot-04.jpg` | App view |
| 5 | `screenshots/screenshot-05.jpg` | App view |
| 6 | `screenshots/screenshot-06.jpg` | App view |
| 7 | `screenshots/screenshot-07.jpg` | App view |
| 8 | `screenshots/screenshot-08.jpg` | App view |
| 9 | `screenshots/screenshot-09.jpg` | App view |
| 10 | `screenshots/screenshot-10.jpg` | App view |

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