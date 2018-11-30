#!/bin/sh

set -e

./gradlew build

./halite -vvv -i replays --width 56 --height 56 "java -jar build/libs/halite3-java.jar" "java -jar build/libs/halite3-java.jar"

../../fohristiwhirl/fluorine/./node_modules/.bin/electron ../../fohristiwhirl/fluorine/ `ls replays/replay-* | sort -r | head -n 1`
