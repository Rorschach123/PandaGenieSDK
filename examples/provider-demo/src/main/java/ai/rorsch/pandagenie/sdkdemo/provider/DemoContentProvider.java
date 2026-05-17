package ai.rorsch.pandagenie.sdkdemo.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class DemoContentProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder
    ) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"_id", "name", "value", "description"});
        String text = MainActivity.readDemoText(getContext());
        cursor.addRow(new Object[]{1, "app_name", "SDK调用演示", "应用名称"});
        cursor.addRow(new Object[]{2, "demo_text", text, "当前展示文字"});
        cursor.addRow(new Object[]{
                3,
                "updated_at",
                MainActivity.formatTime(MainActivity.readUpdatedAt(getContext())),
                "最后更新时间"
        });
        cursor.addRow(new Object[]{4, "demo.open_text_page", "打开文字展示页", "打开 App 并展示当前文字或传入文字"});
        cursor.addRow(new Object[]{5, "demo.get_text", "获取演示文字", "读取当前展示文字"});
        cursor.addRow(new Object[]{6, "demo.update_text", "修改演示文字", "修改当前展示文字"});
        cursor.addRow(new Object[]{7, "provider", "query_and_search_rows", "Provider 示例数据"});
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.dir/vnd.ai.rorsch.pandagenie.sdkdemo.provider.item";
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
