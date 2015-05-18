val buf = Buffer.read(s, path = (userHome/"Documents"/"devel"/"Configuration"/"rec"/"rec-150518_142854Cut.aif").path)

val numFrames = buf.numFrames
val numFFT = numFrames/256
val bufL = Buffer.alloc(s, numFFT)
val bufC = Buffer.alloc(s, numFFT)
val bufF = Buffer.alloc(s, numFFT)
val bufX = Buffer.alloc(s, numFFT)

val x = play {
  val index = Phasor.ar(trig = 0, speed = 1, lo = 0, hi = BufFrames.kr(buf.id))
  val p = BufRd.ar(numChannels = 1, buf = buf.id, index = index, loop = 1, interp = 1)
  val fftBuf = LocalBuf(512)
  val fft = FFT(buf = fftBuf, in = p, winType = 1)
  val tmask = Delay1.kr(1)  // bug in Loudness initialization!!
  val loud0 = Loudness    .kr(fft, tmask = tmask)
  val cent0 = SpecCentroid.kr(fft)
  val flat0 = SpecFlatness.kr(fft)
  
  val secs = index / 44100
  val tr = Impulse.ar(2)

  val lagTime = 2.0 // 1.0 // 0.5
  // val loud1 = (loud0 * 0.05).clip(0, 1)
  //  loud1.poll(tr, "xxxx")
  val loud = Lag.kr(loud0, time = lagTime)
  val cent = Lag.kr(cent0 / 5000.clip(0, 2), time = lagTime)
  val flat = Lag.kr(flat0, time = lagTime)
  
//   secs.roundTo(0.1).poll(tr, "time")
//   loud.roundTo(0.1).poll(tr, "loud")
//   cent.roundTo(1.0).poll(tr, "cent")
//   (flat * 100).roundTo(0.1).poll(tr, "flat")
  
  val fftKRate = 1.0/256 * SampleRate.ir / ControlRate.ir
  fftKRate.poll(0, "indexCtl-rate")
  val indexCtl = Phasor.kr(trig = 0, speed = fftKRate, lo = 0, hi = BufFrames.kr(bufL.id))
  
  BufWr.kr(in = loud, buf = bufL.id, index = indexCtl, loop = 0)
  BufWr.kr(in = cent, buf = bufC.id, index = indexCtl, loop = 0)
  BufWr.kr(in = flat, buf = bufF.id, index = indexCtl, loop = 0)
  
  val loudN = loud.clip(6.0, 12.0) / 6.0 - 1.0  // normalized loudness
  val flatN = flat.clip(0.10, 0.175).linlin(0.175, 0.10, 0.0, 1.0)
  val compAmt = loudN * flatN
  
  val compSig = (1 - compAmt * 0.75) // .linlin(0, 1, 1.0, 0.25)

  compSig.poll(tr, "comp")

  BufWr.kr(in = compSig, buf = bufX.id, index = indexCtl, loop = 0)
  
  Line.kr(dur = BufDur.kr(buf.id), doneAction = freeSelf)
  
  val sigOut = p * compSig
  
  // val p = PlayBuf.ar(numChannels = 1, buf = buf.id, speed = 1.0, loop = 1)
  Out.ar(0, Pan2.ar(sigOut))
}

val futL = bufL.getData()
val futC = bufC.getData()
val futF = bufF.getData()
val futX = bufX.getData()
val dLoud = futL.value.get.get
val dCent = futC.value.get.get
val dFlat = futF.value.get.get
val dComp = futX.value.get.get

Vector(dLoud, dCent, dFlat).plot()
Vector(dLoud.map(_ * 20), dCent.map(_ / 60), dFlat).plot()
dFlat.plot(title = "flatness")
dLoud.plot(title = "loudness")
dComp.map(_.ampdb).plot(title = "compression")
dComp.             plot(title = "compression")

(dLoud zip dFlat).map { case (l, f) =>
  val ln = l.clip(0.3, 0.6).linlin(0.3, 0.6, 0.0, 1.0)
  val fn = f.clip(0.10, 0.175).linlin(0.175, 0.10, 0.0, 1.0)
  ln * fn
} .plot("comp")

// dCent.plot()
