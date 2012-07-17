(ns tv.core
  (:use clojure.pprint
        quil.core)
  (:import com.moviejukebox.thetvdb.TheTVDB))


;; TVDB Stuff
(defn show-to-clojure-map [show]
  {:id (.getId show)
   :name (.getSeriesName show)
   :banner (promise)
   :banner-url (.getBanner show)
   :overview (.getOverview show)
   :imdb-id (.getImdbId show)})

(defn episode-to-clojure-map [episode]
  {:id (.getId episode)
   :name (.getEpisodeName episode)
   :episode-number (.getEpisodeNumber episode)
   :season-number (.getSeasonNumber episode)
   :rating (.getRating episode)
   :banner (.getFilename episode)
   :show-id (.getSeriesId episode)
   :season-id (.getSeasonId episode)
   :overview (.getOverview episode)})

(def tvdb (TheTVDB. "FB7CDE769D2D01C1"))

(defn tvdb-show-search [name]
  (map show-to-clojure-map (.searchSeries tvdb name "en")))

(defn tvdb-episode [show season-number episode-number]
  (episode-to-clojure-map (.getEpisode tvdb (show :id) season-number episode-number "en")))

(defn tvdb-all-episodes [show]
  (map episode-to-clojure-map (.getAllEpisodes tvdb (show :id) "en")))

;; Quil Stuff

(def i (atom nil))
(defn setup []
  (smooth)
  (frame-rate 30)
  (fill 200)
  (stroke 0 0))

(defn draw []
  (stroke (random 255))             ;;Set the stroke colour to a random grey
  (stroke-weight (random 10))       ;;Set the stroke thickness randomly
  (fill (random 255))               ;;Set the fill colour to a random grey

  (let [diam (random 100)           ;;Set the diameter to a value between 0 and 100
        x    (random (width))       ;;Set the x coord randomly within the sketch
        y    (random (height))]     ;;Set the y coord randomly within the sketch
    (ellipse x y diam diam))

  ;(println (quil.applet/current-applet))
  ;(image @i 0 0)
                                        ;(image img 0 0 )
  )       ;;Draw a circle at x y with the correct diameter)




(let [search-string   "Nikita"
      shows           (tvdb-show-search search-string)
      chosen-show-idx (promise)
      show-chooser    (sketch
                       :title "TV Renamer"
                       :setup (fn []
                                (setup)
                                (doseq [s shows]
                                  (deliver (:banner s) (request-image (:banner-url s)))))
                       :draw (fn []
                                        ;(draw)
                               (rect 0 0 800 600)
                               (let [b-height (.height @(:banner (first shows)))]
                                 (doseq [[i s] (map vector (range) shows)]
                                   (if (== i (int (/ (mouse-y) 200)))
                                     (tint 255 255)
                                     (tint 255 128))
                                   (image @(:banner s) 0 (* i 200)))))
                       :mouse-clicked (fn []
                                        (deliver chosen-show-idx (/ (mouse-y) 200)))
                       :size [800 600]
                       )]



  (let [chosen-show (nth shows @chosen-show-idx)
        _           (sketch-close show-chooser)
        ep          (tvdb-episode chosen-show 1 1)]
    (pprint ep)
    )

  )
