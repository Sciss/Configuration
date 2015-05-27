val f = file("/home/hhrutz/Documents/devel/Configuration/rec/config-dry-150515_013550.aif")

val b = Buffer.cue(s, path = f.path, numChannels = 2, bufFrames = 32768)

val x = play {
  DiskIn.ar(numChannels = 2, buf = b.id, loop = 1)
}

val y = play(addAction = addToTail) {
  val in = In.ar(0, numChannels)

  val poll        = true
  val numChannels = 2
  
  // val hi = BHiShelf.ar(in = ..., freq = ..., rs = ..., gain /* dB */ = ...)
  // val lo = BLoShelf.ar(in = ..., freq = ..., rs = ..., gain /* dB */ = ...)
    
  val infC = Seq.fill(numChannels)(inf)

  val dFreq = Dbrown(lo =   0  , hi = 1, step = 0.05, length = infC).linexp(0, 1, 100 /* 200 */, 8000)
  val dGain = Dbrown(lo = -30  , hi = 0, step = 2.0 , length = infC) // .min(0)
  val dQ    = Dbrown(lo =   0.3, hi = 1, step = 0.1 , length = infC)

  def mkFilter(trig: GE, pl: String): GE = {
    // val dFreq = Dbrown(lo =   0  , hi = 1, step = 0.05, length = infC).linexp(0, 1, 100 /* 200 */, 8000)
    // val dGain = Dbrown(lo = -30  , hi = 0, step = 2.0 , length = infC) // .min(0)
    // val dQ    = Dbrown(lo =   0.3, hi = 1, step = 0.1 , length = infC)
    val freq  = Demand.kr(trig = trig, dFreq)
    val gain  = Demand.kr(trig = trig, dGain)
    val q     = Demand.kr(trig = trig, dQ   )
    val freqL = Lag.kr(freq, 1)
    val gainL = Lag.kr(gain, 1)
    val rqL   = Lag.kr(q.reciprocal, 1)
    
    if (poll) {
      (Flatten(freqL) \ 0).poll(1, label = s"freq$pl")
      (Flatten(gainL) \ 0).poll(1, label = s"gain$pl")
      (Flatten(rqL  ) \ 0).poll(1, label = s"rq  $pl")
    }
    
    val pk    = BPeakEQ .ar(in = in, freq = freqL, rq = rqL, gain /* dB */ = gainL)
    pk
  }
  
  val dTr = Dwhite(lo = 10, hi = 40, length = infC)
  val tr  = TDuty.kr(dur = dTr)
  val tr1 = PulseDivider.kr(tr, div = 2, start = 0)  // triggers second
  val tr2 = PulseDivider.kr(tr, div = 2, start = 1)  // triggers first
  val p0  = Impulse.kr(0)
  val f1  = mkFilter(tr1 + p0, "1")
  val f2  = mkFilter(tr2 + p0, "2")
  val tff = ToggleFF.kr(TDelay.kr(tr, dur = 1))     // so inaudible filter has reached new values
  val pan = Slew.kr(in = tff, up = 10.reciprocal, down = 10.reciprocal).linlin(0, 1, -1, 1)
  
  if (poll) pan.poll(1, label = "pan")
  
  val fd  = LinXFade2.ar(inA = f1, inB = f2, pan = pan)
  ReplaceOut.ar(0, fd)
}

y.run(false)
y.run(true )

y.free()

x.free(); b.close(); b.free()
