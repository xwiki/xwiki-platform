FROM debian:12-slim AS builder

ENV MAVEN_VERSION=3.9.6

# Обновляем и устанавливаем необходимые пакеты
RUN apt update && \
    apt upgrade -y && \
    apt-get install --no-install-recommends -y \
        apt-transport-https \
        ca-certificates \
        curl \
        wget \
        gnupg2 \
        zip \
        bzip2 \
        lsof \
        software-properties-common \
        net-tools \
        bc \
        git \
        unzip && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /root

# Устанавливаем Java
RUN mkdir -p /usr/lib/jvm

# Java 11
RUN wget --no-check-certificate https://download.java.net/java/GA/jdk11/9/GPL/openjdk-11.0.2_linux-x64_bin.tar.gz && \
    tar -C /usr/lib/jvm -xvzf openjdk-11.0.2_linux-x64_bin.tar.gz && \
    rm openjdk-11.0.2_linux-x64_bin.tar.gz

# Java 17
RUN wget --no-check-certificate https://download.oracle.com/java/17/archive/jdk-17.0.12_linux-x64_bin.tar.gz && \
    tar -C /usr/lib/jvm -xvzf jdk-17.0.12_linux-x64_bin.tar.gz && \
    rm jdk-17.0.12_linux-x64_bin.tar.gz

## Устанавливаем Maven
RUN wget --no-check-certificate https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz && \
    tar -C /usr/lib/ -xvzf apache-maven-$MAVEN_VERSION-bin.tar.gz && \
    rm apache-maven-$MAVEN_VERSION-bin.tar.gz && \
    mv /usr/lib/apache-maven-$MAVEN_VERSION /usr/lib/maven

# Переменные окружения
ENV JAVA_HOME=/usr/lib/jvm/jdk-17.0.12
ENV MAVEN_HOME=/usr/lib/maven
ENV PATH=$MAVEN_HOME/bin:$JAVA_HOME/bin:$PATH

# Копируем конфигурацию Maven
COPY /config/settings.xml /root/.m2/settings.xml

# Копируем проект Xwiki
RUN mkdir -p /src/xwiki
COPY .. /src/xwiki

# Указываем порт
EXPOSE 8080

# Сборка проекта Xwiki
WORKDIR /src/xwiki
RUN mvn clean install -DskipTests=true -Dxwiki.checkstyle.skip=true -Denforcer.skip=true -Pclover