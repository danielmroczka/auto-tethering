#!/bin/sh
if [ "$TRAVIS_BRANCH" != "master" ]; then
    exit 0;
fi

export GIT_COMMITTER_EMAIL=daniel.mroczka@gmail.com
export GIT_COMMITTER_NAME=danielmroczka

git checkout dev || exit
git merge master
git push --set-upstream origin dev