#!/usr/bin/env bash
export JAVA_PROGRAM_ARGS=`echo "$@"`
mvn clean compile exec:java -Dexec.mainClass="dev.snowdrop.MyIDP" -Dexec.args="$JAVA_PROGRAM_ARGS"
