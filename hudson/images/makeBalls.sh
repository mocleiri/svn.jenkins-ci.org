#!/bin/sh -ex
# build flashing balls

t=/tmp/makeBalls$$

for sz in 16x16 24x24 32x32 48x48
do
  for color in grey blue yellow red
  do
    convert $sz/$color.png \( +clone -fill white -draw 'color 0,0 reset' \) \
         -compose Dst_Over ../resources/images/$sz/$color.gif
    convert $sz/$color.png -fill white -colorize 20% $t.80.png
    convert $sz/$color.png -fill white -colorize 40% $t.60.png
    convert $sz/$color.png -fill white -colorize 60% $t.40.png
    convert $sz/$color.png -fill white -colorize 80% $t.20.png
    ./png2gifanime.sh $sz/$color.png $t.80.png $t.60.png $t.40.png $t.20.png $sz/nothing.png $t.20.png $t.40.png $t.60.png $t.80.png > ../resources/images/$sz/${color}_anime.gif
  done
done

rm $t.*.png
