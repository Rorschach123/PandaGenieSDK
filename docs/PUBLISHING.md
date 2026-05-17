# PandaGenieSDK Public Library Publishing

Current SDK version: `1.0.36`

This release is published as GitHub source, GitHub Release assets, and a JitPack-ready tag. It has not been published to Maven Central unless the maintainer runs the Central Portal steps below with a Sonatype account, signing key, and portal token.

## What Is Already Ready

- Gradle Maven coordinates:
  - `ai.rorsch.pandagenie:pandagenie-sdk:1.0.36`
  - `ai.rorsch.pandagenie:pandagenie-sdk-core:1.0.36`
  - `ai.rorsch.pandagenie:pandagenie-sdk-agent:1.0.36`
  - `ai.rorsch.pandagenie:pandagenie-sdk-provider:1.0.36`
- GitHub repository: `https://github.com/Rorschach123/PandaGenieSDK`
- Recommended release tag: `1.0.36`
- Demo APK: `examples/provider-demo/build/outputs/apk/release/pandagenie-sdk-demo-provider-release.apk`

## Option 1: Use JitPack

JitPack is the fastest public Maven-like distribution path for this repository because it builds from Git tags.

Consumer setup:

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Dependency:

```gradle
dependencies {
    implementation("com.github.Rorschach123.PandaGenieSDK:pandagenie-sdk:1.0.36")
}
```

Publishing steps:

```powershell
cd E:\ProjectAI\PandaGenie\PandaGenieSDK
.\gradlew.bat clean :pandagenie-sdk:assembleRelease :pandagenie-sdk-demo-provider:assembleRelease --no-daemon
git tag -a 1.0.36 -m "PandaGenieSDK 1.0.36"
git push origin main
git push origin 1.0.36
```

Then open:

```text
https://jitpack.io/#Rorschach123/PandaGenieSDK/1.0.36
```

If JitPack has not built the tag yet, click **Get it** or request the dependency once from Gradle.

## Option 2: Publish To GitHub Packages

GitHub Packages is public but still requires authentication for many Gradle consumers.

Add a Maven repository to each publishing module, or centralize it in the root Gradle file:

```gradle
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Rorschach123/PandaGenieSDK")
            credentials {
                username = findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                password = findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

Publish:

```powershell
cd E:\ProjectAI\PandaGenie\PandaGenieSDK
$env:GITHUB_ACTOR="Rorschach123"
$env:GITHUB_TOKEN="<token with write:packages>"
.\gradlew.bat publishAllPublicationsToGitHubPackagesRepository --no-daemon
```

## Option 3: Publish To Maven Central

Maven Central requires more setup than GitHub or JitPack:

1. Create a Sonatype Central Portal account and verify the namespace for `ai.rorsch.pandagenie`.
2. Create a GPG signing key and publish the public key.
3. Add `signing` and Central Portal publishing configuration to the Gradle modules.
4. Store credentials outside git:

```properties
mavenCentralUsername=...
mavenCentralPassword=...
signing.keyId=...
signing.password=...
signing.secretKeyRingFile=C:/Users/Administrator/.gnupg/secring.gpg
```

5. Run the publish task for the staging/portal repository.
6. Verify and release the deployment in the Central Portal.

Maven Central also requires POM metadata such as project name, description, license, developer, SCM URL, and signed artifacts. Do not publish Central artifacts until those fields are complete.

## Local Verification

```powershell
cd E:\ProjectAI\PandaGenie\PandaGenieSDK
.\gradlew.bat clean publishToMavenLocal :pandagenie-sdk-demo-provider:assembleRelease --no-daemon
```

Local Maven output appears under:

```text
%USERPROFILE%\.m2\repository\ai\rorsch\pandagenie\
```

