package ai.rorsch.pandagenie.sdkdemo.provider;

import ai.rorsch.pandagenie.sdk.provider.CapabilityResult;
import ai.rorsch.pandagenie.sdk.provider.PandaGenieCapabilityService;

import org.json.JSONObject;

public class DemoCapabilityService extends PandaGenieCapabilityService {
    @Override
    protected String getManifestJson() {
        return DemoCapabilitySupport.buildManifest(getPackageName());
    }

    @Override
    protected CapabilityResult onInvoke(String capabilityId, JSONObject params, InvokeContext context) {
        String agentPackage = context == null ? "" : context.agentPackageName;
        return DemoCapabilitySupport.invoke(this, capabilityId, params, agentPackage);
    }

    @Override
    protected boolean shouldRequireTrustedAgent() {
        return false;
    }
}
