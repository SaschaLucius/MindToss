---
layout: default
title: Getting Started – MindToss
---

# Getting Started

[← Back to home](./)

---

## Installation

MindToss is distributed as a signed APK via GitHub Releases — no Play Store required.

1. Go to the [latest release](https://github.com/lukulent/MindToss/releases/latest)
2. Download the `.apk` file
3. Open it on your Android device
4. Allow installation from unknown sources if prompted
5. Done

**Requirements:** Android 8.0 (Oreo) or newer.

---

## Resend Setup

MindToss sends emails through the [Resend](https://resend.com) API. You'll need a free Resend account.

### 1. Create an Account

Sign up at [resend.com](https://resend.com). The free tier includes 100 emails/day — more than enough for personal use.

### 2. Get an API Key

1. Go to [resend.com/api-keys](https://resend.com/api-keys)
2. Click **Create API Key**
3. Give it a name (e.g. "MindToss")
4. Set permission to **Sending access**
5. Copy the key — you'll need it in the app

### 3. Sender Email (Optional)

By default, MindToss sends from `onboarding@resend.dev` (Resend's test address). This works fine for personal use.

If you want a custom sender address (e.g. `notes@yourdomain.com`):

1. Add and verify your domain in the [Resend Dashboard](https://resend.com/domains)
2. Enter your custom sender email in MindToss settings

---

## App Configuration

Open MindToss and tap the **gear icon** (⚙️) in the top bar to reach Settings.

### Required

| Setting            | What to enter                                                          |
| ------------------ | ---------------------------------------------------------------------- |
| **Resend API Key** | The API key from step 2 above                                          |
| **Note recipient** | The email address where notes should arrive (e.g. your personal inbox) |

### Optional

| Setting               | What it does                                                                                                                                                                              |
| --------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Sender email**      | Custom sender address. Leave blank to use Resend default.                                                                                                                                 |
| **Task recipient**    | A second email address for tasks. Enables the ✅ button on the main screen. Useful for sending to a kanban board or task manager that accepts email input (e.g. Todoist, Trello, Notion). |
| **Fetch page titles** | When enabled, shared URLs get their page title fetched automatically.                                                                                                                     |
| **Theme**             | System / Light / Dark                                                                                                                                                                     |

---

## Usage

### Basic Flow

1. Open MindToss
2. Type your thought
3. Tap **📧 Mail** (note) or **✅ Task** (task)
4. App sends and closes

The **first line** becomes the email subject, the rest becomes the body.

### Voice Input

Tap the **🎤 microphone** button to dictate. The recognized text is appended to your draft. You can dictate multiple times.

### Sharing from Other Apps

In any app, use the **Share** menu and select MindToss. The shared content appears in the text field, ready to send.

For URLs, MindToss automatically prepends the page title (if "Fetch page titles" is enabled).

### Offline

If you're offline, messages are queued and sent automatically when connectivity returns. A badge in the top bar shows how many messages are pending.

---

## Building from Source

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (includes JDK 17 and Android SDK)
- Android SDK 35

### Build

```bash
# Clone the repository
git clone https://github.com/lukulent/MindToss.git
cd MindToss

# Debug build
./gradlew assembleDebug

# The APK is at app/build/outputs/apk/debug/app-debug.apk
```

### Release Build

Release builds require a signing keystore:

```bash
# Generate a keystore (one-time)
keytool -genkey -v -keystore mindtoss-release.jks \
  -alias mindtoss -keyalg RSA -keysize 2048 -validity 10000

# Build signed release
./gradlew assembleRelease
```

Configure signing in `app/build.gradle.kts` or via environment variables.

---

## GitHub Actions

The repository includes a CI workflow that automatically builds and publishes signed APKs when you push a version tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Required GitHub Secrets:

| Secret              | Description                  |
| ------------------- | ---------------------------- |
| `KEYSTORE_BASE64`   | Base64-encoded keystore file |
| `KEYSTORE_PASSWORD` | Keystore password            |
| `KEY_ALIAS`         | Key alias name               |
| `KEY_PASSWORD`      | Key password                 |
