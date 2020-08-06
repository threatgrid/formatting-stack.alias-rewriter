(ns example-one.input
  (:require
   [clojure.java.io :as io])
  (:import
   (java.io File)))

::io/copy
:jio/copy ;; (should not be replaced at all)

^::io/copy []
^:jio/copy [] ;; (should not be replaced at all)

^{::io/copy 1} []
^{:jio/copy 1} [] ;; (should not be replaced at all)

::jio ;; (should not be replaced at all)

"`io/copy` `#'io/copy` jio/copy :jio/copy ::io/copy"

"`io/copy`
`#'io/copy`
jio/copy
:jio/copy
::io/copy"
