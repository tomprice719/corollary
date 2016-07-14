(ns corollary.edges)

(def colours
  { "TryWeakerQuestion" "#FF0000"
    "LookForSomethingMoreSpecific" "#00FFFF"
    "ClarifyQuestion" "#00FF00"
    "EvaluateApproach" "#FF00FF"
    "ClarifyConcept" "#0000FF"
    "MoreDetails" "#FFFF00"
    })

(def default-colour "#808080")

(defn get-edge-colour [edge-type]
  (get colours edge-type default-colour))


