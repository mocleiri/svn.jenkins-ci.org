#!/bin/bash -ex
# merge back the RC branch.
svnmerge merge -S rc .
svn commit -F svnmerge-commit-message.txt
