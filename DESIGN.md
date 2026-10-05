# Skerry — design

A **skerry** is a small rocky island. The app is a small pill at the camera cutout that stacks live activities the user has allowed.

Package: `io.github.mohuddle.skerry`

Inspired by Dynamic Notch Notification Bar, dynamicSpot, and Eywin Dynamic Island. Rebuilt **without Accessibility** and **without trackers**. Notes that drove the constraint: `~/Downloads/android-dynamic-island-accessibility.md`.

This is a new app. It is not part of Liturgy of the Hours.

## Product

The pill watches **persistent activities** (allowlisted notifications + MediaSessions + optional charging) and shows them at the camera cutout.

- **Allowlist:** nothing from an app appears unless that package is on in Settings.
- **Stack:** several live items at once (cap about 5). Left icon tap **cycles**. Center tap **expands** the current item.
- **Media chrome:** if *any* allowlisted player has a session, the **right side is always play/pause** for that session, even when the current item is a chat.
- **Inline reply:** only when the current notification already has `RemoteInput` and its app is allowlisted. Skerry does not replace Messages bubbles; leave Messages off the list if bubbles are enough.
- **No Accessibility.** Overlay + notification listener + foreground service. No `INTERNET` in v1. No ads, analytics, or crash-upload SDKs.
- Play Store is a later milestone, not v1.

Media does **not** need a Spotify plugin. Any app that publishes a `MediaSession` (Spotify, YouTube, local files, Aftercast, …) is visible once that package is allowlisted.

## Architecture

Kotlin, Jetpack Compose, minSdk 29, targetSdk current.

```
app/
  overlay/     WindowManager TYPE_APPLICATION_OVERLAY at DisplayCutout
  watcher/     NotificationListenerService, MediaSessionManager, battery/headset
  stack/       Live activity list (cap ~5), current index, cycle
  allowlist/   DataStore of package names
  service/     Foreground service hosting the overlay
  ui/          Onboarding, Settings, allowlist picker
```

**Hard no in the manifest:** `BIND_ACCESSIBILITY_SERVICE`, `INTERNET` (v1), ad/analytics libraries.

**Permissions (user-granted, explained in onboarding):**

- `SYSTEM_ALERT_WINDOW` — draw the pill
- `BIND_NOTIFICATION_LISTENER_SERVICE` — read notifications and attach `MediaSessionManager`
- Foreground service (`specialUse` for the host; `mediaPlayback` while a session is active)
- `POST_NOTIFICATIONS` for the quiet “Skerry is on” FGS notice
- Optional: request ignore-battery-optimizations from Settings (not forced)

**Dropped vs Play clones:** cutout-zone swipe shortcuts, `performGlobalAction` (screenshot/recents), window-content scraping, usage-stats foreground spy, weather/location, answering calls, Bluetooth-earbud battery APIs, ads.

OEM killers (Samsung/Xiaomi/Oppo) may still freeze a non-accessibility overlay. Mitigation: FGS + battery-unrestricted CTA. That is the accepted gap.

## Pill UX

Collapsed: `[ app/activity icon | compact title | play/pause if any media in stack ]`

- Left icon: cycle stack (newest-first, wrap).
- Center: expand current.
- Right: `MediaController` play/pause for the active allowlisted session, independent of which item is current.

Expanded: current item’s title, text, notification actions, optional `RemoteInput`. Tap body → content intent. Second center tap or swipe-up collapses.

Charging capsule is a **system toggle**, not an allowlisted app. When on, it joins the stack while charging.

Empty allowlist + charging off = **idle-hidden** in v1 (do not cover the camera for no reason).

## Privacy

See [PRIVACY.md](PRIVACY.md). Notification title/text and media metadata stay in memory for the live stack.

## Play (later, not v1)

- Sideload first; prove the island on a **punch-hole** phone (not the LG G5).
- This privacy policy page.
- Declare overlay and notification-listener use in Play Console.
- `foregroundServiceType` `specialUse` declaration.
- Do not add Accessibility later to “fix” OEM kill.

## Non-goals

Accessibility. Trackers. Screen scraping. Swipe-the-cutout global gestures. Merging this into the Hours app. Using the G5 as the layout device.
