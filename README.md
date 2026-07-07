# Mastchain Mobile - Community Builds

Community-optimized builds of the Mastchain Mobile AIS Catcher app for Android.

## ⚠️ Privacy Fix

The original builds contained **hardcoded personal credentials** as default values in the Mastchain Feed settings. These have been **removed** — all credential fields are now blank:

| Setting | After (✅ Safe) |
|---------|-----------------|
| Username | *(blank — enter your own)* |
| Password | *(blank — enter your own)* |
| Station ID | *(blank — enter your own)* |
| URL | `https://api.mastchain.io/api/upload` *(unchanged)* |

> **You must enter your own Mastchain credentials on first launch.**

---

## 📱 Download

### 🌟 Mastchain Mobile v3.0 (Recommended)
**Mastchain-Mobile-v3.0-release.apk** (9.6 MB)

- ✅ **Release-signed** — works on ALL Android phones
- ✅ APK Signature v1 + v2 + v3 — maximum compatibility
- ✅ Native AIS Catcher with Mastchain integration
- ✅ No hardcoded credentials — enter your own
- ✅ Works with RTL-SDR dongle or network AIS feed
- ✅ Online map, ship tracking, Mastchain dashboard

**Tested on:**
- ✅ Solana Seeker (Android 16)
- ✅ GrapheneOS Google Pixel 7a (Android 17)

👉 [Download from Releases](https://github.com/wallieminer/Mastchain-Portable-/releases/tag/v1.10-community)

---

## 🇷🇴 Descarcă pentru C-Man

Aplicația **Mastchain Mobile** funcționează pe orice telefon Android.

1. Descarcă APK-ul: [Mastchain-Mobile-v3.0-release.apk](https://github.com/wallieminer/Mastchain-Portable-/releases/tag/v1.10-community)
2. Activează **"Instalare din surse necunoscute"** în setările telefonului
3. Instalează APK-ul
4. Deschide aplicația → **Setări** → **Mastchain Feed**
5. Introdu datele tale de conectare Mastchain

✅ Semnat și verificat — merge pe toate telefoanele. \
✅ Fără date personale incluse — introdu tu propriul username, parolă și station ID.

---

## 🔧 What Changed — Build Comparison

### 🤖 Claude Code Changes (Nav Finish Build)
The nav-finish build was created with **Claude Code** and includes:

- ✅ Improved navigation bar flow
- ✅ Better station dashboard integration
- ✅ UI refinements for mobile screens
- ✅ Updated Mastchain stats fragment layout
- ⚠️ Original personal credentials were hardcoded → **now removed**

### 🎮 SwitchBot/OpenClaw Changes (Both Builds)
Both community builds were sanitized by **SwitchBot (OpenClaw)** on the SwitchClaw device:

- ❌ **Removed** hardcoded email → **blank**
- ❌ **Removed** hardcoded password/token → **blank**
- ❌ **Removed** hardcoded station ID → **blank**
- ✅ All three fields now default to **empty** — users must enter their own credentials
- ✅ Mastchain API URL unchanged (required for functionality)

### 📋 Quick Diff (SharedPreferences defaults)

```xml
<!-- BEFORE (dangerous - personal data in APK) -->
<EditTextPreference android:key="hUSERNAME" android:defaultValue="user@example.com" />
<EditTextPreference android:key="hPASSWORD" android:defaultValue="your_password_here" />
<EditTextPreference android:key="hSTATIONID" android:defaultValue="Station-01" />

<!-- AFTER (safe - blank defaults, user enters own credentials) -->
<EditTextPreference android:key="hUSERNAME" android:defaultValue="" />
<EditTextPreference android:key="hPASSWORD" android:defaultValue="" />
<EditTextPreference android:key="hSTATIONID" android:defaultValue="" />
```

---

## 🚀 Installation

1. Download the APK
2. Enable **"Install from unknown sources"** on your Android device
3. Install the APK
4. Open the app → **Settings** → **Mastchain Feed**
5. Enter **your own** Username, Password, and Station ID

---

## 🔒 Security

- ✅ No personal data included in any build
- ✅ No API keys, tokens, or real credentials hardcoded
- ✅ All credential fields are **blank** — users must enter their own
- ✅ Mastchain API URL points to the official endpoint
- ✅ Mastchain Mobile release-signed by WallieMiner (CN=WallieMiner, O=Mastchain Community, C=NL)

## 🔑 Signing Details

| Build | Signer | Scheme | Key |
|-------|--------|--------|-----|
| Mastchain Mobile v3.0 | WallieMiner, Mastchain Community, NL | v1+v2+v3 | Release keystore |

---

## 🏗️ Building from Source

If you want to rebuild from the decompiled source:

```bash
# Decompile
apktool d mastchain-v3.0.apk -o mastchain-decompiled

# Edit res/xml/preferences.xml - change defaults to blank
# Rebuild
apktool b mastchain-decompiled -o mastchain-community.apk
```

---

## 📸 Screenshots

<p align="center">
  <img src="screenshots/01-main-map.jpg" width="200" alt="Main map view" />
  <img src="screenshots/02-map-zoomed.jpg" width="200" alt="Map zoomed in" />
  <img src="screenshots/05-mastchain-dashboard.jpg" width="200" alt="Mastchain dashboard" />
  <img src="screenshots/07-settings-main.jpg" width="200" alt="Settings" />
</p>
<p align="center">
  <img src="screenshots/09-mastchain-stats.jpg" width="200" alt="Mastchain stats" />
  <img src="screenshots/10-app-about.jpg" width="200" alt="App about" />
</p>

| # | File | Description |
|---|------|-------------|
| 1 | `screenshots/01-main-map.jpg` | Main map with ship positions |
| 2 | `screenshots/02-map-zoomed.jpg` | Map zoomed in on vessels |
| 3 | `screenshots/05-mastchain-dashboard.jpg` | Mastchain dashboard tab |
| 4 | `screenshots/07-settings-main.jpg` | Main settings screen |
| 5 | `screenshots/09-mastchain-stats.jpg` | Mastchain statistics |
| 6 | `screenshots/10-app-about.jpg` | App info screen |

---

Built by the community, for the community 🤝
