(defproject
  boot-project
  "0.0.0-SNAPSHOT"
  :dependencies
  [[adzerk/boot-cljs "1.7.170-3" :scope "test"]
   [adzerk/boot-cljs-repl "0.3.0" :scope "test"]
   [adzerk/boot-reload "0.4.2" :scope "test"]
   [pandeiro/boot-http "0.7.0" :scope "test"]
   [org.clojure/clojurescript "1.7.189"]
   [reagent "0.5.1"]
   [com.cemerick/piggieback "0.2.1" :scope "test"]
   [weasel "0.7.0" :scope "test"]
   [org.clojure/tools.nrepl "0.2.12" :scope "test"]
   [crisptrutski/boot-cljs-test "0.2.1-SNAPSHOT" :scope "test"]
   [ankha "0.1.5.1-64423e"]
   [ring "1.4.0"]
   [compojure "1.4.0"]
   [enlive "1.1.6"]
   [org.om/om "0.8.1"]
   [environ "1.0.1"]
   [sablono "0.5.3"]
   [secretary "1.2.3"]
   [clj-http "2.0.0"]
   [re-frame "0.5.0"]
   [org.clojure/core.async "0.2.374"]
   [tailrecursion/ring-proxy "2.0.0-SNAPSHOT" :exclusions [clj-http]]]
  :source-paths
  ["src" "resources"])