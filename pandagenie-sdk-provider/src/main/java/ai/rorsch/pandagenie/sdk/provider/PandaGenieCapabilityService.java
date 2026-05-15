package ai.rorsch.pandagenie.sdk.provider;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import ai.rorsch.pandagenie.sdk.core.SdkConstants;
import ai.rorsch.pandagenie.sdk.core.PandaGenieSdk;

import org.json.JSONObject;

public abstract class PandaGenieCapabilityService extends Service {
    private final Messenger messenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        if (intent == null || !SdkConstants.ACTION_CAPABILITY_SERVICE.equals(intent.getAction())) {
            return null;
        }
        PandaGenieSdk.initializeIfNeeded(this, SdkConstants.ROLE_PROVIDER, SdkConstants.ROLE_AGENT);
        return messenger.getBinder();
    }

    protected abstract String getManifestJson();

    protected abstract CapabilityResult onInvoke(String capabilityId, JSONObject params, InvokeContext context);

    protected boolean shouldAcceptInvoke(InvokeContext context) {
        if (!shouldRequireTrustedAgent()) return true;
        return PandaGenieSdk.isTrustedApp(
                this,
                context.agentPackageName,
                context.agentSignatureSha256,
                SdkConstants.ROLE_AGENT
        );
    }

    protected boolean shouldRequireTrustedAgent() {
        return true;
    }

    private final class IncomingHandler extends Handler {
        IncomingHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == SdkConstants.MSG_GET_MANIFEST) {
                Bundle bundle = new Bundle();
                bundle.putString(SdkConstants.KEY_MANIFEST_JSON, getManifestJson());
                reply(msg, SdkConstants.MSG_RESULT, bundle);
                return;
            }
            if (msg.what == SdkConstants.MSG_INVOKE) {
                handleInvoke(msg);
                return;
            }
            super.handleMessage(msg);
        }
    }

    private void handleInvoke(Message msg) {
        Bundle data = msg.getData();
        String capabilityId = data.getString(SdkConstants.KEY_CAPABILITY_ID, "");
        String paramsJson = data.getString(SdkConstants.KEY_PARAMS_JSON, "{}");
        InvokeContext context = new InvokeContext(
                data.getString(SdkConstants.KEY_GRANT_TOKEN, ""),
                data.getString(SdkConstants.KEY_AGENT_PACKAGE, ""),
                data.getString(SdkConstants.KEY_AGENT_SIGNATURE_SHA256, "")
        );
        if (!shouldAcceptInvoke(context)) {
            Bundle error = new Bundle();
            error.putString(SdkConstants.KEY_ERROR, "caller not authorized");
            reply(msg, SdkConstants.MSG_ERROR, error);
            return;
        }
        try {
            CapabilityResult result = onInvoke(capabilityId, new JSONObject(paramsJson), context);
            Bundle bundle = new Bundle();
            bundle.putString(SdkConstants.KEY_RESULT_JSON, result.toJsonString());
            if (result.isSuccess()) {
                reply(msg, SdkConstants.MSG_RESULT, bundle);
            } else {
                bundle.putString(SdkConstants.KEY_ERROR, result.getError());
                reply(msg, SdkConstants.MSG_ERROR, bundle);
            }
        } catch (Exception e) {
            Bundle bundle = new Bundle();
            bundle.putString(SdkConstants.KEY_ERROR, e.getMessage() == null ? "invoke failed" : e.getMessage());
            reply(msg, SdkConstants.MSG_ERROR, bundle);
        }
    }

    private void reply(Message request, int what, Bundle data) {
        Messenger replyTo = request.replyTo;
        if (replyTo == null) return;
        Message response = Message.obtain(null, what);
        response.setData(data == null ? new Bundle() : data);
        try {
            replyTo.send(response);
        } catch (RemoteException ignored) {
        }
    }

    public static final class InvokeContext {
        public final String grantToken;
        public final String agentPackageName;
        public final String agentSignatureSha256;

        InvokeContext(String grantToken, String agentPackageName, String agentSignatureSha256) {
            this.grantToken = grantToken;
            this.agentPackageName = agentPackageName;
            this.agentSignatureSha256 = agentSignatureSha256;
        }
    }
}
