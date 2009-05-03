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
import org.linkdroid.Constants.Extras;
import org.linkdroid.Constants.WebhookColumns;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

public class WebhookPostService extends Service {
  private static final String TAG = "WebhookPostService";

  public static final String KEY_WEBHOOK_BUNDLE = "webhook";
  public static final String KEY_DATA_BUNDLE = "data";
  public static final String KEY_USER_INPUT = "user_input";
  public static final String KEY_LOG_LEVEL = "log_level";

  private final class ServiceHandler extends Handler {
    public ServiceHandler(Looper looper) {
      super(looper);
    }

    @Override
    public void handleMessage(Message msg) {
      int errorLogLevel = LogsService.LEVEL_ERROR;
      int okLogLevel = LogsService.LEVEL_INFO;
      try {
        final Intent intent = (Intent) msg.obj;
        errorLogLevel = intent.getIntExtra(KEY_LOG_LEVEL,
            LogsService.LEVEL_ERROR);
        okLogLevel = intent.getIntExtra(KEY_LOG_LEVEL, LogsService.LEVEL_INFO);
        final Bundle extras = intent.getExtras();
        final Bundle webhookBundle = extras.getBundle(KEY_WEBHOOK_BUNDLE);
        final Bundle dataBundle = extras.getBundle(KEY_DATA_BUNDLE);
        // Since the newPostDataBundle sanitizes many linkdroid fields, we wait
        // until near last minute to actually enter those keys. User input
        // is one such input. This is so the Webhook service knows that
        // it is actual user input, not a value coming from an externally
        // originating intent.
        if (extras.containsKey(KEY_USER_INPUT)) {
          final String userInput = extras.getString(KEY_USER_INPUT);
          if (userInput != null) {
            dataBundle.putString(Extras.USER_INPUT, userInput);
          }
        }
        PostJob.execute(getContentResolver(), webhookBundle, dataBundle);
        if (okLogLevel > LogsService.LEVEL_NONE) {
          LogsService
              .logMessage(
                  WebhookPostService.this,
                  okLogLevel,
                  WebhookPostService.this
                      .getString(R.string.webhook_post_service_log_completed_post_prefix)
                      + webhookBundle.getString(WebhookColumns.URI));
        }
      } catch (Exception e) {
        LogsService.logMessage(WebhookPostService.this, errorLogLevel, ""
            + e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
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
    LogsService.logSystemDebugMessage(this,
        getString(R.string.webhook_post_service_creating));

    HandlerThread thread = new HandlerThread(TAG);
    thread.start();

    serviceLooper = thread.getLooper();
    serviceHandler = new ServiceHandler(serviceLooper);
  }

  @Override
  public void onDestroy() {
    LogsService.logSystemDebugMessage(this,
        getString(R.string.webhook_post_service_shutting_down));
    serviceLooper.quit();
  }

  @Override
  public void onStart(Intent intent, int startId) {
    try {
      final Bundle extras = intent.getExtras();
      final Bundle webhookBundle = extras.getBundle(KEY_WEBHOOK_BUNDLE);

      LogsService.logSystemDebugMessage(this,
          getString(R.string.webhook_post_service_starting_webhook_post_prefix)
              + webhookBundle.getString(WebhookColumns.URI));

      Message msg = serviceHandler.obtainMessage();
      msg.arg1 = startId;
      msg.obj = intent;
      serviceHandler.sendMessage(msg);
    } catch (Exception e) {
      LogsService.logSystemDebugMessage(this, e.toString());
    }
  }

  public static Bundle newPostDataBundle(Intent originator) {
    Bundle bundle = new Bundle();
    if (originator.getExtras() != null) {
      bundle.putAll(originator.getExtras());
    }
    // Sanitize the linkdroid extras from data bundle in case originating
    // intent extras set them. We do this for all linkdroid Extras except
    // for the SMS extras.
    bundle.remove(Extras.INTENT_ACTION);
    bundle.remove(Extras.INTENT_TYPE);
    bundle.remove(Extras.INTENT_CATEGORIES);
    bundle.remove(Extras.HMAC);
    bundle.remove(Extras.NONCE);
    bundle.remove(Extras.STREAM);
    bundle.remove(Extras.STREAM_HMAC);
    bundle.remove(Extras.STREAM_MIME_TYPE);
    bundle.remove(Extras.USER_INPUT);
    if (originator.getAction() != null) {
      bundle.putString(Extras.INTENT_ACTION, originator.getAction());
    }
    if (originator.getType() != null) {
      bundle.putString(Extras.INTENT_TYPE, originator.getType());
    }
    if (originator.getCategories() != null) {
      bundle.putString(Extras.INTENT_CATEGORIES, StringUtils.join(originator
          .getCategories(), " "));
    }

    return bundle;
  }
}
