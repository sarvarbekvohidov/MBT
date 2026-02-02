# Runtime-only Dockerfile: copy prebuilt jar and run it
FROM eclipse-temurin:17-jdk
WORKDIR /app

# copy the prebuilt jar (committed to repo)
COPY target/SchoolTracking-1.0-SNAPSHOT.jar ./SchoolTracking-1.0-SNAPSHOT.jar

EXPOSE 8080

# create and switch to non-root user
RUN addgroup --system app && adduser --system --ingroup app app
USER app

ENTRYPOINT ["java", "-jar", "SchoolTracking-1.0-SNAPSHOT.jar"]
