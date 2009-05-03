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

import org.linkdroid.Constants.IntentFilterColumns;

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
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class IntentFilterProvider extends ContentProvider implements
    IntentFilterColumns {
  private static final String TAG = "IntentFilterProvider";

  private static final String DATABASE_NAME = "intentfilters.db";
  private static final int DATABASE_VERSION = 1;
  private static final String TABLE_NAME = "intentfilters";

  private static final HashMap<String, String> COLUMNS_PROJECTION;

  private static final int INTENTFILTERS = 1;
  private static final int INTENTFILTER_ID = 2;

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
          + " INTEGER PRIMARY KEY," + ACTION + " TEXT," + CATEGORY + " TEXT,"
          + DATA_AUTHORITY_HOST + " TEXT," + DATA_AUTHORITY_PORT + " TEXT,"
          + DATA_SCHEME + " TEXT," + DATA_TYPE + " TEXT);");
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
    COLUMNS_PROJECTION.put(ACTION, ACTION);
    COLUMNS_PROJECTION.put(CATEGORY, CATEGORY);
    COLUMNS_PROJECTION.put(DATA_AUTHORITY_HOST, DATA_AUTHORITY_HOST);
    COLUMNS_PROJECTION.put(DATA_AUTHORITY_PORT, DATA_AUTHORITY_PORT);
    COLUMNS_PROJECTION.put(DATA_SCHEME, DATA_SCHEME);
    COLUMNS_PROJECTION.put(DATA_TYPE, DATA_TYPE);
  }

  private UriMatcher uriMatcher;

  private DatabaseHelper dbHelper;

  @Override
  public boolean onCreate() {
    dbHelper = new DatabaseHelper(getContext());
    uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    uriMatcher.addURI(AUTHORITY, "intentfilters", INTENTFILTERS);
    uriMatcher.addURI(AUTHORITY, "intentfilters/#", INTENTFILTER_ID);
    return true;
  }

  @Override
  public int delete(Uri uri, String where, String[] whereArgs) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    int count;
    switch (uriMatcher.match(uri)) {
    case INTENTFILTERS:
      count = db.delete(TABLE_NAME, where, whereArgs);
      break;
    case INTENTFILTER_ID:
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
    case INTENTFILTERS:
      return CONTENT_TYPE;
    case INTENTFILTER_ID:
      return CONTENT_ITEM_TYPE;
    default:
      throw new IllegalArgumentException("Unknown URI " + uri);
    }
  }

  @Override
  public Uri insert(Uri uri, ContentValues initialValues) {
    // Validate the requested uri
    if (uriMatcher.match(uri) != INTENTFILTERS) {
      throw new IllegalArgumentException("Unknown URI " + uri);
    }

    ContentValues values;
    if (initialValues != null) {
      values = new ContentValues(initialValues);
    } else {
      values = new ContentValues();
    }

    // Make sure that the fields are all set
    if (values.containsKey(ACTION) == false) {
      values.put(ACTION, "");
    }
    if (values.containsKey(CATEGORY) == false) {
      values.put(CATEGORY, "");
    }
    if (values.containsKey(DATA_AUTHORITY_HOST) == false) {
      values.put(DATA_AUTHORITY_HOST, "");
    }
    if (values.containsKey(DATA_AUTHORITY_PORT) == false) {
      values.put(DATA_AUTHORITY_PORT, "");
    }
    if (values.containsKey(DATA_SCHEME) == false) {
      values.put(DATA_SCHEME, "");
    }
    if (values.containsKey(DATA_TYPE) == false) {
      values.put(DATA_TYPE, "");
    }

    SQLiteDatabase db = dbHelper.getWritableDatabase();
    long rowId = db.insert(TABLE_NAME, ACTION, values);
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
    case INTENTFILTERS:
      qb.setTables(TABLE_NAME);
      qb.setProjectionMap(COLUMNS_PROJECTION);
      break;
    case INTENTFILTER_ID:
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
    case INTENTFILTERS:
      count = db.update(TABLE_NAME, values, where, whereArgs);
      break;
    case INTENTFILTER_ID:
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
    values.put(ACTION, extras.getString(ACTION));
    values.put(CATEGORY, extras.getString(CATEGORY));
    values.put(DATA_AUTHORITY_HOST, extras.getString(DATA_AUTHORITY_HOST));
    values.put(DATA_AUTHORITY_PORT, extras.getString(DATA_AUTHORITY_PORT));
    values.put(DATA_SCHEME, extras.getString(DATA_SCHEME));
    values.put(DATA_TYPE, extras.getString(DATA_TYPE));
    return values;
  }
}
