#!/bin/bash

cd "$(dirname "$0")" # go into directory of script

mkdir -p ../zaubes_web
avconv -y -start_number 16000 -i '../renderZaubes2/frame%d.png' -vcodec libx264 -r 25 -q 80 -pass 1 -s 540x960 -vb 1M -threads 0 -f mp4 -vf fade=type=in:start_frame=0:nb_frames=120 ../zaubes_web/config_zaubes_web.mp4
