# Dockerfile created to work with build.sh
# unable to find a compatible docker image with jdk12 and sbt version 1.3.5

ARG BASE_IMAGE_TAG
FROM openjdk:${BASE_IMAGE_TAG:-12-jdk}

ARG SBT_VERSION
ENV SBT_VERSION ${SBT_VERSION:-1.3.5}

RUN \
  curl -L -o sbt-$SBT_VERSION.rpm https://bintray.com/sbt/rpm/download_file?file_path=sbt-$SBT_VERSION.rpm && \
  rpm -i sbt-$SBT_VERSION.rpm && \
  rm sbt-$SBT_VERSION.rpm && \
  yum -y update && \
  yum -y install sbt && \
  sbt sbtVersion