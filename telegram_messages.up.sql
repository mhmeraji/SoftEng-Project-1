CREATE TABLE "groups_info"
(
  "channel_id"          varchar(32)         NOT NULL, PRIMARY KEY,
  "channel_type"        varchar(32)         NOT NULL,
  "fetch_count"         int                 NOT NULL,
  "verified?"           boolean             NOT NULL
);

CREATE TABLE "users_info"
(
  "user_id"             varchar(32)          NOT NULL, PRIMARY KEY,
  "phone_number"        varchar(32)          NOT NULL
);


CREATE TABLE "messages"
(
  "id"                  varchar(32)                    PRIMARY KEY,
  "pinned"              boolean              NOT NULL,
  "silent"              boolean              NOT NULL,
  "edit_hide"           boolean              NOT NULL,
  "noforwards"          boolean              NOT NULL,
  "mentioned"           boolean              NOT NULL,
  "forwards_number"     int                  NOT NULL,
  "channel_id"          varchar(32)                    REFERENCES groups_info(id),
  "body"                text                 NOT NULL,
  "views"               int                  NOT NULL
);

CREATE TABLE "message-info"
(
  "record_id"           varchar(32)                    REFERENCES message_isin(id),
  "sentiment_score"     int,
  "sentiment_type"      varchar(32),
  "trade_direction"     varchar(8)

);

CREATE TABLE "isins"
(
  "isin"                varchar(32)                  PRIMARY KEY,
  "fa_name"             text                 NOT NULL
);

CREATE TABLE "message_isin"
(
  "id"                  serial                        PRIMARY KEY,
  "message_id"          varchar(32)         REFERENCES messages(id)
  "isin_id"             varchar(32)         REFERENCES isins(isin)
);
