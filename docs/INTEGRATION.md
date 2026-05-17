# PandaGenieSDK Integration Guide

This guide shows how to integrate the SDK as either a callable app or an AI assistant.

## Add Dependency

Published dependency:

```gradle
dependencies {
    implementation("ai.rorsch.pandagenie:pandagenie-sdk:1.0.36")
}
```

Local workspace dependency:

```gradle
dependencies {
    implementation(project(":pandagenie-sdk"))
}
```

## Provider Integration

Use provider mode when your app wants PandaGenie or another approved assistant to call its features.

### 1. Initialize

```java
PandaGenieSdk.initialize(this, PandaGenieSdk.Role.PROVIDER);
```

### 2. Register Service

```xml
<service
    android:name=".MyCapabilityService"
    android:exported="true">
    <intent-filter>
        <action android:name="ai.rorsch.pandagenie.sdk.action.CAPABILITY_SERVICE" />
    </intent-filter>
</service>
```

### 3. Describe Capabilities

```java
@Override
protected String getManifestJson() {
    return new CapabilityManifestBuilder(this)
        .capability(
            "profile.open",
            "Open profile",
            "Open the profile page and show a message from the assistant.",
            "activity",
            "low",
            new JSONArray(),
            new JSONObject()
                .put("userId", "string")
                .put("message", "string")
        )
        .build()
        .toString();
}
```

### 4. Execute

```java
@Override
protected CapabilityResult onInvoke(
    String capabilityId,
    JSONObject params,
    InvokeContext context
) throws Exception {
    if ("profile.open".equals(capabilityId)) {
        Intent intent = new Intent(this, ProfileActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("userId", params.optString("userId"))
            .putExtra("message", params.optString("message"));
        startActivity(intent);
        return CapabilityResult.ok(new JSONObject().put("opened", true));
    }
    return CapabilityResult.fail("unknown capability: " + capabilityId);
}
```

## Agent Integration

Use agent mode when your app plans tasks and calls other apps.

### 1. Initialize

```java
PandaGenieSdk.initialize(this, PandaGenieSdk.Role.AGENT);
```

### 2. Discover Providers

```java
PandaGenieAgentClient client = new PandaGenieAgentClient(this);
List<DiscoveredProvider> providers = client.discoverProviders();
```

### 3. Load Manifests

```java
for (DiscoveredProvider provider : providers) {
    client.fetchManifest(provider, new ManifestCallback() {
        @Override
        public void onManifest(DiscoveredProvider provider, String manifestJson) {
            // Add manifestJson to your planner/tool registry.
        }

        @Override
        public void onError(DiscoveredProvider provider, String error) {
            // Optional diagnostics.
        }
    });
}
```

### 4. Invoke

```java
client.invoke(
    provider,
    "profile.open",
    new JSONObject().put("userId", "42").put("message", "Hello from PandaGenie"),
    null,
    new InvocationCallback() {
        @Override
        public void onResult(DiscoveredProvider provider, String capabilityId, String resultJson) {
            // Show success or use the JSON result in a later step.
        }

        @Override
        public void onError(DiscoveredProvider provider, String capabilityId, String error) {
            // Show a user-friendly failure.
        }
    }
);
```

## Capability Design Tips

- Keep ids stable. Treat capability ids as public API.
- Use clear user-facing titles and descriptions; the assistant will rely on them.
- Prefer small, composable capabilities instead of one huge "do everything" action.
- Mark risky actions clearly. Deleting data, sending messages, or changing account state should require explicit confirmation in the assistant UI.
- Return structured JSON and include user-readable summary fields where possible.
- Do not expose secrets or private internal ids unless the user explicitly selected them.

## Testing Checklist

- Install a release-signed provider APK.
- Register package name and release SHA-256 signature.
- Approve the app in PandaGenie Admin.
- Refresh trust cache or wait for SDK cache expiry.
- Confirm the provider appears in PandaGenie settings as an installed capability app.
- Invoke every capability once with valid and invalid input.

