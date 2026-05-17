package ai.rorsch.pandagenie.sdkdemo.provider;

import android.content.Context;

import ai.rorsch.pandagenie.sdk.provider.CapabilityResult;
import ai.rorsch.pandagenie.sdk.provider.PandaGenieCapabilityProvider;
import ai.rorsch.pandagenie.sdk.provider.PandaGenieCapabilityService;

import org.json.JSONObject;

public class DemoSdkCapabilityProvider extends PandaGenieCapabilityProvider {
    @Override
    protected String getManifestJson() {
        Context context = getContext();
        String packageName = context == null
                ? "ai.rorsch.pandagenie.sdkdemo.provider"
                : context.getPackageName();
        return DemoCapabilitySupport.buildManifest(packageName);
    }

    @Override
    protected CapabilityResult onInvoke(
            String capabilityId,
            JSONObject params,
            PandaGenieCapabilityService.InvokeContext context
    ) {
        Context app = getContext();
        String agentPackage = context == null ? "" : context.agentPackageName;
        return DemoCapabilitySupport.invoke(app, capabilityId, params, agentPackage);
    }

    @Override
    protected boolean shouldRequireTrustedAgent() {
        return false;
    }
}
