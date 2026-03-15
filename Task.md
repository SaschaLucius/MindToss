# MindToss – Anforderungsübersicht

> Schnell Gedanken aus dem Kopf, per Mail weiterleiten, App schließt sich. Fertig.

---

## 1. Projektübersicht

|                      |                                          |
| -------------------- | ---------------------------------------- |
| **App-Name**         | MindToss                                 |
| **Plattform**        | Android (min. API 26 / Android 8.0 Oreo) |
| **Sprache**          | Kotlin                                   |
| **UI-Framework**     | Jetpack Compose                          |
| **HTTP-Client**      | Ktor                                     |
| **Lokaler Speicher** | DataStore                                |
| **Mail-Service**     | Resend API (REST, Plain Text)            |
| **Build-Tool**       | Gradle                                   |
| **Repository**       | GitHub (Open Source)                     |

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
- **Auto-Save Draft:** Inhalt wird automatisch gespeichert beim Wechsel zu Settings oder beim Schließen der App

### 3.2 Settings Screen

Erreichbar über Icon im Main Screen.

**Resend Konfiguration**

- Resend API-Key
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

- MindToss erscheint im nativen Android „Teilen"-Menü
- Empfängt: Text, Links, gemischten Inhalt
- **Bei Links** (wenn Toggle aktiv): Seiten-Titel wird automatisch abgerufen und über den Link eingefügt
- **Bei gemischtem Inhalt** (Text + Link): Erste Zeile = Betreff, restlicher Inhalt = Body; Titel-Fetch gilt nur für Links im Body

---

## 6. Offline-Verhalten

- Wenn kein Internet verfügbar: Nachricht wird in eine **Offline-Queue** gelegt
- Sobald Internet wieder verfügbar: automatisch senden
- Nutzer sieht visuell, dass die Nachricht in der Queue wartet

---

## 7. Theme / Design

- **Minimalistisch & schlicht**
- Unterstützt **System / Hell / Dunkel** (konfigurierbar in Settings)
- Folgt Material Design 3

---

## 8. Build & Release (GitHub Actions)

- Build-Format: **APK** (kein AAB, kein Play Store)
- APK wird mit einem **eigenen Release-Key** signiert
- Key wird als **GitHub Secret** hinterlegt (nie im Repository)
- **GitHub Actions Workflow:**
- Trigger: **Git Tag** (z.B. `v1.0.0`)
- Schritte: Build → Sign → Upload als GitHub Release
- **Lokales Bauen:** via `./gradlew assembleRelease` / `./gradlew assembleDebug` – einfach, ohne extra Schritte

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
