# Estágio 1: Build da aplicação
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copia o pom.xml primeiro (aproveita cache)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o código fonte e faz o build
COPY src ./src
RUN mvn clean package -DskipTests

# Estágio 2: Execução
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copia o JAR do estágio de build
COPY --from=builder /app/target/*.jar app.jar

# Expõe a porta
EXPOSE 8080

# Comando para rodar
