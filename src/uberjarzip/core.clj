(ns uberjarzip.core
  (:require [clojure.java.io :as io]
            [leiningen.core.main :as main :refer [info]]
            [clojure.string :as string])
  (:import [java.util.zip ZipEntry ZipOutputStream]
           [java.io File]))

(defn exit [message]
  (info message)
  (System/exit 1))

(defn insert [v pos item]
  (into [] (apply conj (subvec v 0 pos) item (subvec v pos))))

(defn copy-file [orig-path orig-file dest-path dest-file]
  (io/copy (io/file (format "%s/%s" orig-path orig-file))
           (io/file (format "%s/%s" dest-path dest-file)))
  (format "%s/%s" dest-path dest-file))

(defn zip
  "Writes the contents of input to output, compressed.
  input: something which can be copied from by io/copy.
  output: something which can be opend by io/output-stream.
  The bytes written to the resulting stream will be gzip compressed."
  [input output & opts]
  (with-open [zos (-> output io/output-stream ZipOutputStream.)]
    (.putNextEntry zos (-> input File. .getName ZipEntry.))
    (apply io/copy (io/file input) zos opts))
    output)

(defn get-uberjar-path [{:keys [version target-path] {{:keys [uberjar-name]} :uberjar} :profiles :as project}]
  (let [uj-file (string/join "-" (-> uberjar-name
                                     (string/split #"-")
                                     (insert 1 version)))]
    (copy-file target-path uberjar-name target-path uj-file)))

(defn find-regular-jar [{:keys [target-path name version]}]
  (let [p (re-pattern (str name "-" version "\\.*" "\\.jar"))]
    (first
      (filter #(and (re-matches p %) (neg? (.indexOf % "standalone")))
              (seq (.list (io/file target-path)))))))

(defn strip-suffix [name suffix]
  (subs name 0 (string/last-index-of name suffix)))

(defn uberjarzip
  "zips uberjar file to make it a zip"
  [{group :group target-path :target-path version :version :as project} & args]
  (let [regular-jar (find-regular-jar project)
        regular-jar-path (str target-path "/" regular-jar)
        uberjar (get-uberjar-path project)
        uberjar-zip (zip uberjar (-> regular-jar-path (strip-suffix ".jar") (str ".zip")))]
    (println "uberjarzip wrote: " uberjar-zip)))
