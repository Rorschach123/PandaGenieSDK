package ai.rorsch.pandagenie.sdkdemo.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import ai.rorsch.pandagenie.sdk.core.PandaGenieSdk;
import ai.rorsch.pandagenie.sdk.core.SdkConstants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    public static final String APP_NAME = "SDK调用演示";
    public static final String DEFAULT_TEXT =
            "这是一段来自 SDK 调用演示 App 的文字。PandaGenie 可以打开页面查看它，也可以通过 SDK 接口读取或修改它。";

    private static final String PREFS = "sdk_call_demo";
    private static final String KEY_TEXT = "demo_text";
    private static final String KEY_UPDATED_AT = "updated_at";

    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PandaGenieSdk.initializeIfNeeded(this, SdkConstants.ROLE_PROVIDER, SdkConstants.ROLE_AGENT);
        render(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        render(intent);
    }

    private void render(Intent intent) {
        String incomingText = firstNonEmpty(
                stringExtra(intent, "text"),
                stringExtra(intent, "note"),
                stringExtra(intent, "message"),
                stringExtra(intent, "content")
        );
        if (!TextUtils.isEmpty(incomingText)) {
            saveDemoText(this, incomingText);
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(247, 250, 248));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(28), dp(24), dp(28));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView title = text(APP_NAME, 30, Color.rgb(19, 31, 28), true);
        root.addView(title);
        root.addView(text(
                "这是一个被 PandaGenie 调用的示例 App，用来验证 SDK 的打开、读取和修改能力。",
                15,
                Color.rgb(92, 108, 102),
                false
        ));

        addCard("当前展示文字", readDemoText(this), "最后更新：" + formatTime(readUpdatedAt(this)));
        addCard("可验证能力", "1. 打开 App 展示这段文字\n2. 通过 SDK 接口读取这段文字\n3. 通过 SDK 接口修改这段文字", "");
        addCard("最近一次调用", buildCallSummary(intent), "");

        setContentView(scrollView);
    }

    private String buildCallSummary(Intent intent) {
        if (intent == null || intent.getExtras() == null) {
            return "直接打开应用";
        }
        String title = firstNonEmpty(stringExtra(intent, "title"), "PandaGenie 调用");
        String source = firstNonEmpty(stringExtra(intent, "source"), "PandaGenie");
        String detailId = firstNonEmpty(stringExtra(intent, "detailId"), "");
        StringBuilder builder = new StringBuilder();
        builder.append(title).append("\n来源：").append(source);
        if (!TextUtils.isEmpty(detailId)) {
            builder.append("\n调用 ID：").append(detailId);
        }
        return builder.toString();
    }

    private void addCard(String title, String content, String footer) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), Color.rgb(223, 232, 228));
        card.setBackground(bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(18);

        card.addView(text(title, 19, Color.rgb(20, 33, 30), true));
        TextView contentView = text(content, 17, Color.rgb(37, 52, 48), false);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        contentParams.topMargin = dp(12);
        card.addView(contentView, contentParams);

        if (!TextUtils.isEmpty(footer)) {
            TextView footerView = text(footer, 13, Color.rgb(108, 124, 118), false);
            LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            footerParams.topMargin = dp(14);
            card.addView(footerView, footerParams);
        }
        root.addView(card, params);
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value == null ? "" : value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(dp(2), 1.0f);
        view.setGravity(Gravity.START);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    public static String readDemoText(Context context) {
        if (context == null) {
            return DEFAULT_TEXT;
        }
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_TEXT, DEFAULT_TEXT);
    }

    public static long readUpdatedAt(Context context) {
        if (context == null) {
            return 0L;
        }
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getLong(KEY_UPDATED_AT, 0L);
    }

    public static void saveDemoText(Context context, String value) {
        if (context == null || TextUtils.isEmpty(value)) {
            return;
        }
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        editor.putString(KEY_TEXT, value);
        editor.putLong(KEY_UPDATED_AT, System.currentTimeMillis());
        editor.apply();
    }

    public static String formatTime(long timestamp) {
        if (timestamp <= 0L) {
            return "尚未修改";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date(timestamp));
    }

    private String stringExtra(Intent intent, String key) {
        return intent == null ? "" : intent.getStringExtra(key);
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return "";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
