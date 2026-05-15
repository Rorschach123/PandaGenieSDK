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
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"_id", "name", "value"});
        cursor.addRow(new Object[]{1, "activity", "open_demo_activity"});
        cursor.addRow(new Object[]{2, "activity_detail", "open_detail_page_with_message"});
        cursor.addRow(new Object[]{3, "service", "echo_and_mutation"});
        cursor.addRow(new Object[]{4, "provider", "query_and_search_rows"});
        cursor.addRow(new Object[]{5, "note", "save_and_read_local_note"});
        cursor.addRow(new Object[]{6, "broadcast", "demo_ping"});
        cursor.addRow(new Object[]{7, "status_broadcast", "send_status_event"});
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.dir/vnd.pandagenie.sdkdemo.item";
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
