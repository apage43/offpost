(defproject offpost "0.1.0-SNAPSHOT"
  :description "receive mail, send it off post"
  :url "http://github.com/apage43/offpost"
  :license {:name "WTFPL"
            :url "http://www.wtfpl.net/"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-http "0.6.3"]
                 [cheshire "5.0.1"]
                 [org.subethamail/subethasmtp "3.1.3"]])
