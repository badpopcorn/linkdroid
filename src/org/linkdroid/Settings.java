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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class Settings extends PreferenceActivity implements
    SharedPreferences.OnSharedPreferenceChangeListener {
  private static final String TAG = "Settings";

  // Options names and default values. The names must match what is in the
  // settings.xml file.
  private static final String OPT_NOTIFICATION_ENABLED = "notification_enabled";
  private static final boolean OPT_NOTIFICATION_ENABLED_DEF = true;

  private static final String OPT_NOTIFICATION_LOG_LEVEL = "notification_log_level";
  private static final String OPT_NOTIFICATION_LOG_LEVEL_DEF = "1";

  private static final String OPT_NOTIFICATION_SOUND = "notification_sound";
  private static final String OPT_NOTIFICATION_SOUND_DEF = "content://settings/system/notification_sound";

  private static final String OPT_NOTIFICATION_VIBRATE = "notification_vibrate";
  private static final boolean OPT_NOTIFICATION_VIBRATE_DEF = false;

  private static final String OPT_SYSTEM_LOGGING_VERBOSE = "system_logging_verbose";
  private static final boolean OPT_SYSTEM_LOGGING_VERBOSE_DEF = false;

  private static final String OPT_EVENTS_ENABLED = "events_enabled";
  private static final boolean OPT_EVENTS_ENABLED_DEF = false;

  private static final String OPT_EVENTS_SMS_ENABLED = "events_sms_enabled";
  private static final boolean OPT_EVENTS_SMS_ENABLED_DEF = false;

  private static final String OPT_EVENTS_LOG_LEVEL = "events_log_level";
  private static final String OPT_EVENTS_LOG_LEVEL_DEF = "-1";

  private static final String OPT_EVENTS_WEBHOOK_NAME = "events_webhook_name";
  private static final String OPT_EVENTS_WEBHOOK_NAME_DEF = "";
  private static final String OPT_EVENTS_WEBHOOK_URI = "events_webhook_uri";
  private static final String OPT_EVENTS_WEBHOOK_URI_DEF = "";
  private static final String OPT_EVENTS_WEBHOOK_SECRET = "events_webhook_secret";
  private static final String OPT_EVENTS_WEBHOOK_SECRET_DEF = null;
  private static final String OPT_EVENTS_WEBHOOK_NONCE_RANDOM = "events_webhook_nonce_random";
  private static final int OPT_EVENTS_WEBHOOK_NONCE_RANDOM_DEF = 0;
  private static final String OPT_EVENTS_WEBHOOK_NONCE_TIMESTAMP = "events_webhook_nonce_timestamp";
  private static final int OPT_EVENTS_WEBHOOK_NONCE_TIMESTAMP_DEF = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.settings);
  }

  @Override
  protected void onPause() {
    super.onPause();
    getPreferenceScreen().getSharedPreferences()
        .unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    getPreferenceScreen().getSharedPreferences()
        .registerOnSharedPreferenceChangeListener(this);
  }

  public static boolean isNotificationEnabled(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
        OPT_NOTIFICATION_ENABLED, OPT_NOTIFICATION_ENABLED_DEF);
  }

  public static int getNotificationLogLevel(Context context) {
    String logLevelString = PreferenceManager.getDefaultSharedPreferences(
        context).getString(OPT_NOTIFICATION_LOG_LEVEL,
        OPT_NOTIFICATION_LOG_LEVEL_DEF);
    return Integer.parseInt(logLevelString);
  }

  public static String getNotificationSound(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).getString(
        OPT_NOTIFICATION_SOUND, OPT_NOTIFICATION_SOUND_DEF);
  }

  public static boolean isNotificationVibrate(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
        OPT_NOTIFICATION_VIBRATE, OPT_NOTIFICATION_VIBRATE_DEF);
  }

  public static boolean isLoggingVerbose(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
        OPT_SYSTEM_LOGGING_VERBOSE, OPT_SYSTEM_LOGGING_VERBOSE_DEF);
  }

  public static boolean isEventsEnabled(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
        OPT_EVENTS_ENABLED, OPT_EVENTS_ENABLED_DEF);
  }

  public static boolean isEventsSmsEnabled(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
        OPT_EVENTS_SMS_ENABLED, OPT_EVENTS_SMS_ENABLED_DEF);
  }

  public static int getEventsLogLevel(Context context) {
    String logLevelString = PreferenceManager.getDefaultSharedPreferences(
        context).getString(OPT_EVENTS_LOG_LEVEL, OPT_EVENTS_LOG_LEVEL_DEF);
    return Integer.parseInt(logLevelString);
  }

  public static Bundle getEventsWebhook(Context context) {
    Bundle webhook = new Bundle();
    webhook.putString(WebhookColumns.URI, PreferenceManager
        .getDefaultSharedPreferences(context).getString(OPT_EVENTS_WEBHOOK_URI,
            OPT_EVENTS_WEBHOOK_URI_DEF));
    webhook.putString(WebhookColumns.NAME, PreferenceManager
        .getDefaultSharedPreferences(context).getString(
            OPT_EVENTS_WEBHOOK_NAME, OPT_EVENTS_WEBHOOK_NAME_DEF));
    webhook.putString(WebhookColumns.SECRET, PreferenceManager
        .getDefaultSharedPreferences(context).getString(
            OPT_EVENTS_WEBHOOK_SECRET, OPT_EVENTS_WEBHOOK_SECRET_DEF));
    webhook.putInt(WebhookColumns.NONCE_RANDOM, PreferenceManager
        .getDefaultSharedPreferences(context).getInt(
            OPT_EVENTS_WEBHOOK_NONCE_RANDOM,
            OPT_EVENTS_WEBHOOK_NONCE_RANDOM_DEF));
    webhook.putInt(WebhookColumns.NONCE_TIMESTAMP, PreferenceManager
        .getDefaultSharedPreferences(context).getInt(
            OPT_EVENTS_WEBHOOK_NONCE_TIMESTAMP,
            OPT_EVENTS_WEBHOOK_NONCE_TIMESTAMP_DEF));
    return webhook;
  }

  public static void setEventsWebhook(Context context, Bundle webhook) {
    SharedPreferences.Editor editor = PreferenceManager
        .getDefaultSharedPreferences(context).edit();
    editor.putString(OPT_EVENTS_WEBHOOK_NAME, webhook
        .getString(WebhookColumns.NAME));
    editor.putString(OPT_EVENTS_WEBHOOK_URI, webhook
        .getString(WebhookColumns.URI));
    editor.putString(OPT_EVENTS_WEBHOOK_SECRET, webhook
        .getString(WebhookColumns.SECRET));
    editor.putInt(OPT_EVENTS_WEBHOOK_NONCE_RANDOM, webhook
        .getInt(WebhookColumns.NONCE_RANDOM));
    editor.putInt(OPT_EVENTS_WEBHOOK_NONCE_TIMESTAMP, webhook
        .getInt(WebhookColumns.NONCE_TIMESTAMP));
    editor.commit();
  }

  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
      String key) {
    if (key.equals(OPT_EVENTS_ENABLED) && isEventsEnabled(this)) {
      Log.d(TAG, "Events enabled");
      Intent intent = new Intent(this, BroadcastReceiverService.class);
      startService(intent);
    } else if (key.equals(OPT_EVENTS_ENABLED) && !isEventsEnabled(this)) {
      Log.d(TAG, "Events disabled");
      Intent intent = new Intent(this, BroadcastReceiverService.class);
      stopService(intent);
    } else if (key.equals(OPT_EVENTS_SMS_ENABLED) && isEventsEnabled(this)) {
      Log.d(TAG, "Toggling Sms Events, reregistering intentfilters");
      Intent intent = new Intent(this, BroadcastReceiverService.class);
      startService(intent);
    }
  }

}
