#!/bin/bash

cd "$(dirname "$0")" # go into directory of script

mkdir -p ../spring_web
avconv -y -i '../renderSpringMusic/frame%d.png' -frames 3000 -vcodec libx264 -r 25 -q 80 -pass 1 -s 540x960 -vb 1M -threads 0 -f mp4 -vf fade=type=out:start_frame=2880:nb_frames=120 ../spring_web/config_spring_web.mp4
