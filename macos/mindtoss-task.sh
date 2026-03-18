#!/bin/zsh
# MindToss Quick Action – Sendet markierten oder kopierten Text als Task per Resend API
# API-Key wird sicher aus dem macOS Keychain gelesen.

set -euo pipefail

# --- Hilfsfunktion: Notification anzeigen ---
show_success() {
    osascript -e "display notification \"$1\" with title \"MindToss ✔\""
}

show_error() {
    osascript -e "display notification \"$1\" with title \"MindToss ✘\" sound name \"Basso\""
}

# --- Konfiguration ---
KEYCHAIN_SERVICE="MindToss"
KEYCHAIN_ACCOUNT_API="resend-api-key"
KEYCHAIN_ACCOUNT_TO="task-recipient"
KEYCHAIN_ACCOUNT_FROM="sender-email"
DEFAULT_FROM="onboarding@resend.dev"

# --- API-Key aus Keychain lesen ---
API_KEY=$(security find-generic-password -s "$KEYCHAIN_SERVICE" -a "$KEYCHAIN_ACCOUNT_API" -w 2>/dev/null) || {
    show_error "Resend API-Key nicht im Keychain gefunden. Bitte Setup ausführen."
    exit 1
}

# --- Empfänger aus Keychain lesen ---
TO=$(security find-generic-password -s "$KEYCHAIN_SERVICE" -a "$KEYCHAIN_ACCOUNT_TO" -w 2>/dev/null) || {
    show_error "Task-Empfänger nicht im Keychain gefunden. Bitte Setup ausführen."
    exit 1
}

# --- Absender aus Keychain lesen (optional, Fallback auf Default) ---
FROM=$(security find-generic-password -s "$KEYCHAIN_SERVICE" -a "$KEYCHAIN_ACCOUNT_FROM" -w 2>/dev/null) || FROM="$DEFAULT_FROM"

# --- Text aus Argumenten lesen (von Automator übergeben), Fallback auf Clipboard ---
if [ $# -gt 0 ]; then
    INPUT="$@"
else
    INPUT=$(pbpaste)
fi

if [ -z "$INPUT" ]; then
    show_error "Kein Text markiert."
    exit 1
fi

# --- Subject = erste Zeile, Body = gesamter Text ---
SUBJECT=$(echo "$INPUT" | head -1)

# --- Quell-Metadaten sammeln ---
SOURCE_APP=$(osascript -e 'tell application "System Events" to tell (first process whose frontmost is true) to return name' 2>/dev/null || echo "")
SOURCE_WINDOW=$(osascript -e 'tell application "System Events" to tell (first process whose frontmost is true) to return name of window 1' 2>/dev/null || echo "")
SOURCE_URL=""

# URL aus Browser holen (Safari, Chrome, Arc, Brave, Edge)
case "$SOURCE_APP" in
    Safari)
        SOURCE_URL=$(osascript -e 'tell application "Safari" to get URL of front document' 2>/dev/null || echo "")
        ;;
    "Google Chrome"|Arc|"Brave Browser"|"Microsoft Edge")
        SOURCE_URL=$(osascript -e "tell application \"$SOURCE_APP\" to get URL of active tab of front window" 2>/dev/null || echo "")
        ;;
esac

# Metadaten an Text anhängen
SOURCE_BLOCK=""
if [ -n "$SOURCE_APP" ]; then
    SOURCE_BLOCK=$'\n\n---\n'"Quelle: $SOURCE_APP"
    if [ -n "$SOURCE_WINDOW" ]; then
        SOURCE_BLOCK="$SOURCE_BLOCK"$'\n'"Fenster: $SOURCE_WINDOW"
    fi
    if [ -n "$SOURCE_URL" ]; then
        SOURCE_BLOCK="$SOURCE_BLOCK"$'\n'"URL: $SOURCE_URL"
    fi
fi

FULL_TEXT="${INPUT}${SOURCE_BLOCK}"

# --- JSON sicher erzeugen ---
JSON_BODY=$(python3 -c "
import json, sys
text = sys.stdin.read()
subject = text.splitlines()[0] if text.strip() else ''
print(json.dumps({
    'from': sys.argv[1],
    'to': [sys.argv[2]],
    'subject': subject,
    'text': text
}))
" "$FROM" "$TO" <<< "$FULL_TEXT")

# --- Senden ---
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST 'https://api.resend.com/emails' \
    -H "Authorization: Bearer $API_KEY" \
    -H 'Content-Type: application/json' \
    -d "$JSON_BODY")

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    show_success "Task gesendet: ${SUBJECT:0:50}"
else
    ERROR=$(echo "$BODY" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('message','Unbekannter Fehler'))" 2>/dev/null || echo "$BODY")
    show_error "Fehler: $ERROR"
    exit 1
fi
