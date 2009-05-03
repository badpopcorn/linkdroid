/*
 * Copyright 2009 BadPopcorn, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.linkdroid;

import org.linkdroid.Constants.LogColumns;

import android.app.ListActivity;
import android.app.NotificationManager;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

public class LogsListActivity extends ListActivity implements LogColumns {
  private static final String TAG = "LogsListActivity";

  private Cursor logsCursor;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.logs_list);

    logsCursor = getContentResolver()
        .query(CONTENT_URI, null, null, null, null);

    startManagingCursor(logsCursor);

    ListAdapter adapter = new SimpleCursorAdapter(this,
        android.R.layout.two_line_list_item, logsCursor, new String[] {
            CREATED_AT, MESSAGE }, new int[] { android.R.id.text1,
            android.R.id.text2 });
    setListAdapter(adapter);

    getListView().setOnCreateContextMenuListener(this);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.logs_list_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.logs_list_menu_clear_item:
      Log.i(TAG, "Clearing log");
      getContentResolver().delete(CONTENT_URI, null, null);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onResume() {
    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    manager.cancel(R.layout.logs_list);
    super.onResume();
  }

}
