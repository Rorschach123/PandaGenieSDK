package ai.rorsch.pandagenie.sdkdemo.provider;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;

import ai.rorsch.pandagenie.sdk.provider.CapabilityManifestBuilder;
import ai.rorsch.pandagenie.sdk.provider.CapabilityResult;
import ai.rorsch.pandagenie.sdk.provider.PandaGenieCapabilityService;

import org.json.JSONArray;
import org.json.JSONObject;

public class DemoCapabilityService extends PandaGenieCapabilityService {
    public static final String CONTENT_URI = "content://ai.rorsch.pandagenie.sdkdemo.provider.samples/items";
    public static final String ACTION_DEMO_PING = "ai.rorsch.pandagenie.sdkdemo.provider.DEMO_PING";

    @Override
    protected String getManifestJson() {
        return new CapabilityManifestBuilder("PandaGenie 能力演示", getPackageName())
                .version("1.0.0")
                .description("用于验证 PandaGenieSDK Activity、Service、ContentProvider、Broadcast 和本地数据能力的演示应用。")
                .category("sdk_demo")
                .capability("demo.echo", "回显文本", "返回调用方传入的文本，用于验证 Service 能力调用。", "query", "low", arr("service"), schema("text"))
                .capability("demo.open_activity", "打开演示页面", "打开演示应用页面，并显示调用方传入的标题和内容。", "open_ui", "low", arr("open_ui"), schema("title", "message", "source"))
                .capability("demo.open_detail_page", "打开详情页面", "打开演示应用的详情页面，并根据参数展示指定内容。", "open_ui", "low", arr("open_ui"), schema("title", "message", "detailId"))
                .capability("demo.provider_rows", "读取 Provider 列表", "读取演示应用导出的 ContentProvider 数据。", "provider", "medium", arr("data_read"), schema())
                .capability("demo.provider_search", "搜索 Provider 数据", "按关键字搜索 ContentProvider 中的演示数据。", "provider", "medium", arr("data_read"), schema("keyword"))
                .capability("demo.save_note", "保存本地便签", "将文本保存到演示应用本地存储，用于验证写入类能力。", "mutation", "medium", arr("data_write"), schema("note"))
                .capability("demo.read_note", "读取本地便签", "读取上一次保存到演示应用本地存储的便签。", "query", "low", arr("data_read"), schema())
                .capability("demo.broadcast_ping", "发送演示广播", "发送一个广播并由演示应用接收展示。", "broadcast", "medium", arr("broadcast"), schema("message"))
                .capability("demo.broadcast_status", "发送状态广播", "发送携带状态和消息的广播，用于验证事件类能力。", "event", "medium", arr("broadcast"), schema("status", "message"))
                .capability("demo.long_task", "执行后台任务", "模拟一个后台 Service 任务并返回完成状态。", "service", "medium", arr("service"), schema("name"))
                .buildString();
    }

    @Override
    protected CapabilityResult onInvoke(String capabilityId, JSONObject params, InvokeContext context) {
        try {
            switch (capabilityId) {
                case "demo.echo":
                    return CapabilityResult.ok(new JSONObject()
                            .put("text", params.optString("text", "hello from demo provider"))
                            .put("agent", context.agentPackageName));
                case "demo.open_activity":
                    return openDemoActivity(params, context);
                case "demo.open_detail_page":
                    params.put("title", params.optString("title", "能力详情页"));
                    params.put("message", params.optString("message", "这是由 PandaGenieSDK 打开的详情页面。"));
                    return openDemoActivity(params, context);
                case "demo.provider_rows":
                    return CapabilityResult.ok(readProviderRows(""));
                case "demo.provider_search":
                    return CapabilityResult.ok(readProviderRows(params.optString("keyword", "")));
                case "demo.save_note":
                    return saveNote(params);
                case "demo.read_note":
                    return CapabilityResult.ok(readSavedNote());
                case "demo.broadcast_ping":
                    Intent broadcast = new Intent(ACTION_DEMO_PING);
                    broadcast.setPackage(getPackageName());
                    broadcast.putExtra("message", params.optString("message", "ping"));
                    sendBroadcast(broadcast);
                    return CapabilityResult.ok(new JSONObject().put("broadcastSent", true));
                case "demo.broadcast_status":
                    Intent statusBroadcast = new Intent(ACTION_DEMO_PING);
                    statusBroadcast.setPackage(getPackageName());
                    statusBroadcast.putExtra("status", params.optString("status", "ok"));
                    statusBroadcast.putExtra("message", params.optString("message", "status updated"));
                    sendBroadcast(statusBroadcast);
                    return CapabilityResult.ok(new JSONObject()
                            .put("broadcastSent", true)
                            .put("status", params.optString("status", "ok")));
                case "demo.long_task":
                    return CapabilityResult.ok(new JSONObject()
                            .put("status", "done")
                            .put("name", params.optString("name", "demo")));
                default:
                    return CapabilityResult.fail("unknown capability: " + capabilityId);
            }
        } catch (Exception e) {
            return CapabilityResult.fail(e.getMessage());
        }
    }

    private CapabilityResult openDemoActivity(JSONObject params, InvokeContext context) throws Exception {
        String title = params.optString("title", "SDK page invocation");
        String message = params.optString("message", "This page was opened by PandaGenieSDK.");
        String source = params.optString("source", context.agentPackageName);
        String detailId = params.optString("detailId", "");

        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        open.putExtra("title", title);
        open.putExtra("message", message);
        open.putExtra("source", source);
        open.putExtra("detailId", detailId);
        startActivity(open);

        return CapabilityResult.ok(new JSONObject()
                .put("opened", true)
                .put("page", "MainActivity")
                .put("title", title)
                .put("message", message)
                .put("source", source)
                .put("detailId", detailId));
    }

    private CapabilityResult saveNote(JSONObject params) throws Exception {
        String note = params.optString("note", "PandaGenie SDK note");
        long updatedAt = System.currentTimeMillis();
        SharedPreferences preferences = getSharedPreferences("demo_state", MODE_PRIVATE);
        preferences.edit()
                .putString("note", note)
                .putLong("updatedAt", updatedAt)
                .apply();
        return CapabilityResult.ok(new JSONObject()
                .put("saved", true)
                .put("note", note)
                .put("updatedAt", updatedAt));
    }

    private JSONObject readSavedNote() throws Exception {
        SharedPreferences preferences = getSharedPreferences("demo_state", MODE_PRIVATE);
        return new JSONObject()
                .put("note", preferences.getString("note", ""))
                .put("updatedAt", preferences.getLong("updatedAt", 0L));
    }

    private JSONObject readProviderRows(String keyword) throws Exception {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        JSONArray rows = new JSONArray();
        try (Cursor cursor = getContentResolver().query(Uri.parse(CONTENT_URI), null, null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    String value = cursor.getString(cursor.getColumnIndexOrThrow("value"));
                    if (normalizedKeyword.isEmpty()
                            || name.toLowerCase().contains(normalizedKeyword)
                            || value.toLowerCase().contains(normalizedKeyword)) {
                        rows.put(new JSONObject()
                                .put("id", cursor.getInt(cursor.getColumnIndexOrThrow("_id")))
                                .put("name", name)
                                .put("value", value));
                    }
                }
            }
        }
        return new JSONObject()
                .put("rows", rows)
                .put("count", rows.length())
                .put("keyword", keyword);
    }

    private static JSONArray arr(String... items) {
        JSONArray array = new JSONArray();
        for (String item : items) array.put(item);
        return array;
    }

    private static JSONObject schema(String... fields) {
        JSONObject root = new JSONObject();
        JSONObject props = new JSONObject();
        try {
            root.put("type", "object");
            for (String field : fields) {
                props.put(field, new JSONObject().put("type", "string"));
            }
            root.put("properties", props);
        } catch (Exception ignored) {
        }
        return root;
    }
}
