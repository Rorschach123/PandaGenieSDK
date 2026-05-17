package ai.rorsch.pandagenie.sdk.agent;

import android.content.ComponentName;

public final class DiscoveredProvider {
    public final String appName;
    public final String packageName;
    public final String serviceName;
    public final String signatureSha256;
    public final ComponentName componentName;
    public final String providerAuthority;

    public DiscoveredProvider(
            String appName,
            String packageName,
            String serviceName,
            String signatureSha256,
            ComponentName componentName
    ) {
        this(appName, packageName, serviceName, signatureSha256, componentName, "");
    }

    public DiscoveredProvider(
            String appName,
            String packageName,
            String serviceName,
            String signatureSha256,
            ComponentName componentName,
            String providerAuthority
    ) {
        this.appName = appName;
        this.packageName = packageName;
        this.serviceName = serviceName;
        this.signatureSha256 = signatureSha256;
        this.componentName = componentName;
        this.providerAuthority = providerAuthority == null ? "" : providerAuthority;
    }
}
