# PandaGenieSDK

PandaGenieSDK is the Android interoperability SDK for the PandaGenie ecosystem. It lets Android apps expose local capabilities in a discoverable, permission-aware, and auditable way, so PandaGenie or another approved AI assistant can call those capabilities from a natural-language task.

**Languages:** English | [Simplified Chinese](README_CN.md)

The SDK is distributed as an Android AAR. It connects two kinds of apps:

- **AI Assistant / Agent**: an app with planning ability, such as PandaGenie, that discovers other apps and invokes their exported capabilities.
- **Capability Provider**: a normal Android app that exposes safe operations, data views, or UI entry points for approved assistants to call.

## Ecosystem

| Project | Purpose |
|---|---|
| [PandaGenie](https://cf.pandagenie.ai) | Official website, APK download, developer entry points, SDK registration and module submission. |
| [PandaGenieSource](https://github.com/Rorschach123/PandaGenieSource) | Android app source, official modules, module packaging, and module developer docs. |
| [PandaGenieSDK](https://github.com/Rorschach123/PandaGenieSDK) | Android AAR for app-to-app capability discovery, trust verification, and invocation. |
| [PandaGenie Module Template](https://github.com/Rorschach123/PandaGenie-Module-Template) | Starter project for building hot-loadable PandaGenie modules. |
| [SDK registration](https://cf.pandagenie.ai/sdk) | Submit an app package name, release signing certificate, role, and capability description for review. |
| [Task / Module market](https://cf.pandagenie.ai/marketplace) | Public tasks and modules that PandaGenie can use. |
| [Discord](https://discord.gg/Cfc7pjrjt2) | Developer support, SDK review acceleration, and module publishing help. |

Use the module template when you want to extend PandaGenie itself. Use PandaGenieSDK when you want an independent Android app to become callable by PandaGenie, or when you want to build another AI assistant that calls approved apps.

## Repository Layout

```text
PandaGenieSDK/
  pandagenie-sdk/                Unified AAR facade exported to app developers
  pandagenie-sdk-core/           Shared roles, trust cache, signature and registry helpers
  pandagenie-sdk-agent/          Discovery and invocation client for AI assistant apps
  pandagenie-sdk-provider/       Base service and manifest builder for capability apps
  examples/provider-demo/        Runnable demo app with Service, Activity, Provider and Broadcast examples
  docs/                          Architecture, integration, registration and template guides
  templates/provider-app/        Lightweight guide for creating a standalone provider template repo
```

The public dependency should be the unified AAR:

```gradle
implementation("ai.rorsch.pandagenie:pandagenie-sdk:0.1.0-preview")
```

The internal modules remain split only to keep implementation responsibilities clear.

## Capability Model

An app initializes the SDK with one or both roles:

```java
PandaGenieSdk.initialize(
    context,
    PandaGenieSdk.Options.builder()
        .roles(PandaGenieSdk.Role.PROVIDER)
        .build()
);
```

| Role | Meaning |
|---|---|
| `PROVIDER` | Exposes capabilities that an approved AI assistant can discover and invoke. |
| `AGENT` | Discovers approved providers, reads their capability manifests, and invokes them. |
| `AGENT + PROVIDER` | Useful for apps that both expose their own abilities and call other apps. Each role is reviewed separately. |

The server validates apps by `packageName + release SHA-256 signature + role`. A package can be approved as a provider but not as an agent, or vice versa.

## Provider Quick Start

1. Add the SDK dependency.

```gradle
dependencies {
    implementation("ai.rorsch.pandagenie:pandagenie-sdk:0.1.0-preview")
}
```

2. Initialize the SDK in your `Application`.

```java
public final class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PandaGenieSdk.initialize(this, PandaGenieSdk.Role.PROVIDER);
    }
}
```

3. Register an exported capability service.

```xml
<service
    android:name=".MyCapabilityService"
    android:exported="true">
    <intent-filter>
        <action android:name="ai.rorsch.pandagenie.sdk.action.CAPABILITY_SERVICE" />
    </intent-filter>
</service>
```

4. Declare capabilities and handle invocations.

```java
public final class MyCapabilityService extends PandaGenieCapabilityService {
    @Override
    protected String getManifestJson() {
        return new CapabilityManifestBuilder(this)
            .capability(
                "notes.create",
                "Create note",
                "Create a note from title and body",
                "service",
                "normal",
                new JSONArray().put("storage.write"),
                new JSONObject()
                    .put("title", "string")
                    .put("body", "string")
            )
            .build()
            .toString();
    }

    @Override
    protected CapabilityResult onInvoke(
        String capabilityId,
        JSONObject params,
        InvokeContext context
    ) throws Exception {
        if ("notes.create".equals(capabilityId)) {
            String title = params.optString("title");
            String body = params.optString("body");
            // Save the note in your app.
            return CapabilityResult.ok(new JSONObject().put("created", true));
        }
        return CapabilityResult.fail("unknown capability: " + capabilityId);
    }
}
```

## Agent Quick Start

Agent apps discover provider services, fetch manifests, pass the capability knowledge to their LLM planner, then call the selected capability.

```java
PandaGenieSdk.initialize(context, PandaGenieSdk.Role.AGENT);

PandaGenieAgentClient client = new PandaGenieAgentClient(context);
List<DiscoveredProvider> providers = client.discoverProviders();

for (DiscoveredProvider provider : providers) {
    client.fetchManifest(provider, new ManifestCallback() {
        @Override
        public void onManifest(DiscoveredProvider provider, String manifestJson) {
            // Add manifestJson to the assistant's tool/capability knowledge base.
        }

        @Override
        public void onError(DiscoveredProvider provider, String error) {
            // Ignore unavailable providers or show diagnostics in developer mode.
        }
    });
}
```

Invoke after the assistant has produced a capability call:

```java
client.invoke(
    provider,
    "notes.create",
    new JSONObject().put("title", "Todo").put("body", "Buy milk"),
    null,
    new InvocationCallback() {
        @Override
        public void onResult(DiscoveredProvider provider, String capabilityId, String resultJson) {
            // Render resultJson to the user.
        }

        @Override
        public void onError(DiscoveredProvider provider, String capabilityId, String error) {
            // Explain the failure in user-friendly language.
        }
    }
);
```

## Trust, Review and Blacklist

SDK calls are not purely local trust-by-intent calls. PandaGenie keeps a server-side registry and publishes a compact trust cache:

```text
GET https://cf.pandagenie.ai/sdk/trust-cache?role=agent,provider
```

The cache includes approved and non-blacklisted identities. The identity is derived from:

```text
sha256(packageName|normalizedReleaseSignatureSha256|role)
```

Flow:

1. A developer submits an app on [SDK registration](https://cf.pandagenie.ai/sdk).
2. Admin reviews package name, release signing certificate, app role, app description and capability list.
3. Approved apps are published in the trust cache.
4. Blacklisted package/signature/role identities are removed from the callable set.
5. SDK refreshes the cache periodically, so server-side decisions propagate without app updates.

Provider services reject untrusted agents by default. Agent clients hide untrusted providers by default.

## Demo App and Template

`examples/provider-demo` is intentionally outside the main PandaGenie app project. It is both a runnable demo and the recommended base for a GitHub template repository. The Gradle module name remains `:pandagenie-sdk-demo-provider`.

It demonstrates:

- Service invocation with JSON parameters and JSON result.
- Opening an Activity and displaying passed text.
- Reading rows from a ContentProvider.
- Sending and receiving Broadcast events.
- Local state read/write.
- Long-running task simulation.

For template guidance, see [templates/provider-app/README.md](templates/provider-app/README.md). The suggested external template repository is:

```text
https://github.com/Rorschach123/PandaGenieSDK-Provider-Template
```

If that repository is not created yet, copy the demo provider module into a new repository and keep only the provider app plus the minimal Gradle wrapper.

## Build

This repository can reuse the Gradle wrapper from the main app checkout:

```powershell
cd E:\ProjectAI\PandaGenie\PandaGenieSDK
..\PandaGenie\gradlew.bat -p . :pandagenie-sdk:assembleRelease :pandagenie-sdk-demo-provider:assembleRelease --no-daemon
```

Useful outputs:

```text
pandagenie-sdk/build/outputs/aar/pandagenie-sdk-release.aar
examples/provider-demo/build/outputs/apk/release/pandagenie-sdk-demo-provider-release.apk
```

## Release Signing

Register and test with a release build, not a debug build. Get the SHA-256 certificate fingerprint with one of these commands:

```bash
# From APK
apksigner verify --print-certs app-release.apk

# From keystore
keytool -list -v -keystore release.jks -alias your_alias
```

Expected format:

```text
2A:5F:...:9C
```

The registration form normalizes separators and letter case, but the final value must be a 64-character SHA-256 fingerprint.

## More Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Integration Guide](docs/INTEGRATION.md)
- [Registration and Review](docs/REGISTRATION.md)
- [Demo and Template Guide](docs/DEMO_AND_TEMPLATE.md)

