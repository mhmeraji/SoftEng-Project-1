CREATE TABLE "messages" (

       "id"                serial                                 PRIMARY KEY,
       "message_id"        int                           NOT NULL,
       "type"              varchar(16)                   NOT NULL,
       "send_date"         varchar(32)                   NOT NULL,
       "sender_id"         varchar(64)                   NOT NULL,
       "body"              text                          NOT NULL,
       "sender_phone"      varchar(16)                           ,
       "sentiment_score"   int                                   ,
       "views_count"       int                                   ,
       "is_comment?"       boolean                       NOT NULL,
       "comment_of"        serial                        REFERENCES messages(id) ON DELETE CASCADE,

       "persisted-at"      timestamp  without time zone
                           DEFAULT current_timestamp
);

INSERT INTO messages("message_id", "type", "send_date", "sender_id", "body", "sender_phone", "sentiment_score", "views_count", "is_comment?")
VALUES ('5785', 'message', '2022-04-16T00:27:31', 'Emran_Crypto', 'علاوه بر این موضوع به زودی استارت معرفی ارز های خفن و پر سود و سبد گردانی عمومی ام داریم !!',
'989901253812', 67, 7665, FALSE
);

INSERT INTO messages("message_id", "type", "send_date", "sender_id", "body", "sender_phone", "sentiment_score", "views_count", "is_comment?", "comment_of")
VALUES ('5789', 'message', '2022-04-16T00:27:35', 'Emran_Crypto_ADMIN', 'برای دریافت سیگنال های لحظه ای بازار اوراق ما را دنبال کنید',
'989901253812', 46, 73, TRUE, 1
);

INSERT INTO messages("message_id", "type", "send_date", "sender_id", "body", "sender_phone", "sentiment_score", "views_count", "is_comment?")
VALUES ('523739', 'message', '2022-04-29T12:00:35', 'OMID_TRADER', 'خرید خوبی در خفولا داشتیم. کاملا خالی از فروشنده شده و تماما امروز رنج منفی زدن. امیدوارم همراهان ما در طرف خریدار بوده باشند و در دام سفته باز نیفتاده باشن.
آرزوی موفقیت دارم براتون.',
'989127654777', 60, 1501, FALSE
);
