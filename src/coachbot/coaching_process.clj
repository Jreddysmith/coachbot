;
; Copyright (c) 2016, Courage Labs, LLC.
;
; This file is part of CoachBot.
;
; CoachBot is free software: you can redistribute it and/or modify
; it under the terms of the GNU Affero General Public License as published by
; the Free Software Foundation, either version 3 of the License, or
; (at your option) any later version.
;
; CoachBot is distributed in the hope that it will be useful,
; but WITHOUT ANY WARRANTY; without even the implied warranty of
; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
; GNU Affero General Public License for more details.
;
; You should have received a copy of the GNU Affero General Public License
; along with CoachBot.  If not, see <http://www.gnu.org/licenses/>.
;

(ns coachbot.coaching-process
  (:require [coachbot.env :as env]
            [coachbot.messages :as messages]
            [coachbot.slack :as slack]
            [coachbot.storage :as storage]
            [taoensso.timbre :as log]))

(defn start-coaching! [team-id channel user-id]
  (let [ds (env/datasource)

        [access-token bot-access-token]
        (storage/get-access-tokens ds team-id)]
    (storage/add-coaching-user!
      ds (slack/get-user-info access-token user-id))
    (slack/send-message! bot-access-token channel
                         messages/coaching-hello)))

(defn stop-coaching! [team-id channel user-id]
  (let [ds (env/datasource)
        [access-token bot-access-token]
        (storage/get-access-tokens ds team-id)

        user (slack/get-user-info access-token user-id)]
    (storage/remove-coaching-user! ds user)
    (slack/send-message! bot-access-token channel messages/coaching-goodbye)))

(defn register-custom-question! [team-id user-id question]
  (let [ds (env/datasource)
        [access-token _]
        (storage/get-access-tokens ds team-id)

        user (slack/get-user-info access-token user-id)]
    (storage/add-custom-question! ds user question)))

(defn- with-sending-constructs [user-id team-id channel f]
  (let [ds (env/datasource)

        [_ bot-access-token]
        (storage/get-access-tokens (env/datasource) team-id)

        send-fn (partial slack/send-message! bot-access-token
                         (or channel user-id))]
    (f ds send-fn)))

(defn send-question!
  "Sends a new question to a specific individual."
  [{:keys [id asked-qid team-id] :as user} & [channel]]

  (with-sending-constructs
    id team-id channel
    (fn [ds send-fn]
      (storage/next-question-for-sending! ds asked-qid user send-fn))))

(defn send-question-if-previous-answered!
  "Sends a new question to a specific individual."
  [{:keys [id asked-qid answered-qid team-id] :as user} & [channel]]

  (with-sending-constructs
    id team-id channel
    (fn [ds send-fn]
      (if (= asked-qid answered-qid)
        (storage/next-question-for-sending! ds asked-qid user send-fn)
        (storage/question-for-sending ds asked-qid user send-fn)))))

(defn send-questions!
  "Sends new questions to everyone on a given team that has signed up for
   coaching."
  [team-id]
  (doall (map (partial send-question-if-previous-answered!)
              (storage/list-coaching-users (env/datasource) team-id))))

(defn submit-text! [team-id user-email text]
  ;; If there is an outstanding for the user, submit that
  ;; Otherwise store it someplace for a live person to review
  (let [ds (env/datasource)
        [_ bot-access-token] (storage/get-access-tokens ds team-id)

        {:keys [id asked-qid answered-qid asked-cqid answered-cqid]}
        (storage/get-coaching-user ds team-id user-email)]
    (if asked-qid
      (do
        (storage/submit-answer! ds team-id user-email asked-qid asked-cqid text)
        (slack/send-message! bot-access-token id messages/thanks-for-answer))
      (log/warnf "Text submitted but no question asked: %s/%s %s" team-id
                 user-email text))))

(defn- ensure-user [ds access-token team-id user-id]
  (let [{:keys [email] :as user} (slack/get-user-info access-token user-id)
        get-coaching-user #(storage/get-coaching-user ds team-id email)]
    (if-let [result (get-coaching-user)]
      result
      (do
        (storage/add-coaching-user! ds user)
        (storage/remove-coaching-user! ds user)
        (get-coaching-user)))))

(defn next-question! [team_id channel user-id]
  (let [ds (env/datasource)
        [access-token _] (storage/get-access-tokens ds team_id)
        user (ensure-user ds access-token team_id user-id)]
    (send-question! user channel)))

(defn event-occurred! [team-id email]
  (let [{:keys [active hours-since-question] :as user}
        (storage/get-coaching-user (env/datasource) team-id email)

        should-send-question?
        (or (nil? hours-since-question) (>= hours-since-question 16))]
    (when (and active should-send-question?)
      (send-question-if-previous-answered! user))))