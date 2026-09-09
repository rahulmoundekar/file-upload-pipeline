FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser
COPY target/file-upload-pipeline-0.0.1-SNAPSHOT.jar app.jar
USER 10001
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8083
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
