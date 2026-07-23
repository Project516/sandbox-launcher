FROM eclipse-temurin:17.0.19_10-jdk
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 libegl1 libglfw3 libgl1-mesa-dri \
    && rm -rf /var/lib/apt/lists/*