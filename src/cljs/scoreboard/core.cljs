(ns scoreboard.core
  (:require [scoreboard.events :as e :refer [app-state]]
            [om.core :as om :include-macros true]
            [clojure.string :refer [blank?]]
            [sablono.core :as html :refer-macros [html]]))

(defonce init
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
           :posession :home}))

(defn publish-event! [t & [ev]]
  (fn [e]
    (.preventDefault e)
    (e/publish! (e/event t ev))))

(defn number-editor [cursor owner]
  (reify
    om/IInitState
    (init-state [_]
      {:editing? false})
    om/IDidUpdate
    (did-update [_ prev-props prev-state]
      (let [input (. js/document
                     (getElementById (:dom-id cursor)))]
        (when input
          (.focus input))))
    om/IRenderState
    (render-state [_ {:keys [editing?]}]
      (let [event-info (select-keys cursor [:path :max :min])
            {:keys [dom-id dom-label display-fn number]} cursor
            action-fn (fn [e]
                        (.preventDefault e)
                        (e/publish!
                         (e/event ::update-number
                                  (merge event-info
                                         {:number (.-value
                                                   (. js/document
                                                      (getElementById dom-id)))
                                          :editor owner}))))]
        (html
         [:div {:class dom-id}
          (when dom-label
            [:div.click {:on-click (publish-event! ::inc-number event-info)
                         :on-context-menu (publish-event! ::dec-number event-info)}
             dom-label])
          (if editing?
            [:div
             [:form {:on-submit action-fn}
              [:input {:id dom-id
                       :type "text"
                       :size 3
                       :on-blur action-fn}]]]
            [:div.click {:on-click #(do (.preventDefault %)
                                        (om/set-state! owner :editing? true))}
             (if display-fn (display-fn number) number)])])))))

(defn score-editor [score home?]
  (om/build number-editor score
            {:fn (fn [n]
                   {:path [(if home? :home :away) :score]
                    :number n
                    :dom-id (if home? "homescore" "awayscore")})}))

(defn team [team owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [home? score timeouts posession]} team]
        (html
         [:div.team
          (score-editor score home?)
          [:div.timeouts.click
           {:on-click (publish-event! ::dec-timeout
                                      {:home? home?})}
           timeouts]
          [:div.posession (if posession "X" "_")]])))))

(defn ball-on-editor [ball-on]
  (om/build number-editor ball-on
            {:fn (fn [ball-on]
                   {:path [:ball-on]
                    :number ball-on
                    :dom-id "ball-on"
                    :dom-label "ball on"
                    :max 50
                    :display-fn #(if (zero? %) "G" %)})}))

(defn to-go-editor [to-go]
  (om/build number-editor to-go
            {:fn (fn [to-go]
                   {:path [:to-go]
                    :number to-go
                    :dom-id "to-go"
                    :dom-label "to go"
                    :max 100
                    :display-fn #(if (zero? %) "in." %)})}))

(defn down-editor [down]
  (om/build number-editor down
            {:fn (fn [down]
                   {:path [:down]
                    :number down
                    :dom-id "down"
                    :dom-label "down"
                    :max 4
                    :min 1})}))

(defn qtr-editor [qtr]
  (om/build number-editor qtr
            {:fn (fn [qtr]
                   {:path [:qtr]
                    :number qtr
                    :dom-id "qtr"
                    :dom-label "qtr"
                    :max 4
                    :min 1})}))

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
           [:div.left {:on-click (publish-event! ::inc-score {:home? true})
                       :on-context-menu (publish-event! ::dec-score {:home? true})}
            "Home"]
           [:div.right {:on-click (publish-event! ::inc-score {:home? false})
                        :on-context-menu (publish-event! ::dec-score {:home? false})}
            "Away"]]
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
         [:div.left
          (down-editor (:down app))]
         [:div.left (to-go-editor (:to-go app))]
         [:div.left (ball-on-editor (:ball-on app))]
         [:div.left
          (qtr-editor (:qtr app))]]]))))

(defn main []
  (om/root
   scoreboard
   app-state
   {:target (. js/document (getElementById "app"))}))

(defn dec-0 [n]
  (max 0 (dec n)))

(defn inc-score [{:keys [home?]}]
  (swap! app-state update-in [(if home? :home :away) :score] inc))

(defn dec-score [{:keys [home?]}]
  (swap! app-state update-in [(if home? :home :away) :score] dec-0))

(defn dec-timeout [{:keys [home?]}]
  (swap! app-state update-in [(if home? :home :away) :timeouts]
         #(if (zero? %) 3 (dec-0 %))))

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
  (swap! app-state update-in [:to-go] dec-0))

(defn edit-number [{:keys [editor]}]
  (om/set-state! editor [:editing?] true))

(defn- between [upper lower n]
  (max (or lower 0) (min (or upper 10000) n)))

(defn inc-number [{:keys [path max min]}]
  (swap! app-state update-in path #(between max min (inc (int %)))))

(defn dec-number [{:keys [path max min]}]
  (swap! app-state update-in path #(between max min (dec (int %)))))

(defn update-number [{:keys [path number max min editor]}]
  (if-not (blank? number)
    (swap! app-state update-in path (fn [_] (between max min (int number)))))
  (om/set-state! editor [:editing?] false))

(defonce subscriptions
  (e/subscriptions
   [::inc-score] inc-score
   [::dec-score] dec-score
   [::dec-timeout] dec-timeout
   [::change-posession] change-posession
   [::edit-number] edit-number
   [::inc-number] inc-number
   [::dec-number] dec-number
   [::update-number] update-number))
