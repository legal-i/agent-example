### BUILD image
FROM eclipse-temurin:11-focal AS builder

ARG GITHUB_USER
ENV GITHUB_USER $GITHUB_USER
ARG GITHUB_TOKEN
ENV GITHUB_TOKEN $GITHUB_TOKEN

COPY settings.xml /root/.m2/
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN chmod +x ./mvnw
RUN ./mvnw package -DskipTests

RUN java -Djarmode=layertools -jar target/agent.jar extract

### RUNTIME image
FROM eclipse-temurin:11-focal AS runtime

# add curl for healthcheck
RUN apt-get update \
    && apt-get install --no-install-recommends -y curl  \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd -r legali && useradd --no-log-init -r -g legali legali

# Create base app folder
WORKDIR /app
# Workaround for copy issue
COPY --from=builder /app/dependencies/ ./
RUN true
COPY --from=builder /app/snapshot-dependencies/ ./
RUN true
COPY --from=builder /app/spring-boot-loader/ ./
RUN true
COPY --from=builder /app/application/ ./

EXPOSE 8080
USER legali

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=90.0", "org.springframework.boot.loader.JarLauncher"]

HEALTHCHECK --interval=10s --timeout=3s --retries=3 CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1
