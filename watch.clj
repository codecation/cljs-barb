(require 'cljs.build.api)

(cljs.build.api/watch "src"
  {:main 'barb.core
   :output-to "out/main.js"})
