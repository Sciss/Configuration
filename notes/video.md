video to image sequence:

- `avconv -i '/home/hhrutz/Documents/applications/140616_SteiermarkRiga/photos/2015/04/06/DSCN2051.AVI' -f image2 'frame%03d.png'`

- `avconv -i '/home/hhrutz/Documents/applications/140616_SteiermarkRiga/photos/2015/04/27/P1010149.MOV' -f image2 'frame%d.png'`

attempts for other formats:

- `avconv -y -i 'frame%d.png' -vcodec mpeg4 -r 25 -q 100 -pass 1 -s 1920x1080 -vb 6M -threads 0 -f mp4 -vf "transpose=2" out_mpeg4.mp4`

- `avconv -y -i out.mp4 -vcodec libxvid -q 100 -pass 1 -vf "scale=1280:720" -aspect 16:9 -vb 6M -threads 0 -f mp4 out_xvidl.mp4`

- `avconv -y -i out.mp4 -vcodec libxvid -q 100 -pass 1 -vf "scale=1280:720" -aspect 16:9 -tag:v DIVX -vb 6M -threads 0 -f avi out_xvidt.avi`

- `avconv -y -i out.mp4 -vcodec libxvid -q 90 -pass 1 -vf "scale=1920:1080" -aspect 16:9 -tag:v DIVX -vb 6M -threads 0 -f avi out_xvidu.avi`

- `avconv -y -i out.mp4 -vcodec libxvid -b:v 1500k -pass 1 -vf "scale=1920:1080" -aspect 16:9 -tag:v DIVX -threads 0 -f avi out_xvidu.avi`
(note: rate control doesn't have any effect, ends up with 1.5 GB file)

- `avconv -y -i out.mp4 -vcodec libxvid -b:v 1500k -pass 2 -vf "scale=1920:1080" -aspect 16:9 -tag:v DIVX -threads 0 -f avi out_xvidv.avi`
(kind of obeys the given bitrate)

- `avconv -y -i out.mp4 -vcodec libxvid -b:v 2560k -pass 2 -vf "scale=1920:1080" -aspect 16:9 -threads 0 -f mp4 out_xvidw.mp4`

- `avconv -y -i ../../MutagenTx/renderBetanovuss0_100/out.mp4 -vcodec libxvid -q 100 -pass 1 -b:v 12000k -vf "scale=1024:768" -aspect 16:9 -vb 6M -threads 0 -f mp4 beta2.mp4`
