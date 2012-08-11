(ns tv.seesaw
  (:use seesaw.core
        seesaw.graphics)
  (:import java.net.URL
           java.awt.image.AffineTransformOp
           java.awt.geom.AffineTransform))

(defn draw-image [g2d i x y]
  (.drawImage g2d i (AffineTransformOp. (AffineTransform.) nil) x y))

(defn image-canvas [i-promise]
  (let [done? (atom false)]
    (canvas :size [1 :by 1];; :background "#ffffe9"
            :paint (fn [c g]
                     (when (realized? i-promise)
                       (when-not @done?
                         (config! c :size [(.getWidth @i-promise) :by (.getHeight @i-promise)])
                         (.revalidate c)
                         (reset! done? true))
                       (draw-image g @i-promise 0 0))))))
