(ns novelette-sound.player
  (:require-macros [schema.core :as s])
  (:require [goog.dom :as dom]
            [schema.core :as s]
            [novelette-sound.schemas :as sc]))

(s/defn play-sfx
  "Play a given sfx once."
  [context :- js/AudioContext
   sfx :- sc/AudioData
   output-node :- js/AudioNode]
  (when-not (= :sfx (:type sfx))
    (throw (js/Error. (str "AudioData sfx is not a proper sfx audio: "
                           (pr-str sfx)))))
  (.then (.decodeAudioData context (:data sfx))
         (fn [data]
           (let [source (.createBufferSource context)]
             (set! (.-buffer source) data)
             (.connect source output-node)
             (.start source)))))

(s/defn prepare-audio
  "Prepare an HTML Audio element by creating an appropriate input node."
  [context :- js/AudioContext
   audio :- sc/AudioData]
  (when-not (= :music (:type audio))
    (throw (js/Error. (str "AudioData audio is not a proper music audio: "
                           (pr-str audio)))))
  (.createMediaElementSource context (:data audio)))

(s/defn play-audio
  "Play a given audio."
  [context :- js/AudioContext
   audio :- sc/AudioData
   input-node :- js/AudioNode
   output-node :- js/AudioNode
   loop? :- s/Bool]
  (when-not (= :music (:type audio))
    (throw (js/Error. (str "AudioData audio is not a proper music audio: "
                           (pr-str audio)))))
  (set! (.-loop (:data audio)) loop?)
  (set! (.-currentTime (:data audio)) 0)
  (.connect input-node output-node)
  (.play (:data audio)))
