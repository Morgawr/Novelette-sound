(ns novelette-sound.loader
  (:require-macros [schema.core :as s])
  (:require [goog.dom :as dom]
            [schema.core :as s]
            [novelette-sound.schemas :as sc]))

; This file takes care of loading sound resources.
; It provides functions and callbacks to load various sounds and their given
; id representation.

(s/defn create-context
  "Create and initialize a new AudioContext."
  []
  (js/AudioContext.))

(s/defn load-data
  "Load an audio file in memory, returning a nil atom to be filled with the
  desired AudioData when successfully loaded."
  [uri :- s/Str
   audio-type :- sc/AudioType]
  (let [result (atom nil)
        window (dom/getWindow)
        load-sfx (fn [onerror]
                   (let [request (js/XMLHttpRequest. )]
                     (.open request "GET" uri true)
                     (set! (.-responseType request) "arraybuffer")
                     (set! (.-onload request) #(reset! result
                                                       (sc/AudioData.
                                                         (.-response request)
                                                         audio-type)))
                     (set! (.-onerror request)
                           #(.setTimeout window onerror 500))
                     (.send request)))
        load-music (fn [onerror]
                     (let [sound (js/Audio. )]
                       (.addEventListener sound "loadeddata"
                                          #(reset! result (sc/AudioData.
                                                            sound audio-type)))
                       (set! (.-onerror sound)
                           #(.setTimeout window onerror 500))
                       (set! (.-src sound) uri)))]
    (cond
      (= audio-type :sfx)
      (load-sfx (fn recurse [] (load-sfx recurse)))
      (= audio-type :music)
      (load-music (fn recurse [] (load-music recurse)))
      :else
      (throw (js/Error. (str "Unrecognized audio type " audio-type))))
    result))

(s/defn multi-load-data
  "Load multiple audio files in memory."
  [uris :- [{:uri s/Str
             :id sc/id
             :type sc/AudioType}]]
  (map
    (fn [data]
      (let [{:keys [uri id type]} data]
        [id (load-data uri type)]))
    uris))

(s/defn verify-loaded
  "Verify if the given bulk of data has been loaded into memory or not.
  It returns a tuple with the given data, number of how many uris have been
  loaded, number of how many uris have yet to load and a boolean stating
  whether or not everything was loaded, in that order.
  i.e.: (data loaded-num to-load-num finished-loading?)"
  [data :- s/Any]
  (let [to-load (atom 0)
        loaded (atom 0)]
    [(doall (map (fn [[id media]]
                   (cond
                     (not (instance? cljs.core.Atom media))
                     (do
                       (swap! loaded inc)
                       [id media])
                     @media
                     (do
                       (swap! loaded inc)
                       [id @media])
                     :else
                     (do
                       (swap! to-load inc)
                       [id media])))
                 data))
     @loaded @to-load (zero?  @to-load)]))
