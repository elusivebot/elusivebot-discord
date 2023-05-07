FROM eclipse-temurin:17-jre
LABEL version="0.1.0" description="ElusiveBot Discord endpoint service" maintainer="bryan@degrendel.com"

RUN apt update && apt install unzip

# TODO: Update this to an argument, investigate whether there's a good
# way to automagically grab it from gradle.properties and apply to this
# command and the LABEL version.
COPY app/build/distributions/app-0.1.0-SNAPSHOT.zip .
RUN unzip app-0.1.0-SNAPSHOT.zip

CMD [ "./app-0.1.0-SNAPSHOT/bin/app" ]
