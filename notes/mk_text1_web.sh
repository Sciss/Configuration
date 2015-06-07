#!/bin/bash

cd "$(dirname "$0")" # go into directory of script

mkdir -p ../text_web
avconv -y -start_number 3000 -i '../renderText1/frame%d.png' -frames 3000 -vcodec libx264 -r 25 -q 80 -pass 1 -s 540x960 -vb 1M -threads 0 -f mp4 -vf fade=type=in:start_frame=0:nb_frames=120,fade=type=out:start_frame=2880:nb_frames=120 ../text_web/config_text1_web.mp4
