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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;

public class IntentFilterEditActivity extends Activity implements
    IntentFilterColumns {

  private Long rowId;
  private EditText actionText;
  private EditText categoryText;
  private EditText dataAuthHostText;
  private EditText dataAuthPortText;
  private EditText dataSchemeText;
  private EditText dataTypeText;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.intentfilter_edit);

    rowId = null;
    actionText = (EditText) findViewById(R.id.intentfilter_edit_action_name);
    categoryText = (EditText) findViewById(R.id.intentfilter_edit_category_name);
    dataAuthHostText = (EditText) findViewById(R.id.intentfilter_edit_data_auth_host_name);
    dataAuthPortText = (EditText) findViewById(R.id.intentfilter_edit_data_auth_port_name);
    dataSchemeText = (EditText) findViewById(R.id.intentfilter_edit_data_scheme_name);
    dataTypeText = (EditText) findViewById(R.id.intentfilter_edit_data_type_name);

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
    actionText.setOnKeyListener(onKeyListener);
    categoryText.setOnKeyListener(onKeyListener);
    dataAuthHostText.setOnKeyListener(onKeyListener);
    dataAuthPortText.setOnKeyListener(onKeyListener);
    dataSchemeText.setOnKeyListener(onKeyListener);
    dataTypeText.setOnKeyListener(onKeyListener);

    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      if (extras.containsKey(_ID)) {
        rowId = extras.getLong(_ID);
      }
      if (extras.containsKey(ACTION)) {
        actionText.setText(extras.getString(ACTION).toString());
      }
      if (extras.containsKey(CATEGORY)) {
        categoryText.setText(extras.getString(CATEGORY).toString());
      }
      if (extras.containsKey(DATA_AUTHORITY_HOST)) {
        dataAuthHostText.setText(extras.getString(DATA_AUTHORITY_HOST)
            .toString());
      }
      if (extras.containsKey(DATA_AUTHORITY_PORT)) {
        dataAuthPortText.setText(extras.getString(DATA_AUTHORITY_PORT)
            .toString());
      }
      if (extras.containsKey(DATA_SCHEME)) {
        dataSchemeText.setText(extras.getString(DATA_SCHEME).toString());
      }
      if (extras.containsKey(DATA_TYPE)) {
        dataTypeText.setText(extras.getString(DATA_TYPE).toString());
      }
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.intentfilter_edit_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.intentfilter_edit_menu_save_item:
      finishSaved();
      return true;
    case R.id.intentfilter_edit_menu_cancel_item:
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
    bundle.putString(ACTION, actionText.getText().toString().trim());
    bundle.putString(CATEGORY, categoryText.getText().toString().trim());
    bundle.putString(DATA_AUTHORITY_HOST, dataAuthHostText.getText().toString()
        .trim());
    bundle.putString(DATA_AUTHORITY_PORT, dataAuthPortText.getText().toString()
        .trim());
    bundle.putString(DATA_SCHEME, dataSchemeText.getText().toString().trim());
    bundle.putString(DATA_TYPE, dataTypeText.getText().toString().trim());
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
