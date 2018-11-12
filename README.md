# raml-to-jsonschema
RAML to JSON Schema converter

# Release and deploy to Nexus

> If the SSL cert for https://artifakt.ssbmod.net has expired, you need to allow maven to use insecure certs

```
export MAVEN_OPTS="-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true"
```

1. `mvn --settings=.maven/settings.xml clean install`
1. `mvn --settings=.maven/settings.xml release:clean release:prepare`
1. `mvn --settings=.maven/settings.xml release:perform`
