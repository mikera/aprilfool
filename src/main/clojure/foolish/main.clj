(ns foolish.main
  (:use mikera.cljutils.error)
  (:import [foolish App Colours]
           [mikera.engine PersistentTreeGrid IPointVisitor])
  )


(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defonce ^{:dynamic true :tag App} app nil)

(defn set-tile [game x y z c]
  (let [^PersistentTreeGrid tile (:tiles game)]
    (assoc game :tiles (.set tile (int x) (int y) (int z) c))))

(defn get-tile [game x y z]
  (let [^PersistentTreeGrid tile (:tiles game)]
    (.get tile (int x) (int y) (int z))))

(defn fill-rect [game x1 y1 x2 y2 c]
  (reduce (fn [game [x y]]
            (set-tile game x y 0 c))
          game
          (for [x (range x1 (inc x2))
                y (range y1 (inc y2))]
            [x y])))


(defn create-game []
  (let [game{:tiles PersistentTreeGrid/EMPTY
             :hero-loc [0 0]}]
    (-> game
      (set-tile 0 0 0 102)
      (fill-rect 0 0 10 0 102))
    ))

(defn process-click 
  "Processes a click event from the GUI"
  ([event]
    (let [x (:x event) y (:y event) z (:z event)
          ;; game (game-state app)
          event-type (:type event)
          
          ]
	      (cond
          :else
            (error "Click event type not recognised: " event-type)))))

(defn process-user-events 
  "Handle all game UI events accumulated from the App"
  ([]
    (let [events (.retrieveEvents app)]
      (doseq [event events]
        (let [type (:event event)]
          (cond
            (= :click type)
              (do
                ;; (println "Click event: " event)
                (process-click event))
            :else
              (println (str "Unrecognised event: " event))))))))

(defn main-eval
  "Evaluates a command in the main namespace"
  ([command]
    (binding [*ns* (find-ns 'foolish.main)] 
      (eval (read-string command)))))

(def last-time
  (atom nil))

(defn launch-main-loop 
  "Runs the main game loop running infinitely in a future unless an excpetion occurs"
  ([]
    (reset! last-time (System/currentTimeMillis))
    (future 
      (try
       (loop [] 
         (let [time (System/currentTimeMillis)
               t (* 0.001 (- time (long @last-time)))]
           (reset! last-time time)
           ;; (update-game! game/run-timestep t) 
           )
         (process-user-events)
         (Thread/sleep 20)
         (when (.isRunning app) 
           (recur)))
       (catch Throwable t
         (.printStackTrace t)))
      (println "Main loop exited."))))

(defn -main 
  ([] (-main nil))
  ([args]
    (println "Foolish Game starting...")
    (def app (App. "Foolish"))
    (set! (.renderGameState app) (create-game))
    (when-not args 
      (println "Launched from REPL")
      (set! (.replLaunched app) true))
    (launch-main-loop))) 

(comment

  ) 
