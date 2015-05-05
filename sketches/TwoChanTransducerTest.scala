val x = play {
  val a1 = "amp1".kr(1)
  val a2 = "amp2".kr(1)
  val sig = WhiteNoise.ar(Seq(0.1, 0.1)) * LFPulse.ar("freq".ar(Seq(20, 20.1))) * LFTri.ar(Seq(0.1, 0.11))
  sig * Seq(a1, a2)
}

x.free()

val x = play {
  Out.ar("bus".kr(0), WhiteNoise.ar(0.1) * LFPulse.ar("freq".ar(20)) * LFTri.ar(0.1))
}

x.set("bus" -> 1)

