# Estágio 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Copia arquivos de configuração
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia código fonte e compila
COPY src ./src
RUN mvn clean package -DskipTests -B

# Estágio 2: Execução
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Instala curl para healthcheck (opcional)
RUN apk add --no-cache curl

# Copia o JAR do builder
COPY --from=builder /build/target/*.jar /app/app.jar

# Verifica se o JAR existe (debug)
RUN ls -la /app/

# Expõe a porta
EXPOSE 8080

# Comando de entrada com verificação
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
