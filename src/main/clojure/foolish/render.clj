(ns foolish.render
  (:import [foolish App Colours]
           [mikera.vectorz Vector4]
           [clojure.lang IPersistentVector]
           [mikera.engine PersistentTreeGrid IPointVisitor])
  )

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

; main rendering code. Should call appropriate rendering functions

(def ^:dynamic *app* nil)
(def ^:dynamic *game* nil)
(def ^:dynamic *cursor* nil)

(def c (Vector4.))

;; ===================================================================================
;; Visitor for Map Tile rendering

(def ^IPointVisitor map-visitor
  (reify IPointVisitor
    (visit [this ^int x ^int y ^int z value]
      (let [^App app *app*
            t (double (.getTime app))
            ^Vector4 c c
            sx (* x 8)
            sy (* y 8)
            tx (* (mod value 100) 8)
            ty (* (quot value 100) 8)]
        (.set c Colours/WHITE)
        (.drawSprite app 
          sx sy 8 8 ;; screen rectangle
          z  ;; depth 
          tx,ty,8,8 ;; source texture rectangle
          c)
        ;; (println tx ty)
        ))))

;; ===================================================================================
;; Visitor for Map Tile rendering

(def ^IPointVisitor thing-visitor
  (reify IPointVisitor
    (visit [this ^int x ^int y ^int z values]
      (let [^IPersistentVector values values
            ^App app *app*
            t (double (.getTime app))
            cz (int (.cursorZ app))
            ^Vector4 c c]
        (when (<= z cz)
          ;; iterate over things in cell
          (dotimes [i (.count values)]
            (let [thing (.nth values i)
                  sprx (long (or (:spritex thing) 31))
                  spry (long (or (:spritey thing) 1))
                  ^Vector4 colour (or (:colour thing) Colours/WHITE)
                  _ (.set c colour)
                  colour-noise (:colour-noise thing)
                  _ (when colour-noise
                      (Colours/addNoise c (double colour-noise) (Colours/hash (long (:id thing)))))
                  _ (when (< z cz)
                      (.multiply c Colours/GREY_90)) 
                  sx (- (* (- x y) 24) 16)
                  sy (+ (* (+ x y) 12) (* z -32) 16)
                  tx (* 32 sprx)
                  ty (* 32 spry)
                  tw 32
                  th 32]
            
              (.drawSprite app 
                sx sy 32 32 ;; screen rectangle
                (+ x y z)  ;; depth 
                tx,ty,tw,th ;; source texture rectangle
                c)))
          )))))

;; ===============================================================================
;; main renderer function

(defn render-sprites 
  ([game ^App app]
    (binding [*app* app
             *game* game]
      
     ;; render all map tiles
     (let [^PersistentTreeGrid map (:tiles game) 
           hloc (:hero-loc game)
           hx (float (hloc 0))
           hy (float (hloc 1))
           scale (.DRAW_SCALE app)
           scrx (.scrollX app)
           scry (.scrollY app)
           x1 (int (/ scrx scale))
           y1 (int (/ scry scale))
           x2 (+ x1 (/ (.width app) scale))
           y2 (+ y1 (/ (.height app) scale))
           ]
       
      (.drawSprite app 
         (- hx 8) (- hy 16) 16 16 ;; screen rectangle
          0.0  ;; depth
          0,0,16,16 ;; source texture square block
          Colours/WHITE)
       
      (.visitPoints map map-visitor 
        x1 y1 -2 x2 y2 2
        )
       
;     ;; render things
;     (let [^PersistentTreeGrid things (:things game) 
;           ^Location cursor *cursor*
;           cx (.x cursor) cy (.y cursor) cz (.z cursor)
;           ]
;      (.visitPoints things thing-visitor 
;        ;; (- cx 50) (- cy 50) (- cz 50) (+ cx 50) (+ cy 50) cz
;        0 0 (- cz 50) 100 100 cz
;        )
;       
;    ;; conditionally draw mouse cursor if rendering cursor location
;      (let [t (double (.getTime app))
;            alpha (+ 0.4 (* 0.3 (Math/sin (* 15 t))))
;            sx (- (* (- cx cy) 24) 24)
;            sy (+ (* (+ cx cy) 12) (* cz -32))
;            ^Vector4 c c] 
;        (.setValues c 1.0 1.0 0.0 alpha) ;; semi-transparent yellow
;        (.drawSprite app 
;          sx sy 48 64 ;; screen rectangle
;          (+ cx cy cz)  ;; depth
;          (+ 8 (* 64 17)),0,48,64 ;; source texture square block
;          c))
;       
;      )
      
          
     ) )))
