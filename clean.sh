#!/bin/bash

rm -rf $HUDSON_HOME/plugins/*

cd analysis-core
mvn clean package
cp target/*.hpi $HUDSON_HOME/plugins

cd ../analysis-test
mvn clean install

cd ../checkstyle
mvn clean package
cp target/*.hpi $HUDSON_HOME/plugins

cd ../dry
mvn clean package
cp target/*.hpi $HUDSON_HOME/plugins

cd ../findbugs
mvn clean package
cp target/*.hpi $HUDSON_HOME/plugins

cd ../pmd
mvn clean package
cp target/*.hpi $HUDSON_HOME/plugins

cd ../tasks
mvn clean package
cp target/*.hpi $HUDSON_HOME/plugins

cd ../warnings
mvn clean package
cp target/*.hpi $HUDSON_HOME/plugins

cd ~/Hudson
./hudson.sh

