# MastChain AIS Station - Community Builds

Community-optimized builds of the MastChain AIS Catcher app for Android.

## ⚠️ Privacy Fix

The original builds contained **hardcoded personal credentials** as default values in the MastChain Feed settings. These have been **removed** — all credential fields are now blank:

| Setting | After (✅ Safe) |
|---------|-----------------|
| Username | *(blank — enter your own)* |
| Password | *(blank — enter your own)* |
| Station ID | *(blank — enter your own)* |
| URL | `https://api.mastchain.io/api/upload` *(unchanged)* |

> **You must enter your own MastChain credentials on first launch.**

---

## 📱 Available Builds

### 🌟 Moonraise Tool3 (Recommended)

| Build | File | Size | Description |
|-------|------|------|-------------|
| **Tool3 v6** | `builds/moonraise-tool3-v6.apk` | 3.3 MB | Official Moonraise TWA — lightweight web wrapper for `tool3.xyz` |

**Why recommended:**
- ✅ Release-signed by Moonraise (CN=Cong Hung) — installs on all Android devices
- ✅ APK Signature v3 — works on Android 5.0+
- ✅ Only 3.3 MB — lightweight
- ✅ No hardcoded credentials — clean
- ✅ Always up-to-date — loads the web interface from tool3.xyz
- ⚠️ Requires internet connection (loads web app)

### 🔧 AIS Catcher (Advanced / Offline)

| Build | File | Size | Description |
|-------|------|------|-------------|
| **v3.0 Community** | `builds/mastchain-v3.0-community.apk` | 9.6 MB | Original v3.0 base, credentials removed |
| **Nav Finish Community** | `builds/mastchain-nav-finish-community.apk` | 9.6 MB | Nav-finish build with UI improvements, credentials removed |

**For advanced users:**
- ✅ Native app — works offline with SDR dongle
- ✅ AIS Catcher + MastChain upload integration
- ⚠️ Debug-signed — may not install on all devices
- ⚠️ Larger download (9.6 MB)
- ⚠️ Requires RTL-SDR dongle for full functionality

---

## 🔧 What Changed — Build Comparison

### 🌙 Moonraise Tool3 (Official TWA)
- Official app from Moonraise, signed with their release key
- TWA (Trusted Web Activity) wrapper loading `tool3.xyz`
- No credentials, no personal data — completely clean
- v3 APK signature scheme for maximum compatibility

### 🤖 Claude Code Changes (Nav Finish Build)
The nav-finish build was created with **Claude Code** and includes:

- ✅ Improved navigation bar flow
- ✅ Better station dashboard integration
- ✅ UI refinements for mobile screens
- ✅ Updated MastChain stats fragment layout
- ⚠️ Original personal credentials were hardcoded → **now removed**

### 🎮 SwitchBot/OpenClaw Changes (Both AIS Catcher Builds)
Both community builds were sanitized by **SwitchBot (OpenClaw)** on the SwitchClaw device:

- ❌ **Removed** hardcoded email → **blank**
- ❌ **Removed** hardcoded password/token → **blank**
- ❌ **Removed** hardcoded station ID → **blank**
- ✅ All three fields now default to **empty** — users must enter their own credentials
- ✅ MastChain API URL unchanged (required for functionality)

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

### Moonraise Tool3 (Recommended)
1. Download `moonraise-tool3-v6.apk` from the builds folder
2. Enable **"Install from unknown sources"** on your Android device
3. Install the APK
4. Open the app → it loads `tool3.xyz`
5. Log in with your MastChain credentials

### AIS Catcher (Advanced)
1. Download the APK of your choice
2. Enable **"Install from unknown sources"** on your Android device
3. Install the APK
4. Open the app → **Settings** → **MastChain Feed**
5. Enter **your own** Username, Password, and Station ID

---

## 🔒 Security

- ✅ No personal data included in any build
- ✅ No API keys, tokens, or real credentials hardcoded
- ✅ All credential fields are **blank** — users must enter their own
- ✅ MastChain API URL points to the official endpoint
- ✅ Moonraise Tool3 APK is release-signed by Moonraise (CN=Cong Hung, VN)

---

## 📊 Build Comparison

| | Moonraise Tool3 v6 | AIS Catcher v3.0 | AIS Catcher Nav Finish |
|---|---|---|---|
| **Package** | com.moonraise.tool3 | com.jvdegithub.aiscatcher | com.jvdegithub.aiscatcher |
| **Type** | TWA (web wrapper) | Native app | Native app |
| **Size** | 3.3 MB | 9.6 MB | 9.6 MB |
| **Signing** | Moonraise release key | Debug key | Debug key |
| **Min Android** | 5.0 (SDK 21) | 6.0 (SDK 23) | 6.0 (SDK 23) |
| **Target Android** | 15 (SDK 35) | 14 (SDK 34) | 14 (SDK 34) |
| **Needs Internet** | ✅ Yes | ❌ No (SDR mode) | ❌ No (SDR mode) |
| **Needs SDR Dongle** | ❌ No | ✅ Yes | ✅ Yes |
| **Credentials** | None (web login) | None (removed) | None (removed) |
