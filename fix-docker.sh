#!/bin/sh
# script to fix docker window issues on linux

xhost +local:

pactl load-module module-native-protocol-unix auth-anonymous=1 socket=/run/user/1000/pulse/native