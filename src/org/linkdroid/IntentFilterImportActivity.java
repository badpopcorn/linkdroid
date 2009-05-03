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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONException;
import org.json.JSONObject;
import org.linkdroid.Constants.IntentFilterColumns;
import org.linkdroid.Constants.IntentFilterJsonFields;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class IntentFilterImportActivity extends Activity {
  private static final String TAG = "IntentFilterImportActivity";

  private static final int ACTIVITY_CREATE = 1;

  @Override
  public void onResume() {
    super.onResume();
    try {
      InputStream inputStream = getContentResolver().openInputStream(
          getIntent().getData());
      String jsonString = readToString(inputStream);
      JSONObject jsonObject = new JSONObject(jsonString);
      Intent intent = new Intent(this, IntentFilterEditActivity.class);
      if (jsonObject.has(IntentFilterJsonFields.ACTION)) {
        intent.putExtra(IntentFilterColumns.ACTION, jsonObject.get(
            IntentFilterJsonFields.ACTION).toString());
      }
      if (jsonObject.has(IntentFilterJsonFields.CATEGORY)) {
        intent.putExtra(IntentFilterColumns.CATEGORY, jsonObject.get(
            IntentFilterJsonFields.CATEGORY).toString());
      }
      if (jsonObject.has(IntentFilterJsonFields.DATA_AUTHORITY_HOST)) {
        intent.putExtra(IntentFilterColumns.DATA_AUTHORITY_HOST, jsonObject
            .get(IntentFilterJsonFields.DATA_AUTHORITY_HOST).toString());
      }
      if (jsonObject.has(IntentFilterJsonFields.DATA_AUTHORITY_PORT)) {
        intent.putExtra(IntentFilterColumns.DATA_AUTHORITY_PORT, jsonObject
            .get(IntentFilterJsonFields.DATA_AUTHORITY_PORT).toString());
      }
      if (jsonObject.has(IntentFilterJsonFields.DATA_SCHEME)) {
        intent.putExtra(IntentFilterColumns.DATA_SCHEME, jsonObject.get(
            IntentFilterJsonFields.DATA_SCHEME).toString());
      }
      if (jsonObject.has(IntentFilterJsonFields.DATA_TYPE)) {
        intent.putExtra(IntentFilterColumns.DATA_TYPE, jsonObject.get(
            IntentFilterJsonFields.DATA_TYPE).toString());
      }
      startActivityForResult(intent, ACTIVITY_CREATE);
    } catch (JSONException e) {
      Log.e(TAG, e.toString());
      Toast.makeText(IntentFilterImportActivity.this,
          getString(R.string.intentfilter_import_json_error),
          Toast.LENGTH_SHORT).show();
      finish();
    } catch (IOException e) {
      Log.e(TAG, e.toString());
      Toast.makeText(IntentFilterImportActivity.this,
          getString(R.string.intentfilter_import_io_error), Toast.LENGTH_SHORT)
          .show();
      finish();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (intent != null) {
      Bundle extras = intent.getExtras();
      switch (requestCode) {
      case ACTIVITY_CREATE:
        switch (resultCode) {
        case RESULT_OK:
          // We have returned from the Edit dialog, and we now do a brand new
          // insert of this webhook.
          ContentValues values = IntentFilterProvider
              .populateContentValues(extras);
          getContentResolver().insert(IntentFilterProvider.CONTENT_URI, values);
          Toast
              .makeText(IntentFilterImportActivity.this,
                  getString(R.string.intentfilter_import_saved),
                  Toast.LENGTH_SHORT).show();
          break;
        default:
          Toast.makeText(IntentFilterImportActivity.this,
              getString(R.string.intentfilter_import_cancelled),
              Toast.LENGTH_SHORT).show();
        }
        break;
      }
      finish();
    }
  }

  private static String readToString(InputStream inputStream)
      throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        inputStream));
    StringBuilder stringBuilder = new StringBuilder();
    String line = null;
    while ((line = reader.readLine()) != null) {
      stringBuilder.append(line + "\n");
    }
    inputStream.close();
    return stringBuilder.toString();
  }
}
