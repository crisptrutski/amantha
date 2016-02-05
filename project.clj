(defproject
  boot-project
  "0.0.0-SNAPSHOT"
  :dependencies
  [[adzerk/boot-cljs "1.7.170-3" :scope "test"]
   [adzerk/boot-cljs-repl "0.3.0" :scope "test"]
   [adzerk/boot-reload "0.4.2" :scope "test"]
   [adzerk/boot-test "1.1.0" :scope "tests"]
   [com.cemerick/piggieback "0.2.1" :scope "test"]
   [crisptrutski/boot-cljs-test "0.3.0-SNAPSHOT" :scope "test"]
   [crisptrutski/boot-lein "0.1.0-SNAPSHOT" :scope "test"]
   [org.clojure/clojurescript "1.7.189" :scope "provided"]
   [org.clojure/tools.nrepl "0.2.12" :scope "test"]
   [pandeiro/boot-http "0.7.0" :scope "test"]
   [weasel "0.7.0" :scope "test"]
   [environ "1.0.1" :scope "test"]
   [reagent "0.5.1"]
   [cljsjs/jquery "2.1.4-0"]
   [cljsjs/bootstrap "3.3.6-0"]
   [org.clojure/core.async "0.2.374"]]
  :source-paths
  ["src" "resources"])