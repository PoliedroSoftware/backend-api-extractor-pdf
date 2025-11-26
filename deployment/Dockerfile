# Multi-stage build para optimizar el tamaño de la imagen
FROM eclipse-temurin:24-jdk-alpine AS builder
WORKDIR /build

# Instalar bash para el gradlew
RUN apk add --no-cache bash

# Copiar archivos de gradle
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties .
COPY main.gradle .
COPY lombok.config .

# Copiar código fuente
COPY applications applications
COPY domain domain
COPY infrastructure infrastructure

# Convertir line endings (CRLF a LF), dar permisos y construir
RUN sed -i 's/\r$//' gradlew && \
    chmod +x gradlew && \
    ./gradlew :app-service:bootJar --no-daemon -x validateStructure

# Imagen final
FROM eclipse-temurin:24-jre-alpine
WORKDIR /app

# Instalar wget para healthcheck
RUN apk add --no-cache wget

# Crear usuario no-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copiar el JAR desde el builder (usar wildcard para evitar problemas con versiones)
COPY --from=builder /build/applications/app-service/build/libs/*.jar app.jar

# Variables de entorno
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -Djava.security.egd=file:/dev/./urandom"
ENV SERVER_PORT=8080

# Exponer puerto
EXPOSE 8080

# Cambiar a usuario no-root
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Punto de entrada
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
