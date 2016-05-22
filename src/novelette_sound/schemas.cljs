(ns novelette-sound.schemas
  (:require-macros [schema.core :as s])
  (:require [schema.core :as s]
            [cljs.reader]))

(s/defschema function (s/pred fn? 'fn?))

; The id of an element can either be a string or a keyword (prefer using keywords).
(s/defschema id (s/cond-pre s/Str s/Keyword))

; The type of audio that can be loaded/used. It can either be a short sound
; effect or a longer, more complex, piece of music.
(s/defschema AudioType (s/enum :sfx :music))

; This is the audio data as returned by the load-data function.
(s/defrecord AudioData
  [data :- (s/cond-pre js/ArrayBuffer js/Audio)
   type :- AudioType])
(cljs.reader/register-tag-parser! "novelette-sound.schemas.AudioData"
                                  map->AudioData)
