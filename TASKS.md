# Tasks

v1 progress for **Skerry**. Product: [DESIGN.md](DESIGN.md). Home: [README.md](README.md). Privacy: [PRIVACY.md](PRIVACY.md).

**2 of 12 done. Next: Task 3.**

One task per working session so a day’s token budget stays bounded. After a task’s “Done when” is true, mark it here, refresh the README progress table, commit, and push.

## Board

| # | Task | Status | Notes |
|---|---|---|---|
| 1 | Gradle scaffold | Done | `assembleDebug`. No `INTERNET`. Empty Settings. |
| 2 | Cutout overlay | Done | Static pill at `DisplayCutout`. Overlay permission deep link. Pixel 9 Pro check: window at (427, 42), 426×120, on the punch hole. |
| 3 | Foreground service | Remaining | “Skerry is on”; overlay dies with the service. |
| 4 | Allowlist store | Remaining | DataStore of package names + unit tests. |
| 5 | Allowlist UI | Remaining | Installed-apps picker. Empty list = nothing shown. |
| 6 | Notification watcher | Remaining | Listener; drop non-allowlisted packages in process. |
| 7 | Stack + cycle | Remaining | Cap ~5. Left icon cycles. Center expands. |
| 8 | Media + right play/pause | Remaining | Any allowlisted `MediaSession`. Right control stays media. |
| 9 | Actions + inline reply | Remaining | Content intent; `RemoteInput` when present. |
| 10 | Charging toggle | Remaining | Battery broadcasts; joins stack only if enabled. |
| 11 | Survival Settings | Remaining | Battery-unrestricted CTA; hide when off; revoked grants. |
| 12 | Device pass | Remaining | Emulator fake cutout **and** a punch-hole phone. |

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

_(Task 12 fills this in.)_

## Session prompt

> Do only Task N in TASKS.md. Follow DESIGN.md. Stop when that task’s Done when is true. Do not start the next task. No subagents.
