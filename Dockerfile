FROM maven:3.9.6-eclipse-temurin-17

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests

RUN ls target

CMD ["sh", "-c", "java -jar target/*.jar --server.port=$PORT"]