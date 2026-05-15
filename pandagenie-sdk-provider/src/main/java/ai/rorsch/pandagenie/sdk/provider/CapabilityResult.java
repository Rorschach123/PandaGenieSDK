package ai.rorsch.pandagenie.sdk.provider;

import org.json.JSONObject;

public final class CapabilityResult {
    private final boolean success;
    private final JSONObject data;
    private final String error;

    private CapabilityResult(boolean success, JSONObject data, String error) {
        this.success = success;
        this.data = data == null ? new JSONObject() : data;
        this.error = error == null ? "" : error;
    }

    public static CapabilityResult ok(JSONObject data) {
        return new CapabilityResult(true, data, "");
    }

    public static CapabilityResult fail(String error) {
        return new CapabilityResult(false, new JSONObject(), error);
    }

    public boolean isSuccess() {
        return success;
    }

    public JSONObject getData() {
        return data;
    }

    public String getError() {
        return error;
    }

    public String toJsonString() {
        try {
            return new JSONObject()
                    .put("success", success)
                    .put("data", data)
                    .put("error", error)
                    .toString();
        } catch (Exception e) {
            return "{\"success\":false,\"error\":\"result serialization failed\"}";
        }
    }
}
