(ns sneer.profile
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.rx :refer [subject* shared-latest]]
   [sneer.commons :refer [produce!]]
   [sneer.party :refer [party-puk]])
  (:import
   [sneer Profile]
   [sneer.tuples Tuple TupleSpace]
   [rx.subjects Subject]))

(defn reify-profile [party ^TupleSpace tuple-space]

  (letfn [(payloads-of [type]
            (rx/map
             (fn [^Tuple tuple] (.payload tuple))
             (.. tuple-space
                 filter
                 (type type)
                 (author (party-puk party))
                 tuples)))

          (payload-subject [tuple-type]
            (let [latest (shared-latest (payloads-of tuple-type))
                  publish #(.. tuple-space
                               publisher
                               (type tuple-type)
                               (pub %))]
              (subject*
               latest
               (reify rx.Observer
                 (onNext [this value]
                   (publish value))))))
          
          (local-payloads-of [type]
            (let [tuple (.. tuple-space
                            filter
                            (author (party-puk party))
                            (type type)
                            localTuples
                            toBlocking
                            (firstOrDefault nil))]))]

    (let [^Subject preferred-nickname (payload-subject "profile/preferred-nickname")
          ^Subject own-name (payload-subject "profile/own-name")
          ^Subject selfie (payload-subject "profile/selfie")
          ^Subject city (payload-subject "profile/city")
          ^Subject country (payload-subject "profile/country")
          isOwnNameLocallyAvailable (not (nil? (local-payloads-of "profile/own-name")))]

      (reify Profile
        (ownName [this]
          (.asObservable own-name))
        (setOwnName [this value]
          (rx/on-next own-name value))
        (selfie [this]
          (.asObservable selfie))
        (setSelfie [this value]
          (rx/on-next selfie value))
        (preferredNickname [this]
          (.asObservable preferred-nickname))
        (setPreferredNickname [this value]
          (rx/on-next preferred-nickname value))
        (city [this]
          (.asObservable city))
        (setCity [this value]
          (rx/on-next city value))
        (country [this]
          (.asObservable country))
        (setCountry [this value]
          (rx/on-next country value))
        (isOwnNameLocallyAvailable [this]
          isOwnNameLocallyAvailable)))))

(defn produce-profile [tuple-space profiles party]
  (produce! #(reify-profile % tuple-space) profiles party))
