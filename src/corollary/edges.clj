(ns corollary.edges)

(def colours
  { "TryWeakerQuestion" "#FF0000"
    "LookForSomethingMoreSpecific" "#0000FF"
    "ClarifyQuestion" "#00FF00"
    "EvaluateApproach" "#C000C0"
    "ClarifyConcept" "#00C0C0"
    "MoreDetails" "#C0C000"
    "default" "#A0A0A0"
    })

(defn get-edge-colour [edge-type]
  (get colours edge-type (colours "default")))


