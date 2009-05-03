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

import java.io.ByteArrayOutputStream;

import org.apache.james.mime4j.decoder.Base64OutputStream;
import org.linkdroid.Constants.Extras;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.gsm.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {
  private static final String UTF8 = "UTF-8";
  private static final String PDUS_KEY = "pdus";

  @Override
  public void onReceive(Context context, Intent originator) {
    try {
      LogsService.logEventMessage(context, ""
          + context.getString(R.string.smsreceiver_received_intent));

      Bundle webhookBundle = Settings.getEventsWebhook(context);

      Object messages[] = (Object[]) originator.getExtras().get("pdus");
      for (int n = 0; n < messages.length; n++) {
        try {
          SmsMessage smsMessage = SmsMessage
              .createFromPdu((byte[]) messages[n]);

          // Get the pdu byte stream, and base64 encode it.
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          Base64OutputStream b64os = new Base64OutputStream(baos);
          b64os.write((byte[]) messages[n]);
          b64os.close();
          final String base64EncodedString = new String(baos.toByteArray(),
              UTF8);

          // Populate the dataBundle with the message body and sender, and
          // the original pdu for more advanced server side decoding.
          Bundle dataBundle = WebhookPostService.newPostDataBundle(originator);
          dataBundle.remove(PDUS_KEY);
          dataBundle.putString(Extras.SMS_PDU, base64EncodedString);
          dataBundle.putString(Extras.SMS_BODY, smsMessage
              .getDisplayMessageBody());
          dataBundle.putString(Extras.SMS_FROM, smsMessage
              .getDisplayOriginatingAddress());

          // Now post the sms message
          Intent intent = new Intent(context, WebhookPostService.class);
          intent.putExtra(WebhookPostService.KEY_LOG_LEVEL, Settings
              .getEventsLogLevel(context));
          intent.putExtra(WebhookPostService.KEY_WEBHOOK_BUNDLE, webhookBundle);
          intent.putExtra(WebhookPostService.KEY_DATA_BUNDLE, dataBundle);
          context.startService(intent);
        } catch (Exception e) {
          LogsService.logEventMessage(context, context
              .getString(R.string.smsreceiver_error_prefix)
              + e.toString());
        }
      }
    } catch (Exception e) {
      LogsService.logEventMessage(context, context
          .getString(R.string.smsreceiver_error_prefix)
          + e.toString());
    }
  }

}
