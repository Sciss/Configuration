/*
 *  Configuration.scala
 *  (Configuration)
 *
 *  Copyright (c) 2015 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.configuration

import de.sciss.lucre.synth.{Bus, Group, Server, Synth, Txn}
import de.sciss.osc.TCP
import de.sciss.synth.proc.AuralSystem
import de.sciss.synth.{GE, SynthGraph, addAfter, addBefore, addToHead, addToTail}
import de.sciss.{numbers, synth}
import scopt.OptionParser

import scala.concurrent.ExecutionContext
import scala.concurrent.stm.Ref

object Configuration {
  final val numTransducers = 9

  final val USE_LOUDNESS = true

  var orderlyQuit = false

  private var _controlView: ControlView[D] = _   // created once in `run`
  private var _minimal      : Boolean = false
  private var _oldLoudness  : Boolean = false
  private var _loudnessLo   : Float   = 12.0f
  private var _loudnessHi   : Float   = 18.0f
  private var _loudnessCmp  : Float   = 0.25f
  private var _loudnessFreq : Float   = 800f
  private var _sparse       : Boolean = true

  def minimal     : Boolean = _minimal
  def oldLoudness : Boolean = _oldLoudness
  def loudnessLo  : Float   = _loudnessLo
  def loudnessHi  : Float   = _loudnessHi
  def loudnessCmp : Float   = _loudnessCmp
  def loudnessFreq: Float   = _loudnessFreq
  def sparse      : Boolean = _sparse

  def controlView: ControlView[D] = _controlView

  private val masterVolumeRef = Ref(0)
  def masterVolume(implicit tx: Txn): Int = masterVolumeRef.get(tx.peer)
  def masterVolume_=(value: Int)(implicit tx: Txn): Unit = {
    require(value > -80 && value <= 0, s"Illegal volume $value dB")
    masterVolumeRef.set(value)(tx.peer)
    import numbers.Implicits._
    _controlView.infraOption.foreach(_.masterGroup.set("amp" -> value.dbamp))
  }

  def main(args: Array[String]): Unit = {
    OutputRedirection()

    if (args.headOption == Some("--make")) {
      val proc = QuadGraphDB.make()
      idleThread()
      import ExecutionContext.Implicits.global
      proc.onComplete(_ => sys.exit())
    } else {
      var masterVolume0 = -6
      val parser = new OptionParser[Unit]("Configuration") {
        // opt[Unit]('v', "verbose") text "Verbose output" action { (_,_) => verbose = true }
        // opt[File]('d', "dir") required() text "Database directory" action { (f,_) => dir = f }
        opt[Int]("volume") /* required() */ text "Master volume in decibels (default: -6)" action { (d,_) => masterVolume0 = d }
        // opt[Int]('m', "num-matches") text "Maximum number of matches (default 1)" action { (i,_) => numMatches = i }
        // arg[File]("input") required() text "Meta file of input to process" action { (f,_) => inFile = f }
        opt[Unit  ]("minimal"        ) text "Minimal GUI, no testing buttons" action { (_,_) => _minimal      = true }
        opt[Unit  ]("old-mode"       ) text "Old loudness mode"               action { (_,_) => _oldLoudness  = true }
        opt[Double]("loudness-low"   ) text "Loudness comp low  threshold"    action { (v,_) => _loudnessLo   = v.toFloat }
        opt[Double]("loudness-high"  ) text "Loudness comp high threshold"    action { (v,_) => _loudnessHi   = v.toFloat }
        opt[Double]("loudness-amount") text "Loudness comp amount"            action { (v,_) => _loudnessCmp  = v.toFloat }
        opt[Double]("loudness-freq"  ) text "Loudness comp min freq"          action { (v,_) => _loudnessFreq = v.toFloat }
        opt[Unit  ]("no-sparse"      ) text "Disable new sparse mode"         action { (_,_) => _sparse       = false }
      }
      if (!parser.parse(args)) sys.exit(1)
      masterVolumeRef.single.set(masterVolume0)

      run()
    }
  }

  def run(): Unit = {
    val quad = QuadGraphDB.open()
    import quad.cursor

    killSuperCollider()
    cursor.step { implicit tx =>
      val boids = BoidProcess[D]
      // boids.period = 0.01 // 5
      if (minimal) boids.start()
      _controlView = ControlView(boids, quad)
      boot()
    }
  }

  def killSuperCollider(): Unit = {
    import sys.process._
    Seq("killall", "scsynth").!
    Thread.sleep(1000)
  }

  def restart(): Unit = {
    println("Restarting...")
    killSuperCollider()
    _controlView.cursor.step { implicit tx =>
      _controlView.disposeAural()
      boot()
    }
  }

  private def mkLoudnessComp(fft: GE): GE = {
    import synth._
    import ugen._

    val tMask   = Delay1.kr(1)  // bug in Loudness initialization!!
    val loud0   = Loudness    .kr(fft, tmask = tMask)
    val cent0   = SpecCentroid.kr(fft)
    val flat0   = SpecFlatness.kr(fft)
    val lagTime = 2.0 // 1.0 // 0.5
    val loud    = Lag.kr(loud0, time = lagTime)
    val freqOk  = Lag.kr(cent0 > loudnessFreq, time = lagTime)
    val flat    = Lag.kr(flat0, time = lagTime)

    val loudN   = loud.clip(loudnessLo, loudnessHi).linlin(loudnessLo, loudnessHi, 0.0, 1.0)
    val flatN   = flat.clip(0.10, 0.175).linlin(0.175, 0.10, 0.0, 1.0)
    val compAmt = loudN * flatN * freqOk
    val compSig = compAmt.linlin(0, 1, 1.0, loudnessCmp)
    compSig
  }

  private def mkMovingEQ(in: GE): GE = {
    val poll        = false
    // val numChannels = 1

    import synth._
    import ugen._

    // val hi = BHiShelf.ar(in = ..., freq = ..., rs = ..., gain /* dB */ = ...)
    // val lo = BLoShelf.ar(in = ..., freq = ..., rs = ..., gain /* dB */ = ...)

    val infC = inf // Seq.fill(numChannels)(inf)

    val dFreq = Dbrown(lo =   0  , hi = 1, step = 0.05, length = infC).linexp(0, 1, 100 /* 200 */, 8000)
    val dGain = Dbrown(lo = -30  , hi = 0, step = 2.0 , length = infC) // .min(0)
    val dQ    = Dbrown(lo =   0.3, hi = 1, step = 0.1 , length = infC)

    def mkFilter(trig: GE, pl: String): GE = {
      // val dFreq = Dbrown(lo =   0  , hi = 1, step = 0.05, length = infC).linexp(0, 1, 100 /* 200 */, 8000)
      // val dGain = Dbrown(lo = -30  , hi = 0, step = 2.0 , length = infC) // .min(0)
      // val dQ    = Dbrown(lo =   0.3, hi = 1, step = 0.1 , length = infC)
      val freq  = Demand.kr(trig = trig, dFreq).max(100)    // the author of Demand should be decapitated
      val gain  = Demand.kr(trig = trig, dGain)
      val q     = Demand.kr(trig = trig, dQ   ).max(0.3)    // the author of Demand should be decapitated
      val freqL = Lag.kr(freq, 1)
      val gainL = Lag.kr(gain, 1)
      val rqL   = Lag.kr(q.reciprocal, 1)

      if (poll) {
        (Flatten(freqL) \ 0).poll(1, label = s"freq$pl")
        (Flatten(gainL) \ 0).poll(1, label = s"gain$pl")
        (Flatten(rqL  ) \ 0).poll(1, label = s"rq  $pl")
      }

      //  CheckBadValues.ar(in, id = 1000)
      val pk    = BPeakEQ .ar(in = in, freq = freqL, rq = rqL, gain /* dB */ = gainL)
      //      CheckBadValues.ar(pk, id = 2000)
      //      val check = Impulse.kr(0)
      //      in   .poll(check, "--in")
      //      freqL.poll(check, "--fr")
      //      rqL  .poll(check, "--rq")
      //      gainL.poll(check, "--ga")
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
    fd
  }

  def boot()(implicit tx: D#Tx): Unit = {
    val sCfg = Server.Config()
    sCfg.deviceName         = Some("Configuration")
    // sCfg.audioBusChannels
    sCfg.inputBusChannels   = 0
    sCfg.outputBusChannels  = numTransducers
    sCfg.memorySize = 256 * 1024
    // sCfg.wireBuffers
    sCfg.transport  = TCP
    sCfg.pickPort()
    val aural = AuralSystem()

    def started(s: Server)(implicit tx: D#Tx): Unit = {
      val graphNorm = SynthGraph {
        import synth._
        import ugen._
        val in      = In.ar("in".kr, 1)
        val normDur = 2.0
        val ceil1   = -1.0.dbamp
        val ceil2   = -0.2.dbamp
        val sig0    = Normalizer.ar(in, level = ceil1, dur = normDur)
        // sig0.poll(sig0.abs > 1, label = "NORM")
        // val sig     = Limiter.ar(sig0, level = -0.2.dbamp)
        val sig1 = LeakDC.ar(sig0.clip(-ceil1, ceil1)).clip(-ceil2, ceil2)  // nu is gut!
        // sig .poll(sig .abs > 1, label = "LIM ")
        // val sig = sig0

        val sig2 = if (USE_LOUDNESS) {
          val fftBuf = LocalBuf(512, 1)
          val fft = FFT(fftBuf, in = in \ 0, winType = 1 /* Hann */)

          val lComp = if (oldLoudness) {
            val loudFloor = LFNoise1.kr(60.reciprocal).linlin(-1, 1, 10, 13)
            val loudCeil  = LFNoise1.kr(60.reciprocal).linlin(-1, 1, 20, 25)
            val loudness  = Lag.kr(Loudness.kr(fft), time = 4.0).clip(loudFloor, loudCeil)
            loudness.linlin(loudFloor, loudCeil, 1.0, 0.25) // compress loud parts
          } else {
            mkLoudnessComp(fft)
          }
          sig1 * lComp
        } else sig1

        val sig = if (sparse) mkMovingEQ(sig2) else sig2

        val outBus    = "out".kr

//        val pollTr    = outBus.sig_==(0) * Impulse.kr(30)
//        lComp.ampdb.roundTo(0.1).poll(pollTr, "lcomp")

        Out.ar(outBus, sig)
      }

      val graphMaster = SynthGraph {
        import synth._
        import ugen._
        val bus   = "bus".kr
        val in    = In.ar(bus, numTransducers)
        val gain  = "amp".kr(1f)
        val sig   = in * gain

//        val fftBuf    = LocalBuf(512, 1)
//        val fft       = FFT(fftBuf, in = in \ 0, winType = 1 /* Hann */)
//        val centroid  = Lag.kr(SpecCentroid.kr(fft))
//        val flatness  = Lag.kr(SpecFlatness.kr(fft))
//        val loudness  = Lag.kr(Loudness    .kr(fft))
//        centroid.poll(1, "cent")
//        flatness.poll(1, "flat")
//        loudness.poll(1, "loud")

        ReplaceOut.ar(bus, sig)
      }

      val normGroup   = Group(s.defaultGroup)
      val masterGroup = Group(normGroup, addAfter)
      val channels    = Vec.tabulate(numTransducers) { i =>
        val group = Group(normGroup, addBefore)
        val bus   = Bus.audio(s, 1)
        new Infra.Channel(i, group, bus)
      }
      val infra = new Infra(server = s, channels = channels, masterGroup = masterGroup, normGroup = normGroup)

      channels.foreach { ch =>
        val syn = Synth.play(graphNorm, nameHint = Some("norm"))(target = normGroup, addAction = addToTail,
          args = List("out" -> ch.index))
        syn.read(ch.bus -> "in")
      }

      import numbers.Implicits._
      Synth.play(graphMaster, nameHint = Some("master"))(target = masterGroup, addAction = addToHead,
        args = List("bus" -> 0, "amp" -> masterVolume.dbamp))

      _controlView.infra = infra
    }

    aural.addClient(new AuralSystem.Client {
      private val lastServer = Ref.make[Server]

      def auralStarted(s: Server)(implicit tx: Txn): Unit = {
        lastServer.set(s)(tx.peer)
        tx.afterCommit(_controlView.cursor.step { implicit tx => started(s) })
      }

      def auralStopped()(implicit tx: Txn): Unit = {
        println("AuralSystem stopped.")
        if (!orderlyQuit) {
          lastServer.get(tx.peer).peer.dispose()
          // _controlView.auralBoidsOption.foreach(_.debugPrint())
          tx.afterCommit(restart())
        }
      }
    })
    aural.start(sCfg)
  }
}