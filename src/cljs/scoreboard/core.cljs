(ns scoreboard.core
  (:require [scoreboard.events :as e :refer [app-state]]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(reset! app-state
        {:text "ScoreBoard+"
         :home {:home? true
                :score 0
                :timeouts 3}
         :away {:home? false
                :score 0
                :timeouts 3}
         :ball-on 50
         :to-go 10
         :down 1
         :qtr 1
         :time "15:00"
         :posession :home})

(defn publish-event! [t & [ev]]
  (fn [e]
    (.preventDefault e)
    (e/publish! (e/event t ev))))

(defn team [team owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [home? score timeouts posession]} team]
        (html
         [:div.team
          [:div.score.click
           {:on-click (publish-event! ::inc-score
                                      {:home? home?})
            :on-context-menu (publish-event! ::dec-score
                                             {:home? home?})}
           score]
          [:div.timeouts.click
           {:on-click (publish-event! ::dec-timeout
                                      {:home? home?})}
           timeouts]
          [:div.posession (if posession "X" "_")]])))))

(defn ball-on-editor [ball-on owner]
  (reify
    om/IInitState
    (init-state [_]
      {:editing? false})
    om/IDidUpdate
    (did-update [_ prev-props prev-state]
      (let [ball-on-input (. js/document
                             (getElementById "ball-on"))]
        (if ball-on-input
          (.focus ball-on-input))))
    om/IRenderState
    (render-state [_ {:keys [editing?]}]
      (html
       [:div.ball-on.left
        [:div "ball on"]
        (if editing?
          [:div
           [:form
            [:input#ball-on {:type "text" :size 3}]
            [:br]
            [:input {:type "submit"
                     :value "change"
                     :on-click #(do (.preventDefault %)
                                    (e/publish!
                                     (e/event ::change-ball-on
                                              {:ball-on (.-value (. js/document
                                                                    (getElementById "ball-on")))
                                               :editor owner})))}]]]
          [:div.click {:on-click (publish-event! ::edit-ball-on
                                           {:editor owner})}
           ball-on])]))))

(defn scoreboard [app owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div#scoreboard.noselect
        [:div.top
         [:div.home
          (om/build team
                    (:home app)
                    {:fn #(assoc %
                            :posession (= :home (:posession app)))})]
         [:div.keys
          [:div.teams
           [:div.left "Home"]
           [:div.right "Away"]]
          [:div.timeouts "TIMEOUTS"]
          [:div.posession.click
           {:on-click (fn [e]
                        (.preventDefault e)
                        (e/publish! (e/event ::change-posession)))}
           "POSESSION"]]
         [:div.away
          (om/build team
                    (:away app)
                    {:fn #(assoc %
                            :posession (= :away (:posession app)))})]]
        [:div.bottom
         [:div.down.left
          [:div "down"]
          [:div.click {:on-click (publish-event! ::inc-down)}
           (:down app)]]
         [:div.togo.left
          [:div "to go"]
          [:div.click {:on-click (publish-event! ::inc-to-go)
                 :on-context-menu (publish-event! ::dec-to-go)}
           (:to-go app)]]
         (om/build ball-on-editor
                   (:ball-on app))
         [:div.quarter.left
          [:div "qtr"]
          [:div.click {:on-click (publish-event! ::inc-qtr)}
           (:qtr app)]]]]))))

(defn main []
  (om/root
   scoreboard
   app-state
   {:target (. js/document (getElementById "app"))}))

(defn inc-score [{:keys [home?]}]
  (swap! app-state update-in [(if home? :home :away) :score] inc))

(defn dec-score [{:keys [home?]}]
  (swap! app-state update-in [(if home? :home :away) :score] dec))

(defn dec-timeout [{:keys [home?]}]
  (swap! app-state update-in [(if home? :home :away) :timeouts]
         #(if (zero? %) 3 (dec %))))

(defn change-posession [_]
  (swap! app-state update-in [:posession] #(if (= :home %) :away :home)))

(defn inc-down [_]
  (swap! app-state update-in [:down]
         #(if (= 4 %) 1 (inc %))))

(defn inc-qtr [_]
  (swap! app-state update-in [:qtr]
         #(if (= 4 %) 1 (inc %))))

(defn inc-to-go [_]
  (swap! app-state update-in [:to-go] inc))

(defn dec-to-go [_]
  (swap! app-state update-in [:to-go] dec))

(defn edit-ball-on [{:keys [editor]}]
  (om/set-state! editor [:editing?] true))

(defn change-ball-on [{:keys [ball-on editor]}]
  (swap! app-state update-in [:ball-on] (fn [_] ball-on))
  (om/set-state! editor [:editing?] false))

(defonce subscriptions
  (e/subscriptions
   [::inc-score] inc-score
   [::dec-score] dec-score
   [::dec-timeout] dec-timeout
   [::change-posession] change-posession
   [::inc-qtr] inc-qtr
   [::inc-down] inc-down
   [::inc-to-go] inc-to-go
   [::dec-to-go] dec-to-go
   [::edit-ball-on] edit-ball-on
   [::change-ball-on] change-ball-on))
