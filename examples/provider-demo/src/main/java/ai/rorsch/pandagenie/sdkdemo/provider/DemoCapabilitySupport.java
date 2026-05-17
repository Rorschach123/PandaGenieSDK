package ai.rorsch.pandagenie.sdkdemo.provider;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import ai.rorsch.pandagenie.sdk.provider.CapabilityManifestBuilder;
import ai.rorsch.pandagenie.sdk.provider.CapabilityResult;

import org.json.JSONArray;
import org.json.JSONObject;

final class DemoCapabilitySupport {
    private static final String VERSION = "1.0.36";
    private static final String APP_NAME = MainActivity.APP_NAME;
    private static final Uri SAMPLE_ITEMS_URI =
            Uri.parse("content://ai.rorsch.pandagenie.sdkdemo.provider.samples/items");
    private static final String ACTION_DEMO_PING =
            "ai.rorsch.pandagenie.sdkdemo.provider.DEMO_PING";

    private DemoCapabilitySupport() {
    }

    static String buildManifest(String packageName) {
        return new CapabilityManifestBuilder(APP_NAME, packageName)
                .version(VERSION)
                .description("用于验证 PandaGenieSDK 的打开页面、读取文字、修改文字以及兼容组件调用能力。")
                .category("sdk_demo")
                .capability(
                        "demo.open_text_page",
                        "打开文字展示页",
                        "打开 SDK 调用演示 App，并展示当前文字或传入文字。",
                        "open_ui",
                        "low",
                        arr("open_ui"),
                        schema("text", "message", "title", "source", "detailId")
                )
                .capability(
                        "demo.get_text",
                        "获取演示文字",
                        "读取 SDK 调用演示 App 当前保存并展示的文字。",
                        "query",
                        "low",
                        arr("data_read"),
                        schema()
                )
                .capability(
                        "demo.update_text",
                        "修改演示文字",
                        "把 SDK 调用演示 App 当前展示的文字修改为传入内容。",
                        "mutation",
                        "medium",
                        arr("data_write"),
                        schema("text", "note", "message", "content")
                )
                .capability("demo.echo", "回显文本", "返回传入文本，用于测试 SDK 调用链。", "query", "low", arr(), schema("text", "message"))
                .capability("demo.open_activity", "打开演示页", "兼容旧版本：打开演示页面。", "open_ui", "low", arr("open_ui"), schema("title", "message"))
                .capability("demo.open_detail_page", "打开详情页", "兼容旧版本：打开带参数的演示页面。", "open_ui", "low", arr("open_ui"), schema("title", "message", "detailId"))
                .capability("demo.provider_rows", "读取 Provider 数据", "读取演示 App 暴露的 Provider 数据。", "provider", "low", arr("data_read"), schema())
                .capability("demo.provider_search", "搜索 Provider 数据", "按关键词搜索演示 Provider 数据。", "provider", "low", arr("data_read"), schema("keyword"))
                .capability("demo.save_note", "保存本地便签", "兼容旧版本：保存演示文字。", "mutation", "medium", arr("data_write"), schema("note", "text"))
                .capability("demo.read_note", "读取本地便签", "兼容旧版本：读取演示文字。", "query", "low", arr("data_read"), schema())
                .capability("demo.broadcast_ping", "发送广播 Ping", "发送一个演示广播。", "broadcast", "low", arr(), schema("message"))
                .capability("demo.broadcast_status", "发送状态广播", "发送一个带状态的演示广播。", "broadcast", "low", arr(), schema("status", "message"))
                .capability("demo.long_task", "执行长任务", "模拟耗时任务并返回完成状态。", "task", "low", arr(), schema("label"))
                .buildString();
    }

    static CapabilityResult invoke(Context context, String capabilityId, JSONObject params, String agentPackageName) {
        if (context == null) {
            return CapabilityResult.fail("context unavailable");
        }
        JSONObject safeParams = params == null ? new JSONObject() : params;
        String id = capabilityId == null ? "" : capabilityId;
        try {
            if ("demo.open_text_page".equals(id) || "demo.open_activity".equals(id) || "demo.open_detail_page".equals(id)) {
                String text = firstNonEmpty(
                        safeParams.optString("text"),
                        safeParams.optString("message"),
                        safeParams.optString("note"),
                        safeParams.optString("content")
                );
                if (!TextUtils.isEmpty(text)) {
                    MainActivity.saveDemoText(context, text);
                }
                openDemoActivity(context, safeParams);
                return CapabilityResult.ok(baseResult(context)
                        .put("opened", true)
                        .put("text", MainActivity.readDemoText(context)));
            }

            if ("demo.get_text".equals(id) || "demo.read_note".equals(id)) {
                return CapabilityResult.ok(baseResult(context));
            }

            if ("demo.update_text".equals(id) || "demo.save_note".equals(id)) {
                String text = firstNonEmpty(
                        safeParams.optString("text"),
                        safeParams.optString("note"),
                        safeParams.optString("message"),
                        safeParams.optString("content"),
                        safeParams.optString("value")
                );
                if (TextUtils.isEmpty(text)) {
                    return CapabilityResult.fail("请传入要保存的文字内容");
                }
                MainActivity.saveDemoText(context, text);
                return CapabilityResult.ok(baseResult(context).put("saved", true));
            }

            if ("demo.provider_rows".equals(id)) {
                return CapabilityResult.ok(readProviderRows(context, ""));
            }

            if ("demo.provider_search".equals(id)) {
                return CapabilityResult.ok(readProviderRows(context, safeParams.optString("keyword")));
            }

            if ("demo.echo".equals(id)) {
                String text = firstNonEmpty(safeParams.optString("text"), safeParams.optString("message"), "pong");
                return CapabilityResult.ok(new JSONObject()
                        .put("text", text)
                        .put("agentPackageName", agentPackageName == null ? "" : agentPackageName));
            }

            if ("demo.broadcast_ping".equals(id) || "demo.broadcast_status".equals(id)) {
                Intent intent = new Intent(ACTION_DEMO_PING);
                intent.setPackage(context.getPackageName());
                intent.putExtra("message", firstNonEmpty(safeParams.optString("message"), "来自 PandaGenie 的广播测试"));
                intent.putExtra("status", safeParams.optString("status"));
                context.sendBroadcast(intent);
                return CapabilityResult.ok(new JSONObject().put("sent", true));
            }

            if ("demo.long_task".equals(id)) {
                return CapabilityResult.ok(new JSONObject()
                        .put("status", "done")
                        .put("label", firstNonEmpty(safeParams.optString("label"), "演示长任务")));
            }

            return CapabilityResult.fail("unsupported demo capability: " + id);
        } catch (Exception e) {
            return CapabilityResult.fail(e.getMessage() == null ? "invoke failed" : e.getMessage());
        }
    }

    private static JSONObject baseResult(Context context) throws Exception {
        long updatedAt = MainActivity.readUpdatedAt(context);
        return new JSONObject()
                .put("appName", APP_NAME)
                .put("text", MainActivity.readDemoText(context))
                .put("updatedAt", updatedAt)
                .put("updatedAtText", MainActivity.formatTime(updatedAt));
    }

    private static void openDemoActivity(Context context, JSONObject params) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        putExtraIfPresent(intent, "title", params.optString("title"));
        putExtraIfPresent(intent, "message", params.optString("message"));
        putExtraIfPresent(intent, "source", params.optString("source"));
        putExtraIfPresent(intent, "detailId", params.optString("detailId"));
        putExtraIfPresent(intent, "text", firstNonEmpty(
                params.optString("text"),
                params.optString("note"),
                params.optString("content")
        ));
        context.startActivity(intent);
    }

    private static JSONObject readProviderRows(Context context, String keyword) throws Exception {
        JSONArray rows = new JSONArray();
        Cursor cursor = context.getContentResolver().query(SAMPLE_ITEMS_URI, null, null, null, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String name = readColumn(cursor, "name");
                    String value = readColumn(cursor, "value");
                    String description = readColumn(cursor, "description");
                    if (!TextUtils.isEmpty(keyword)
                            && !name.contains(keyword)
                            && !value.contains(keyword)
                            && !description.contains(keyword)) {
                        continue;
                    }
                    rows.put(new JSONObject()
                            .put("name", name)
                            .put("value", value)
                            .put("description", description));
                }
            } finally {
                cursor.close();
            }
        }
        return new JSONObject()
                .put("count", rows.length())
                .put("rows", rows)
                .put("keyword", keyword == null ? "" : keyword)
                .put("source", SAMPLE_ITEMS_URI.toString());
    }

    private static String readColumn(Cursor cursor, String name) {
        int index = cursor.getColumnIndex(name);
        if (index < 0) {
            return "";
        }
        String value = cursor.getString(index);
        return value == null ? "" : value;
    }

    private static void putExtraIfPresent(Intent intent, String key, String value) {
        if (!TextUtils.isEmpty(value)) {
            intent.putExtra(key, value);
        }
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return "";
    }

    private static JSONArray arr(String... values) {
        JSONArray array = new JSONArray();
        if (values != null) {
            for (String value : values) {
                array.put(value);
            }
        }
        return array;
    }

    private static JSONObject schema(String... fields) {
        JSONObject schema = new JSONObject();
        try {
            schema.put("type", "object");
            JSONObject properties = new JSONObject();
            if (fields != null) {
                for (String field : fields) {
                    properties.put(field, new JSONObject().put("type", "string"));
                }
            }
            schema.put("properties", properties);
        } catch (Exception ignored) {
        }
        return schema;
    }
}
