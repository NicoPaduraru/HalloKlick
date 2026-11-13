# Stufe 1: Anwendung bauen
FROM eclipse-temurin:21-jdk AS bau
WORKDIR /bau
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
# Die Tests laufen in der CI (GitHub Actions), nicht beim Bauen des Images
RUN ./mvnw -B -q package -DskipTests

# Stufe 2: schlankes Image nur mit Java-Laufzeit und der fertigen Anwendung
FROM eclipse-temurin:21-jre
RUN useradd --system --no-create-home begleiter
WORKDIR /app
COPY --from=bau /bau/target/digitalbegleiter-*.jar app.jar
# Nicht als root laufen
USER begleiter
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
