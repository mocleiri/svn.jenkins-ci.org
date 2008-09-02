#!/bin/sh
for f in ?.png
do
  dst=$(echo $f | sed -e s/.png/s.png/)
  echo $dst
  convert $f -resize 209x170 $dst
done
