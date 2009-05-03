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

import android.net.Uri;
import android.provider.BaseColumns;

public interface Constants {

  public static interface IntentFilterColumns extends BaseColumns {
    public static final String AUTHORITY = "org.linkdroid.intentfilters";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
        + "/intentfilters");
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.linkdroid.intentfilter";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.linkdroid.intentfilter";
    public static final String DEFAULT_SORT_ORDER = "action ASC";
    public static final String ACTION = "action";
    public static final String CATEGORY = "category";
    public static final String DATA_AUTHORITY_HOST = "data_authority_host";
    public static final String DATA_AUTHORITY_PORT = "data_authority_port";
    public static final String DATA_SCHEME = "data_scheme";
    public static final String DATA_TYPE = "data_type";
  }

  public static interface LogColumns extends BaseColumns {
    public static final String AUTHORITY = "org.linkdroid.logs";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
        + "/logs");
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.linkdroid.log";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.linkdroid.log";
    public static final String DEFAULT_SORT_ORDER = "created_at DESC";
    public static final String MESSAGE = "description";
    public static final String LEVEL = "level";
    public static final String CREATED_AT = "created_at";
  }

  public static interface WebhookColumns extends BaseColumns {
    public static final String AUTHORITY = "org.linkdroid.webhooks";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
        + "/webhooks");
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.linkdroid.webhook";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.linkdroid.webhook";
    public static final String DEFAULT_SORT_ORDER = "name ASC";
    public static final String NAME = "name";
    public static final String URI = "uri";
    public static final String SECRET = "secret";
    public static final String NONCE_RANDOM = "nonce_random";
    public static final String NONCE_TIMESTAMP = "nonce_timestamp";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";
  }

  public static interface Actions {
  }

  public static interface Extras {
    public static final String INTENT_ACTION = "org.linkdroid.intent.extra.intent_action";
    public static final String INTENT_TYPE = "org.linkdroid.intent.extra.intent_type";
    public static final String INTENT_CATEGORIES = "org.linkdroid.intent.extra.intent_categories";
    public static final String HMAC = "org.linkdroid.intent.extra.hmac";
    public static final String NONCE = "org.linkdroid.intent.extra.nonce";
    public static final String STREAM = "org.linkdroid.intent.extra.stream";
    public static final String STREAM_HMAC = "org.linkdroid.intent.extra.stream_hmac";
    public static final String STREAM_MIME_TYPE = "org.linkdroid.intent.extra.stream_mime_type";
    public static final String SMS_PDU = "org.linkdroid.intent.extra.sms_pdu";
    public static final String SMS_BODY = "org.linkdroid.intent.extra.sms_body";
    public static final String SMS_FROM = "org.linkdroid.intent.extra.sms_from";
    public static final String USER_INPUT = "org.linkdroid.intent.extra.user_input";
  }

  public static interface IntentFilterJsonFields {
    public static final String ACTION = "action";
    public static final String CATEGORY = "category";
    public static final String DATA_AUTHORITY_HOST = "data_authority_host";
    public static final String DATA_AUTHORITY_PORT = "data_authority_port";
    public static final String DATA_SCHEME = "data_scheme";
    public static final String DATA_TYPE = "data_type";
  }

  public static interface WebhookJsonFields {
    public static final String NAME = "name";
    public static final String URI = "uri";
    public static final String SECRET = "secret";
    public static final String NONCE_RANDOM = "nonce_random";
    public static final String NONCE_TIMESTAMP = "nonce_timestamp";
  }
}
