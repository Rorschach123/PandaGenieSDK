package ai.rorsch.pandagenie.sdk.core;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SdkRegistryClient {
    private final String baseUrl;
    private int connectTimeoutMs = 8000;
    private int readTimeoutMs = 10000;

    public SdkRegistryClient() {
        this(SdkConstants.DEFAULT_REGISTRY_BASE_URL);
    }

    public SdkRegistryClient(String baseUrl) {
        this.baseUrl = baseUrl == null || baseUrl.trim().isEmpty()
                ? SdkConstants.DEFAULT_REGISTRY_BASE_URL
                : baseUrl.replaceAll("/+$", "");
    }

    public void setTimeouts(int connectTimeoutMs, int readTimeoutMs) {
        this.connectTimeoutMs = Math.max(connectTimeoutMs, 1000);
        this.readTimeoutMs = Math.max(readTimeoutMs, 1000);
    }

    public JSONObject verifyApp(String packageName, String signatureSha256, String role) throws Exception {
        JSONObject body = new JSONObject()
                .put("package_name", packageName)
                .put("signature_sha256", signatureSha256)
                .put("role", role);
        return postJson("/sdk/verify/app", body);
    }

    public JSONObject fetchTrustCache(String role, String since) throws Exception {
        StringBuilder path = new StringBuilder("/sdk/trust-cache");
        path.append("?role=").append(encode(role));
        if (since != null && !since.trim().isEmpty()) {
            path.append("&since=").append(encode(since));
        }
        return getJson(path.toString());
    }

    public JSONObject requestGrantToken(
            String agentPackageName,
            String agentSignatureSha256,
            String providerPackageName,
            String providerSignatureSha256,
            String capabilityIdsJson
    ) throws Exception {
        JSONObject body = new JSONObject()
                .put("agent_package_name", agentPackageName)
                .put("agent_signature_sha256", agentSignatureSha256)
                .put("provider_package_name", providerPackageName)
                .put("provider_signature_sha256", providerSignatureSha256)
                .put("capability_ids", capabilityIdsJson);
        return postJson("/sdk/grants/token", body);
    }

    public JSONObject checkBlacklist(JSONObject body) throws Exception {
        return postJson("/sdk/blacklist/check", body);
    }

    public JSONObject logInvocation(String token, String capabilityId, boolean success, int latencyMs, String error) throws Exception {
        JSONObject body = new JSONObject()
                .put("token", token)
                .put("capability_id", capabilityId)
                .put("success", success)
                .put("latency_ms", latencyMs)
                .put("error", error == null ? "" : error);
        return postJson("/sdk/invocations/log", body);
    }

    public JSONObject postJson(String path, JSONObject body) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream out = conn.getOutputStream()) {
            out.write(bytes);
        }
        int code = conn.getResponseCode();
        String text = readAll(code >= 200 && code < 400 ? conn.getInputStream() : conn.getErrorStream());
        JSONObject json = text.isEmpty() ? new JSONObject() : new JSONObject(text);
        if (code < 200 || code >= 400) {
            throw new IllegalStateException("HTTP " + code + ": " + text);
        }
        return json;
    }

    public JSONObject getJson(String path) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        int code = conn.getResponseCode();
        String text = readAll(code >= 200 && code < 400 ? conn.getInputStream() : conn.getErrorStream());
        JSONObject json = text.isEmpty() ? new JSONObject() : new JSONObject(text);
        if (code < 200 || code >= 400) {
            throw new IllegalStateException("HTTP " + code + ": " + text);
        }
        return json;
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    private static String readAll(InputStream input) throws Exception {
        if (input == null) return "";
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }
}
