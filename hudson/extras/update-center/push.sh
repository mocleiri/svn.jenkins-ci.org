#!/bin/sh -ex

# generate output.json
mvn clean install exec:java

# push that to the website
rm -rf www || true
svn co -N https://www.dev.java.net/svn/hudson/trunk/www
cp output.json www/update-center.json
svn commit -m "pushing new update center site" www/update-center.json
