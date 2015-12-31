(set-env!
 :source-paths    #{"src"}
 :resource-paths  #{"resources"}
 :dependencies '[[adzerk/boot-cljs          "1.7.170-3"   :scope "test"]
                 [adzerk/boot-cljs-repl     "0.3.0"       :scope "test"]
                 [adzerk/boot-reload        "0.4.2"       :scope "test"]
                 [pandeiro/boot-http        "0.7.0"       :scope "test"]
                 [org.clojure/clojurescript "1.7.189"                  ]
                 [reagent                   "0.5.1"                    ]
                 [com.cemerick/piggieback   "0.2.1"       :scope "test"]
                 [weasel                    "0.7.0"       :scope "test"]
                 [org.clojure/tools.nrepl   "0.2.12"      :scope "test"]
                 [adzerk/boot-test          "1.1.0"     :scope "tests"]
                 [crisptrutski/boot-cljs-test "0.2.1-SNAPSHOT" :scope "test"]

                 [cljsjs/jquery "2.1.4-0"]
                 [cljsjs/bootstrap "3.3.6-0"]

                 [ankha "0.1.5.1-64423e"]
                 [ring "1.4.0"]
                 [compojure "1.4.0"]
                 [enlive "1.1.6"]
                 [environ "1.0.1"]
                 [secretary "1.2.3"]
                 [clj-http "2.0.0"]
                 [re-frame "0.5.0"]
                 [org.clojure/core.async "0.2.374"]
                 [tailrecursion/ring-proxy "2.0.0-SNAPSHOT" :exclusions [clj-http]]])

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload    :refer [reload]]
 '[pandeiro.boot-http    :refer [serve]]
 '[adzerk.boot-test      :refer :all]
 '[crisptrutski.boot-cljs-test :refer [test-cljs]])

(deftask testing []
  (merge-env! :source-paths #{"test"}))

(deftask build []
  (comp (speak)
        (cljs)))

(deftask run []
  (comp (serve)
        (watch)
        (cljs-repl)
        (reload)
        (build)))

(deftask production []
  (task-options! cljs {:optimizations :advanced})
  identity)

(deftask development []
  (task-options! cljs {:optimizations :none :source-map true}
                 reload {:on-jsload 'amantha.app/refresh})
  identity)

(deftask dev
  "Simple alias to run application in development mode"
  []
  (comp (development)
        (run)))


