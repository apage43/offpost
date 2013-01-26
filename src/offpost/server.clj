(ns offpost.server
  (:import [org.subethamail.smtp MessageHandlerFactory MessageHandler
            RejectException]
           org.subethamail.smtp.server.SMTPServer)
  (:require [clojure.java.io :as io]))

(defn- start-server [^MessageHandlerFactory mhf port]
  (doto (SMTPServer. mhf)
    (.setPort port)
    (.start)))

(defn- message-handler [router]
  (let [mfrom (atom "")
        mdeliver (atom (fn [_]))
        rctx (atom nil)]
    (reify MessageHandler
      (from [_ from] (reset! mfrom from))
      (recipient [_ rcpt] 
        (or (when-let [[routectx handler] (router rcpt @mfrom)]
              (reset! rctx routectx)
              (reset! mdeliver handler))
            (throw (RejectException. 533 (str "<" rcpt "> address unknown.")))))
      (data [_ istream] (@mdeliver istream @rctx))
      (done [_]))))

(defn- handler-factory [router]
  (reify MessageHandlerFactory
    (create [_ _ctx] (message-handler router))))

;; Route set is a seq of vecs
;; [Pred Deliverfn]
;; Where Pred is a (fn accept [to]) or a
;; regex to match "from" on
;;
;; router returns nil or [handler fn, pred-rval]

(defn make-router [routes]
  (fn [to _from]
    (loop [[[pred handler] & more] routes]
      (if-let [rval (pred to)]
        [rval handler]
        (if more (recur more))))))

(defn- regex? [o] (instance? java.util.regex.Pattern o))

(defn wrap-pred [pred]
  "Predicate helper. If `pred` is a string, returns #(if (= pred %) %), if
   a regex, returns (partial re-matches pred), otherwise returns pred."
  (cond
    (regex? pred) (partial re-matches pred)
    (string? pred) #(if (= pred %) %)
    :else pred))

(defmacro router [& routes]
  `(make-router
     (for [[pred# handler#] ~(mapv vec (partition 2 routes))]
       [(wrap-pred pred#) handler#])))

(defn boot-server [router & [port]]
  (start-server (handler-factory router) (or port 25000)))

(defn byteslurp
  "Read an io/copy'able to the end and give back the byte array"
  [stream]
  (let [baos (java.io.ByteArrayOutputStream.)]
    (io/copy stream baos)
    (.toByteArray baos)))

(defn strify
  "Read an io/copy'able to the end and give back a string"
  [stream]
  (String. (byteslurp stream)))

(comment
  (def my-router
    (router
      #"bug-(\d+)@localhost"
      (fn [stream [_ id]]
        (println "Got message for bug: " id)
        (println (strify stream)))

      #"meat-(\d+)@localhost"
      (fn [stream [_ id]]
        (println "Got message for meat: " id)
        (println (strify stream)))))

  (do (my-router "bug-12@localhost" "noone"))
  (def s (boot-server my-router))
  (.stop s))
