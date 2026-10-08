# Synthetic demo fixtures

These contain no real victim content or executable malware.

- `synthetic-message.png`: choose Screenshot in the scanner.
- `synthetic-qr.png`: choose QR image. It encodes a reserved `.example` domain.
- `synthetic-invoice.pdf`: a ZIP with an Android manifest marker and a misleading PDF extension. Choose File; it demonstrates signature and disguise checks. It is not a genuine installable APK.

Copy to the phone Downloads folder before presenting. Keep the device offline.

`synthetic-message-hindi.png` and `synthetic-message-gujarati.png` contain authored OTP/password-request fixtures rendered by the Android native-script OCR tests. Both images were read offline by SafeX AI 1.2.0 and produced BLOCK results. They contain no real person’s information. Use the Screenshot picker to demonstrate Hindi/Gujarati OCR without a network connection.
