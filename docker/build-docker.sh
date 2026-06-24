#!/bin/sh

docker build -f docker/java8.Dockerfile -t sandbox-java8 .
docker build -f docker/java16.Dockerfile -t sandbox-java16 .
docker build -f docker/java17.Dockerfile -t sandbox-java17 .
docker build -f docker/java21.Dockerfile -t sandbox-java21 .
docker build -f docker/java25.Dockerfile -t sandbox-java25 .