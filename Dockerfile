# Build the project inside temporary Docker image
FROM sbtscala/scala-sbt:eclipse-temurin-focal-11.0.17_8_1.8.2_2.13.10 AS builder

RUN mkdir -p /tmp/app-build
WORKDIR /tmp/app-build
COPY build.sbt ./
COPY src ./src
COPY project/plugins.sbt ./project/

RUN sbt assembly

# Build the target Docker image
FROM eclipse-temurin:11-alpine

RUN mkdir /opt/app

COPY --from=builder /tmp/app-build/target/scala-*/location-streaming-assembly*.jar /opt/app/location-streaming.jar

RUN printf "#!/bin/sh \
\njava -cp /opt/app/location-streaming.jar pl.edu.geolocation.\$1 \
\n" > /bin/app-runner && chmod +x /bin/app-runner
