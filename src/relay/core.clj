(ns relay.core
  (:gen-class)
  (:import
   java.util.zip.Inflater
    java.util.zip.InflaterInputStream
    java.io.ByteArrayInputStream
   java.util.zip.Deflater
   java.util.zip.DeflaterOutputStream
   java.io.ByteArrayOutputStream)
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :refer [response]]
            [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [clojure.string :as str]))


(defn deflate-string [s]
  (let [bytes (.getBytes s "UTF-8")
        deflater (Deflater. 9 true)  ; 9 is the highest compression level
        bos (ByteArrayOutputStream. 1024)
        dos (DeflaterOutputStream. bos deflater)]
    (.write dos bytes)
    (.close dos)
    (.encodeToString (java.util.Base64/getEncoder) (.toByteArray bos))))


(defn change-xml [xml-str]
  (let [data (xml/parse-str xml-str)]
    (xml/emit-str
     (assoc-in data [:attrs :Destination] "https://sso.awsdev.pason.com/saml2/logout"))))


(defn inflate-base64 [base64-str]
  (let [decoder (java.util.Base64/getDecoder)
        byte-arr (.decode decoder base64-str)
        byte-arr-input-stream (io/input-stream byte-arr)
        inflater (Inflater. true) ; Specify true if the compressed data includes a header
        inflater-stream (InflaterInputStream. byte-arr-input-stream inflater)]
    (slurp inflater-stream)))

(defn html-response [params]
  ;; (print params)
  (let [sam-res (get-in params [:query-params "SAMLResponse"])
        relay-state (get-in params [:query-params "RelayState"])
        signature (get-in params [:query-params "Signature" ])
        sig-alg (get-in params [:query-params "SigAlg"])]
    (response
     (str "<html>"
          "<head>"
          "<meta charset=\"utf-8\" />"
          "</head>"
          "<body>"
          "<form action=\"https://sso.awsdev.pason.com/saml2/logout\" method=\"post\">"
          "<div>"
          "<input name=\"SAMLResponse\" value=\"" sam-res "\"/>"
          "<input name=\"RelayState\" value=\"" relay-state "\"/>"
          "<input name=\"Signature\" value=\"" signature "\"/>"
          "<input name=\"SigAlg\" value=\"" sig-alg "\"/>"
          "</div>"
          "<div>"
          "<input type=\"submit\" value=\"Continue\"/>"
          "</div>"
          "<noscript>"
          "<div>"
          "<input type=\"submit\" value=\"Continue\"/>"
          "</div>"
          "</noscript>"
          "</form>"
          "</body>"
          "</html>"))))

(def app
  (-> html-response
      wrap-params))

(defn html-response-to-cognito [params]
  ;; (print params)
  (let [sam-res (get-in params [:query-params "SAMLResponse"])
        my-sam-res (deflate-string (change-xml (inflate-base64 sam-res)))
        relay-state (get-in params [:query-params "RelayState"])
        signature (get-in params [:query-params "Signature"])
        sig-alg (get-in params [:query-params "SigAlg"])]
    (response
     (str "<html>"
          "<head>"
          "<meta charset=\"utf-8\" />"
          "</head>"
          "<body>"
          "<form action=\"https://sso.awsdev.pason.com/saml2/logout\" method=\"post\">"
          "<div>"
          "<input name=\"SAMLResponse\" value=\"" my-sam-res "\"/>"
          "<input name=\"RelayState\" value=\"" relay-state "\"/>"
          "<input name=\"Signature\" value=\"" signature "\"/>"
          "<input name=\"SigAlg\" value=\"" sig-alg "\"/>"
          "</div>"
          "<div>"
          "<input type=\"submit\" value=\"Continue\"/>"
          "</div>"
          "<noscript>"
          "<div>"
          "<input type=\"submit\" value=\"Continue\"/>"
          "</div>"
          "</noscript>"
          "</form>"
          "</body>"
          "</html>"))))

(def app-to-cognito
  (-> html-response-to-cognito
      wrap-params))


;; (defn handler [request]
;;   (print request)
;;   (html-response (:params (wrap-params request))))


(defn -main []
  (jetty/run-jetty app-to-cognito {:port 3000}))

;; (-main)

;; (print "cram")


;; (defn update-xml-destination [xml-str new-destination]
;;   (let [xml-data (xml/parse-str xml-str)
;;         zipped-xml (zip/xml-zip xml-data)
;;         destination-path [:LogoutResponse :attrs :Destination]
;;         updated-zipped-xml (zip/replace zipped-xml destination-path new-destination)
;;         updated-xml-data (zip/xml-> updated-zipped-xml :content)
;;         updated-xml-str (xml/emit-str updated-xml-data)]
;;     updated-xml-str))


;; (defn replace-destination [xml-str]
;;   (let [xml-data (xml/parse-str xml-str)
;;         zipped (zip/xml-zip xml-data)]
;;     (-> zipped
;;         (zip-xml/xml-> :LogoutResponse)
;;         (zip-xml/attr :Destination "foo")
;;         (zip-xml/xml-> :content)
;;         (xml/emit-str)
;;         )))


(def xml-input "<samlp:LogoutResponse ID=\"_3b9ef597-f496-461c-bc63-ef5448c317d7\" Version=\"2.0\" IssueInstant=\"2024-04-23T01:45:13.665Z\" Destination=\"https://sso.awsdev.pason.com/saml2/logout\" InResponseTo=\"_bcaea5e3-97c1-4d7b-a5da-10ed09c7dde0\" xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"><Issuer xmlns=\"urn:oasis:names:tc:SAML:2.0:assertion\">https://sts.windows.net/c655950e-7130-45b5-b9c1-88f9a658b29a/</Issuer><samlp:Status><samlp:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/></samlp:Status></samlp:LogoutResponse>")


;; (defn str-deflate-base64 [str]
;;   (let [baos (java.io.BufferedOutputStream. str)
;;         gzip (zip)
;;         ])

;;   )


;; (defn deflate-string [input-str]
;;   (let [deflated-bytes (-> input-str
;;                            (.getBytes)
;;                            (java.util.zip.Deflater.)
;;                            (io/input-stream)
;;                            (.toByteArray))]
;;     (str/trim (base64/encode (String. deflated-bytes "ISO-8859-1")))))

;; (defn deflate-str [input-str]
;;   (let [deflated-bytes (-> input-str
;;                            ;; (.getBytes)
;;                            (io/output-stream)
;;                            (java.util.zip.GZIPOutputStream.)
;;                            (.toByteArray))]
;;     (str/trim (base64/encode (String. deflated-bytes "ISO-8859-1")))))

;; (str (base64/encode (.getBytes "fpp")))



;; (defn deflate-string-2 [input]
;;   (with-open [output-stream (io/output-stream (java.io.ByteArrayOutputStream.))]
;;     (with-open [deflater-stream (java.util.zip.DeflaterOutputStream. output-stream)]
;;       (.write deflater-stream (.getBytes input "UTF-8")))
;;     (String. (.toByteArray ((.getBuffer output-stream))) "UTF-8")))


