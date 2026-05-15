package ai.rorsch.pandagenie.sdkdemo.provider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class DemoBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String message = intent == null ? "ping" : intent.getStringExtra("message");
        String status = intent == null ? "" : intent.getStringExtra("status");
        String prefix = status == null || status.trim().isEmpty() ? "" : status + " - ";
        Toast.makeText(context, "PandaGenie SDK: " + prefix + (message == null ? "ping" : message), Toast.LENGTH_SHORT).show();
    }
}
