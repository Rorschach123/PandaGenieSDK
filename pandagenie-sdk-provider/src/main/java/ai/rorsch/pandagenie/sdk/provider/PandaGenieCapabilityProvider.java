package ai.rorsch.pandagenie.sdk.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import ai.rorsch.pandagenie.sdk.core.PandaGenieSdk;
import ai.rorsch.pandagenie.sdk.core.SdkConstants;

import org.json.JSONObject;

public abstract class PandaGenieCapabilityProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        if (getContext() != null) {
            PandaGenieSdk.initializeIfNeeded(
                    getContext(),
                    SdkConstants.ROLE_PROVIDER,
                    SdkConstants.ROLE_AGENT
            );
        }
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Bundle out = new Bundle();
        try {
            if (SdkConstants.METHOD_GET_MANIFEST.equals(method)) {
                out.putString(SdkConstants.KEY_MANIFEST_JSON, getManifestJson());
                return out;
            }
            if (SdkConstants.METHOD_INVOKE.equals(method)) {
                Bundle data = extras == null ? new Bundle() : extras;
                String capabilityId = data.getString(SdkConstants.KEY_CAPABILITY_ID, "");
                String paramsJson = data.getString(SdkConstants.KEY_PARAMS_JSON, "{}");
                if (paramsJson == null || paramsJson.trim().isEmpty()) {
                    paramsJson = "{}";
                }
                JSONObject params = new JSONObject(paramsJson);
                PandaGenieCapabilityService.InvokeContext context =
                        new PandaGenieCapabilityService.InvokeContext(
                                data.getString(SdkConstants.KEY_GRANT_TOKEN, ""),
                                data.getString(SdkConstants.KEY_AGENT_PACKAGE, ""),
                                data.getString(SdkConstants.KEY_AGENT_SIGNATURE_SHA256, "")
                        );
                if (!shouldAcceptInvoke(context)) {
                    out.putString(SdkConstants.KEY_ERROR, "caller not authorized");
                    return out;
                }
                CapabilityResult result = onInvoke(capabilityId, params, context);
                if (result == null) {
                    result = CapabilityResult.fail("empty capability result");
                }
                out.putString(SdkConstants.KEY_RESULT_JSON, result.toJsonString());
                if (!result.isSuccess()) {
                    out.putString(SdkConstants.KEY_ERROR, result.getError());
                }
                return out;
            }
            out.putString(SdkConstants.KEY_ERROR, "unsupported SDK provider method: " + method);
        } catch (Exception e) {
            out.putString(
                    SdkConstants.KEY_ERROR,
                    e.getMessage() == null ? "SDK provider call failed" : e.getMessage()
            );
        }
        return out;
    }

    protected abstract String getManifestJson();

    protected abstract CapabilityResult onInvoke(
            String capabilityId,
            JSONObject params,
            PandaGenieCapabilityService.InvokeContext context
    );

    protected boolean shouldAcceptInvoke(PandaGenieCapabilityService.InvokeContext context) {
        if (!shouldRequireTrustedAgent()) {
            return true;
        }
        return getContext() != null && PandaGenieSdk.isTrustedApp(
                getContext(),
                context.agentPackageName,
                context.agentSignatureSha256,
                SdkConstants.ROLE_AGENT
        );
    }

    protected boolean shouldRequireTrustedAgent() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
