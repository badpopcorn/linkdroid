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

import org.linkdroid.Constants.WebhookColumns;

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
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class WebhooksListActivity extends ListActivity implements
    WebhookColumns {
  private static final String TAG = "WebhooksListActivity";

  private static final int ACTIVITY_CREATE = 1;
  private static final int ACTIVITY_EDIT = 2;

  private Cursor webhooksCursor;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.webhooks_list);

    webhooksCursor = getContentResolver().query(CONTENT_URI, null, null, null,
        null);

    startManagingCursor(webhooksCursor);

    ListAdapter adapter = new SimpleCursorAdapter(this,
        android.R.layout.simple_list_item_2, webhooksCursor, new String[] {
            NAME, URI }, new int[] { android.R.id.text1, android.R.id.text2 });
    setListAdapter(adapter);

    getListView().setOnCreateContextMenuListener(this);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.webhooks_list_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.webhooks_list_menu_new_item:
      // Opens the a blank Edit Webhook activity.
      startActivityForResult(new Intent(this, WebhookEditActivity.class),
          ACTIVITY_CREATE);
      return true;
    case R.id.webhooks_list_menu_view_log_item:
      startActivity(new Intent(this, LogsListActivity.class));
      return true;
    case R.id.webhooks_list_menu_settings_item:
      startActivity(new Intent(this, Settings.class));
      return true;
    case R.id.webhooks_list_menu_about_item:
      startActivity(new Intent(this, About.class));
      return true;
    case R.id.webhooks_list_menu_intentfilters_item:
      startActivity(new Intent(this, IntentFiltersListActivity.class));
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    if (isSendAction()) {
      enqueueWebhookPostAndFinish(getIntent(), position, null);
    } else {
      startWebhookEditActivity(position);
    }
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    MenuInflater inflater = getMenuInflater();
    if (isSendAction()) {
      inflater.inflate(R.menu.webhooks_list_context_menu_send, menu);
    } else {
      inflater.inflate(R.menu.webhooks_list_context_menu, menu);
    }
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
    case R.id.webhooks_list_context_menu_send:
      enqueueWebhookPostAndFinish(getIntent(), info.position, null);
      return true;
    case R.id.webhooks_list_context_menu_send_extra:
      final EditText sendExtraEditText = new EditText(this);
      sendExtraEditText.setSingleLine();
      new AlertDialog.Builder(this).setTitle(
          getString(R.string.webhooks_list_dialog_send_extra_title))
          .setMessage(
              getString(R.string.webhooks_list_dialog_send_extra_message))
          .setView(sendExtraEditText).setPositiveButton(
              getString(R.string.misc_send_button),
              new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                  String value = sendExtraEditText.getText().toString();
                  Intent intent = getIntent();
                  enqueueWebhookPostAndFinish(intent, info.position, value);
                }
              }).setNegativeButton(getString(R.string.misc_cancel_button),
              new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                  Toast.makeText(WebhooksListActivity.this,
                      getString(R.string.misc_canceled), Toast.LENGTH_SHORT)
                      .show();
                }
              }).show();
      return true;
    case R.id.webhooks_list_context_menu_edit_item:
      startWebhookEditActivity(info.position);
      return true;
    case R.id.webhooks_list_context_menu_delete_item:
      new AlertDialog.Builder(this).setTitle(
          getString(R.string.webhooks_list_dialog_delete_title)).setMessage(
          getString(R.string.webhooks_list_dialog_delete_message))
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
    case R.id.webhooks_list_context_menu_set_events_webhook_item:
      try {
        Cursor c = webhooksCursor;
        c.moveToPosition(info.position);
        Settings.setEventsWebhook(this, obtainWebhookBundleFromCursor(c));
        Toast.makeText(WebhooksListActivity.this,
            getString(R.string.misc_saved), Toast.LENGTH_SHORT).show();
      } catch (Exception e) {
        Log.w(TAG, "Error saving events webhook: " + e.toString());
        Toast.makeText(WebhooksListActivity.this,
            getString(R.string.misc_failed_save), Toast.LENGTH_SHORT).show();
      }
      return true;
    }
    return super.onContextItemSelected(item);
  }

  private void enqueueWebhookPostAndFinish(Intent intent, int position,
      String userInput) {
    enqueueWebhookPost(intent, position, userInput);
    Toast.makeText(WebhooksListActivity.this,
        getString(R.string.webhooks_list_sent_toast), Toast.LENGTH_SHORT)
        .show();
    finish();
  }

  private void enqueueWebhookPost(Intent intent, int position, String userInput) {
    Cursor c = webhooksCursor;
    c.moveToPosition(position);
    Bundle webhookBundle = obtainWebhookBundleFromCursor(c);
    Bundle dataBundle = WebhookPostService.newPostDataBundle(intent);
    Intent i = new Intent(this, WebhookPostService.class);
    i.putExtra(WebhookPostService.KEY_WEBHOOK_BUNDLE, webhookBundle);
    i.putExtra(WebhookPostService.KEY_DATA_BUNDLE, dataBundle);
    if (userInput != null) {
      i.putExtra(WebhookPostService.KEY_USER_INPUT, userInput);
    }
    startService(i);
  }

  protected static Bundle obtainWebhookBundleFromCursor(Cursor c) {
    Bundle bundle = new Bundle();
    bundle.putLong(_ID, c.getLong(c.getColumnIndexOrThrow(_ID)));
    bundle.putString(NAME, c.getString(c.getColumnIndexOrThrow(NAME)));
    bundle.putString(URI, c.getString(c.getColumnIndexOrThrow(URI)));
    bundle.putString(SECRET, c.getString(c.getColumnIndexOrThrow(SECRET)));
    bundle
        .putInt(NONCE_RANDOM, c.getInt(c.getColumnIndexOrThrow(NONCE_RANDOM)));
    bundle.putInt(NONCE_TIMESTAMP, c.getInt(c
        .getColumnIndexOrThrow(NONCE_TIMESTAMP)));
    return bundle;
  }

  protected void startWebhookEditActivity(int position) {
    Cursor c = webhooksCursor;
    c.moveToPosition(position);
    Intent intent = new Intent(this, WebhookEditActivity.class);
    intent.putExtras(obtainWebhookBundleFromCursor(c));
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
            // We have returned from the Edit dialog, and we now do a brand new
            // insert of this webhook.
            ContentValues values = WebhooksProvider
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
            // We have returned from the Edit dialog, and we now do an update
            // of this webhook.
            Long id = extras.getLong(_ID);
            if (id != null) {
              Uri uri = ContentUris.withAppendedId(CONTENT_URI, id);
              ContentValues values = WebhooksProvider
                  .populateContentValues(extras);
              getContentResolver().update(uri, values, null, null);
            }
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

  private boolean isSendAction() {
    Intent intent = getIntent();
    String action = intent.getAction();
    return action != null && Intent.ACTION_SEND.compareTo(action) == 0;
  }
}