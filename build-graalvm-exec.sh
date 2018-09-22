#!/usr/bin/env bash
set -ex

# 1. Download GraalVM
# 2. Set up GRAALVM_HOME and add GRAALVM_HOME/bin to PATH
# 3. Requires JDK 8

#mvn clean install

$GRAALVM_HOME/bin/native-image -cp ./target/raml-to-jsonschema-0.2-SNAPSHOT.jar -H:Name=raml-to-json -H:Class=no.ssb.raml.Main -H:+ReportUnsupportedElementsAtRuntime

# Fails due to: graal Detected a started Thread in the image heap. Threads running in the image generator are no longer running at image run time. The object was probably created by a class initializer and is reachable from a static field. By default, all class initialization is done during native image building.You can manually delay class initialization to image run time by using the option --delay-class-initialization-to-runtime=<class-name>. Or you can write your own initialization methods and call them explicitly from your main entry point
