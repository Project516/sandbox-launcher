# sandbox-launcher

A JavaFX Minecraft launcher that launches Minecraft in a sandbox environment.

This uses docker for isolated environments.

Currently the features are:
* Install multiple isolated instances
* Launch those instances in a Docker container 
* Change instance name and icon
* Change your player username

More features will be added in the future. 

There may be bugs!

## Setup

**Make sure you have Java 25 and Docker installed!**

Currently the way to setup and use this is to:

1. Clone the repository.
2. Run `/docker/build-docker.sh` to build the docker containers
3. Run `./gradlew run` to launch the project

From here, you can select and instance to install and run it.
