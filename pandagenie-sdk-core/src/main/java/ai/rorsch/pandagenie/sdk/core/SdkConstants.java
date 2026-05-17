package ai.rorsch.pandagenie.sdk.core;

public final class SdkConstants {
    private SdkConstants() {}

    public static final String SDK_VERSION = "1.0.36";
    public static final String DEFAULT_REGISTRY_BASE_URL = "https://cf.pandagenie.ai";

    public static final String ACTION_CAPABILITY_SERVICE = "ai.rorsch.pandagenie.sdk.action.CAPABILITY_SERVICE";

    public static final String META_ROLE = "ai.rorsch.pandagenie.sdk.ROLE";
    public static final String META_NAME = "ai.rorsch.pandagenie.sdk.NAME";
    public static final String META_VERSION = "ai.rorsch.pandagenie.sdk.VERSION";
    public static final String META_REGISTRY_BASE_URL = "ai.rorsch.pandagenie.sdk.REGISTRY_BASE_URL";
    public static final String META_PROVIDER_AUTHORITY = "ai.rorsch.pandagenie.sdk.PROVIDER_AUTHORITY";

    public static final String ROLE_AGENT = "agent";
    public static final String ROLE_PROVIDER = "provider";

    public static final int MSG_GET_MANIFEST = 1001;
    public static final int MSG_INVOKE = 1002;
    public static final int MSG_RESULT = 1003;
    public static final int MSG_ERROR = 1004;

    public static final String METHOD_GET_MANIFEST = "getManifest";
    public static final String METHOD_INVOKE = "invoke";

    public static final String KEY_MANIFEST_JSON = "manifest_json";
    public static final String KEY_CAPABILITY_ID = "capability_id";
    public static final String KEY_PARAMS_JSON = "params_json";
    public static final String KEY_RESULT_JSON = "result_json";
    public static final String KEY_ERROR = "error";
    public static final String KEY_GRANT_TOKEN = "grant_token";
    public static final String KEY_AGENT_PACKAGE = "agent_package";
    public static final String KEY_AGENT_SIGNATURE_SHA256 = "agent_signature_sha256";
}
