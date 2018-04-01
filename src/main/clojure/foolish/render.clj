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
            cz (int (.cursorZ app))
            ^Vector4 c c]
        (when (<= z cz)
          ;; main tile
          (when-let [bshape (:block-shape value)] ;; we have a block tile
            (let [sprx (long (or (:spritex value) 16))
                  spry (long (or (:spritey value) 1))
                  ^Vector4 colour (or (:colour value) Colours/WHITE)
                  _ (.set c colour)
                  colour-noise (:colour-noise value)
                  _ (when colour-noise
                      (Colours/addNoise c (double colour-noise) (Colours/hash x y z)))
                  _ (when (< z cz)
                      (.multiply c Colours/GREY_90)) 
                  sx (- (* (- x y) 24) 24)
                  sy (+ (* (+ x y) 12) (* z -32))
                  tx (+ 8 (* 64 sprx))
                  ty (* 64 spry)
                  tw 48
                  th 64]
            
            (.drawSprite app 
              sx sy 48 64 ;; screen rectangle
               (+ x y z)  ;; depth 
               tx,ty,tw,th ;; source texture rectangle
               c)))
          
          ;; floor covering
          (when-let [fvalue (:floor value)]
            (let [sprx (long (or (:spritex value) 16)) ;; note: use spritex from underlying tile block
                  spry (long (or (:spritey fvalue) 1))
	                ^Vector4 colour (or (:colour fvalue) Colours/WHITE)
	                _ (.set c colour)
	                colour-noise (:colour-noise fvalue)
	                _ (when colour-noise
	                    (Colours/addNoise c (double colour-noise) (Colours/hash x y z)))
	                _ (when (< z cz)
	                    (.multiply c Colours/GREY_90)) 
	                sx (- (* (- x y) 24) 24)
	                sy (+ (* (+ x y) 12) (* z -32))
	                tx (+ 8 (* 64 sprx))
	                ty (* 64 spry)
	                tw 48
	                th 64]
	            
              (.drawSprite app 
                sx sy 48 64 ;; screen rectangle
                (+ x y z)  ;; depth
                tx,ty,tw,th ;; source texture rectangle
                c)))
          
          ;; vegetation
          (when-let [vvalue (:vegetation value)]
            (let [sprx (long (or (:spritex vvalue) 0))
                  spry (long (or (:spritey vvalue) 0))
	                ^Vector4 colour (or (:colour vvalue) Colours/WHITE)
	                _ (.set c colour)
	                colour-noise (:colour-noise vvalue)
	                _ (when colour-noise
	                    (Colours/addNoise c (double colour-noise) (Colours/hash x y z)))
	                _ (when (< z cz)
	                    (.multiply c Colours/GREY_90)) 
	                sx (- (* (- x y) 24) 24)
	                sy (+ (* (+ x y) 12) (* z -32))
	                tx (+ 8 (* 64 sprx))
	                ty (* 64 spry)
	                tw 48
	                th 64]
	            
              (.drawSprite app 
                sx sy 48 64 ;; screen rectangle
                (+ x y z)  ;; depth
                tx,ty,tw,th ;; source texture rectangle
                c))))))))

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
    (comment
      (binding [*app* app
               *game* game
               *cursor* (ocore/loc (.cursorX app) (.cursorY app) (.cursorZ app))]
      
       ;; render all map tiles
       (let [^PersistentTreeGrid map (:world game) 
             ^Location cursor *cursor*
             cx (.x cursor) cy (.y cursor) cz (.z cursor)
             ]
        (.visitPoints map map-visitor 
          ;; (- cx 50) (- cy 50) (- cz 50) (+ cx 50) (+ cy 50) cz
          0 0 (- cz 50) 100 100 cz
          )
       
       ;; render things
       (let [^PersistentTreeGrid things (:things game) 
             ^Location cursor *cursor*
             cx (.x cursor) cy (.y cursor) cz (.z cursor)
             ]
        (.visitPoints things thing-visitor 
          ;; (- cx 50) (- cy 50) (- cz 50) (+ cx 50) (+ cy 50) cz
          0 0 (- cz 50) 100 100 cz
          )
       
      ;; conditionally draw mouse cursor if rendering cursor location
        (let [t (double (.getTime app))
              alpha (+ 0.4 (* 0.3 (Math/sin (* 15 t))))
              sx (- (* (- cx cy) 24) 24)
              sy (+ (* (+ cx cy) 12) (* cz -32))
              ^Vector4 c c] 
          (.setValues c 1.0 1.0 0.0 alpha) ;; semi-transparent yellow
          (.drawSprite app 
            sx sy 48 64 ;; screen rectangle
            (+ cx cy cz)  ;; depth
            (+ 8 (* 64 17)),0,48,64 ;; source texture square block
            c))
       
        )
      
          
       ) ))))
