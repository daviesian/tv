(ns tv.core
  (:use clojure.pprint
        tv.seesaw
        seesaw.core
        seesaw.graphics
        seesaw.chooser
        )
  (:import com.moviejukebox.thetvdb.TheTVDB
           java.net.URL
           javax.imageio.ImageIO))

(def test-state (atom {}))
(defmacro with-exception-printer [& body]
  `(try
     ~@body
     (catch Exception e# (println e#))))

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

(defn list-all-files [dir extension]
  (let [entries (map #(java.io.File. (str (.getAbsolutePath dir) "\\" %)) (.list dir))
        ;;_ (pprint entries)
        files (filter #(and (.isFile %) (re-find extension (.getName %))) entries)
        subdirs (filter #(.isDirectory %) entries)]
    ;; (pprint subdirs)
    (concat files (apply concat (map #(list-all-files % extension) subdirs)))
    ))

(native!)


(def chosen-show (let [video-folder       (choose-file :type :open
                                                       :selection-mode :dirs-only)
                       search-string      (.getName video-folder)
                       shows              (tvdb-show-search search-string)
                       shows-with-banners (filter #(:banner-url %) shows)
                       chosen-show        (promise)
                       show-chooser       (let [f (frame :title "TV Renamer"
                                                         :on-close :dispose
                                                         :size [1024 :by 768])
                                                cs (map (fn [show]
                                                          (let [c (image-canvas (:banner show))]
                                                            (listen c :mouse-clicked (fn [e]
                                                                                       (deliver chosen-show show)
                                                                                       (dispose! f)))
                                                            (future
                                                              (deliver (:banner show) (ImageIO/read (URL. (:banner-url show))))
                                                              (repaint! c))
                                                            c)) shows-with-banners)]

                                            (config! f :content (scrollable (vertical-panel :items cs)))
                                            (listen f :window-closed (fn [e] (when-not (realized? chosen-show)
                                                                              (deliver chosen-show nil))))
                                            f)]
                   (show! show-chooser)
                   (swap! test-state #(assoc % :show @chosen-show))
                   (swap! test-state #(assoc % :directory video-folder))
                   @chosen-show))


(pprint chosen-show)

(defn rename-video-files [directory show]
  (let [video-files (list-all-files directory #".avi|.mp4|.mkv|.flv")
        video-details (map (fn [f]
                             (let [[_ s1 e1 s2 e2 s3 e3 s4 e4 ext] (re-find #"(?i)(?:S(\d+)E(\d+)|(\d+)x(\d+)|\[(\d).?(\d\d)\]|Season (\d+),? Episode (\d+)).*\.(...)$" (.getName f))
                                   s                               (Integer/parseInt (or s1 s2 s3 s4))
                                   e                               (Integer/parseInt (or e1 e2 e3 e4))
                                   name                            (:name (tvdb-episode show s e))
                                   ]
                               {:file f
                                :season s
                                :episode e
                                :name name
                                :show-name (:name show)
                                :new-name (java.io.File. (str (.getParent f) "\\" (.getName directory) " - " s "x" (format "%02d" e) " - " name "." ext))}))
                           video-files)
        ep (tvdb-episode show 1 1)]

    (doseq [v video-details]
      ;(.renameTo (v :file) (v :new-name))
      (pprint v)))))

(rename-video-files (:directory @test-state) (:show @test-state))
