package ai.rorsch.pandagenie.sdk.provider;

import ai.rorsch.pandagenie.sdk.core.SdkConstants;

import org.json.JSONArray;
import org.json.JSONObject;

public final class CapabilityManifestBuilder {
    private final JSONObject root = new JSONObject();
    private final JSONArray capabilities = new JSONArray();

    public CapabilityManifestBuilder(String appName, String packageName) {
        put(root, "sdkVersion", SdkConstants.SDK_VERSION);
        put(root, "role", SdkConstants.ROLE_PROVIDER);
        put(root, "appName", appName);
        put(root, "packageName", packageName);
        put(root, "capabilities", capabilities);
    }

    public CapabilityManifestBuilder version(String version) {
        put(root, "version", version);
        return this;
    }

    public CapabilityManifestBuilder description(String description) {
        put(root, "description", description);
        return this;
    }

    public CapabilityManifestBuilder category(String category) {
        put(root, "category", category);
        return this;
    }

    public CapabilityManifestBuilder capability(
            String id,
            String title,
            String description,
            String kind,
            String riskLevel,
            JSONArray permissions,
            JSONObject inputSchema
    ) {
        JSONObject item = new JSONObject();
        put(item, "id", id);
        put(item, "title", title);
        put(item, "description", description);
        put(item, "kind", kind);
        put(item, "riskLevel", riskLevel);
        put(item, "permissions", permissions == null ? new JSONArray() : permissions);
        put(item, "inputSchema", inputSchema == null ? new JSONObject() : inputSchema);
        capabilities.put(item);
        return this;
    }

    public JSONObject build() {
        return root;
    }

    public String buildString() {
        return root.toString();
    }

    private static void put(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (Exception ignored) {
        }
    }
}
