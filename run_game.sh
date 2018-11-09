#!/bin/sh

set -e

./gradlew build

./halite -vvv -i replays --width 32 --height 32 "java -jar build/libs/halite3-java.jar" "java -jar build/libs/halite3-java.jar"
