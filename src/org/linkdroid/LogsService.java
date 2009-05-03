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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class LogsService extends Service {
  private static final String TAG = "LogsService";

  public static final String KEY_LEVEL = "level";
  public static final String KEY_MESSAGE = "message";

  public static final int LEVEL_NONE = -1;
  public static final int LEVEL_DEBUG = 0;
  public static final int LEVEL_INFO = 1;
  public static final int LEVEL_WARNING = 2;
  public static final int LEVEL_ERROR = 3;

  private final class ServiceHandler extends Handler {
    public ServiceHandler(Looper looper) {
      super(looper);
    }

    @Override
    public void handleMessage(Message msg) {
      try {
        Intent intent = (Intent) msg.obj;
        Bundle extras = intent.getExtras();

        final String message = extras.getString(KEY_MESSAGE);
        final int logLevel = extras.getInt(KEY_LEVEL, 0);

        // Save the log message to the database
        ContentValues values = new ContentValues();
        values.put(LogColumns.MESSAGE, message);
        values.put(LogColumns.LEVEL, logLevel);
        getContentResolver().insert(LogColumns.CONTENT_URI, values);

        // Now we do notifications on if necessary
        if (Settings.isNotificationEnabled(LogsService.this)
            && logLevel >= Settings.getNotificationLogLevel(LogsService.this)) {
          NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
          Notification notification = new Notification(
              android.R.drawable.stat_sys_upload_done, null, System
                  .currentTimeMillis());

          // Set our sound if we have it.
          String sound = Settings.getNotificationSound(LogsService.this);
          if (sound != null && sound.trim().length() > 0) {
            notification.sound = Uri.parse(sound);
          }

          // Set vibration on if needed
          if (Settings.isNotificationVibrate(LogsService.this)) {
            notification.defaults = Notification.DEFAULT_VIBRATE;
          }

          // Set activity to launch when user clicks notification
          PendingIntent contentIntent = PendingIntent.getActivity(
              LogsService.this, 0, new Intent(LogsService.this,
                  LogsListActivity.class), 0);
          // Set the info for the views that show in the notification panel.
          notification.setLatestEventInfo(LogsService.this, LogsService.this
              .getString(R.string.notification_title), message, contentIntent);
          manager.notify(R.layout.logs_list, notification);
        }
      } catch (Exception e) {
        Log.e(TAG, e.toString());
      }
      stopSelf(msg.arg1);
    }
  }

  private volatile ServiceHandler serviceHandler;
  private volatile Looper serviceLooper;

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    HandlerThread thread = new HandlerThread(TAG);
    thread.start();

    serviceLooper = thread.getLooper();
    serviceHandler = new ServiceHandler(serviceLooper);
  }

  @Override
  public void onDestroy() {
    serviceLooper.quit();
  }

  @Override
  public void onStart(Intent intent, int startId) {
    Message msg = serviceHandler.obtainMessage();
    msg.arg1 = startId;
    msg.obj = intent;
    serviceHandler.sendMessage(msg);
  }

  public static void logMessage(Context context, int level, String message) {
    Log.d(TAG, "log: " + message);
    Intent i = new Intent(context, LogsService.class);
    i.putExtra(LogsService.KEY_LEVEL, level);
    i.putExtra(LogsService.KEY_MESSAGE, message);
    context.startService(i);
  }

  public static void logEventMessage(Context context, String message) {
    final int level = Settings.getEventsLogLevel(context);
    if (level > LEVEL_NONE) {
      logMessage(context, level, message);
    }
  }

  public static void logSystemDebugMessage(Context context, String message) {
    Log.d(TAG, "log: " + message);
    if (Settings.isLoggingVerbose(context)) {
      Intent i = new Intent(context, LogsService.class);
      i.putExtra(LogsService.KEY_LEVEL, LEVEL_DEBUG);
      i.putExtra(LogsService.KEY_MESSAGE, message);
      context.startService(i);
    }
  }
}
