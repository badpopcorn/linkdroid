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

import org.apache.commons.lang.StringUtils;
import org.linkdroid.Constants.IntentFilterColumns;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class BroadcastReceiverService extends Service implements
    IntentFilterColumns {
  private static final String TAG = "BroadcastReceiverService";

  private static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

  private final BroadcastReceiver smsReceiver = new SmsReceiver();

  private final BroadcastReceiver receiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent originator) {
      LogsService.logEventMessage(context, ""
          + getString(R.string.broadcastreceiverservice_received_intent_prefix)
          + originator.getAction());

      Bundle webhookBundle = Settings.getEventsWebhook(context);
      Bundle dataBundle = WebhookPostService.newPostDataBundle(originator);
      Intent intent = new Intent(context, WebhookPostService.class);
      intent.putExtra(WebhookPostService.KEY_LOG_LEVEL, Settings
          .getEventsLogLevel(context));
      intent.putExtra(WebhookPostService.KEY_WEBHOOK_BUNDLE, webhookBundle);
      intent.putExtra(WebhookPostService.KEY_DATA_BUNDLE, dataBundle);
      context.startService(intent);
    }
  };

  private ContentObserver contentObserver;
  private Cursor intentFiltersCursor;

  @Override
  public IBinder onBind(Intent arg0) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    if (!Settings.isEventsEnabled(this)) {
      Log.d(TAG, "Service created but events is disabled.");
      stopSelf();
      return;
    }
    try {
      Log.d(TAG, "Creating service");

      LogsService.logSystemDebugMessage(this, ""
          + getString(R.string.broadcastreceiverservice_create));

      contentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
          super.onChange(selfChange);
          Intent i = new Intent(BroadcastReceiverService.this,
              BroadcastReceiverService.class);
          startService(i);
        }
      };

      intentFiltersCursor = getContentResolver().query(CONTENT_URI, null, null,
          null, null);
      intentFiltersCursor.registerContentObserver(contentObserver);
    } catch (Exception e) {
      LogsService.logSystemDebugMessage(this, e.toString());
    }
  }

  @Override
  public void onStart(Intent intent, int startId) {
    if (!Settings.isEventsEnabled(this)) {
      Log.d(TAG, "Service started but events is disabled.");
      stopSelf();
      return;
    }
    LogsService.logSystemDebugMessage(this, ""
        + getString(R.string.broadcastreceiverservice_start));

    try {
      unregisterReceiver(receiver);
      Log.d(TAG, "unregistered receiver for service start");
    } catch (Exception e) {
      // Do Nothing
    }
    try {
      unregisterReceiver(smsReceiver);
      Log.d(TAG, "Unregistered SmsReceiver for service start");
    } catch (Exception e) {
      // Do nothing.
    }

    if (Settings.isEventsSmsEnabled(this)) {
      try {
        enableSmsReceiver();
      } catch (Exception e) {
        Log.w(TAG, e.toString());
        LogsService.logSystemDebugMessage(this, e.toString());
      }
    }

    Cursor c = intentFiltersCursor;
    c.requery();
    if (c.moveToFirst()) {
      do {
        try {
          enableIntentFilterAtCursor(c);
        } catch (Exception e) {
          Log.w(TAG, e.toString());
          LogsService.logSystemDebugMessage(this, e.toString());
        }
      } while (intentFiltersCursor.moveToNext());
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    LogsService.logSystemDebugMessage(this, ""
        + getString(R.string.broadcastreceiverservice_stop));
    try {
      intentFiltersCursor.close();
    } catch (Exception e) {
      Log.w(TAG, "Problem closing cursor: " + e.toString());
      LogsService.logSystemDebugMessage(this, e.toString());
    }
    try {
      unregisterReceiver(smsReceiver);
      Log.d(TAG, "Unregistered SmsReceiver");
    } catch (Exception e) {
      // Do nothing.
    }
    try {
      unregisterReceiver(receiver);
      Log.d(TAG, "Unregistered BroadcastReceiverService");
    } catch (Exception e) {
      // Do nothing
    }
  }

  private void enableIntentFilterAtCursor(Cursor c)
      throws MalformedMimeTypeException {
    final IntentFilter intentFilter = new IntentFilter();
    final String intentStr = c.getString(c.getColumnIndexOrThrow(ACTION));
    intentFilter.addAction(intentStr);
    final String categoryStr = c.getString(c.getColumnIndexOrThrow(CATEGORY));
    if (StringUtils.isBlank(categoryStr)) {
      intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
    } else {
      intentFilter.addCategory(categoryStr);
    }
    final String dataAuthHostStr = c.getString(c
        .getColumnIndexOrThrow(DATA_AUTHORITY_HOST));
    final String dataAuthPortStr = c.getString(c
        .getColumnIndexOrThrow(DATA_AUTHORITY_PORT));
    if (!StringUtils.isBlank(dataAuthHostStr)) {
      intentFilter.addDataAuthority(dataAuthHostStr, dataAuthPortStr);
    }
    final String dataSchemeStr = c.getString(c
        .getColumnIndexOrThrow(DATA_SCHEME));
    if (!StringUtils.isBlank(dataSchemeStr)) {
      intentFilter.addDataScheme(dataSchemeStr);
    }
    final String dataTypeStr = c.getString(c.getColumnIndexOrThrow(DATA_TYPE));
    if (!StringUtils.isBlank(dataTypeStr)) {
      intentFilter.addDataType(dataTypeStr);
    }
    registerReceiver(receiver, intentFilter);
    Log.d(TAG, "registered intent filter " + intentStr);
  }

  private void enableSmsReceiver() {
    final IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(ACTION_SMS_RECEIVED);
    intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
    registerReceiver(smsReceiver, intentFilter);
    Log.d(TAG, "registered SMS Receiver");
  }
}