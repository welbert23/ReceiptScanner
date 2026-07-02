# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in this app, please report it by opening an issue at:

https://github.com/welbert23/ReceiptScanner/issues

Please include the following details in your report:

- Type of vulnerability
- Steps to reproduce
- Affected versions
- Any potential impact

We will acknowledge receipt within 48 hours and provide an estimated timeline for a fix.

## Data Privacy

- All receipt data is stored **locally on device** using Room database
- No data is sent to external servers unless Gemini AI mode is enabled
- When Gemini AI mode is active, receipt images are sent to Google's servers for processing
- No analytics or tracking SDKs are included in this app

## Permissions

- **No camera permission required** — the ML Kit Document Scanner handles its own system document scanner UI
- Storage access is limited to the app's own file directories

## Best Practices

- Keep your Gemini API key confidential
- Do not share the APK from untrusted sources
- Always download the app from the official GitHub releases
