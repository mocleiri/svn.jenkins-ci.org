#!/bin/sh -ex
# build flashing balls
for sz in 16x16 24x24 32x32 48x48
do
  for color in grey blue yellow red
  do
    convert $sz/$color.png \( +clone -fill white -draw 'color 0,0 reset' \) \
         -compose Dst_Over ../resources/images/$sz/$color.gif
    ./png2gifanime.sh $sz/$color.png $sz/nothing.png > ../resources/images/$sz/${color}_anime.gif
  done
done
