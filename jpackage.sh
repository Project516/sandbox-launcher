#!/bin/sh

rm -rf sandbox-launcher

jpackage --input app/build/libs/ \
  --name sandbox-launcher \
  --main-jar app-all.jar \
  --main-class dev.project516.sandbox.Main \
  --type app-image \
  --app-version 1.0.0

zip -r sandbox-launcher.zip sandbox-launcher

rm -rf sandbox-launcher