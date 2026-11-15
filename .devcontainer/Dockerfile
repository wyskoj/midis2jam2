FROM mcr.microsoft.com/devcontainers/base:ubuntu-22.04

USER root

ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH="/opt/android-sdk/platform-tools:${PATH}"

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        curl \
        unzip \
        python3 \
        python3-venv \
        ffmpeg \
        fluxbox \
        libasound2 \
        libgl1 \
        libgl1-mesa-dri \
        libxcursor1 \
        libopenal1 \
        libxext6 \
        libxinerama1 \
        libxi6 \
        libxrandr2 \
        libxrender1 \
        libxss1 \
        libxtst6 \
        mesa-utils \
        novnc \
        pulseaudio \
        pulseaudio-utils \
        websockify \
        x11-utils \
        x11vnc \
        xvfb \
    && mkdir -p /opt/android-sdk \
    && chown -R vscode:vscode /opt/android-sdk

USER vscode
