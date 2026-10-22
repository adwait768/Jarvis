# 768 AI — by 768 Community

A minimal, premium personal assistant prototype with voice input, local commands, and a responsive mobile-first interface.

## Install / run

This is a static web app. Publish the repository with GitHub Pages over HTTPS, then open the published URL in Chrome on Android and choose **Install app** or **Add to Home screen**. HTTPS is required for browser microphone features.

## Prototype features

- Minimal dark interface with electric-blue accents
- 768 AI branding, custom app icon, and launch screen
- Local time, date, status, calculations, YouTube, and Google search shortcuts
- PWA manifest and service-worker caching
- The repository also includes a companion `app.js` implementation with tasks, notes, chat history, voice output, hands-free wake-word, and optional Anthropic API integration where connected to its matching UI

## Privacy and storage

The app's supported tasks, notes, settings, chat history, and configured API key are stored in browser local storage on that device. Do not share an API key or commit it to the repository. Data saved in one browser/device does not automatically sync to another.

## Branding

**768 AI**  
*by 768 Community*

The rebrand is intended to preserve the prototype's existing functions. Test voice recognition on the target browser and device after deployment.
