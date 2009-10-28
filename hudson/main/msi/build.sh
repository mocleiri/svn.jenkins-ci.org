#!/bin/bash -ex
war="$1"
if [ ! -e "$war" ]; then
  echo "build.sh path/to/hudson.war"
  exit 1
fi

rm -rf tmp || true
mkdir tmp || true
unzip -p "$war" 'WEB-INF/lib/hudson-core-*.jar' > tmp/core.jar
unzip -p tmp/core.jar windows-service/hudson.exe > tmp/hudson.exe
unzip -p tmp/core.jar windows-service/hudson.xml > tmp/hudson.xm_
# replace executable name to the bundled JRE
sed -e 's|executable.*|executable>%BASE%\\jre\\bin\\java</executable>|' < tmp/hudson.xm_ > tmp/hudson.xml

# capture JRE
javac FindJava.java
JREDIR=$(java -cp . FindJava)
echo "JRE=$JREDIR"
heat dir "$JREDIR" -o jre.wxs -sfrag -sreg -nologo -srd -gg -cg JreComponents -dr JreDir -var var.JreDir

# version
v=$(unzip -p "$war" META-INF/MANIFEST.MF | grep Implementation-Version | cut -d ' ' -f2 | tr -d '\r')
echo version=$v

candle -dVERSION=$v -dJreDir="$JREDIR" -dWAR="$war" -nologo -ext WixUIExtension -ext WixUtilExtension hudson.wxs jre.wxs
light -o hudson-$v.msi -nologo -ext WixUIExtension -ext WixUtilExtension hudson.wixobj jre.wixobj
