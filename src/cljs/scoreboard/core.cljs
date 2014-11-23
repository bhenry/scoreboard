(ns scoreboard.core
  (:require [scoreboard.events :as e :refer [app-state]]
            [scoreboard.instructions :as i]
            [om.core :as om :include-macros true]
            [clojure.string :refer [blank?]]
            [sablono.core :as html :refer-macros [html]]))

(defonce init
  (reset! app-state
          {:home {:home? true
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
           :possession :home}))

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
            {:keys [dom-id dom-label click-dec? display-fn number
                    label-tip number-tip]} cursor
            click (if click-dec? ::dec-number ::inc-number)
            right-click (if click-dec? ::inc-number ::dec-number)
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
            [:div.click {:on-click (publish-event! click event-info)
                         :on-context-menu (publish-event! right-click event-info)}
             dom-label
             label-tip])
          (if editing?
            [:div
             [:form {:on-submit action-fn}
              [:input {:id dom-id
                       :type "text"
                       :size 3
                       :on-blur action-fn}]]]
            [:div.tipper {:on-click #(do (.preventDefault %)
                                         (om/set-state! owner :editing? true))}
             (if display-fn (display-fn number) number)
             number-tip])])))))

(defn score-editor [score home?]
  (om/build number-editor score
            {:fn (fn [n]
                   {:path [(if home? :home :away) :score]
                    :number n
                    :number-tip i/score
                    :dom-id (if home? "homescore" "awayscore")})}))

(defn team [team owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [home? score timeouts possession]} team]
        (html
         [:div.team
          (score-editor score home?)
          [:div.timeouts.click
           {:on-click (publish-event! ::dec-timeout
                                      {:home? home?})}
           timeouts
           i/timeout]
          [:div.possession (if possession "X" "_")]])))))

(defn- show-ball-on [y]
  (let [>? (> y 50)
        <? (< y 50)
        <> (= y 50)
        yd (if >? (- 100 y) y)
        ydln (if (zero? yd) "G" yd)]
    (cond
     >? (str ydln " >")
     <? (str "< " ydln)
     <> (str "< " ydln " >"))))

(defn ball-on-editor [ball-on possession]
  (om/build number-editor ball-on
            {:fn (fn [ball-on]
                   {:path [:ball-on]
                    :number ball-on
                    :dom-id "ball-on"
                    :dom-label "ball on"
                    :label-tip i/ball-on-label
                    :number-tip i/ball-on-number
                    :click-dec? (= possession :away)
                    :max 100
                    :min 0
                    :display-fn show-ball-on})}))

(defn to-go-editor [to-go]
  (om/build number-editor to-go
            {:fn (fn [to-go]
                   {:path [:to-go]
                    :number to-go
                    :dom-id "to-go"
                    :dom-label "to go"
                    :label-tip i/to-go-label
                    :number-tip i/to-go-number
                    :max 100
                    :display-fn #(if (zero? %) "in." %)})}))

(defn down-editor [down]
  (om/build number-editor down
            {:fn (fn [down]
                   {:path [:down]
                    :number down
                    :dom-id "down"
                    :dom-label "down"
                    :label-tip i/down-label
                    :number-tip i/down-number
                    :max 4
                    :min 1})}))

(defn qtr-editor [qtr]
  (om/build number-editor qtr
            {:fn (fn [qtr]
                   {:path [:qtr]
                    :number qtr
                    :dom-id "qtr"
                    :dom-label "qtr"
                    :label-tip i/qtr-label
                    :number-tip i/qtr-number
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
                            :possession (= :home (:possession app)))})]
         [:div.keys
          [:div.teams
           [:div.left.click
            {:on-click (publish-event! ::inc-score {:home? true})
             :on-context-menu (publish-event! ::dec-score {:home? true})}
            "Home"
            i/score-header]
           [:div.right.click
            {:on-click (publish-event! ::inc-score {:home? false})
             :on-context-menu (publish-event! ::dec-score {:home? false})}
            "Away"
            i/score-header]]
          [:div.timeouts "TIMEOUTS"]
          [:div.possession.click
           {:on-click (fn [e]
                        (.preventDefault e)
                        (e/publish! (e/event ::change-possession)))}
           "POSSESSION"
           i/possession]]
         [:div.away
          (om/build team
                    (:away app)
                    {:fn #(assoc %
                            :possession (= :away (:possession app)))})]]
        [:div.bottom
         [:div.left
          (down-editor (:down app))]
         [:div.left (to-go-editor (:to-go app))]
         [:div.left (ball-on-editor (:ball-on app) (:possession app))]
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

(defn change-possession [_]
  (swap! app-state update-in [:possession] #(if (= :home %) :away :home)))

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
   [::change-possession] change-possession
   [::edit-number] edit-number
   [::inc-number] inc-number
   [::dec-number] dec-number
   [::update-number] update-number))
