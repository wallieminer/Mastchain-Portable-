# 🚢 MastChain Portable

**AIS-catcher Android app with MastChain integration** — Track ships and earn tokens!

Built by [WallieMiner](https://github.com/wallieminer) based on [AIS-catcher](https://github.com/jvde-github/AIS-catcher-for-Android) by jvde-github.

---

## 📱 What is MastChain Portable?

MastChain Portable is a modified version of the AIS-catcher Android app that **uploads AIS data directly to the MastChain network**. With an RTL-SDR dongle and an Android phone, you can track ships and **earn MAST tokens**!

### 🔥 What We Changed

| Feature | Original | MastChain Portable |
|---------|----------|-------------------|
| **Name** | AIS-catcher | MastChain Portable |
| **MastChain Upload** | ❌ No HTTP output | ✅ HttpOutputManager (335 lines) |
| **MastChain Feed Settings** | ❌ No UI | ✅ URL, User, Password, Protocol, Interval |
| **SSL Certificate Fix** | ❌ Crashes on GrapheneOS | ✅ onReceivedSslError bypass |
| **Mixed Content** | ❌ Blocks HTTP+HTTPS | ✅ MIXED_CONTENT_ALWAYS_ALLOW |
| **Network Security** | ❌ Blocks MastChain API | ✅ api.mastchain.io trusted |
| **Station Support** | ❌ No station ID | ✅ Station ID (WallieM3) |

### 🛠️ Technical Details

We built **10 versions** to fix the black map on GrapheneOS/Vanadium:

1. **V1**: Original + MastChain HTTP upload → ✅ Upload works, map black
2. **V2**: + network_security_config → map black
3. **V3**: + onReceivedSslError + mixed content → ✅ **MAP WORKS!** 🎉
4. **V4**: + global SSL bypass in MainActivity → map black
5. **V5**: + HTTPS tile proxy → map black (Chromium internal SSL)
6. **V6**: + proxy ALL HTTPS → map black
7. **V7**: + buffered proxy → build error
8. **V8**: + JavaScript HTTPS→HTTP injection → map black
9. **V9**: original WebView without fixes → map black
10. **V3-fix**: original WebView + onReceivedSslError + mixed content → ✅ **FINAL VERSION** 🏆

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

## 👥 Contributors

- **[WallieMiner](https://github.com/wallieminer)** — MastChain integration, HTTP output, SSL fixes, build & testing
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