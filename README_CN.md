# PandaGenieSDK

PandaGenieSDK 是 PandaGenie 生态中的 Android 应用互操作 SDK。它让 Android 应用可以用可发现、可鉴权、可审计的方式暴露本地能力，使 PandaGenie 或其他已审核通过的 AI 助手能够根据自然语言任务调用这些能力。

**语言：** [English](README_EN.md) | 简体中文

SDK 以 Android AAR 的形式分发，主要连接两类应用：

- **AI 助手 / Agent**：具备规划能力的应用，例如 PandaGenie。它负责发现其他应用暴露的能力，并根据用户需求调用这些能力。
- **能力应用 / Provider**：普通 Android 应用。它通过 SDK 暴露安全的操作、数据视图或 UI 入口，供已审核通过的助手调用。

## 生态关系

| 项目 | 说明 |
|---|---|
| [PandaGenie 官网](https://cf.pandagenie.ai) | APK 下载、开发者入口、SDK 应用注册、模块提交和市场入口。 |
| [PandaGenieSource](https://github.com/Rorschach123/PandaGenieSource) | Android App 源码、官方模块源码、模块打包脚本和模块开发文档。 |
| [PandaGenieSDK](https://github.com/Rorschach123/PandaGenieSDK) | Android AAR，用于应用之间的能力发现、信任校验和调用。 |
| [PandaGenie Module Template](https://github.com/Rorschach123/PandaGenie-Module-Template) | PandaGenie 热加载模块开发模板。 |
| [SDK 应用注册](https://cf.pandagenie.ai/sdk) | 提交应用包名、Release 签名证书、角色和能力说明，审核通过后才能参与 SDK 调用。 |
| [任务 / 模块市场](https://cf.pandagenie.ai/marketplace) | PandaGenie 可使用的公开任务和模块。 |
| [Discord](https://discord.gg/Cfc7pjrjt2) | 开发者交流、SDK 审核加速、模块发布支持和问题反馈。 |

如果你想扩展 PandaGenie 自身能力，请使用模块模板。如果你希望一个独立 Android 应用可以被 PandaGenie 调用，或者你想开发另一个可调用已审核应用的 AI 助手，请使用 PandaGenieSDK。

## 仓库结构

```text
PandaGenieSDK/
  pandagenie-sdk/                对外发布的统一 AAR 门面
  pandagenie-sdk-core/           角色、信任缓存、签名和注册表辅助逻辑
  pandagenie-sdk-agent/          AI 助手侧的发现和调用客户端
  pandagenie-sdk-provider/       能力应用侧的基础 Service 和 Manifest 构建工具
  examples/provider-demo/        可运行 Demo，包含 Service、Activity、Provider、Broadcast 示例
  docs/                          架构、接入、注册和模板说明
  templates/provider-app/        用于创建独立 Provider 模板仓库的轻量说明
```

开发者对外接入时应依赖统一 AAR：

```gradle
implementation("ai.rorsch.pandagenie:pandagenie-sdk:0.1.0-preview")
```

内部模块拆分只是为了让职责更清晰。

## 能力模型

应用初始化 SDK 时可以声明一个或多个角色：

```java
PandaGenieSdk.initialize(
    context,
    PandaGenieSdk.Options.builder()
        .roles(PandaGenieSdk.Role.PROVIDER)
        .build()
);
```

| 角色 | 含义 |
|---|---|
| `PROVIDER` | 暴露能力，供已审核通过的 AI 助手发现和调用。 |
| `AGENT` | 发现已审核通过的能力应用，读取能力清单，并调用能力。 |
| `AGENT + PROVIDER` | 适合同时暴露自身能力、又调用其他应用能力的应用。两个角色会分别审核。 |

服务端通过 `packageName + release SHA-256 signature + role` 校验应用身份。同一个包名可以只通过 Provider 审核，但不具备 Agent 调用权限，反之亦然。

## Provider 快速接入

1. 添加 SDK 依赖。

```gradle
dependencies {
    implementation("ai.rorsch.pandagenie:pandagenie-sdk:0.1.0-preview")
}
```

2. 在 `Application` 中初始化 SDK。

```java
public final class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PandaGenieSdk.initialize(this, PandaGenieSdk.Role.PROVIDER);
    }
}
```

3. 注册导出的能力服务。

```xml
<service
    android:name=".MyCapabilityService"
    android:exported="true">
    <intent-filter>
        <action android:name="ai.rorsch.pandagenie.sdk.action.CAPABILITY_SERVICE" />
    </intent-filter>
</service>
```

4. 声明能力并处理调用。

```java
public final class MyCapabilityService extends PandaGenieCapabilityService {
    @Override
    protected String getManifestJson() {
        return new CapabilityManifestBuilder(this)
            .capability(
                "notes.create",
                "创建笔记",
                "根据标题和正文创建一条笔记",
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
            // 在你的应用中保存笔记。
            return CapabilityResult.ok(new JSONObject().put("created", true));
        }
        return CapabilityResult.fail("unknown capability: " + capabilityId);
    }
}
```

## Agent 快速接入

Agent 应用负责发现能力应用、获取能力清单、把能力知识提供给自己的 LLM 规划器，然后执行规划器返回的能力调用。

```java
PandaGenieSdk.initialize(context, PandaGenieSdk.Role.AGENT);

PandaGenieAgentClient client = new PandaGenieAgentClient(context);
List<DiscoveredProvider> providers = client.discoverProviders();

for (DiscoveredProvider provider : providers) {
    client.fetchManifest(provider, new ManifestCallback() {
        @Override
        public void onManifest(DiscoveredProvider provider, String manifestJson) {
            // 将 manifestJson 加入助手的工具/能力知识库。
        }

        @Override
        public void onError(DiscoveredProvider provider, String error) {
            // 可以忽略不可用应用，或在开发者模式下显示诊断信息。
        }
    });
}
```

当助手生成能力调用后执行：

```java
client.invoke(
    provider,
    "notes.create",
    new JSONObject().put("title", "待办").put("body", "买牛奶"),
    null,
    new InvocationCallback() {
        @Override
        public void onResult(DiscoveredProvider provider, String capabilityId, String resultJson) {
            // 将 resultJson 以合适方式展示给用户。
        }

        @Override
        public void onError(DiscoveredProvider provider, String capabilityId, String error) {
            // 用用户能理解的语言解释失败原因。
        }
    }
);
```

## 信任、审核与黑名单

SDK 调用不是单纯依赖本地 Intent 的弱信任模型。PandaGenie 会维护服务端注册表，并下发紧凑的信任缓存：

```text
GET https://cf.pandagenie.ai/sdk/trust-cache?role=agent,provider
```

缓存中只包含已审核通过且未被拉黑的身份。身份计算方式：

```text
sha256(packageName|normalizedReleaseSignatureSha256|role)
```

流程：

1. 开发者在 [SDK 应用注册](https://cf.pandagenie.ai/sdk) 提交应用。
2. Admin 审核包名、Release 签名证书、应用角色、应用说明和能力列表。
3. 审核通过的应用会进入信任缓存。
4. 被加入黑名单的包名/签名/角色身份会从可调用集合中移除。
5. SDK 周期性刷新缓存，因此服务端审核和黑名单变更不需要更新 App 即可生效。

Provider 服务默认拒绝未受信任的 Agent。Agent 客户端默认隐藏未受信任的 Provider。

## Demo 应用与模板

`examples/provider-demo` 被刻意放在主 PandaGenie App 工程之外。它既是可运行 Demo，也可以作为 GitHub 模板仓库的基础。Gradle 模块名仍保持为 `:pandagenie-sdk-demo-provider`。

Demo 覆盖：

- Service JSON 参数调用和 JSON 结果返回。
- 打开 Activity 并展示传入文本。
- 读取 ContentProvider 数据。
- 发送和接收 Broadcast 事件。
- 本地状态读写。
- 长耗时任务模拟。

模板说明见 [templates/provider-app/README.md](templates/provider-app/README.md)。建议的外部模板仓库：

```text
https://github.com/Rorschach123/PandaGenieSDK-Provider-Template
```

如果该仓库尚未创建，可以先将 Demo Provider 模块复制到新仓库，只保留 Provider 应用和最小 Gradle 配置。

## 构建

当前 SDK 仓库可以复用主 App checkout 中的 Gradle Wrapper：

```powershell
cd E:\ProjectAI\PandaGenie\PandaGenieSDK
..\PandaGenie\gradlew.bat -p . :pandagenie-sdk:assembleRelease :pandagenie-sdk-demo-provider:assembleRelease --no-daemon
```

常用产物：

```text
pandagenie-sdk/build/outputs/aar/pandagenie-sdk-release.aar
examples/provider-demo/build/outputs/apk/release/pandagenie-sdk-demo-provider-release.apk
```

## Release 签名

注册和测试应使用 Release 构建，不要使用 Debug 构建。可以用以下命令获取 SHA-256 证书指纹：

```bash
# 从 APK 读取
apksigner verify --print-certs app-release.apk

# 从 keystore 读取
keytool -list -v -keystore release.jks -alias your_alias
```

期望格式：

```text
2A:5F:...:9C
```

注册表单会自动归一化分隔符和大小写，但最终必须是 64 位十六进制 SHA-256 指纹。

## 更多文档

- [架构说明](docs/ARCHITECTURE.md)
- [接入指南](docs/INTEGRATION.md)
- [注册和审核](docs/REGISTRATION.md)
- [Demo 与模板指南](docs/DEMO_AND_TEMPLATE.md)

