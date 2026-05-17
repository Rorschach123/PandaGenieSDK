# PandaGenieSDK Provider 模板说明

这个目录记录推荐的独立 Provider 示例模板工程结构。当前可运行示例源码位于：

```text
PandaGenieSDK/examples/provider-demo
```

## 创建 GitHub Template 的推荐步骤

1. 新建仓库，例如 `PandaGenieSDK-Provider-Template`。
2. 将 `examples/provider-demo` 中的 Provider 示例整理为独立 `app/` 工程。
3. 保留最小 Gradle 工程文件，删除本地构建缓存和私有签名材料。
4. 替换 demo 包名、应用名称和能力清单。
5. 使用发布后的 SDK AAR 依赖，不依赖本地 project module。
6. 在 README 中说明注册步骤、签名获取方式和能力清单示例。

推荐依赖：

```gradle
implementation("ai.rorsch.pandagenie:pandagenie-sdk:1.0.36")
```

## 不要提交

- release keystore
- 签名密码
- 本地 `signing.properties`
- 生成的 APK、AAB、AAR
- Gradle build/cache 目录

发布为 GitHub Template 后，开发者可以点击 **Use this template** 快速创建自己的 Provider 应用。

## English

This folder documents the recommended standalone template project for PandaGenieSDK provider apps.

The runnable demo currently lives in `PandaGenieSDK/examples/provider-demo`. Copy it into a clean repository, replace the package name, app name, and capability manifest, then depend on the published SDK artifact.
