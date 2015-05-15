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
import de.sciss.synth.{SynthGraph, addAfter, addBefore, addToHead, addToTail}
import de.sciss.{numbers, synth}
import scopt.OptionParser

import scala.concurrent.ExecutionContext
import scala.concurrent.stm.Ref

object Configuration {
  final val numTransducers = 9

  var orderlyQuit = false

  private var _controlView: ControlView[D] = _   // created once in `run`
  private var _minimal: Boolean = false
  def minimal: Boolean = _minimal

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
        opt[Unit]("minimal") text "Minimal GUI, no testing buttons" action { (_,_) => _minimal = true }
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
  }

  def restart(): Unit = {
    println("Restarting...")
    killSuperCollider()
    _controlView.cursor.step { implicit tx =>
      _controlView.disposeAural()
      boot()
    }
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

        val fftBuf    = LocalBuf(512, 1)
        val fft       = FFT(fftBuf, in = in \ 0, winType = 1 /* Hann */)
        // val centroid  = Lag.kr(SpecCentroid.kr(fft))
        // val flatness  = Lag.kr(SpecFlatness.kr(fft))
        val loudFloor = LFNoise1.kr(60.reciprocal).linlin(-1, 1, 10, 13)
        val loudCeil  = LFNoise1.kr(60.reciprocal).linlin(-1, 1, 20, 25)

        val loudness  = Lag.kr(Loudness.kr(fft), time = 4.0).clip(loudFloor, loudCeil)
        // centroid.poll(1, "cent")
        // flatness.poll(1, "flat")
        // loudness.poll(1, "loud")
        val lComp     = loudness.linlin(loudFloor, loudCeil, 1.0, 0.25)  // compress loud parts
        val sig       = sig1 * lComp
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