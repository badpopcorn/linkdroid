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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.CheckBox;
import android.widget.EditText;

public class WebhookEditActivity extends Activity implements WebhookColumns {

  private Long rowId;
  private EditText nameText;
  private EditText uriText;
  private EditText secretText;
  private CheckBox nonceRandomCheckbox;
  private CheckBox nonceTimestampCheckbox;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.webhook_edit);

    rowId = null;
    nameText = (EditText) findViewById(R.id.webhook_edit_name);
    uriText = (EditText) findViewById(R.id.webhook_edit_uri);
    secretText = (EditText) findViewById(R.id.webhook_edit_secret);
    nonceRandomCheckbox = (CheckBox) findViewById(R.id.webhook_edit_nonce_random);
    nonceTimestampCheckbox = (CheckBox) findViewById(R.id.webhook_edit_nonce_timestamp);

    final OnKeyListener onKeyListener = new OnKeyListener() {
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
          switch (keyCode) {
          case KeyEvent.KEYCODE_ENTER:
          case KeyEvent.KEYCODE_TAB:
            return true;
          }
        }
        return false;
      }
    };
    nameText.setOnKeyListener(onKeyListener);
    uriText.setOnKeyListener(onKeyListener);
    secretText.setOnKeyListener(onKeyListener);

    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      rowId = extras.getLong(_ID);
      String name = extras.getString(NAME);
      String uri = extras.getString(URI);
      String secret = extras.getString(SECRET);
      Integer nonceRandom = extras.getInt(NONCE_RANDOM);
      Integer nonceTimestamp = extras.getInt(NONCE_TIMESTAMP);
      if (name != null) {
        nameText.setText(name);
      }
      if (uri != null) {
        uriText.setText(uri);
      }
      if (secret != null) {
        secretText.setText(secret);
      }
      if (nonceRandom != null) {
        nonceRandomCheckbox.setChecked(nonceRandom > 0);
      }
      if (nonceTimestamp != null) {
        nonceTimestampCheckbox.setChecked(nonceTimestamp > 0);
      }
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.webhook_edit_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.webhook_edit_menu_save_item:
      finishSaved();
      return true;
    case R.id.webhook_edit_menu_cancel_item:
      finishCanceled();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  protected void finishSaved() {
    Bundle bundle = new Bundle();
    if (rowId != null) {
      bundle.putLong(_ID, rowId);
    }
    bundle.putString(NAME, nameText.getText().toString().trim());
    bundle.putString(URI, uriText.getText().toString().trim());
    bundle.putString(SECRET, secretText.getText().toString().trim());
    bundle.putInt(NONCE_RANDOM, nonceRandomCheckbox.isChecked() ? 1 : 0);
    bundle.putInt(NONCE_TIMESTAMP, nonceTimestampCheckbox.isChecked() ? 1 : 0);
    Intent intent = new Intent();
    intent.putExtras(bundle);
    setResult(RESULT_OK, intent);
    finish();
  }

  protected void finishCanceled() {
    Intent intent = new Intent();
    setResult(RESULT_CANCELED, intent);
    finish();
  }
}
