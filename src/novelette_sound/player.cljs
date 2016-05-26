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

(s/defn stop-audio
  "Stop a given audio."
  [audio :- sc/AudioData]
  (when-not (= :music (:type audio))
    (throw (js/Error. (str "AudioData audio is not a proper music audio: "
                           (pr-str audio)))))
  (.pause (:data audio)))

; This is a map with the math functions for transitioning of values over time.
(def transition-map
  {
   :linear #(+ (/ (* %1 %2) %3) %4)
   :quadratic #(+ (* %1 (/ (Math/pow %2 2) (Math/pow %3 2))) %4)
   :cubic #(+ (* %1 (/ (Math/pow %2 3) (Math/pow %3 3))) %4)
   :quartic #(+ (* %1 (/ (Math/pow %2 4) (Math/pow %3 4))) %4)
   :sinusoidal #(+ (* -1 %1 (Math/cos (* (/ %2 %3) (/ Math/PI 2)))) %1 %4)
   :exponential #(+ (* %1 (Math/pow 2 (* 10 (dec (/ %2 %3))))) %4)
   })

(s/defn transition-volume
  "Interpolate and transition the volume of a GainNode within a given
  timeframe in milliseconds. When the transition is done, execute end-fn."
  ([node :- js/GainNode
    end-volume :- s/Num
    duration :- s/Int
    interpolation :- s/Keyword
    end-fn :- (s/cond-pre sc/function (s/pred nil?))]
   (when-not (<= 0 end-volume 1)
     (throw (js/Error. "Interpolation value must be between 0.0 and 1.0")))
   (let [start-time (.getTime (js/Date.))
         start-volume (.-value (.-gain node))
         fixed-step 17 ; update every 17ms
         change (- end-volume start-volume)]
     ((fn step [last-time total]
        (let [current-time (.getTime (js/Date.))
              delta (- current-time last-time)
              new-total (+ total delta)]
          (if (>= total duration)
            (do
              (set! (.-value (.-gain node)) end-volume)
              (when-not (nil? end-fn)
                (end-fn)))
            (do
              (set! (.-value (.-gain node))
                    ((interpolation transition-map)
                     change new-total duration start-volume))
              (.setTimeout (dom/getWindow)
                           step fixed-step current-time new-total )))))
      start-time 0)))
  ([node :- js/GainNode
    end-volume :- s/Num
    duration :- s/Int
    interpolation :- s/Keyword]
   (transition-volume node end-volume duration interpolation nil)))
