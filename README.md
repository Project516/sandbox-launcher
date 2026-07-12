# sandbox-launcher

A JavaFX Minecraft launcher that launches Minecraft in a sandbox environment.

![screenshot](assets/screenshot.png)

This uses docker for isolated environments.

Currently the features are:
* Install multiple isolated instances
* Launch those instances in a Docker container 
* Change instance name and icon
* Change your player username

More features will be added in the future.

There may be bugs!

## Setup

**Make sure you have Java 25 and [Docker](https://www.docker.com/) installed!**

Also Linux is the only supported OS for now. Windows and Mac support may be added, and it may be possible to use a xserver alternative to get this to work now, but it has not been tested.

I recommend using [Eclipse Temurin](https://adoptium.net/temurin/releases?version=25&os=any&arch=any). For Docker, you do not need [Docker Desktop](https://docs.docker.com/desktop/). [Docker Engine](https://docs.docker.com/engine/) is all that is needed.

Currently the way to setup and use this is to:

1. Clone the repository.
2. Run `/docker/build-docker.sh` to build the docker containers
3. Run `./gradlew run` to launch the project

From here, you can select an instance to install and run it.
