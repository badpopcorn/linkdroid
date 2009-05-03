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

import org.linkdroid.Constants.IntentFilterColumns;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class IntentFiltersListActivity extends ListActivity implements
    IntentFilterColumns {
  private static final String TAG = "IntentFiltersListActivity";

  private static final int ACTIVITY_CREATE = 1;
  private static final int ACTIVITY_EDIT = 2;
  private static final int WEBHOOK_ACTIVITY_EDIT = 3;

  private Cursor intentFiltersCursor;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.intentfilters_list);

    intentFiltersCursor = getContentResolver().query(CONTENT_URI, null, null,
        null, null);

    startManagingCursor(intentFiltersCursor);

    ListAdapter adapter = new SimpleCursorAdapter(this,
        android.R.layout.simple_list_item_1, intentFiltersCursor,
        new String[] { ACTION }, new int[] { android.R.id.text1 });
    setListAdapter(adapter);

    getListView().setOnCreateContextMenuListener(this);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.intentfilters_list_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.intentfilters_list_menu_new_item:
      startActivityForResult(new Intent(this, IntentFilterEditActivity.class),
          ACTIVITY_CREATE);
      return true;
    case R.id.intentfilters_list_menu_event_webhook_item:
      Intent intent = new Intent(this, WebhookEditActivity.class);
      Bundle extras = Settings.getEventsWebhook(this);
      intent.putExtras(extras);
      startActivityForResult(intent, WEBHOOK_ACTIVITY_EDIT);
      return true;
    case R.id.intentfilters_list_menu_about_item:
      startActivity(new Intent(this, AboutIntentFilters.class));
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    startIntentFilterEditActivity(position);
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.intentfilters_list_context_menu, menu);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    final AdapterContextMenuInfo info;
    try {
      info = (AdapterContextMenuInfo) item.getMenuInfo();
    } catch (ClassCastException e) {
      Log.e(TAG, "bad MenuInfo", e);
      return false;
    }
    switch (item.getItemId()) {
    case R.id.intentfilters_list_context_menu_edit_item:
      startIntentFilterEditActivity(info.position);
      return true;
    case R.id.intentfilters_list_context_menu_delete_item:
      new AlertDialog.Builder(this).setTitle(
          getString(R.string.intentfilters_list_dialog_delete_title))
          .setMessage(
              getString(R.string.intentfilters_list_dialog_delete_message))
          .setPositiveButton(getString(R.string.misc_yes_button),
              new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                  Uri webhookUri = ContentUris.withAppendedId(CONTENT_URI,
                      info.id);
                  getContentResolver().delete(webhookUri, null, null);
                }
              }).setNegativeButton(getString(R.string.misc_no_button),
              new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
              }).show();
      return true;
    }
    return super.onContextItemSelected(item);
  }

  protected static Bundle obtainIntentFilterBundleFromCursor(Cursor c) {
    Bundle bundle = new Bundle();
    bundle.putLong(_ID, c.getLong(c.getColumnIndexOrThrow(_ID)));
    bundle.putString(ACTION, c.getString(c.getColumnIndexOrThrow(ACTION)));
    bundle.putString(CATEGORY, c.getString(c.getColumnIndexOrThrow(CATEGORY)));
    bundle.putString(DATA_AUTHORITY_HOST, c.getString(c
        .getColumnIndexOrThrow(DATA_AUTHORITY_HOST)));
    bundle.putString(DATA_AUTHORITY_PORT, c.getString(c
        .getColumnIndexOrThrow(DATA_AUTHORITY_PORT)));
    bundle.putString(DATA_SCHEME, c.getString(c
        .getColumnIndexOrThrow(DATA_SCHEME)));
    bundle
        .putString(DATA_TYPE, c.getString(c.getColumnIndexOrThrow(DATA_TYPE)));
    return bundle;
  }

  protected void startIntentFilterEditActivity(int position) {
    Cursor c = intentFiltersCursor;
    c.moveToPosition(position);
    Intent intent = new Intent(this, IntentFilterEditActivity.class);
    intent.putExtras(obtainIntentFilterBundleFromCursor(c));
    startActivityForResult(intent, ACTIVITY_EDIT);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (intent != null) {
      Bundle extras = intent.getExtras();
      switch (requestCode) {
      case ACTIVITY_CREATE:
        if (resultCode == RESULT_OK) {
          try {
            ContentValues values = IntentFilterProvider
                .populateContentValues(extras);
            getContentResolver().insert(CONTENT_URI, values);
            toastSaved(this);
          } catch (Exception e) {
            toastFailedSave(this);
          }
        }
        break;
      case ACTIVITY_EDIT:
        if (resultCode == RESULT_OK) {
          try {
            Long id = extras.getLong(_ID);
            if (id != null) {
              Uri uri = ContentUris.withAppendedId(CONTENT_URI, id);
              ContentValues values = IntentFilterProvider
                  .populateContentValues(extras);
              getContentResolver().update(uri, values, null, null);
            }
            toastSaved(this);
          } catch (Exception e) {
            toastFailedSave(this);
          }
        }
        break;
      case WEBHOOK_ACTIVITY_EDIT:
        if (resultCode == RESULT_OK) {
          try {
            Settings.setEventsWebhook(this, extras);
            toastSaved(this);
          } catch (Exception e) {
            toastFailedSave(this);
          }
        }
        break;
      }
    }
  }

  private static void toastSaved(Context context) {
    Toast.makeText(context, context.getString(R.string.misc_saved),
        Toast.LENGTH_SHORT).show();
  }

  private static void toastFailedSave(Context context) {
    Toast.makeText(context, context.getString(R.string.misc_failed_save),
        Toast.LENGTH_SHORT).show();
  }
}
