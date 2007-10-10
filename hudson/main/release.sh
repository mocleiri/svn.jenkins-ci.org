#!/bin/sh -ex
#
# Kohsuke's automated release script. Sorry for my checking this in,
# but Maven doesn't let me run release goals unless I have this in CVS.

# make sure we have up to date workspace and M2 repo copy
cvs -q update -Pd
old=$PWD
cd "$JAVANET_M2_REPO"
cd org/jvnet/hudson
svn update
cd "$old"

tag=hudson-$(show-pom-version pom.xml | sed -e "s/-SNAPSHOT//g" -e "s/\\./_/g")
mvn -B -Dtag=$tag release:prepare || mvn install release:prepare
mvn release:perform

id=$(show-pom-version target/checkout/pom.xml)
#./publish-javadoc.sh
javanettasks uploadFile hudson /releases/$id                "`date +"%Y/%m/%d"` release" Stable target/checkout/war/target/hudson.war
javanettasks uploadFile hudson /releases/source-bundles/$id "`date +"%Y/%m/%d"` release" Stable target/checkout/target/hudson-$id-src.zip
javanettasks announce hudson "Hudson $id released" << EOF
See <a href="https://hudson.dev.java.net/changelog.html">the changelog</a> for details.
EOF

# this is for the JNLP start
cp target/checkout/war/target/hudson.war target/checkout/war/target/hudson.jar
javanettasks uploadFile hudson /releases/jnlp/hudson.jar "version $id" Stable target/checkout/war/target/hudson.jar | tee target/upload.log

# replace the jar file link accordingly
WWW=../../../www
cvs update -l
jarUrl=$(cat target/upload.log | grep "^Posted" | sed -e "s/Posted //g")
perl -p -i.bak -e "s|https://.+hudson\.jar|$jarUrl|" $WWW/hudson.jnlp
cp $WWW/hudson.jnlp $WWW/$id.jnlp

# update changelog.html
ruby update.changelog.rb $id < $WWW/changelog.html > $WWW/changelog.new
mv $WWW/changelog.new $WWW/changelog.html

# push changes to the maven repository
ruby push-m2-repo.rb $JAVANET_M2_REPO $id

./publish-javadoc.sh

cd ../../../www
cvs update -l
cvs commit -m "Hudson $id released" changelog.html hudson.jnlp
