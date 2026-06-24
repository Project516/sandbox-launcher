FROM eclipse-temurin:8-jdk
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 libegl1 libglfw3 libgl1-mesa-dri \
    libx11-6 libxext6 libxrandr2 libxinerama1 libxcursor0 libxi6 \
    && rm -rf /var/lib/apt/lists/*