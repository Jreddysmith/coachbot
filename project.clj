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

(defproject coachbot "0.1.0-SNAPSHOT"
  :description "Simple, elegant, automatic motivation"
  :dependencies [[camel-snake-kebab "0.4.0"]
                 [clj-cron-parse "0.1.4"]
                 [clj-http "3.4.1"]
                 [clj-time "0.13.0"]
                 [clojurewerkz/quartzite "2.0.0" :exclusions [clj-time]]
                 [com.fzakaria/slf4j-timbre "0.3.2"]
                 [com.h2database/h2 "1.4.193"]
                 [com.zaxxer/HikariCP "2.5.1"]
                 [com.taoensso/timbre "4.8.0"]
                 [frankiesardo/linked "1.2.9"]
                 [http-kit "2.2.0"]
                 [instaparse "1.4.5"]
                 [metosin/compojure-api "1.1.10"]
                 [mysql/mysql-connector-java "6.0.5"]
                 [org.clojars.scstarkey/honeysql "0.8.2"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/java.jdbc "0.6.1"]
                 [org.flywaydb/flyway-core "4.0.3"]
                 [semantic-csv "0.1.0"]
                 [slingshot "0.12.2"]]
  :ring {:handler coachbot.handler/app}
  :uberjar-name "server.jar"
  :test-paths ["spec"]
  :min-lein-version "2.0.0"
  :profiles {:dev {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]
                                  [cheshire "5.6.3"]
                                  [ring/ring-mock "0.3.0"]
                                  [speclj "3.3.2"]]
                   :plugins [[lein-ancient "0.6.10"]
                             [lein-bikeshed "0.4.1"]
                             [lein-kibit "0.1.3"]
                             [ikitommi/lein-ring "0.9.8-FIX"]
                             [speclj "3.3.2"]]}
             :uberjar {:aot :all
                       :dependencies [[javax.servlet/servlet-api "2.5"]]}})