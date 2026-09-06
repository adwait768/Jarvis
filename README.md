# Trevor

A voice-activated daily assistant — chat, "Hey Trevor" hands-free wake word, tasks, and notes. Made by Adwait Suryawanshi.

Runs as a free installable web app (PWA). No app store, no build tools — just static files.

## 1. Put it on GitHub

1. Create a new repository on GitHub, e.g. `trevor-assistant`.
2. Upload these five files to the repo root: `index.html`, `app.js`, `manifest.json`, `sw.js`, `icon.svg`.
3. Commit.

## 2. Turn on GitHub Pages

1. In the repo, go to **Settings → Pages**.
2. Under **Source**, choose **Deploy from a branch**, branch `main`, folder `/ (root)`. Save.
3. Wait a minute, then GitHub shows your live URL — something like:
   `https://yourusername.github.io/trevor-assistant/`

This URL is real HTTPS, which is required for the microphone and wake-word features to work at all.

## 3. Install it on your Android phone

1. Open the `https://...github.io/...` link in **Chrome**.
2. Tap the **⋮** menu → **Add to Home screen** → **Install**.
3. Trevor now opens full-screen from your home screen like a normal app.

## 4. Turn on chat replies (optional)

Voice, tasks, and notes work immediately with no setup. For Trevor to actually reply to what you say or type:

1. Get an API key at [console.anthropic.com](https://console.anthropic.com).
2. In Trevor, go to **Settings → Anthropic API key** and paste it in.
3. The key is saved only in your phone's browser storage — it is never written into these files or uploaded to GitHub.

**Do not** paste your key into `app.js` and commit it — that makes it public to anyone who visits your repo, and they could run up charges on your account.

Usage of the Claude API is paid per request (typically fractions of a cent for short exchanges); check current pricing at [anthropic.com/pricing](https://www.anthropic.com/pricing).

## 5. Using "Hey Trevor"

- Turn on the hands-free toggle in Settings.
- Say your wake phrase (default "hey trevor"), then your request — either together in one sentence, or pause after the wake word and Trevor will prompt "Yes?" and listen for what follows.
- It listens only while the app is open on screen; Android does not allow web pages to listen in the background with the screen off.

## Notes on customizing

- Change the wake phrase any time in Settings — no code edit needed.
- Colors and fonts live in the `<style>` block at the top of `index.html`.
- `sw.js` caches the app shell for offline use; it always skips the cache for actual AI requests so replies stay live.
