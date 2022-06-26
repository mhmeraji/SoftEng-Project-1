CREATE TABLE "groups_info"
(
  "channel_id"          varchar(32)         NOT NULL  PRIMARY KEY,
  "channel_type"        varchar(32)         NOT NULL,
  "fetch_count"         int                 NOT NULL,
  "verified?"           boolean             NOT NULL
);

CREATE TABLE "users_info"
(
  "user_id"             varchar(32)          NOT NULL PRIMARY KEY,
  "phone_number"        varchar(32)          NOT NULL,
  "channel_id"          varchar(32)          REFERENCES groups_info(channel_id)
);


CREATE TABLE "messages"
(
  "id"                  serial                        PRIMARY KEY,
  "m_id"                bigint               NOT NULL,
  "date"                bigint               NOT NULL,
  "edit_date"           bigint,
  "pinned"              boolean,
  "silent"              boolean,
  "edit_hide"           boolean,
  "noforwards"          boolean,
  "mentioned"           boolean,
  "legacy"              boolean,
  "from_scheduled"      boolean,
  "out"                 boolean,
  "forwards"            bigint,
  "post"                boolean,
  "channel-id"          varchar(32)          REFERENCES groups_info(channel_id),
  "message"             text,
  "views"               int,
  "persisted-at"        timestamp
                        without time zone
                        DEFAULT
                        current_timestamp
);

CREATE TABLE "isins" (
       "isin"         varchar(64)            PRIMARY KEY,
       "short-name"   varchar(64)            NOT NULL,
       "market-type"  varchar(32)            NOT NULL
);


CREATE TABLE "message_isin"
  (
    "id"                  serial                        PRIMARY KEY,
    "message_id"          bigint              REFERENCES messages(id),
    "isin_id"             varchar(32)         REFERENCES isins(isin)
  );

CREATE TABLE "message_info"
(
  "record_id"           int                   REFERENCES message_isin(id),
  "sentiment_score"     int,
  "sentiment_type"      varchar(32),
  "trade_direction"     varchar(8)

);
