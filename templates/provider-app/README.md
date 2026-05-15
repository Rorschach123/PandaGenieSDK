# PandaGenieSDK Provider Template

This folder documents the recommended standalone template project for SDK provider apps.

The current runnable source lives in:

```text
PandaGenieSDK/examples/provider-demo
```

To create the GitHub template project:

1. Create a new repository, for example `PandaGenieSDK-Provider-Template`.
2. Copy the demo provider module into that repository as `app/`.
3. Keep only the minimal Gradle project files needed to build the app.
4. Replace the demo package name, app name and capabilities.
5. Depend on the published SDK artifact instead of local project modules.
6. Add a README with registration steps and capability examples.

Recommended dependency:

```gradle
implementation("ai.rorsch.pandagenie:pandagenie-sdk:0.1.0-preview")
```

Do not commit:

- Release keystores.
- Signing passwords.
- Local `signing.properties`.
- Generated APK/AAB/AAR outputs.

Publish this repository as a GitHub Template so developers can click **Use this template** and start from a clean provider app.
