# Demo Provider and Template Guide

`examples/provider-demo` is the reference provider app. It is intentionally separate from the PandaGenie main app so developers can copy, run and modify it without understanding the full assistant codebase. In Gradle it is still named `:pandagenie-sdk-demo-provider`.

## What the Demo Covers

| Capability type | Demo purpose |
|---|---|
| Service | Echo parameters and return structured JSON. |
| Activity | Open a screen and display text passed by the assistant. |
| ContentProvider | Query sample rows owned by the demo app. |
| Broadcast | Send a local/system-style event and read the latest status. |
| Local state | Save and read a note in app storage. |
| Long task | Simulate a slower action and return progress-like result data. |

## Build the Demo

```powershell
cd E:\ProjectAI\PandaGenie\PandaGenieSDK
..\PandaGenie\gradlew.bat -p . :pandagenie-sdk-demo-provider:assembleRelease --no-daemon
```

Output:

```text
examples/provider-demo/build/outputs/apk/release/pandagenie-sdk-demo-provider-release.apk
```

## Turn the Demo into a GitHub Template

Recommended template repository name:

```text
PandaGenieSDK-Provider-Template
```

Suggested template contents:

```text
PandaGenieSDK-Provider-Template/
  app/
    src/main/AndroidManifest.xml
    src/main/java/.../DemoCapabilityService.java
    src/main/java/.../MainActivity.java
    src/main/java/.../DemoContentProvider.java
    src/main/java/.../DemoBroadcastReceiver.java
  README.md
  build.gradle
  settings.gradle
  gradle.properties
  .gitignore
```

Keep the template small:

- Include one provider app only.
- Depend on the published `ai.rorsch.pandagenie:pandagenie-sdk` artifact.
- Do not include PandaGenie app source or server source.
- Do not commit keystores or signing passwords.
- Include a sample capability manifest and registration checklist.

## Template README Checklist

The template README should answer:

- What does this demo app expose?
- How do I change the package name?
- How do I add a capability?
- How do I get the release SHA-256 signature?
- How do I submit the app for review?
- How do I test from PandaGenie after approval?

## Demo Registration

Register the demo as a provider:

```text
Package: ai.rorsch.pandagenie.sdkdemo.provider
Role: provider
App name: PandaGenie SDK Demo Provider
Description: Demonstrates Service, Activity, ContentProvider, Broadcast and local-state capabilities for SDK integration testing.
```

If the release signing certificate changes, submit the new SHA-256 fingerprint.
