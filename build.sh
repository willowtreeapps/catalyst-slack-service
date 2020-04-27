#!/usr/bin/env bash

# for teamcity builds

if [[ $# -eq 0 ]]
  then
    echo "invalid bias-correct url parameter"
    exit 1
fi

url="$1/corrector/correct"
echo "integration testing against bias-correct url = $url"

docker build -t sbt135-jdk12 .
docker run -it --rm -v $PWD:/app -w /app -e BIAS_CORRECT_URL=${url} sbt135-jdk12 sbt clean compile test
