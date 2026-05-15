package ai.rorsch.pandagenie.sdk.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class PandaGenieSdk {
    private static final String PREFS = "pandagenie_sdk_trust_cache";
    private static final String KEY_ITEMS = "items_json";
    private static final String KEY_LAST_REFRESH_MS = "last_refresh_ms";
    private static final long DEFAULT_REFRESH_TTL_MS = 6L * 60L * 60L * 1000L;

    private static final Object LOCK = new Object();
    private static volatile Context appContext;
    private static volatile Options options = Options.builder().build();
    private static volatile SdkRegistryClient registryClient = new SdkRegistryClient();
    private static volatile boolean refreshRunning;

    private PandaGenieSdk() {}

    public static final class Role {
        public static final String AGENT = SdkConstants.ROLE_AGENT;
        public static final String PROVIDER = SdkConstants.ROLE_PROVIDER;

        private Role() {}
    }

    public static final class Options {
        public final String registryBaseUrl;
        public final Set<String> roles;
        public final boolean refreshOnInit;
        public final long refreshTtlMs;

        private Options(Builder builder) {
            this.registryBaseUrl = builder.registryBaseUrl;
            this.roles = Collections.unmodifiableSet(new LinkedHashSet<>(builder.roles));
            this.refreshOnInit = builder.refreshOnInit;
            this.refreshTtlMs = Math.max(builder.refreshTtlMs, 60_000L);
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder toBuilder() {
            Builder builder = new Builder()
                    .registryBaseUrl(registryBaseUrl)
                    .refreshOnInit(refreshOnInit)
                    .refreshTtlMs(refreshTtlMs);
            for (String role : roles) builder.addRole(role);
            return builder;
        }

        public static final class Builder {
            private String registryBaseUrl = SdkConstants.DEFAULT_REGISTRY_BASE_URL;
            private final Set<String> roles = new LinkedHashSet<>();
            private boolean refreshOnInit = true;
            private long refreshTtlMs = DEFAULT_REFRESH_TTL_MS;

            public Builder registryBaseUrl(String registryBaseUrl) {
                if (registryBaseUrl != null && !registryBaseUrl.trim().isEmpty()) {
                    this.registryBaseUrl = registryBaseUrl.trim();
                }
                return this;
            }

            public Builder roles(String... roles) {
                if (roles != null) {
                    for (String role : roles) addRole(role);
                }
                return this;
            }

            public Builder addRole(String role) {
                String normalized = normalizeRole(role);
                if (!normalized.isEmpty()) roles.add(normalized);
                return this;
            }

            public Builder refreshOnInit(boolean refreshOnInit) {
                this.refreshOnInit = refreshOnInit;
                return this;
            }

            public Builder refreshTtlMs(long refreshTtlMs) {
                this.refreshTtlMs = refreshTtlMs;
                return this;
            }

            public Options build() {
                return new Options(this);
            }
        }
    }

    public static void initialize(Context context, Options newOptions) {
        if (context == null) throw new IllegalArgumentException("context == null");
        synchronized (LOCK) {
            appContext = context.getApplicationContext();
            options = newOptions == null ? Options.builder().build() : newOptions;
            registryClient = new SdkRegistryClient(options.registryBaseUrl);
        }
        if (options.refreshOnInit) {
            refreshTrustedAppsAsync();
        }
    }

    public static void initialize(Context context, String... roles) {
        initialize(context, Options.builder().roles(roles).build());
    }

    public static void initializeIfNeeded(Context context, String... roles) {
        if (context == null) return;
        boolean shouldRefresh = false;
        synchronized (LOCK) {
            if (appContext == null) {
                appContext = context.getApplicationContext();
                options = Options.builder().roles(roles).build();
                registryClient = new SdkRegistryClient(options.registryBaseUrl);
                shouldRefresh = options.refreshOnInit;
            } else {
                Options.Builder builder = options.toBuilder();
                int before = options.roles.size();
                builder.roles(roles);
                Options merged = builder.build();
                if (merged.roles.size() != before) {
                    options = merged;
                    shouldRefresh = options.refreshOnInit;
                }
            }
        }
        if (shouldRefresh || isCacheStale()) {
            refreshTrustedAppsAsync();
        }
    }

    public static boolean isInitialized() {
        return appContext != null;
    }

    public static SdkRegistryClient getRegistryClient() {
        return registryClient;
    }

    public static String getOwnIdentitySha256(Context context, String role) {
        try {
            String signature = SdkSignatureUtils.getOwnSignatureSha256(context.getApplicationContext());
            return SdkSignatureUtils.identitySha256(context.getPackageName(), signature, role);
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isOwnRoleTrusted(Context context, String role) {
        try {
            String signature = SdkSignatureUtils.getOwnSignatureSha256(context.getApplicationContext());
            return isTrustedApp(context, context.getPackageName(), signature, role);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isTrustedApp(Context context, String packageName, String signatureSha256, String role) {
        String normalizedRole = normalizeRole(role);
        if (context == null || packageName == null || packageName.trim().isEmpty() || normalizedRole.isEmpty()) {
            return false;
        }
        initializeIfNeeded(context, normalizedRole);
        if (isCacheStale()) {
            refreshTrustedAppsAsync();
        }
        String identity = SdkSignatureUtils.identitySha256(packageName, signatureSha256, normalizedRole);
        if (identity.isEmpty()) return false;
        return readCachedIdentitySet(context.getApplicationContext()).contains(normalizedRole + ":" + identity);
    }

    public static void refreshTrustedAppsAsync() {
        Context context = appContext;
        if (context == null || refreshRunning) return;
        refreshRunning = true;
        new Thread(() -> {
            try {
                refreshTrustedApps();
            } catch (Exception ignored) {
            } finally {
                refreshRunning = false;
            }
        }, "PandaGenieSdkTrustRefresh").start();
    }

    public static JSONObject refreshTrustedApps() throws Exception {
        Context context = appContext;
        if (context == null) throw new IllegalStateException("PandaGenieSdk is not initialized");

        Set<String> roles = options.roles.isEmpty()
                ? Collections.singleton(SdkConstants.ROLE_PROVIDER)
                : options.roles;
        JSONArray merged = new JSONArray();
        for (String role : roles) {
            JSONObject response = registryClient.fetchTrustCache(role, null);
            JSONObject data = response.optJSONObject("data");
            JSONArray items = data != null ? data.optJSONArray("items") : response.optJSONArray("items");
            if (items == null) continue;
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item != null) merged.put(item);
            }
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ITEMS, merged.toString())
                .putLong(KEY_LAST_REFRESH_MS, SystemClock.elapsedRealtime())
                .apply();
        return new JSONObject().put("items", merged).put("count", merged.length());
    }

    private static boolean isCacheStale() {
        Context context = appContext;
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long last = prefs.getLong(KEY_LAST_REFRESH_MS, 0L);
        return last <= 0L || SystemClock.elapsedRealtime() - last > options.refreshTtlMs;
    }

    private static Set<String> readCachedIdentitySet(Context context) {
        LinkedHashSet<String> identities = new LinkedHashSet<>();
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ITEMS, "[]");
        try {
            JSONArray items = new JSONArray(raw == null ? "[]" : raw);
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) continue;
                String role = normalizeRole(item.optString("role"));
                String identity = item.optString("identity_sha256", "").trim().toLowerCase(Locale.US);
                if (!role.isEmpty() && !identity.isEmpty()) {
                    identities.add(role + ":" + identity);
                }
            }
        } catch (Exception ignored) {
        }
        return identities;
    }

    private static String normalizeRole(String role) {
        String normalized = role == null ? "" : role.trim().toLowerCase(Locale.US);
        if (SdkConstants.ROLE_AGENT.equals(normalized) || SdkConstants.ROLE_PROVIDER.equals(normalized)) {
            return normalized;
        }
        return "";
    }
}
