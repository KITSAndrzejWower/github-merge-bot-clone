(ns github-merge-bot.core
  (:require [tentacles.pulls :as pulls]
            [clojure.pprint :as pprint]
            [clojure.tools.logging :as log]
            [clj-jgit.porcelain :as git])
  (:import (java.util UUID)
           (org.eclipse.jgit.transport UsernamePasswordCredentialsProvider))
  (:gen-class))

(defn mergeable? [pull-id]
  (:mergeable (pulls/specific-pull "sdduursma" "github-merge-bot-test" pull-id)))

; TODO: `last` correct?
(defn oldest [pulls]
  (last (sort-by :created-at pulls)))

(defn pull-requests-to-update
  "Pull requests to update with their base branch."
  [pulls]
  [(oldest (filter #(mergeable? (:number %))
                  pulls))])

(defn update-pull [owner repo pull-request]
  ;; TODO: Clone every time?
  (let [repo (:repo (git/git-clone-full (str "https://github.com/" owner "/" repo ".git")
                                        (str "./tmp/" (UUID/randomUUID) owner "/" repo)))
        head-branch (:ref (:head pull-request))]
    (println "Cloned repo to" (.getPath (.getDirectory (.getRepository repo))))
    (git/git-fetch repo "origin")
    (git/git-checkout repo head-branch true false (str "origin/" head-branch))
    (git/git-rebase repo "origin/master")
    ; git/with-credentials didn't seem to work so using JGit here directly instead.
    (-> repo
        (.push)
        (.setRemote "origin")
        (.setForce true)
        (.setCredentialsProvider (UsernamePasswordCredentialsProvider. "sdduursma" "xxx"))
        (.call))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [pull-requests (pull-requests-to-update (pulls/pulls "sdduursma" "github-merge-bot-test"))]
    (doseq [pr pull-requests]
      (update-pull "sdduursma" "github-merge-bot-test" pr))))
