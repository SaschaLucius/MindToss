# MindToss – Anforderungsübersicht

> Schnell Gedanken aus dem Kopf, per Mail weiterleiten, App schließt sich. Fertig.

---

## 1. Projektübersicht

|                      |                                             |
| -------------------- | ------------------------------------------- |
| **App-Name**         | MindToss                                    |
| **Package**          | `lukulent.mindtoss.app`                     |
| **Plattform**        | Android (min. API 26, target API 35)        |
| **Sprache**          | Kotlin 2.1.0                                |
| **UI-Framework**     | Jetpack Compose (BOM 2024.10.00, Material3) |
| **HTTP-Client**      | Ktor 3.0.3 (OkHttp Engine)                  |
| **Lokaler Speicher** | DataStore Preferences                       |
| **Offline-Queue**    | WorkManager 2.10.0 (Jetpack Library)        |
| **Serialisierung**   | kotlinx.serialization 1.7.3                 |
| **Mail-Service**     | Resend REST API v1 (Plain Text)             |
| **Build-Tool**       | Gradle 8.9 / AGP 8.7.3                      |
| **Repository**       | GitHub (Open Source)                        |

---

## 2. Kernkonzept

Der Nutzer öffnet MindToss, tippt oder spricht einen Gedanken, drückt Senden – die App schließt sich. Der Gedanke landet per Mail an der richtigen Stelle (Notiz oder Task) und kann dort in Ruhe bearbeitet werden.

---

## 3. Screens

### 3.1 Main Screen

- Großes, mehrzeiliges Textfeld für die Notiz
- Mikrofon-Button für **Voice-to-Text** (Android native Speech-to-Text API) → Ergebnis erscheint im Textfeld
- Zwei Send-Buttons nebeneinander:
- 📧 **Mail-Button** → sendet an primäre Empfänger-Adresse (Notizen)
- ✅ **Task-Button** → sendet an zweite Empfänger-Adresse (Tasks/Kanban)
- Task-Button wird **versteckt**, wenn keine zweite Mail-Adresse konfiguriert ist
- Nach erfolgreichem Senden: **App schließt sich automatisch**, keine Meldung
- Bei Fehler: **Fehlermeldung anzeigen**, App bleibt offen
- **Auto-Save Draft:** Inhalt wird kontinuierlich in DataStore gespeichert (bei jeder Änderung)

### 3.2 Settings Screen

Erreichbar über Icon im Main Screen.

**Resend Konfiguration**

- Resend API-Key (Eingabe maskiert, ein-/ausblendbar)
- Absender-E-Mail (z.B. `notes@meinedomain.de`)

**Empfänger**

- Empfänger-E-Mail – Notizen (primär, Pflichtfeld)
- Empfänger-E-Mail – Tasks (optional; Task-Button erscheint nur wenn ausgefüllt)

**Verhalten**

- Seiten-Titel bei Links abrufen: Ein/Aus (Toggle)

**Erscheinungsbild**

- Theme: System / Hell / Dunkel

**Historie**

- Liste aller gesendeten Nachrichten (unbegrenzt, lokal gespeichert)
- Je Eintrag sichtbar: Inhalt, Datum/Uhrzeit, Typ (📧 oder ✅)
- Aktionen pro Eintrag: **Erneut senden** | **Löschen** | **Kopieren**

---

## 4. Mail-Format

| Feld        | Wert                   |
| ----------- | ---------------------- |
| **Format**  | Plain Text             |
| **Betreff** | Erste Zeile des Textes |
| **Body**    | Alle weiteren Zeilen   |
| **Service** | Resend REST API        |

---

## 5. Android Share Target

- MindToss erscheint im nativen Android „Teilen"-Menü (via `ACTION_SEND` Intent-Filter)
- Empfängt: `text/plain` – Text, Links, gemischten Inhalt
- **Bei Links** (wenn Toggle aktiv): Seiten-Titel wird automatisch abgerufen und über den Link eingefügt
- **Bei gemischtem Inhalt** (Text + Link): Erste Zeile = Betreff, restlicher Inhalt = Body; Titel-Fetch gilt nur für Links im Body

---

## 6. Offline-Verhalten

- Wenn kein Internet verfügbar: Nachricht wird in eine **Offline-Queue** gelegt
- Technologie: **WorkManager** (Jetpack Library, `androidx.work`) mit `NetworkType.CONNECTED` Constraint
- Retry-Strategie: Exponential Backoff (ab 30 Sekunden)
- Sobald Internet wieder verfügbar: automatisch senden
- Nutzer sieht visuell (Badge in der TopBar), dass Nachrichten in der Queue warten

---

## 7. Theme / Design

- **Minimalistisch & schlicht**
- Unterstützt **System / Hell / Dunkel** (konfigurierbar in Settings)
- Folgt Material Design 3
- Nutzt **Dynamic Color** auf Android 12+ (Material You), Fallback auf eigenes Farbschema

---

## 8. Build & Release (GitHub Actions)

- Build-Format: **APK** (kein AAB, kein Play Store)
- APK wird mit einem **eigenen Release-Key** signiert
- Key wird als **GitHub Secret** hinterlegt (nie im Repository)
- **GitHub Actions Workflow:**
  - Trigger: **Git Tag** (z.B. `v1.0.0`)
  - Schritte: Build → Sign → Upload als GitHub Release
  - Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
- **Lokales Bauen:** via `./gradlew assembleRelease` / `./gradlew assembleDebug` – einfach, ohne extra Schritte
- **Voraussetzung:** Android Studio (bringt JDK 17 und Android SDK mit)

---

## 9. Zu erstellende Dokumente

| Datei                             | Inhalt                                                                                      |
| --------------------------------- | ------------------------------------------------------------------------------------------- |
| `README.md`                       | Projektbeschreibung, Screenshots, Setup-Anleitung, Resend-Konfiguration, Signing, Beitragen |
| `.github/copilot-instructions.md` | Architektur-Überblick, Konventionen, Dateistruktur, Hinweise für AI-assistiertes Coding     |

---

## 10. Bewusst ausgeschlossen

Die folgenden Features wurden evaluiert und **bewusst nicht aufgenommen** um den minimalistischen Scope zu wahren:

- Onboarding-Flow beim ersten Start
- App Shortcuts (Long-Press Icon)
- Bild-Anhänge
- Audio-Anhänge / Voice Memo als Datei
- HTML-Mail-Format
- AAB / Google Play Store
- Tags oder Kategorien
- Export der Historie
- Widget / Quick Settings Tile
