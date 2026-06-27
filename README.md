# 🚢 MastChain Mobile

**AIS-catcher Android app met MastChain integratie** — Track schepen en verdien tokens!

Gebouwd door [WallieMiner](https://github.com/wallieminer-tech) op basis van [AIS-catcher](https://github.com/jvde-github/AIS-catcher-for-Android) door jvde-github.

---

## 📱 Wat is MastChain Mobile?

MastChain Mobile is een gemodificeerde versie van de AIS-catcher Android app die **rechtstreeks AIS-data uploadt naar het MastChain netwerk**. Met je RTL-SDR dongle en een Android telefoon kun je schepen tracken en **MAST tokens verdienen**!

### 🔥 Wat we hebben aangepast

| Feature | Origineel | MastChain Mobile |
|---------|-----------|-----------------|
| **Naam** | AIS-catcher | MastChain Mobile |
| **MastChain Upload** | ❌ Geen HTTP output | ✅ HttpOutputManager (335 regels) |
| **MastChain Feed Settings** | ❌ Geen UI | ✅ URL, User, Password, Protocol, Interval |
| **SSL Certificaat Fix** | ❌ Crasht op GrapheneOS | ✅ onReceivedSslError bypass |
| **Mixed Content** | ❌ Blokkeert HTTP+HTTPS | ✅ MIXED_CONTENT_ALWAYS_ALLOW |
| **Network Security** | ❌ Blokkeert MastChain API | ✅ api.mastchain.io vertrouwd |
| **Station Support** | ❌ Geen station ID | ✅ Station ID (WallieM3) |

### 🛠️ Technische Details

We hebben **8 versies** gebouwd om de zwarte kaart op GrapheneOS/Vanadium te fixen:

1. **V1**: Origineel + MastChain HTTP upload → ✅ Upload werkt, kaart zwart
2. **V2**: + network_security_config → kaart zwart
3. **V3**: + onReceivedSslError + mixed content → ✅ **KAART WERKT!** 🎉
4. **V4**: + globale SSL bypass in MainActivity → kaart zwart
5. **V5**: + HTTPS tile proxy → kaart zwart (Chromium interne SSL)
6. **V6**: + proxy ALLE HTTPS → kaart zwart
7. **V7**: + gebufferde proxy → build error
8. **V8**: + JavaScript HTTPS→HTTP injectie → kaart zwart
9. **V9**: originele WebView zonder fixes → kaart zwart
10. **V3-fix**: originele WebView + onReceivedSslError + mixed content → ✅ **FINALE VERSIE** 🏆

**De oplossing**: Simpelweg `handler.proceed()` in `onReceivedSslError()` + `setMixedContentMode(MIXED_CONTENT_ALWAYS_ALLOW)`. Dat was het. 8 versies voor 2 regels code. 😂

---

## 📲 Download

Download de laatste APK van de [Releases](https://github.com/wallieminer-tech/MastChain-Mobile/releases) pagina.

## 🔧 Setup

1. Installeer de APK op je Android telefoon
2. Sluit een **RTL-SDR dongle** aan via USB OTG
3. Accepteer de USB permission popup
4. Ga naar **Settings → MastChain Feed**
5. Vul in:
   - **URL**: `https://api.mastchain.io/api/upload`
   - **User**: `jouw@email.com`
   - **Password**: `jouw MastChain token`
   - **Protocol**: AISCATCHER
   - **Interval**: 60
6. Start de app en zie schepen verschijnen! 🚢

## 🏗️ Bouwen vanaf broncode

```bash
git clone https://github.com/wallieminer-tech/MastChain-Mobile.git
cd MastChain-Mobile
./gradlew assembleDebug
```

De APK staat dan in `app/build/outputs/apk/debug/app-debug.apk`.

**Let op**: Je hebt de Android SDK en NDK nodig om de native C++ code te compileren.

## 📊 MastChain Dashboard

Na het instellen zie je je station op het [MastChain Dashboard](https://app.mastchain.io/):

- ✅ Station online (groene puntjes)
- ✅ AIS berichten ontvangen
- ✅ Range circle zichtbaar
- ✅ Schepen op de kaart

## 🔧 Hardware

- **RTL-SDR dongle** (bijv. RTL-SDR Blog V4)
- **AIS antenne** (162 MHz marine band)
- **Android telefoon** met USB OTG
- **USB OTG adapter**

## 🙏 Credits

- [jvde-github](https://github.com/jvde-github/AIS-catcher) — Originele AIS-catcher app
- [MastChain](https://mastchain.io/) — Decentraal AIS netwerk
- [SwitchBot](https://github.com/wallieminer-tech) — AI assistent die de code schreef 😄

## 📜 Licentie

Gebaseerd op AIS-catcher, zie originele [licentie](https://github.com/jvde-github/AIS-catcher-for-Android/blob/main/LICENSE).

---

*Gebouwd met ❤️ en 8 versies trial-and-error op een Nintendo Switch (SwitchClaw) en Steam Deck (SteamClaw)*