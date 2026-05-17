package ai.rorsch.pandagenie.sdk.agent;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import ai.rorsch.pandagenie.sdk.core.SdkConstants;
import ai.rorsch.pandagenie.sdk.core.PandaGenieSdk;
import ai.rorsch.pandagenie.sdk.core.SdkSignatureUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PandaGenieAgentClient {
    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private long requestTimeoutMs = 8000L;
    private boolean requireTrustedProviders = true;

    public PandaGenieAgentClient(Context context) {
        this.appContext = context.getApplicationContext();
        PandaGenieSdk.initializeIfNeeded(this.appContext, SdkConstants.ROLE_AGENT, SdkConstants.ROLE_PROVIDER);
    }

    public void setRequestTimeoutMs(long requestTimeoutMs) {
        this.requestTimeoutMs = Math.max(requestTimeoutMs, 1000L);
    }

    public void setRequireTrustedProviders(boolean requireTrustedProviders) {
        this.requireTrustedProviders = requireTrustedProviders;
    }

    public List<DiscoveredProvider> discoverProviders() {
        PackageManager pm = appContext.getPackageManager();
        Map<String, DiscoveredProvider> providers = new LinkedHashMap<>();
        Set<String> providerPackages = new LinkedHashSet<>();
        try {
            List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_META_DATA);
            for (PackageInfo packageInfo : packages) {
                if (packageInfo == null || packageInfo.applicationInfo == null) continue;
                ApplicationInfo appInfo = packageInfo.applicationInfo;
                if (appInfo.metaData == null) continue;
                String packageName = appInfo.packageName;
                if (appContext.getPackageName().equals(packageName)) continue;
                String authority = appInfo.metaData.getString(SdkConstants.META_PROVIDER_AUTHORITY, "");
                String role = appInfo.metaData.getString(SdkConstants.META_ROLE, "");
                if (authority == null || authority.trim().isEmpty()) continue;
                if (role != null && !role.trim().isEmpty()
                        && !SdkConstants.ROLE_PROVIDER.equalsIgnoreCase(role.trim())) {
                    continue;
                }
                String signature = "";
                try {
                    signature = SdkSignatureUtils.getSignatureSha256(appContext, packageName);
                } catch (Exception ignored) {
                }
                if (requireTrustedProviders
                        && !PandaGenieSdk.isTrustedApp(appContext, packageName, signature, SdkConstants.ROLE_PROVIDER)) {
                    continue;
                }
                CharSequence label = appInfo.loadLabel(pm);
                providers.put(packageName + "@" + authority, new DiscoveredProvider(
                        label == null ? packageName : label.toString(),
                        packageName,
                        "",
                        signature,
                        null,
                        authority.trim()
                ));
                providerPackages.add(packageName);
            }
        } catch (Exception ignored) {
        }

        Intent intent = new Intent(SdkConstants.ACTION_CAPABILITY_SERVICE);
        List<ResolveInfo> services = pm.queryIntentServices(intent, PackageManager.GET_META_DATA);
        for (ResolveInfo info : services) {
            if (info.serviceInfo == null || !info.serviceInfo.exported) continue;
            String packageName = info.serviceInfo.packageName;
            if (appContext.getPackageName().equals(packageName)) continue;
            if (providerPackages.contains(packageName)) continue;
            String serviceName = info.serviceInfo.name;
            String signature = "";
            try {
                signature = SdkSignatureUtils.getSignatureSha256(appContext, packageName);
            } catch (Exception ignored) {
            }
            if (requireTrustedProviders
                    && !PandaGenieSdk.isTrustedApp(appContext, packageName, signature, SdkConstants.ROLE_PROVIDER)) {
                continue;
            }
            CharSequence label = info.serviceInfo.loadLabel(pm);
            if (label == null) label = info.loadLabel(pm);
            providers.put(packageName + "/" + serviceName, new DiscoveredProvider(
                    label == null ? packageName : label.toString(),
                    packageName,
                    serviceName,
                    signature,
                    new ComponentName(packageName, serviceName)
            ));
        }
        return new ArrayList<>(providers.values());
    }

    public void fetchManifest(DiscoveredProvider provider, ManifestCallback callback) {
        send(provider, SdkConstants.MSG_GET_MANIFEST, new Bundle(), new ResultCallback() {
            @Override
            public void onSuccess(Bundle data) {
                callback.onManifest(provider, data.getString(SdkConstants.KEY_MANIFEST_JSON, "{}"));
            }

            @Override
            public void onError(String error) {
                callback.onError(provider, error);
            }
        });
    }

    public void invoke(
            DiscoveredProvider provider,
            String capabilityId,
            JSONObject params,
            String grantToken,
            InvocationCallback callback
    ) {
        Bundle bundle = new Bundle();
        bundle.putString(SdkConstants.KEY_CAPABILITY_ID, capabilityId);
        bundle.putString(SdkConstants.KEY_PARAMS_JSON, params == null ? "{}" : params.toString());
        bundle.putString(SdkConstants.KEY_GRANT_TOKEN, grantToken == null ? "" : grantToken);
        bundle.putString(SdkConstants.KEY_AGENT_PACKAGE, appContext.getPackageName());
        try {
            bundle.putString(SdkConstants.KEY_AGENT_SIGNATURE_SHA256, SdkSignatureUtils.getOwnSignatureSha256(appContext));
        } catch (Exception ignored) {
            bundle.putString(SdkConstants.KEY_AGENT_SIGNATURE_SHA256, "");
        }
        send(provider, SdkConstants.MSG_INVOKE, bundle, new ResultCallback() {
            @Override
            public void onSuccess(Bundle data) {
                callback.onResult(provider, capabilityId, data.getString(SdkConstants.KEY_RESULT_JSON, "{}"));
            }

            @Override
            public void onError(String error) {
                callback.onError(provider, capabilityId, error);
            }
        });
    }

    private void send(DiscoveredProvider provider, int what, Bundle data, ResultCallback callback) {
        if (provider.providerAuthority != null && !provider.providerAuthority.trim().isEmpty()) {
            sendViaProvider(provider, what, data, callback);
            return;
        }
        sendViaService(provider, what, data, callback);
    }

    private void sendViaProvider(DiscoveredProvider provider, int what, Bundle data, ResultCallback callback) {
        new Thread(() -> {
            try {
                String method = what == SdkConstants.MSG_GET_MANIFEST
                        ? SdkConstants.METHOD_GET_MANIFEST
                        : SdkConstants.METHOD_INVOKE;
                Bundle response = appContext.getContentResolver().call(
                        Uri.parse("content://" + provider.providerAuthority),
                        method,
                        null,
                        data == null ? new Bundle() : data
                );
                mainHandler.post(() -> {
                    if (response == null) {
                        callback.onError("SDK provider returned empty response");
                        return;
                    }
                    String error = response.getString(SdkConstants.KEY_ERROR, "");
                    if (error != null && !error.trim().isEmpty()) {
                        callback.onError(error);
                    } else {
                        callback.onSuccess(response);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(
                        e.getMessage() == null ? "SDK provider call failed" : e.getMessage()
                ));
            }
        }, "PandaGenieSdkProviderCall").start();
    }

    private void sendViaService(DiscoveredProvider provider, int what, Bundle data, ResultCallback callback) {
        if (provider.componentName == null) {
            callback.onError("SDK provider has no service endpoint");
            return;
        }
        Intent intent = new Intent(SdkConstants.ACTION_CAPABILITY_SERVICE);
        intent.setComponent(provider.componentName);
        final boolean[] completed = {false};
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Messenger serviceMessenger = new Messenger(service);
                Handler replyHandler = new Handler(Looper.getMainLooper(), msg -> {
                    if (completed[0]) return true;
                    completed[0] = true;
                    safeUnbind(this);
                    if (msg.what == SdkConstants.MSG_RESULT) {
                        callback.onSuccess(msg.getData());
                    } else {
                        callback.onError(msg.getData().getString(SdkConstants.KEY_ERROR, "SDK call failed"));
                    }
                    return true;
                });
                Message message = Message.obtain(null, what);
                message.setData(data == null ? new Bundle() : data);
                message.replyTo = new Messenger(replyHandler);
                try {
                    serviceMessenger.send(message);
                } catch (RemoteException e) {
                    completed[0] = true;
                    safeUnbind(this);
                    callback.onError(e.getMessage() == null ? "remote service unavailable" : e.getMessage());
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                if (!completed[0]) {
                    completed[0] = true;
                    callback.onError("remote service disconnected");
                }
            }
        };
        boolean bound = appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        if (!bound) {
            callback.onError("unable to bind provider service");
            return;
        }
        mainHandler.postDelayed(() -> {
            if (!completed[0]) {
                completed[0] = true;
                safeUnbind(connection);
                callback.onError("SDK request timed out");
            }
        }, requestTimeoutMs);
    }

    private void safeUnbind(ServiceConnection connection) {
        try {
            appContext.unbindService(connection);
        } catch (Exception ignored) {
        }
    }

    private interface ResultCallback {
        void onSuccess(Bundle data);
        void onError(String error);
    }

    public interface ManifestCallback {
        void onManifest(DiscoveredProvider provider, String manifestJson);
        void onError(DiscoveredProvider provider, String error);
    }

    public interface InvocationCallback {
        void onResult(DiscoveredProvider provider, String capabilityId, String resultJson);
        void onError(DiscoveredProvider provider, String capabilityId, String error);
    }
}
