(ns data-fetcher.core
  (:require [clj-http.client :as http]
            [taoensso.timbre :as timbre]
            [cheshire.core :as cheshire]))

;; https://tg.i-c-a.su/rss/breakingmash?limit=100


;; (timbre/spy (cheshire/parse-string
;;               (:body (http/get "https://tg.i-c-a.su/json/omid_trader?limit=4"))
;;               true))

;; (defn foo
;;   "I don't do a whole lot."
;;   [x]
;;   (println x "Hello, World!"))

;; {:_ "messages.channelMessages", :inexact false, :pts 3284, :count 1685, :messages
;;  [{:date 1550684110, :silent true, :_ "messageService", :out false, :reply_to {:_ "messageReplyHeader", :reply_to_msg_id 2378}, :peer_id {:_ "peerChannel", :channel_id 1242291809}, :id 2379, :post true, :action {:_ "messageActionPinMessage"}, :legacy false, :media_unread false, :mentioned false, :message nil}
;;   {:date 1550679805, :pinned true, :edit_date 1550684105, :silent false, :_ "message", :edit_hide false, :forwards 10, :post_author "Omid", :out false, :peer_id {:_ "peerChannel", :channel_id 1242291809}, :id 2378, :post true, :noforwards false, :legacy false, :media_unread false, :mentioned false, :from_scheduled false, :message "توقف فعالیت کانال<br />\n<br />\nدوستان عزیز و محترم طبق اطلاعیه سازمان بورس مبنی بر فعالیت و مشاوره در زمینه بازار سرمایه تا اطلاع ثانوی به جهت دریافت مجوز های لازمه از سازمان بورس کانال راتعلیق خواهم کرد.<br />\nقابل توجه دوستانی که در مورد مشاوره یا vip سوال میفرمایند. بنده هیچ گونه کانال vip ندارم و سبد گردانی هم انجام نخواهم داد لطفا سوال نفرمایید.<br />\nشاد و پیروز باشید.", :views 2350}
;;   {:date 1550511584, :pinned false, :silent false, :_ "message", :edit_hide false, :forwards 1, :post_author "Omid", :out false, :peer_id {:_ "peerChannel", :channel_id 1242291809}, :id 2362, :post true, :noforwards false, :legacy false, :media_unread false, :mentioned false, :from_scheduled false, :message "🔹انس جهانی 1327<br />\n<br />\n🔹مثقال طلا  1.800.000<br />\n🔹گرم 18 عیار 415.500<br />\n<br />\n🔹سکه امامی 4،480،000<br />\n🔹نیم سکه 2،470،000", :views 2545}
;;   {:date 1550393763, :pinned false, :silent false, :_ "message", :entities [{:_ "messageEntityHashtag", :offset 0, :length 11}], :edit_hide false, :forwards 12, :post_author "Omid", :out false, :peer_id {:_ "peerChannel", :channel_id 1242291809}, :id 2337, :post true, :noforwards false, :legacy false, :media_unread false, :media {:_ "messageMediaPhoto",
;;                                                                                                                                                                                                                                                                                                                                             :photo {:_ "photo", :has_stickers false, :id 6001435396036210074, :access_hash 7100017193023924306, :file_reference {:_ "bytes", :bytes "AkoL3mEAAAkhYn0cmUomtvqRVOatYUJGrhbvq10="}, :date 1550393762, :sizes [{:_ "photoStrippedSize", :type "i", :bytes {:_ "bytes", :bytes "ARwo0aaWwcYp1Mb7w6VTZKQhQn+Nh9KdnnFL3puf3nalcdh1FHeiqWpLQtNP3hSmofMbNS1caZN37U3+Oo/MbFHmNgmjlHcm70UiHK5NFNEs"}, :inflated {:_ "bytes", :bytes "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDACgcHiMeGSgjISMtKygwPGRBPDc3PHtYXUlkkYCZlo+AjIqgtObDoKrarYqMyP/L2u71////m8H////6/+b9//j/2wBDASstLTw1PHZBQXb4pYyl+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj/wAARCAAcACgDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwDRppbBxinUxvvDpVNkpCFCf42H0p2ecUvem5/edqVx2HUUd6KpaktC00/eFKah8xs1LVxpk3ftTf46j8xsUeY2CaOUdybvRSIcrk0U0Sz/2Q=="}} {:_ "photoSize", :type "m", :w 320, :h 226, :size 15691} {:_ "photoSize", :type "x", :w 800, :h 566, :size 56644} {:_ "photoSize", :type "y", :w 1280, :h 906, :size 100280}], :dc_id 4}}, :mentioned false, :from_scheduled false, :message "#باتلاق_ضرر", :views 2120}],
;;  :chats
;;  [{:min false, :date 1528523758, :creator false, :scam false, :has_link false, :_ "channel", :broadcast true, :call_active false, :megagroup false, :username "omid_trader", :fake false, :has_geo false, :title "OMID TRADER", :access_hash 3479878662606531776, :restricted false, :photo {:_ "chatPhoto", :has_video false, :photo_id 1500964970168428513, :stripped_thumb {:_ "bytes", :bytes "AQgI0d05uduCEz1x2ooooBs/"}, :dc_id 4}, :id 1242291809, :gigagroup false, :slowmode_enabled false, :signatures false, :call_not_empty false, :noforwards false, :verified false, :left true}],
;;  :users [{:deleted false, :min false, :first_name "Channel", :bot true, :scam false, :_ "user", :support false, :username "Channel_Bot", :fake false, :bot_inline_geo false, :access_hash 5729957751266450063, :restricted false, :photo {:_ "userProfilePhoto", :has_video false, :photo_id 587627495930570665, :stripped_thumb {:_ "bytes", :bytes "AQgIsfL5X+1nNFFFcDOtHw=="}, :dc_id 1}, :id 136817688, :bot_chat_history false, :mutual_contact false, :apply_min_photo true, :bot_info_version 4, :bot_nochats false, :contact false, :self false, :verified false}],
;;  :sponsored_messages []}

;; {:date           1550511584,
;;  :pinned         false,
;;  :silent         false,
;;  :_              "message",
;;  :edit_hide      false,
;;  :forwards       1,
;;  :post_author    "Omid",
;;  :out            false,
;;  :peer_id
;;  {:_ "peerChannel", :channel_id 1242291809},
;;  :id             2362,
;;  :post           true,
;;  :noforwards     false,
;;  :legacy         false,
;;  :media_unread   false,
;;  :mentioned      false,
;;  :from_scheduled false,
;;  :message        "🔹انس جهانی 1327<br />\n<br />\n🔹مثقال طلا  1.800.000<br />\n🔹گرم 18 عیار 415.500<br />\n<br />\n🔹سکه امامی 4،480،000<br />\n🔹نیم سکه 2،470،000",
;;  :views          2545}



;; (timbre/spy (cheshire/parse-string
;;               (:body (http/get "https://tg.i-c-a.su/json/omid_trader?limit=4"))
;;               true))
