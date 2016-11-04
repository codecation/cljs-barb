(defproject barb "0.1.0-SNAPSHOT"
  :description "Genetic image generator"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/clojurescript "1.9.293"]
                 [org.clojure/core.async "0.2.395"]]
  :jvm-opts ^:replace ["-Xmx1g" "-server"]
  :plugins [[lein-npm "0.6.1"]
            [lein-cljsbuild "1.1.4"]]
  :npm {:dependencies [[source-map-support "0.4.0"]]}
  :source-paths ["src" "target/classes"]
  :clean-targets ["out" "release"]
  :target-path "target"
  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.1"]
                                  [org.clojure/tools.nrepl "0.2.10"]
                                  [org.clojure/test.check "0.9.0"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}
  :cljsbuild {
              :builds [{
                        ; The path to the top-level ClojureScript source directory:
                        :source-paths ["src"]
                        ; The standard ClojureScript compiler options:
                        ; (See the ClojureScript compiler documentation for details.)
                        :compiler {
                                   :output-to "out/main.js"  ; default: target/cljsbuild-main.js
                                   :optimizations :whitespace
                                   :pretty-print true}}]})
