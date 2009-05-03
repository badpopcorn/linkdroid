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
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.james.mime4j.decoder.Base64OutputStream;
import org.linkdroid.Constants.Extras;
import org.linkdroid.Constants.WebhookColumns;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class PostJob {
  private static final String TAG = "PostJob";

  private static final String UTF8 = "UTF-8";
  private static final Charset UTF8_CHARSET = Charset.forName(UTF8);

  private static final String USER_AGENT = "LinkDroid/0.9.13";
  private static final String USER_AGENT_KEY = "http.useragent";

  /**
   * Posts the data to our webhook.
   * 
   * The HMAC field calculation notes:
   * <ol>
   * <li>The Extras.HMAC field is calculated from all the non Extras.STREAM
   * fields; this includes all the original bundle extra fields, plus the
   * linkdroid fields (e.g Extras.STREAM_MIME_TYPE, and including
   * Extras.STREAM_HMAC); the NONCE is NOT prepended to the input since it will
   * be somewhere in the data bundle.</li>
   * <li>The Extras.STREAM_HMAC field is calculated by digesting the concat of
   * the NONCE (first) and the actual binary data obtained from the
   * EXTRAS_STREAM uri; this STREAM_HMAC field (along with the other
   * Extras.STREAM_* fields) are added to the data bundle, which will also be
   * hmac'd.</li>
   * <li>If no hmac secret is set, then the NONCEs will not be set in the upload
   * </li>
   * </ol>
   * 
   * @param contentResolver
   * @param webhook
   * @param data
   * @throws UnsupportedEncodingException
   * @throws IOException
   * @throws InvalidKeyException
   * @throws NoSuchAlgorithmException
   */
  public static void execute(ContentResolver contentResolver, Bundle webhook,
      Bundle data) throws UnsupportedEncodingException, IOException,
      InvalidKeyException, NoSuchAlgorithmException {

    // Set constants that may be used in this method.
    final String secret = webhook.getString(WebhookColumns.SECRET);

    // This is the multipart form object that will contain all the data bundle
    // extras; it also will include the data of the extra stream and any other
    // extra linkdroid specific field required.
    MultipartEntity entity = new MultipartEntity();

    // Do the calculations to create our nonce string, if necessary.
    String nonce = obtainNonce(webhook);

    // Add the nonce to the data bundle if we have it.
    if (nonce != null) {
      data.putString(Extras.NONCE, nonce);
    }

    // We have a stream of data, so we need to add that to the multipart form
    // upload with a possible HMAC.
    if (data.containsKey(Intent.EXTRA_STREAM)) {
      Uri mediaUri = (Uri) data.get(Intent.EXTRA_STREAM);

      // Open our mediaUri, base 64 encode it and add it to our multipart upload
      // entity.
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      Base64OutputStream b64os = new Base64OutputStream(baos);
      InputStream is = contentResolver.openInputStream(mediaUri);
      byte[] bytes = new byte[1024];
      int count;
      while ((count = is.read(bytes)) != -1) {
        b64os.write(bytes, 0, count);
      }
      is.close();
      baos.close();
      b64os.close();
      final String base64EncodedString = new String(baos.toByteArray(), UTF8);
      entity.addPart(Extras.STREAM, new StringBody(base64EncodedString,
          UTF8_CHARSET));

      // Add the mimetype of the stream to the data bundle.
      final String mimeType = contentResolver.getType(mediaUri);
      if (mimeType != null) {
        data.putString(Extras.STREAM_MIME_TYPE, mimeType);
      }

      // Do the hmac calculation of the stream and add it to the data bundle.
      // NOTE: This hmac string will be included as part of the input to the
      // other Extras hmac.
      if (shouldDoHmac(webhook)) {
        InputStream inputStream = contentResolver.openInputStream(mediaUri);
        final String streamHmac = hmacSha1(inputStream, secret, nonce);
        inputStream.close();
        data.putString(Extras.STREAM_HMAC, streamHmac);
        Log.d(TAG, "STREAM_HMAC: " + streamHmac);
      }
    }

    // Calculate the Hmac for all the items by iterating over the data bundle
    // keys in order.
    if (shouldDoHmac(webhook)) {
      final String dataHmac = calculateBundleExtrasHmac(data, secret);
      data.putString(Extras.HMAC, dataHmac);
      Log.d(TAG, "HMAC: " + dataHmac);
    }

    // Dump all the data bundle keys into the form.
    for (String k : data.keySet()) {
      Object value = data.get(k);
      // If the value is null, the key will still be in the form, but with
      // an empty string as its value.
      if (value != null) {
        entity.addPart(k, new StringBody(value.toString(), UTF8_CHARSET));
      } else {
        entity.addPart(k, new StringBody("", UTF8_CHARSET));
      }
    }

    // Create the client and request, then populate it with our multipart form
    // upload entity as part of the POST request; finally post the form.
    final String webhookUri = webhook.getString(WebhookColumns.URI);
    final HttpClient client = new DefaultHttpClient();
    client.getParams().setParameter(USER_AGENT_KEY, USER_AGENT);
    final HttpPost request = new HttpPost(webhookUri);
    request.setEntity(entity);
    HttpResponse response = client.execute(request);
    switch (response.getStatusLine().getStatusCode()) {
    case HttpStatus.SC_OK:
    case HttpStatus.SC_CREATED:
    case HttpStatus.SC_ACCEPTED:
      break;
    default:
      throw new RuntimeException(response.getStatusLine().toString());
    }
  }

  private static boolean shouldDoHmac(Bundle webhook) {
    return webhook.containsKey(WebhookColumns.SECRET)
        && !TextUtils.isEmpty(webhook.getString(WebhookColumns.SECRET));
  }

  private static String obtainNonce(Bundle webhook) {
    String nonce = null;
    if (shouldDoHmac(webhook)) {
      Integer nonceRandom = webhook.getInt(WebhookColumns.NONCE_RANDOM);
      if (nonceRandom != null && nonceRandom > 0) {
        nonce = Double.toString(Math.random());
      }
      Integer nonceTimestamp = webhook.getInt(WebhookColumns.NONCE_TIMESTAMP);
      if (nonceTimestamp != null && nonceTimestamp > 0) {
        if (nonce != null) {
          nonce += " " + Long.toString(System.currentTimeMillis());
        } else {
          nonce = Long.toString(System.currentTimeMillis());
        }
      }
      Log.d(TAG, "NONCE " + nonce);
    }
    return nonce;
  }

  private static final String calculateBundleExtrasHmac(Bundle data,
      String secretString) throws NoSuchAlgorithmException, IOException,
      InvalidKeyException {
    Mac mac = initHmacSha1(secretString, null);
    SortedSet<String> keys = new TreeSet<String>(data.keySet());
    for (String key : keys) {
      mac.update(key.getBytes(UTF8));
      Object value = data.get(key);
      // We only add the value to the hmac digest if it exists; the key is still
      // part of the digest calculation.
      if (value != null) {
        mac.update(value.toString().getBytes(UTF8));
      }
    }
    return hmacDigestToHexString(mac.doFinal());
  }

  private static Mac initHmacSha1(String secretString, String nonce)
      throws NoSuchAlgorithmException, IOException, InvalidKeyException {
    SecretKey key = new SecretKeySpec(secretString.getBytes(), "HmacSHA1");
    Mac mac = Mac.getInstance("HmacSHA1");
    mac.init(key);
    if (nonce != null) {
      mac.update(nonce.getBytes(UTF8));
    }
    return mac;
  }

  private static final char[] HEX_CHAR = { '0', '1', '2', '3', '4', '5', '6',
      '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

  private static String hmacDigestToHexString(byte[] digest) {
    char[] hexDigest = new char[40];
    for (int i = 0; i < 20; ++i) {
      int byteValue = 0xFF & digest[i]; // signed to unsigned. Java, man.
      hexDigest[i * 2] = HEX_CHAR[byteValue >> 4];
      hexDigest[i * 2 + 1] = HEX_CHAR[byteValue & 0xf];
    }
    return new String(hexDigest);
  }

  private static String hmacSha1(InputStream plainText, String secretString,
      String nonce) throws IOException, InvalidKeyException,
      NoSuchAlgorithmException {
    Mac mac = initHmacSha1(secretString, nonce);

    // digest the content.
    byte[] bytes = new byte[1024];
    int count;
    while ((count = plainText.read(bytes)) != -1) {
      mac.update(bytes, 0, count);
    }

    // Return the string digest
    return hmacDigestToHexString(mac.doFinal());
  }
}
