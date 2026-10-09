# Hackathon rehearsal

Use synthetic content, not another person's private messages. Install SafeX AI and the separate Demo Sender APK. Keep the phone offline during the presentation.

1. Open SafeX AI. Continue past optional setup. Show the local privacy explanation and the manual scanner.
2. Tap the credential-scam example and Analyze. Explain the request for an OTP / password and the authority / urgency evidence. Show that a blocked result has no open button.
3. Check the safety-advice example. It should not be treated as an OTP theft request. Explain that low risk does not certify safety.
4. Choose the deceptive-link example. Show structural reasons and the on-device URL model status. Do not open any real suspicious link.
5. Select a clear screenshot containing a synthetic scam request, then select a QR image containing the synthetic link. Both reuse the message / link analyzer offline. `app/src/androidTest/assets/synthetic-qr.png` is a reserved-domain fixture.
6. Select a renamed APK / synthetic ZIP containing `AndroidManifest.xml` with a `.pdf` filename. Show the signature mismatch and disguised package evidence.
7. Enable Android notification access and warning permission in SafeX AI Settings. Open Demo Sender and tap Post synthetic scam. Show the actual Android warning, then open its saved evidence. Post safety advice and verify it does not create a scam warning.
8. Show search, warnings filter, full evidence in History and deletion / retention settings. Explain per-app opt-in controls.

9. Enable the floating assistant, leave SafeX AI, and display an authored suspicious message in another app. Use the shield to approve a fresh capture, crop the message, review OCR, and analyze privately. Show that history changes only after Save. Decline a second capture to demonstrate recovery. See [the full floating demo](floating-assistant.md).

For repeatable emulator setup (debug build only):

```sh
adb shell pm grant com.sentinel.ai android.permission.POST_NOTIFICATIONS
adb shell pm grant com.sentinel.demo android.permission.POST_NOTIFICATIONS
adb shell cmd notification allow_listener com.sentinel.ai/com.sentinel.ai.listeners.SentinelNotificationListener
```

Listener permission does not switch protection on by itself: enable notification protection in SafeX AI. Demo Sender is supported only in debug builds, and its results are visibly marked synthetic.

Pitch: “SafeX AI checks suspicious content locally and explains the evidence before you trust it. It works offline and requires no cloud submission. Android permissions control which notifications it can see.”

Disclose the model scope: the text classifier is trained on authored synthetic data; the URL classifier uses a historical UCI benchmark and synthetic robustness variants; neither establishes current-world accuracy. This is a functioning prototype, not a promise of complete detection or antivirus protection.

For a multilingual demo, switch language in Settings and use the OTP/safety sample buttons. The samples load messages in the selected language. Show translated evidence while pointing out that the original message remains intact. Demonstrate larger text, then reset to 100%. The same screenshot pipeline reads all three scripts offline; rehearse with clear native-script fixtures on your presentation device.
