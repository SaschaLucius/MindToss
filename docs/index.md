---
layout: default
title: MindToss
---

# MindToss

> Gedanken raus, Mail raus, fertig.

MindToss is a minimalist Android app for capturing thoughts instantly. Type or dictate a note, hit send — the thought lands in your inbox, the app closes. No friction, no overhead.

Born from the frustration of losing fleeting ideas before they can be written down, MindToss reduces the capture process to its absolute minimum: **open → type → send → done**.

---

## Why MindToss?

Most note-taking apps want to _be_ your system. MindToss doesn't. It's the **front door** to whatever system you already use — your inbox, a task board, a read-later list. It gets thoughts out of your head and into the right place via email, then gets out of your way.

### The Philosophy

- **Speed over features** — The app should close before you can think about organizing.
- **Email as universal inbox** — Every productivity system accepts email. No proprietary sync, no lock-in.
- **One job, done well** — Capture. That's it.

---

## What It Does

- **Quick capture** — Large text field, voice-to-text, auto-closing after send
- **Two channels** — Separate recipients for notes and tasks (mail vs. task button)
- **Offline queue** — No internet? Messages queue up and send automatically when you're back online
- **Share target** — Share links and text from any app directly into MindToss
- **Smart link handling** — Automatically fetches page titles for shared URLs
- **Draft persistence** — Your draft survives app restarts
- **Send history** — Review, resend, copy, or delete past messages
- **Theming** — System / Light / Dark with Material You dynamic colors

[See all features →](features)

---

## How It Works

1. Open MindToss (or share content from another app)
2. Type or dictate your thought
3. Tap **Mail** (note) or **Task** (task/kanban)
4. The message is sent via [Resend](https://resend.com) API as a plain-text email
5. The app closes automatically

The **first line** becomes the email subject, everything else becomes the body. Simple as that.

[Getting started →](setup)

---

## Inspired By

MindToss stands on the shoulders of apps that pioneered the "quick capture to email" concept:

### [BrainToss](https://www.braintoss.com/)

The original "toss it to your future self" app. BrainToss captures text, photos, and voice memos and emails them to you. MindToss takes this core idea — **instant email capture** — and strips it down to plain text with a focus on speed and auto-close.

### [Drafts](https://getdrafts.app/)

Drafts popularized the idea of "write first, decide later" on iOS. Its action system lets you route text anywhere after capture. MindToss borrows the **capture-first mindset** but skips the routing step — your destinations are pre-configured, so sending is a single tap.

### [Email Me](https://emailmeapp.net/)

Email Me is the "send yourself a quick email" utility. It's the closest sibling to MindToss in concept. MindToss differentiates with **two separate channels** (notes vs. tasks), **offline queuing**, **share target integration**, and **automatic link title fetching**.

### What's Different

|                 | BrainToss          | Drafts         | Email Me     | **MindToss**            |
| --------------- | ------------------ | -------------- | ------------ | ----------------------- |
| Platform        | Android, iOS       | iOS, Mac       | Android, iOS | **Android**             |
| Capture method  | Text, Photo, Voice | Text           | Text         | **Text, Voice-to-Text** |
| Delivery        | Email              | Actions (many) | Email        | **Email (Resend API)**  |
| Auto-close      | Yes                | No             | No           | **Yes**                 |
| Offline queue   | No                 | No             | No           | **Yes (WorkManager)**   |
| Share target    | No                 | Yes (iOS)      | No           | **Yes**                 |
| Link titles     | No                 | No             | No           | **Yes**                 |
| Dual recipients | No                 | Via actions    | No           | **Yes (Notes + Tasks)** |
| Open source     | No                 | No             | No           | **Yes**                 |

---

## Technical Overview

MindToss is a single-module Android app built with modern Android tooling:

- **Kotlin** with Jetpack Compose (Material Design 3)
- **Resend REST API** for email delivery (plain text, no SMTP)
- **WorkManager** for offline message queuing
- **DataStore Preferences** for settings, drafts, and history
- **Ktor** (OkHttp engine) for HTTP
- **MVVM** architecture, no DI framework

Minimum Android 8.0 (API 26), targeting Android 15 (API 35).

The app is fully open source and distributed as a signed APK via GitHub Releases — no Play Store involved.

---

## Links

- [Features](features) — Detailed feature list
- [Getting Started](setup) — Installation, Resend setup, configuration
- [GitHub Repository](https://github.com/lukulent/MindToss) — Source code
