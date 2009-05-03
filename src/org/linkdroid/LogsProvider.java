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

import org.linkdroid.Constants.LogColumns;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class LogsProvider extends ContentProvider implements LogColumns {
  private static final String TAG = "LogsProvider";

  private static final String DATABASE_NAME = "logs.db";
  private static final int DATABASE_VERSION = 1;
  private static final String TABLE_NAME = "logs";

  private static final HashMap<String, String> COLUMNS_PROJECTION;

  private static final int LOGS = 1;
  private static final int LOG_ID = 2;

  static {
    COLUMNS_PROJECTION = new HashMap<String, String>();
    COLUMNS_PROJECTION.put(_ID, _ID);
    COLUMNS_PROJECTION.put(MESSAGE, MESSAGE);
    COLUMNS_PROJECTION.put(CREATED_AT, CREATED_AT);
  }

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
          + " INTEGER PRIMARY KEY," + LEVEL + " INTEGER, " + MESSAGE + " TEXT,"
          + CREATED_AT + " INTEGER);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
          + newVersion + ", which will destroy all old data");
      db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
      onCreate(db);
    }
  }

  private UriMatcher uriMatcher;

  private DatabaseHelper dbHelper;

  @Override
  public boolean onCreate() {
    dbHelper = new DatabaseHelper(getContext());
    uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    uriMatcher.addURI(AUTHORITY, "logs", LOGS);
    uriMatcher.addURI(AUTHORITY, "logs/#", LOG_ID);
    return true;
  }

  @Override
  public int delete(Uri uri, String where, String[] whereArgs) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    int count;
    switch (uriMatcher.match(uri)) {
    case LOGS:
      count = db.delete(TABLE_NAME, where, whereArgs);
      break;
    case LOG_ID:
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
    case LOGS:
      return CONTENT_TYPE;
    case LOG_ID:
      return CONTENT_ITEM_TYPE;
    default:
      throw new IllegalArgumentException("Unknown URI " + uri);
    }
  }

  @Override
  public Uri insert(Uri uri, ContentValues initialValues) {
    // Validate the requested uri
    if (uriMatcher.match(uri) != LOGS) {
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
    if (values.containsKey(LEVEL) == false) {
      values.put(LEVEL, 0);
    }
    if (values.containsKey(MESSAGE) == false) {
      values.put(MESSAGE, "");
    }

    SQLiteDatabase db = dbHelper.getWritableDatabase();
    long rowId = db.insert(TABLE_NAME, MESSAGE, values);
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
    case LOGS:
      qb.setTables(TABLE_NAME);
      qb.setProjectionMap(COLUMNS_PROJECTION);
      break;

    case LOG_ID:
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
    int count;
    switch (uriMatcher.match(uri)) {
    case LOGS:
      count = db.update(TABLE_NAME, values, where, whereArgs);
      break;
    case LOG_ID:
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
}
