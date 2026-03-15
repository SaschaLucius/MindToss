---
layout: default
title: Features – MindToss
---

# Features

[← Back to home](./)

---

## Quick Capture

The main screen is a single large text field. No folders, no tags, no categories — just a place to dump your thought. Type it out or use voice input, then send.

**Voice-to-Text** uses Android's built-in Speech Recognition API. Tap the microphone, speak, and the transcribed text appears in the field. You can dictate multiple times — each result is appended on a new line.

**Draft auto-save** means your text is never lost. Every keystroke is persisted to local storage. Close the app, come back later — your draft is still there.

---

## Two Channels: Notes & Tasks

MindToss supports **two independent email recipients**:

- **Mail button (📧)** — Sends to your primary recipient (notes, ideas, read-later)
- **Task button (✅)** — Sends to a second recipient (task manager, kanban board)

The task button only appears when a task recipient is configured. If you only need one destination, you'll never see it.

### Mail Format

| Field   | Value                                                             |
| ------- | ----------------------------------------------------------------- |
| Format  | Plain text                                                        |
| Subject | First line of your text                                           |
| Body    | All remaining lines                                               |
| Sender  | Your configured email or Resend default (`onboarding@resend.dev`) |

---

## Auto-Close

After a successful send, MindToss **closes itself immediately**. No confirmation screen, no animation, no "message sent" toast. The thought is gone — you're back to whatever you were doing.

If sending fails, the app stays open and shows the error with an option to copy it to clipboard.

---

## Offline Queue

No internet? No problem. MindToss detects network availability and automatically queues messages for later delivery.

- Uses **Android WorkManager** with a network connectivity constraint
- Retry strategy: exponential backoff starting at 30 seconds
- Messages send automatically when connectivity returns
- A **badge indicator** in the top bar shows pending messages

You can send and close the app even while offline — WorkManager handles delivery in the background.

---

## Share Target

MindToss registers as an Android share target for `text/plain` content. This means you can share links, text, or mixed content from **any app** directly into MindToss.

When you share content:

1. MindToss opens with the shared text pre-filled in the draft field
2. You can edit it before sending, or just hit send immediately

### Smart Link Handling

When the "Fetch page titles" setting is enabled and you share a URL:

1. MindToss first checks if the sharing app provided a page title (via `EXTRA_SUBJECT`)
2. If not, it fetches the page's `<title>` tag via HTTP
3. The title is inserted above the URL, becoming the email subject

**Before:**

```
https://example.com/interesting-article
```

**After:**

```
The Interesting Article Title
https://example.com/interesting-article
```

HTML entities in fetched titles (like `&#x2F;` or `&amp;`) are automatically decoded.

---

## Send History

The settings screen includes a full history of all sent messages:

- **Content preview** with timestamp and type indicator (📧 note / ✅ task)
- **Resend** — Send the message again with current settings
- **Copy** — Copy the content to clipboard
- **Delete** — Remove the entry from history

History is stored locally in DataStore as serialized JSON. It persists across app updates and is never sent anywhere.

---

## Theming

MindToss follows Material Design 3 and supports three theme modes:

- **System** — Follows your device's dark/light setting
- **Light** — Always light
- **Dark** — Always dark

On Android 12+ devices, MindToss uses **Dynamic Color (Material You)**, adapting its color palette to your wallpaper. On older devices, it falls back to a teal-based color scheme.

---

## Settings

All configuration lives in a single settings screen:

| Setting           | Description                                              |
| ----------------- | -------------------------------------------------------- |
| Resend API Key    | Your Resend account API key (masked input)               |
| Sender email      | Custom sender address, or leave blank for Resend default |
| Note recipient    | Primary email address (required)                         |
| Task recipient    | Second email address (optional, enables task button)     |
| Fetch page titles | Toggle automatic title fetching for shared links         |
| Theme             | System / Light / Dark                                    |
