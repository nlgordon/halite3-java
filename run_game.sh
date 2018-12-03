#!/bin/sh

set -e

./gradlew build

#./halite -vvv -i replays --width 56 --height 56 "java -jar build/libs/halite3-java.jar" "java -jar build/libs/halite3-java.jar"
./halite -vvv -i replays --width 64 --height 64 "java -jar build/libs/halite3-java.jar" "java -jar build/libs/halite3-java.jar" "java -jar build/libs/halite3-java.jar" "java -jar build/libs/halite3-java.jar"

#./watch_latest_test_run.sh
