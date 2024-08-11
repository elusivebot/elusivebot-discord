FROM eclipse-temurin:17-jre
LABEL version="0.1.0" description="ElusiveBot Discord endpoint service" maintainer="bryan@degrendel.com"

# Path of the archive without .zip (i.e. app-0.1.0-SNAPSHOT).
ARG base_filename
# Path to directory containing the distribution zip
ARG jar_path=app/build/distributions

RUN apt update && apt install unzip

COPY $jar_path/${base_filename}.zip .
RUN unzip -d hold ${base_filename}.zip && cd hold && ls && mv * /app && cd .. && rmdir hold

CMD [ "/app/bin/app" ]
