# offpost

receive mail, send it off post

a light wrapper around [subethasmtp](https://code.google.com/p/subethasmtp/).

```clojure
(ns my-ns
  (:require [clojure.java.io :as io]
            [offpost.server :refer [router boot-server]))

(defn -main [& args]
 (->
   (router
     "mailbox@domain.com"
     (fn [inputstream address]
       (println "Got mail for" address "saving to message.txt")
       (io/copy inputstream (file "message.txt"))

     #"robot-(\d+)@domain.com"
     (fn [inputstream [address id]]
       (println "Got mail for" address)
       (println "Robot ID #" id)
       (io/copy inputstream (file (str "robots/" id ".txt"))))
   (boot-server 25000)))
```
