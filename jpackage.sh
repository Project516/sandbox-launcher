#!/bin/sh

jpackage --input app/build/libs/ \
  --name sandbox-launcher \
  --main-jar app-all.jar \
  --main-class dev.project516.sandbox.Main \
  --type app-image \
  --java-options '--enable-preview'