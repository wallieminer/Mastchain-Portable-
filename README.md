# MastChain AIS Station - Community Builds

Community-optimized builds of the MastChain AIS Catcher app for Android.

## ⚠️ Privacy Fix

The original builds contained **hardcoded personal credentials** as default values in the MastChain Feed settings. These have been replaced with neutral placeholders:

| Setting | Before (⚠️ LEAK) | After (✅ Safe) |
|---------|-------------------|-----------------|
| Username | `wallieminer@protonmail.com` | `user@example.com` |
| Password | *(base64 encoded token)* | `your_api_key_here` |
| Station ID | `WallieM3` | `AIS-Station-01` |
| URL | `https://api.mastchain.io/api/upload` | `https://api.mastchain.io/api/upload` *(unchanged)* |

> **You must enter your own MastChain credentials on first launch.**

---

## 📱 Available Builds

| Build | File | Size | Description |
|-------|------|------|-------------|
| **v3.0 Community** | `builds/mastchain-v3.0-community.apk` | 9.5 MB | Original v3.0 base, credentials sanitized |
| **Nav Finish Community** | `builds/mastchain-nav-finish-community.apk` | 9.5 MB | Nav-finish build with UI improvements, credentials sanitized |

---

## 🔧 What Changed — Build Comparison

### 🤖 Claude Code Changes (Nav Finish Build)
The nav-finish build was created with **Claude Code** and includes:

- ✅ Improved navigation bar flow
- ✅ Better station dashboard integration
- ✅ UI refinements for mobile screens
- ✅ Updated MastChain stats fragment layout
- ⚠️ Original personal credentials were hardcoded → **now fixed**

### 🎮 SwitchBot/OpenClaw Changes (Both Builds)
Both community builds were sanitized by **SwitchBot (OpenClaw)** on the SwitchClaw device:

- ❌ **Removed** hardcoded email (`wallieminer@protonmail.com`)
- ❌ **Removed** hardcoded password/token (base64 encoded)
- ❌ **Removed** hardcoded station ID (`WallieM3`)
- ✅ **Replaced with** neutral placeholders: `user@example.com`, `your_api_key_here`, `AIS-Station-01`
- ✅ MastChain API URL unchanged (required for functionality)

### 📋 Quick Diff (SharedPreferences defaults)

```xml
<!-- BEFORE (dangerous - personal data in APK) -->
<EditTextPreference android:key="hUSERNAME" android:defaultValue="wallieminer@protonmail.com" />
<EditTextPreference android:key="hPASSWORD" android:defaultValue="6mlgmE9UhB5iAa4mOyFdCaZmiWG5t39K5yOC0/H92Hk=" />
<EditTextPreference android:key="hSTATIONID" android:defaultValue="WallieM3" />

<!-- AFTER (safe - neutral placeholders) -->
<EditTextPreference android:key="hUSERNAME" android:defaultValue="user@example.com" />
<EditTextPreference android:key="hPASSWORD" android:defaultValue="your_api_key_here" />
<EditTextPreference android:key="hSTATIONID" android:defaultValue="AIS-Station-01" />
```

---

## 🚀 Installation

1. Download the APK of your choice
2. Enable **"Install from unknown sources"** on your Android device
3. Install the APK
4. Open the app → **Settings** → **MastChain Feed**
5. Enter **your own** Username, Password, and Station ID

---

## 🔒 Security

- ✅ No personal data included in any build
- ✅ No API keys, tokens, or real credentials hardcoded
- ✅ Default values are neutral placeholders only
- ✅ MastChain API URL points to the official endpoint

---

## 🏗️ Building from Source

If you want to rebuild from the decompiled source:

```bash
# Decompile
apktool d mastchain-v3.0.apk -o mastchain-decompiled

# Edit res/xml/preferences.xml - change defaults
# Rebuild
apktool b mastchain-decompiled -o mastchain-community.apk
```

---

Built by the community, for the community 🤝

## 📸 Screenshots

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