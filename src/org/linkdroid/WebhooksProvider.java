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

import java.util.HashMap;

import org.linkdroid.Constants.WebhookColumns;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class WebhooksProvider extends ContentProvider implements WebhookColumns {
  private static final String TAG = "WebhookProvider";

  private static final String DATABASE_NAME = "webhooks.db";
  private static final int DATABASE_VERSION = 1;
  private static final String TABLE_NAME = "webhooks";

  private static final HashMap<String, String> COLUMNS_PROJECTION;

  private static final int WEBHOOKS = 1;
  private static final int WEBHOOK_ID = 2;

  /**
   * This class helps open, create, and upgrade the database file.
   */
  private static class DatabaseHelper extends SQLiteOpenHelper {

    DatabaseHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      Log.w(TAG, "Creating database from version " + DATABASE_VERSION);
      db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + _ID
          + " INTEGER PRIMARY KEY," + NAME + " TEXT," + URI + " TEXT," + SECRET
          + " TEXT," + NONCE_RANDOM + " INTEGER," + NONCE_TIMESTAMP
          + " INTEGER," + CREATED_AT + " INTEGER," + UPDATED_AT + " INTEGER);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
          + newVersion + ", which will destroy all old data");
      db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
      onCreate(db);
    }
  }

  static {
    COLUMNS_PROJECTION = new HashMap<String, String>();
    COLUMNS_PROJECTION.put(_ID, _ID);
    COLUMNS_PROJECTION.put(NAME, NAME);
    COLUMNS_PROJECTION.put(URI, URI);
    COLUMNS_PROJECTION.put(SECRET, SECRET);
    COLUMNS_PROJECTION.put(NONCE_RANDOM, NONCE_RANDOM);
    COLUMNS_PROJECTION.put(NONCE_TIMESTAMP, NONCE_TIMESTAMP);
    COLUMNS_PROJECTION.put(CREATED_AT, CREATED_AT);
    COLUMNS_PROJECTION.put(UPDATED_AT, UPDATED_AT);
  }

  private UriMatcher uriMatcher;

  private DatabaseHelper dbHelper;

  @Override
  public boolean onCreate() {
    dbHelper = new DatabaseHelper(getContext());
    uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    uriMatcher.addURI(AUTHORITY, "webhooks", WEBHOOKS);
    uriMatcher.addURI(AUTHORITY, "webhooks/#", WEBHOOK_ID);
    return true;
  }

  @Override
  public int delete(Uri uri, String where, String[] whereArgs) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    int count;
    switch (uriMatcher.match(uri)) {
    case WEBHOOKS:
      count = db.delete(TABLE_NAME, where, whereArgs);
      break;
    case WEBHOOK_ID:
      String id = uri.getPathSegments().get(1);
      count = db.delete(TABLE_NAME, _ID + "=" + id
          + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
          whereArgs);
      break;
    default:
      throw new IllegalArgumentException("Unknown URI " + uri);
    }
    getContext().getContentResolver().notifyChange(uri, null);
    return count;
  }

  @Override
  public String getType(Uri uri) {
    switch (uriMatcher.match(uri)) {
    case WEBHOOKS:
      return CONTENT_TYPE;
    case WEBHOOK_ID:
      return CONTENT_ITEM_TYPE;
    default:
      throw new IllegalArgumentException("Unknown URI " + uri);
    }
  }

  @Override
  public Uri insert(Uri uri, ContentValues initialValues) {
    // Validate the requested uri
    if (uriMatcher.match(uri) != WEBHOOKS) {
      throw new IllegalArgumentException("Unknown URI " + uri);
    }

    ContentValues values;
    if (initialValues != null) {
      values = new ContentValues(initialValues);
    } else {
      values = new ContentValues();
    }

    Long now = Long.valueOf(System.currentTimeMillis());

    // Make sure that the fields are all set
    if (values.containsKey(CREATED_AT) == false) {
      values.put(CREATED_AT, now);
    }
    if (values.containsKey(UPDATED_AT) == false) {
      values.put(UPDATED_AT, now);
    }
    if (values.containsKey(NAME) == false) {
      Resources r = Resources.getSystem();
      values.put(NAME, r.getString(android.R.string.untitled));
    }
    if (values.containsKey(URI) == false) {
      values.put(URI, "");
    }
    if (values.containsKey(SECRET) == false) {
      values.put(SECRET, "");
    }
    if (values.containsKey(NONCE_RANDOM) == false) {
      values.put(NONCE_RANDOM, 0);
    }
    if (values.containsKey(NONCE_TIMESTAMP) == false) {
      values.put(NONCE_TIMESTAMP, 0);
    }

    SQLiteDatabase db = dbHelper.getWritableDatabase();
    long rowId = db.insert(TABLE_NAME, URI, values);
    if (rowId > 0) {
      Uri retUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
      getContext().getContentResolver().notifyChange(retUri, null);
      return retUri;
    }

    throw new SQLException("Failed to insert row into " + uri);
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

    switch (uriMatcher.match(uri)) {
    case WEBHOOKS:
      qb.setTables(TABLE_NAME);
      qb.setProjectionMap(COLUMNS_PROJECTION);
      break;
    case WEBHOOK_ID:
      qb.setTables(TABLE_NAME);
      qb.setProjectionMap(COLUMNS_PROJECTION);
      qb.appendWhere(_ID + "=" + uri.getPathSegments().get(1));
      break;
    default:
      throw new IllegalArgumentException("Unknown URI " + uri);
    }

    // If no sort order is specified use the default
    String orderBy;
    if (TextUtils.isEmpty(sortOrder)) {
      orderBy = DEFAULT_SORT_ORDER;
    } else {
      orderBy = sortOrder;
    }

    // Get the database and run the query
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    Cursor c = qb.query(db, projection, selection, selectionArgs, null, null,
        orderBy);

    // Tell the cursor what uri to watch, so it knows when its source data
    // changes
    c.setNotificationUri(getContext().getContentResolver(), uri);
    return c;
  }

  @Override
  public int update(Uri uri, ContentValues values, String where,
      String[] whereArgs) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();

    // Set updated
    Long now = Long.valueOf(System.currentTimeMillis());
    values.put(UPDATED_AT, now);

    int count;
    switch (uriMatcher.match(uri)) {
    case WEBHOOKS:
      count = db.update(TABLE_NAME, values, where, whereArgs);
      break;
    case WEBHOOK_ID:
      String id = uri.getPathSegments().get(1);
      count = db.update(TABLE_NAME, values, _ID + "=" + id
          + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
          whereArgs);
      break;
    default:
      throw new IllegalArgumentException("Unknown URI " + uri);
    }

    getContext().getContentResolver().notifyChange(uri, null);
    return count;
  }

  public static ContentValues populateContentValues(Bundle extras) {
    ContentValues values = new ContentValues();
    values.put(NAME, extras.getString(NAME));
    values.put(URI, extras.getString(URI));
    values.put(SECRET, extras.getString(SECRET));
    values.put(NONCE_RANDOM, extras.getInt(NONCE_RANDOM));
    values.put(NONCE_TIMESTAMP, extras.getInt(NONCE_TIMESTAMP));
    return values;
  }

}
