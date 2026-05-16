# PandaGenieSDK

**面向 Android 的 PandaGenie 应用互操作 SDK**

[官网](https://cf.pandagenie.ai) | [SDK 注册](https://cf.pandagenie.ai/sdk) | [SDK Provider 模板](https://github.com/Rorschach123/PandaGenieSDK-Provider-Template) | [PandaGenieSource](https://github.com/Rorschach123/PandaGenieSource) | [Discord](https://discord.gg/Cfc7pjrjt2) | [English](README_EN.md)

---

## SDK 是什么

PandaGenieSDK 是一个 Android AAR，用于让不同 App 之间以可发现、可审核、可权限控制的方式互相调用能力。

它服务两类应用：

- **AI 助手应用**：具备任务规划能力的应用，例如 PandaGenie。它可以发现已审核的能力应用，并在用户授权后调用这些能力。
- **能力应用**：普通 Android 应用。它通过 SDK 暴露可调用能力，例如打开指定页面、查询本地数据、执行 Service、提供 Provider 数据或接收 Broadcast。

SDK 的目标不是绕过 Android 权限，而是把“应用能做什么、谁能调用、调用是否可信、结果如何审计”做成统一协议。

## 生态关系

| 项目 | 作用 |
|---|---|
| [PandaGenie 官网](https://cf.pandagenie.ai) | APK 下载、任务市场、模块市场、SDK 注册、模块提交和开发者入口。 |
| [PandaGenieSource](https://github.com/Rorschach123/PandaGenieSource) | PandaGenie App 源码、官方模块、模块打包和模块目录。 |
| [PandaGenieSDK](https://github.com/Rorschach123/PandaGenieSDK) | 当前仓库：Android AAR、Agent/Provider SDK、Demo 和文档。 |
| [PandaGenieSDK Provider Template](https://github.com/Rorschach123/PandaGenieSDK-Provider-Template) | 独立能力应用模板，适合第三方 App 快速接入。 |
| [PandaGenie Module Template](https://github.com/Rorschach123/PandaGenie-Module-Template) | PandaGenie 内部热加载模块模板。 |

简单区分：

- 想扩展 PandaGenie 内部能力：开发 **模块**。
- 想让自己的独立 App 被 PandaGenie 调用：接入 **PandaGenieSDK Provider**。
- 想做一个能调用其他 App 的 AI 助手：接入 **PandaGenieSDK Agent**。

## 仓库结构

```text
PandaGenieSDK/
  pandagenie-sdk/             统一 AAR 门面，开发者主要依赖这个模块
  pandagenie-sdk-core/        角色、签名、信任缓存、注册表等公共逻辑
  pandagenie-sdk-agent/       AI 助手侧发现和调用能力应用
  pandagenie-sdk-provider/    能力应用侧服务基类与 manifest 构建器
  examples/provider-demo/     可运行 Demo，覆盖 Activity/Service/Provider/Broadcast
  docs/                       架构、接入、注册审核和模板说明
  templates/provider-app/     独立 Provider 模板仓库说明
```

## 快速接入：能力应用

```gradle
dependencies {
    implementation("ai.rorsch.pandagenie:pandagenie-sdk:0.1.0-preview")
}
```

```java
public final class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PandaGenieSdk.initialize(this, PandaGenieSdk.Role.PROVIDER);
    }
}
```

能力应用需要：

1. 声明可调用能力清单。
2. 暴露 SDK 能发现的 Service/Provider/Activity/Broadcast。
3. 校验调用方是否是已审核 AI 助手。
4. 返回结构化 JSON 结果。
5. 到 [SDK 注册页](https://cf.pandagenie.ai/sdk) 提交包名、Release SHA-256 签名、应用说明和能力列表。

## 快速接入：AI 助手

```java
PandaGenieSdk.initialize(context, PandaGenieSdk.Role.AGENT);

PandaGenieAgentClient client = new PandaGenieAgentClient(context);
List<DiscoveredProvider> providers = client.discoverProviders();
```

AI 助手需要：

1. 通过 SDK 发现已安装能力应用。
2. 读取能力应用 manifest。
3. 将能力清单交给自己的 LLM 规划器。
4. 根据模型返回的调用链执行 SDK 调用。
5. 校验能力应用是否在服务端下发的可信名单中。

## 信任、审核与黑名单

服务端按下面的身份生成可信记录：

```text
sha256(packageName|normalizedReleaseSignatureSha256|role)
```

一个应用可以同时申请 Agent 和 Provider，但两个角色会独立审核。被加入黑名单的 `包名 + 签名 + 角色` 不会出现在可信缓存中，SDK 也会拒绝其调用或隐藏其能力。

可信缓存接口：

```text
GET https://cf.pandagenie.ai/sdk/trust-cache?role=agent,provider
```

## Demo 与模板

- 可运行 Demo：`examples/provider-demo`
- 独立模板仓库：[PandaGenieSDK-Provider-Template](https://github.com/Rorschach123/PandaGenieSDK-Provider-Template)

Demo 展示：

- Service JSON 调用。
- 打开 Activity 并传递文本。
- 从 ContentProvider 读取数据。
- BroadcastReceiver 接收调用。
- Release 签名和注册信息示例。

## 文档

- [架构说明](docs/ARCHITECTURE.md)
- [接入指南](docs/INTEGRATION.md)
- [注册与审核](docs/REGISTRATION.md)
- [Demo 与模板](docs/DEMO_AND_TEMPLATE.md)

## English

English documentation is available in [README_EN.md](README_EN.md).
