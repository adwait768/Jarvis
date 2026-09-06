
# JARVIS v1 — phone build

1. Extract this ZIP.
2. Open the extracted `JARVIS_v1_fixed` folder in AndroidIDE.
3. Wait for project sync to finish.
4. Tap Build/Run.
5. If AndroidIDE asks to install missing SDK/build tools, allow it.
6. If you use the terminal, run:
   `./gradlew assembleDebug`

The project includes the Gradle wrapper now, so the earlier
"./gradlew: No such file or directory" error should be resolved.

AndroidIDE may automatically supply its Android aapt2 override when
building inside the IDE.
