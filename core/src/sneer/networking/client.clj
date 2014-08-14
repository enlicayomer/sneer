(ns sneer.networking.client
  (:require [sneer.networking.udp :as udp]
            [clojure.core.async :as async :refer [<! >!]])
  (:import [java.net InetSocketAddress]
           [sneer.commons SystemReport]))

(defn start [puk & [server-host server-port]]

  (let [packets-in (async/chan)
        packets-out (async/chan)
        to-server (async/chan 1)]

    (async/thread

      ; ensure no network activity takes place on caller thread to workaround android limitation
      (let [server-addr (InetSocketAddress. (or server-host "dynamic.sneer.me") (or server-port 5555))
            udp-server (udp/serve-udp packets-in packets-out)
            ping [server-addr {:intent :ping :from puk}]]

        ; server ping loop
        (async/go-loop []
          (when  (>! packets-out ping)
            (<! (async/timeout 20000))
            (recur)))

        ; just report received packets for now
        (async/go-loop []
          (when-let [packet (<! packets-in)]
            (SystemReport/updateReport "packet" packet)
            (recur)))

        (async/pipe (async/map (fn [value] [server-addr value]) [to-server])
                    packets-out)))

    {:packets-out packets-out :to-server to-server}))

(defn stop [client]
  (async/close! (:packets-out client)))
