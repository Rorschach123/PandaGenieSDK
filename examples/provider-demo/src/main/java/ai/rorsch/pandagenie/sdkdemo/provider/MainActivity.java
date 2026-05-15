package ai.rorsch.pandagenie.sdkdemo.provider;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import ai.rorsch.pandagenie.sdk.core.PandaGenieSdk;

public class MainActivity extends Activity {
    private TextView titleView;
    private TextView messageView;
    private TextView sourceView;
    private TextView detailView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PandaGenieSdk.initializeIfNeeded(this, PandaGenieSdk.Role.PROVIDER);
        setContentView(createContentView());
        render(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        render(intent);
    }

    private LinearLayout createContentView() {
        float density = getResources().getDisplayMetrics().density;
        int padding = (int) (24 * density);
        int gap = (int) (14 * density);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(padding, padding, padding, padding);
        root.setBackgroundColor(0xFFF6FAF7);

        TextView header = text("PandaGenie 能力演示", 22f, 0xFF10352B, true);
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        titleView = text("", 20f, 0xFF14231F, true);
        messageView = text("", 17f, 0xFF3D4C47, false);
        sourceView = text("", 14f, 0xFF668078, false);
        detailView = text("", 14f, 0xFF668078, false);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(padding, padding, padding, padding);
        card.setBackgroundColor(0xFFFFFFFF);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = gap * 2;
        root.addView(card, cardParams);

        card.addView(titleView, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(-1, -2);
        messageParams.topMargin = gap;
        card.addView(messageView, messageParams);
        LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(-1, -2);
        sourceParams.topMargin = gap;
        card.addView(sourceView, sourceParams);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = gap / 2;
        card.addView(detailView, detailParams);

        TextView footer = text(
                "已导出示例能力：\n"
                        + "· 打开页面 demo.open_activity / demo.open_detail_page\n"
                        + "· 读取 Provider demo.provider_rows / demo.provider_search\n"
                        + "· 保存和读取本地便签 demo.save_note / demo.read_note\n"
                        + "· 发送广播 demo.broadcast_ping / demo.broadcast_status\n"
                        + "· 执行后台任务 demo.long_task",
                14f,
                0xFF6D7D78,
                false
        );
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = gap * 2;
        root.addView(footer, footerParams);
        return root;
    }

    private void render(Intent intent) {
        String title = intent == null ? "" : intent.getStringExtra("title");
        String message = intent == null ? "" : intent.getStringExtra("message");
        String source = intent == null ? "" : intent.getStringExtra("source");
        String detailId = intent == null ? "" : intent.getStringExtra("detailId");
        titleView.setText(emptyToDefault(title, "等待 SDK 调用"));
        messageView.setText(emptyToDefault(
                message,
                "通过 PandaGenie 或其他 AI 助手调用 demo.open_activity 后，这里会展示调用方传入的文字。"
        ));
        sourceView.setText("来源应用：" + emptyToDefault(source, "本地启动"));
        detailView.setText(detailId == null || detailId.trim().isEmpty() ? "" : "详情 ID：" + detailId);
    }

    private TextView text(String value, float sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(0f, 1.15f);
        if (bold) view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        return view;
    }

    private static String emptyToDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
