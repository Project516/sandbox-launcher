FROM eclipse-temurin:8-jdk
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 libegl1 libglfw3 libgl1-mesa-dri \
    libxtst6 libxxf86vm1 libopenal1 alsa-utils libasound2-plugins libpulse0 \
    && printf 'pcm.!default {\n type pulse\n}\nctl.!default {\n type pulse\n}' > /etc/asound.conf \
    && rm -rf /var/lib/apt/lists/*