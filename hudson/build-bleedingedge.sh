#!/bin/bash -e
# build the main and all plugins with the latest main build (instead of the default last stable build)
cd $(dirname $0)
v=$(main/show-pom-version.rb < main/pom.xml)
cp plugins/pom.xml plugins/pom.bak
plugins/update-dependency-hudson-version.rb $v < plugins/pom.bak > plugins/pom.xml
echo Building everythig with $v
exec mvn "$@"
