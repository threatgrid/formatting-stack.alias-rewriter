(ns example-one.input
  (:require
   [clojure.java.io :as jio])
  (:import
   (java.io File)))

::jio/copy
:jio/copy ;; (should not be replaced at all)

^::jio/copy []
^:jio/copy [] ;; (should not be replaced at all)

^{::jio/copy 1} []
^{:jio/copy 1} [] ;; (should not be replaced at all)

::jio ;; (should not be replaced at all)

"`jio/copy` `#'jio/copy` jio/copy :jio/copy ::jio/copy"

"`jio/copy`
`#'jio/copy`
jio/copy
:jio/copy
::jio/copy"
