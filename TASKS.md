# Tasks

v1 progress for **Skerry**. Product: [DESIGN.md](DESIGN.md). Home: [README.md](README.md). Privacy: [PRIVACY.md](PRIVACY.md).

**12 of 12 done.** The island redesign below is separate and not done.

One task per working session so a day’s token budget stays bounded. After a task’s “Done when” is true, mark it here, refresh the README progress table, commit, and push.

## Island redesign — pick up here

Stopped 2026-10-08. This pickup list is on `main`. The redesign code is still only in the working tree, on top of `5bdd466` (“Finish the v1 island through the device pass.”). Commit and push that code only after the device checks below are honest. Commit as `mohuddle` / `18602380+mohuddle@users.noreply.github.com`. Do not change git config. Do not use the Mobitecture bot identity.

v1 tasks 1–12 stay Done. Rows 7–9 describe the compact pill those tasks shipped. This redesign replaces that UX: hole-height capsule, side icons, and the gestures in [DESIGN.md](DESIGN.md). Do not bring back a title row on the collapsed capsule, a visible chin under it, or Accessibility.

### Already in the working tree

- `IslandLayout.kt`: `layoutIsland`, `capsuleWidth`, `classifyGesture`. Collapsed capsule is 32dp, centered on the hole. Expanded card starts at the status-bar bottom. `TOUCH_LIP_DP = 32` extends the window under the status bar as a clear strip.
- `IslandSides.kt`, `DismissedKeys.kt`. Dismiss is in-memory and filtered in `reconcile`, so the next notification pass does not put a dismissed item back. `refreshPill()` starts with `dismissed.retain(presentKeys())`.
- `ActivityStack.expand()` and `focus(key)`. `cycle()` remains for the old tests. The UI does not cycle.
- `PillChrome.kt` and `SkerryOverlay.kt` draw the capsule, an expanded card, and a 10dp neck. The overlay does **not** call `shiftContentBelowStatusBar`.
- Gestures: tap opens the content intent (charging is a no-op; media-only opens that app’s launcher); long-press (~380ms) expands; swipe up collapses; swipe down restores the newest dismissed key or expands; swipe sideways dismisses.
- `DESIGN.md`, `README.md`, and the device-pass note in this file match that UX.
- Last full `:app:testDebugUnitTest :app:assembleDebug --offline` passed **before** the slop edit. The installed APK is that build.

### Proven on the Pixel with that APK (view slop still 24dp)

- Charging-only collapsed frame `[536,49][745,310]`. Two icons (app left, bolt right) `[506,49][775,310]`.
- Long-press on the clear strip expanded. Swipe up on the opaque card collapsed. Swipe sideways dismissed. A tap on the opaque card body at (280, 340) opened `MainActivity` (ActivityTaskManager result code 2, uid 10433).
- Screenshots: `/tmp/skerry-refs/final-collapsed-top.png`, `/tmp/skerry-refs/icons-top.png`, `/tmp/skerry-refs/final-expanded-top.png`.

### Not proven

- Presses on the visible capsule (y≈102) belong to the status bar. A long-press at (640, 102) opened the shade.
- A tap at (640, 260) on the clear strip opened Messages. Long-press at that point did expand. Treat clear-strip taps as unreliable.
- Swipe down restore failed twice. Travel in the ~106px strip did not clear the ~80px slop, so the frame stayed charging-only `[536,49][745,310]`.
- Media play/pause on the expanded card, and inline reply, were not rechecked in this pass.

### Phone when this stopped

Pixel 9 Pro `47101FDAP004DK`, product `caiman`, 1280×2856, cutout `Rect(586, 0 - 695, 204)`. Status bar owns `[0,0][1280,204]` (`TRUSTED_OVERLAY`, `BLOCK_UNTRUSTED`). Airplane mode is 0. `dumpsys battery reset` was run. Live battery: AC powered, USB powered false, status 4, level 100, and updates are not stopped.

Skerry’s process was still up, `SkerryService` was foreground, and the listener was bound, but the overlay window was gone. Charging toggle is **on** (it was off before this work). Skerry is on the allowlist (it was not). Debug notification id 7 may still be posted; it had been dismissed in memory. Focus was Messages. Do not `pm clear`. Do not reboot. Do not force the battery exemption. Do not allowlist `com.android.shell`.

### Remaining

- [ ] **1. Rebuild the slop change and reinstall.** `PillChrome.kt` has `private val slop = dp(12)` (was `dp(24)`). It is not compiled or installed. Classifier tests pass slop in, so they should still pass.

  `JAVA_HOME=/home/anon/.local/jdk-21 ANDROID_SDK_ROOT=/home/anon/android-sdk ./gradlew :app:testDebugUnitTest :app:assembleDebug --offline`

  `adb -s 47101FDAP004DK install -r app/build/outputs/apk/debug/app-debug.apk`

  `install -r` stops the process, drops Skerry-posted notifications, and clears in-memory dismiss keys. The allowlist and charging toggle survive. If the installer shows `PackageUpdateActivity`, press Home, open Skerry, and turn Island on. The Island switch’s clickable parent was near `[1027,590][1200,750]`; dump the hierarchy again before tapping. Do not start the service with `am start-foreground-service`.

- [ ] **2. Prove swipe-down restore.** Post a debug notification with single-word extras: `adb -s 47101FDAP004DK shell am broadcast -a io.github.mohuddle.skerry.DEBUG_POST -n io.github.mohuddle.skerry/.debug.DebugPoster --es title Alpha --es text Bodyone --ei id 7`. Go Home. Expect a two-icon frame near `[506,49][775,310]`. Swipe sideways on the strip under the status bar, both points below y=204, about (540, 260)→(740, 260). The frame should shrink to the charging-only width. Then swipe down in that strip, travel well past ~40px, about (640, 220)→(640, 300). The frame should widen back to two icons. Count the overlay with `dumpsys input` and the `skerry,` token (a bare `skerry` match also hits `MainActivity`).

- [ ] **3. If that swipe still misses, stop changing the shape.** Do not draw a chin, an egg, a dumbbell, or a mushroom. Do not add Accessibility, `TYPE_ACCESSIBILITY_OVERLAY`, or `setTrustedOverlay`. Keep the clear strip. Write down that swipe up and swipe down are reliable on the opaque card, and that restore from the collapsed strip did not land. DESIGN.md already says the status bar takes the camera row.

- [ ] **4. Check play/pause on the expanded card.** With Skerry allowlisted, `adb -s 47101FDAP004DK shell am broadcast -a io.github.mohuddle.skerry.DEBUG_POST -n io.github.mohuddle.skerry/.debug.DebugPoster --es what media` toggles the debug session. Long-press the strip to expand. Tap the Play/Pause chip and confirm the session changes and the gesture listener does not take the tap. While that session is active the foreground service types include `mediaPlayback` (`0x40000002`).

- [ ] **5. Put the phone back.** Turn Show charging off. Remove Skerry from the allowlist if the list should be empty again. `install -r` clears Skerry’s own notifications; cancel id 7 if a later post is still up. Leave airplane mode off. Do not `pm clear`. Leave Home only if this session opened the other app.

- [ ] **6. Update GitHub.** One commit on `main`, then push to `origin`. The message names the hole-height capsule, the side icons, the five gestures, and the status-bar limit: presses on the visible capsule still go to the status bar. Record the device result in this file first. Do not claim the Play-store gesture spec is fully met.

- [ ] **7. Confirm `topics/skerry.md` matches what shipped.** The v1 note that `shiftContentBelowStatusBar` pads the controls is history. This overlay does not call it.

## Board

| # | Task | Status | Notes |
|---|---|---|---|
| 1 | Gradle scaffold | Done | `assembleDebug`. No `INTERNET`. Empty Settings. |
| 2 | Cutout overlay | Done | Static pill at `DisplayCutout`. Overlay permission deep link. Pixel 9 Pro check: window at (427, 42), 426×120, on the punch hole. |
| 3 | Foreground service | Done | Quiet “Skerry is on”. Pixel 9 Pro: Island off and swiping the notice both remove the pill and the service. |
| 4 | Allowlist store | Done | DataStore of package names. Unit tests cover add, remove, and a new store reading the same file. |
| 5 | Allowlist UI | Done | Launchable apps, search, toggles. Pixel 9 Pro: Aftercast stayed on across force-stop. |
| 6 | Notification watcher | Done | Listener offers allowlisted notifications only. Pixel and emulator: an allowlisted debug notification appeared; a shell notification did not. |
| 7 | Stack + cycle | Done | Cap 5, newest first. Left tap cycles the compact title. Center tap expands and collapses. |
| 8 | Media + right play/pause | Done | Right control pauses and resumes an allowlisted session while a chat item is current. |
| 9 | Actions + inline reply | Done | Body tap opens the content intent. Reply field only when `RemoteInput` is present. The reply text is not stored. |
| 10 | Charging toggle | Done | Default off. Joins the stack when enabled and plugged in. Leaves when the toggle is off or the device is unplugged. |
| 11 | Survival Settings | Done | Overlay revoke hides the pill and Settings explains why. Battery button opens the system page. Island off removes the pill and the service. |
| 12 | Device pass | Done | Pixel 9 Pro and fake-cutout emulator. See the log below. |

### Later (not v1)

- [ ] Themes / pill color
- [ ] Idle camera-matching notch
- [ ] Stack-count badge
- [ ] Weather, calls, Bluetooth battery
- [ ] Play Store listing

---

## Task 1: Gradle scaffold

**Files:** Android app module, `settings.gradle.kts`, `app/build.gradle.kts`, `AndroidManifest.xml` (no `INTERNET`, no accessibility), empty Settings `MainActivity`, `LICENSE` already here.

**Produces:** package `io.github.mohuddle.skerry`, minSdk 29, Compose + Material 3, titled “Skerry”.

**Done when:** `./gradlew assembleDebug` succeeds. Manifest has neither `INTERNET` nor `BIND_ACCESSIBILITY_SERVICE`.

**Stop.** Do not draw an overlay.

---

## Task 2: Cutout overlay

**Files:** overlay window helper, overlay-permission onboarding in Settings.

**Produces:** a static pill positioned with `DisplayCutout`. `TYPE_APPLICATION_OVERLAY`, `FLAG_NOT_FOCUSABLE` + `FLAG_NOT_TOUCH_MODAL`. Visible over the launcher once overlay is granted.

**Done when:** on an emulator with a fake cutout (or a punch-hole phone), the pill sits at the camera hole. Settings explains and deep-links to overlay permission.

**Stop.** Do not start a foreground service yet.

---

## Task 3: Foreground service

**Files:** `SkerryService`, start/stop from Settings, FGS notification channel.

**Produces:** quiet “Skerry is on” notification. Overlay is created in `onStart` and removed in `onDestroy`. `foregroundServiceType` `specialUse` (add `mediaPlayback` later in Task 8 if required).

**Done when:** toggling Island off in Settings removes the pill and the FGS notice. Swiping the FGS notice stops the service and the pill.

**Stop.** Do not parse notifications.

---

## Task 4: Allowlist store

**Files:** `allowlist/` DataStore wrapper. JVM unit tests.

**Produces:** add/remove/contains/list of package names. Default empty.

**Done when:** unit tests cover persist across process (in-memory fake or DataStore test). No UI required.

**Stop.** Do not build the picker yet.

---

## Task 5: Allowlist UI

**Files:** Settings screen listing launchable apps with search and toggles.

**Produces:** user can allow/deny packages. Empty allowlist means the watcher will show nothing (Task 6).

**Done when:** toggling an app, killing the process, and reopening Settings still shows that app on.

**Stop.** Do not attach the notification listener yet.

---

## Task 6: Notification watcher

**Files:** `NotificationListenerService`, onboarding to the system notification-access screen.

**Produces:** posted/updated/removed callbacks. Packages not on the allowlist are dropped in process; payload is not stored.

**Done when:** with one app allowed, that app’s notification is offered to the stack API (even if the pill still ignores it). A second app not on the list never appears. Airplane mode still works (no network needed).

**Stop.** Do not implement cycle/expand yet; a single current item in the pill is enough.

---

## Task 7: Stack + cycle

**Files:** `stack/` model + overlay hit targets.

**Produces:** cap ~5, newest first. Left icon cycles (wrap). Center tap expands/collapses current.

**Done when:** two allowlisted notifications: left tap switches the compact title; center tap expands the current one.

**Stop.** Do not wire MediaSession yet.

---

## Task 8: Media session + right play/pause

**Files:** `MediaSessionManager` bound with the listener component.

**Produces:** any allowlisted app’s session. Right control is play/pause for that session even when the current stack item is a chat.

**Done when:** play a local or streaming player that is allowlisted; pause from the pill while a chat item is current; play resumes in that player.

**Stop.** Do not implement RemoteInput yet.

---

## Task 9: Actions + inline reply

**Files:** expanded actions row; `RemoteInput` send.

**Produces:** body tap → content `PendingIntent`. Reply field only if the notification has `RemoteInput` and the package is allowlisted.

**Done when:** tap opens the right conversation. Reply on a supporting notification (e.g. Messages) sends without opening the app. An allowlisted app without `RemoteInput` shows no reply field.

**Stop.** Do not add charging yet.

---

## Task 10: Charging toggle

**Files:** battery/power receiver; Settings toggle default off.

**Produces:** when enabled and charging, a charging item joins the stack. Not an allowlisted app.

**Done when:** plug in with toggle on → charging appears. Toggle off → it leaves the stack. Unplug → it leaves.

**Stop.** Do not do OEM battery screens yet.

---

## Task 11: Survival Settings

**Files:** Settings rows for overlay, notification access, battery unrestricted; handle revoked grants.

**Produces:** CTAs to the system pages. If overlay or listener is revoked, hide the pill and explain why. Island-off hides the pill.

**Done when:** revoking overlay permission hides the pill without a crash. Battery CTA opens the system page (grant is the user’s choice).

**Stop.** Device pass is Task 12.

---

## Task 12: Device pass

**Files:** none except bugfixes. Manual checklist.

**Done when:** emulator with a fake cutout **and** a punch-hole phone (not the G5 as layout device):

1. Allow two apps; both notifications cycle on the left icon.
2. Center tap expands; tap body opens the app.
3. Allowlisted player: right play/pause works while a chat item is current.
4. Inline reply works on a supporting notification.
5. Charging toggle on/off.
6. Airplane mode the whole time after grants.
7. Stop/start Island from Settings; reboot then start again.

Record device, Android version, pass/fail at the bottom of this file.

---

## Device pass log

Checked 2026-10-08. Airplane mode stayed on after the overlay and notification-access grants, including across both reboots. Test notifications were local debug posts and a Clock alarm labeled for the pass. The allowlist was cleared again afterward.

| Device | Android | Result |
|---|---|---|
| Pixel 9 Pro (`caiman`, serial `47101FDAP004DK`), punch hole | 17 (SDK 37) | Pass |
| Emulator `skerry_cutout` (`emulator-5556`), punch cutout `Rect(492, 0 - 610, 128)` | 15 (SDK 35) | Pass |

1. Skerry and Clock were allowlisted. Left taps cycled Clock’s “Upcoming alarm” with the Skerry items. A shell notification that was not allowlisted never replaced them.
2. Center tap expanded the current item. Tapping the body delivered the content intent on both devices (`LAUNCH_SINGLE_TOP`, result code 3).
3. The right control paused, then resumed, an allowlisted session while the chat item stayed current. The foreground service reported `specialUse|mediaPlayback` while that session was active.
4. A notification with `RemoteInput` showed the reply field and Send. After Send, that notification became “Reply received” / “Sent from the pill”. The reply text was not in the notification record or the app log. A notification without `RemoteInput` showed no reply field.
5. With the toggle on and power connected, cycling reached “Charging”. Turning the toggle off removed it. Unplug, using the plug extra, removed it as well.
6. Airplane mode stayed on for the whole pass.
7. Island off removed the pill and the foreground service. Island on brought them back. After reboot, Island stayed off until it was started again, and the pill came back on both devices.

The island was redesigned after this pass: a hole-height capsule with side icons, and the gestures in DESIGN.md. The expanded card still starts at the bottom of the status bar, because that band belongs to the status bar.

## Session prompt

> Do only Task N in TASKS.md. Follow DESIGN.md. Stop when that task’s Done when is true. Do not start the next task. No subagents.
