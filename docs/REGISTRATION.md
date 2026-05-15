# SDK Registration and Review

Every app that participates in PandaGenieSDK must be registered and reviewed before it is trusted by production builds.

## Where to Register

Open:

```text
https://cf.pandagenie.ai/sdk
```

Use Discord for review questions or urgent review requests:

```text
https://discord.gg/Cfc7pjrjt2
```

## Application Roles

| Role | Use when | Review focus |
|---|---|---|
| Capability app / Provider | Your app exposes features for assistants to call. | Capability descriptions, risk level, exported service, data access, user safety. |
| AI assistant / Agent | Your app discovers and calls other capability apps. | User confirmation flow, capability filtering, audit behavior, abuse prevention. |
| Both | Your app both exposes and calls capabilities. | Both role reviews are required. |

## Required Fields

| Field | Example | Notes |
|---|---|---|
| Developer email | `dev@example.com` | Used for review feedback. |
| Developer name | `PandaGenie Team` | Organization or individual name. |
| App name | `PandaGenie Demo Provider` | User-facing name. |
| Package name | `ai.rorsch.pandagenie.sdkdemo.provider` | Must match the release APK. |
| Role | `provider` or `agent` | One submission per role if needed. |
| Release SHA-256 signature | `2A:5F:...:9C` | Must come from the release signing certificate. |
| Website or project URL | `https://github.com/...` | Optional but recommended. |
| App description | Short paragraph | Explain what the app does and why it needs SDK access. |
| Capability list | JSON array or clear text list | Include ids, descriptions, inputs and risk. |

## Signature Commands

From an APK:

```bash
apksigner verify --print-certs app-release.apk
```

From a keystore:

```bash
keytool -list -v -keystore release.jks -alias your_alias
```

The SHA-256 value can include colons. The registry normalizes it before comparing.

## Capability List Example

```json
[
  {
    "id": "profile.open",
    "title": "Open profile",
    "kind": "activity",
    "riskLevel": "low",
    "description": "Open a profile page and display text passed by the assistant.",
    "inputSchema": {
      "userId": "string",
      "message": "string"
    }
  },
  {
    "id": "notes.create",
    "title": "Create note",
    "kind": "service",
    "riskLevel": "normal",
    "description": "Create a local note from title and body.",
    "inputSchema": {
      "title": "string",
      "body": "string"
    }
  }
]
```

## Review States

| State | Meaning |
|---|---|
| Pending | Submitted and waiting for admin review. SDK trust cache will not include it yet. |
| Approved | Callable by production SDK clients after trust cache refresh. |
| Rejected | Not approved. Update the submission and contact reviewers if needed. |
| Blacklisted | Previously approved identity is blocked due to abuse, security issue, or policy problem. |

## Blacklist Rules

The blacklist is based on package name plus release signature and role. It can block:

- A provider app from being called.
- An agent app from calling providers.
- A single role while leaving another role unaffected.

This keeps remediation precise. For example, an app may remain a provider while its agent role is suspended.

