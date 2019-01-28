FROM statisticsnorway/alpine-jdk11-buildtools:latest as build

WORKDIR /
RUN git clone https://github.com/statisticsnorway/raml-to-jsonschema.git
WORKDIR /raml-to-jsonschema
#RUN mvn -B verify dependency:go-offline
#RUN mvn -B -o dependency:copy-dependencies
RUN mvn -B clean install

RUN ["jlink", "--strip-debug", "--no-header-files", "--no-man-pages", "--compress=2", "--module-path", "/opt/jdk/jmods", "--output", "/linked",\
    "--add-modules", "jdk.unsupported,java.base,java.logging,java.sql,java.naming,java.desktop"]

#
# Build image
#
FROM alpine:latest

#
# Resources from build image
#
COPY --from=build /linked /opt/jdk/
COPY --from=build /raml-to-jsonschema/target/raml-to-jsonschema-*.jar /opt/raml-to-jsonschema/bin/

ENV PATH=/opt/jdk/bin:$PATH

RUN mkdir -p /raml-project

VOLUME ["/raml-project"]

WORKDIR /raml-project

CMD ["java", "-cp", "/opt/raml-to-jsonschema/bin/*", "no.ssb.raml.RamltoJsonSchemaConverter", "jsonschemas/", "schemas/"]
