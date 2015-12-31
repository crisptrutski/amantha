(ns amantha.utils.server
  (:import
    [java.net ServerSocket InetAddress]))

(defn get-free-port!
  "Warning: opens and closes socket to determine this, so not called automatically"
  []
  (let [socket (ServerSocket. 0)
        port   (.getLocalPort socket)]
    (.close socket)
    port))

(defn guess-host-name []
  (.getCanonicalHostName (InetAddress/getLocalHost)))

(defn ensure-port [conf]
  (if (:port conf) conf (assoc conf :port (get-free-port!))))

