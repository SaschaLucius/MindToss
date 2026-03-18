# MindToss macOS Quick Action

Sendet markierten Text (oder Clipboard-Inhalt) als Task per E-Mail – direkt aus dem Rechtsklick-Menü oder per Tastenkürzel.

## 1. Einmalige Einrichtung

### API-Key und Empfänger im Keychain speichern

Öffne das Terminal und führe diese drei Befehle aus. Ersetze die Platzhalter durch deine echten Werte:

```bash
# Resend API-Key
security add-generic-password -s "MindToss" -a "resend-api-key" -w "re_DEIN_API_KEY" -U

# Task-Empfänger E-Mail
security add-generic-password -s "MindToss" -a "task-recipient" -w "deine-task@email.com" -U

# Absender E-Mail (optional – ohne wird onboarding@resend.dev verwendet)
security add-generic-password -s "MindToss" -a "sender-email" -w "notes@deinedomain.de" -U
```

> Die Werte werden im macOS Keychain gespeichert – verschlüsselt und nur für deinen Benutzer zugänglich. Sie erscheinen nie im Klartext in einer Datei.

### Werte später ändern

Einfach den gleichen Befehl nochmal ausführen (das `-U` Flag aktualisiert bestehende Einträge).

### Werte anzeigen

```bash
security find-generic-password -s "MindToss" -a "resend-api-key" -w
security find-generic-password -s "MindToss" -a "task-recipient" -w
```

## 2. Quick Action erstellen (Automator)

1. Öffne **Automator** (Spotlight → "Automator")
2. **Neues Dokument** → Typ: **Quick Action**
3. Oben konfigurieren:
   - "Workflow empfängt" → **Text**
   - "in" → **jedem Programm**
4. Links in der Bibliothek suchen: **Shell-Skript ausführen**
5. In die rechte Fläche ziehen
6. Konfigurieren:
   - Shell: `/bin/zsh`
   - Input übergeben: **als Argumente**
7. Script-Inhalt **komplett ersetzen** mit:

```bash
/Users/slucius/Repositories/MindToss/MindToss1/macos/mindtoss-task.sh "$@"
```

> Ersetze `/pfad/zu/` mit dem tatsächlichen Pfad, z.B.:
> `~/Repositories/MindToss/MindToss1/macos/mindtoss-task.sh "$@"`

8. **Speichern** als: `MindToss Task`

## 3. Tastenkürzel zuweisen

1. **Systemeinstellungen** → **Tastatur** → **Tastaturkurzbefehle**
2. Links: **Services** (oder **App-Kurzbefehle** je nach macOS-Version)
3. Suche nach **MindToss Task** unter "Text"
4. Doppelklick rechts → Kürzel eingeben: **⌃Space** (Control + Leertaste)

> **Wichtig:** macOS belegt ⌃Space standardmäßig für den Wechsel der Eingabequelle. Dieses Kürzel muss zuerst deaktiviert werden:
> **Systemeinstellungen → Tastatur → Tastaturkurzbefehle → Eingabequellen** → Häkchen bei "Vorherige Eingabequelle auswählen" entfernen.

## 4. Verwendung

### Per Rechtsklick

1. Text markieren in beliebiger App
2. Rechtsklick → **Services** → **MindToss Task**

### Per Tastenkürzel

1. Text markieren
2. **⌃Space** drücken

### Aus dem Clipboard

1. Text kopieren (⌘C)
2. **⌃Space** drücken (ohne etwas zu markieren)

### Ergebnis

- **Erfolg:** macOS-Notification "Task gesendet: [Betreff]"
- **Fehler:** macOS-Notification mit Fehlermeldung + Ton

## 5. Funktionsweise

- **Subject:** Erste Zeile des Textes
- **Body:** Gesamter Text
- **Absender:** Konfigurierter Sender oder `onboarding@resend.dev`
- **API-Key:** Wird bei jedem Aufruf sicher aus dem Keychain gelesen
- **Keine Logs, keine temporären Dateien, keine Klartext-Credentials**

## 6. Deinstallation

```bash
# Quick Action entfernen
rm ~/Library/Services/MindToss\ Task.workflow

# Keychain-Einträge entfernen
security delete-generic-password -s "MindToss" -a "resend-api-key"
security delete-generic-password -s "MindToss" -a "task-recipient"
security delete-generic-password -s "MindToss" -a "sender-email"
```
