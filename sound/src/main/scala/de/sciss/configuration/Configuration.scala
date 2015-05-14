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

import de.sciss.lucre.synth.{Sys, Bus, Group, Server, Synth, Txn}
import de.sciss.osc.TCP
import de.sciss.synth
import de.sciss.synth.proc.AuralSystem
import de.sciss.synth.{SynthGraph, addAfter, addBefore, addToTail}

import scala.concurrent.ExecutionContext

object Configuration {
  final val numTransducers = 9

  var orderlyQuit = false

  def main(args: Array[String]): Unit = {
    if (args.headOption == Some("--make")) {
      val proc = QuadGraphDB.make()
      idleThread()
      import ExecutionContext.Implicits.global
      proc.onComplete(_ => sys.exit())
    } else {
      run()
    }
  }

  def run(): Unit = {
    val quad = QuadGraphDB.open()
    import quad.cursor

    import sys.process._
    Seq("killall", "scsynth").!
    cursor.step { implicit tx =>
      val boids = BoidProcess[D]
      // boids.period = 0.01 // 5
      val view = ControlView(boids, quad)
      boot(view)
    }
  }

  def boot[S <: Sys[S]](view: ControlView[S])(implicit tx: S#Tx): Unit = {
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

    def started(s: Server)(implicit tx: S#Tx): Unit = {
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
        val sig = LeakDC.ar(sig0.clip(-ceil1, ceil1)).clip(-ceil2, ceil2)  // nu is gut!
        // sig .poll(sig .abs > 1, label = "LIM ")
        // val sig = sig0
        Out.ar("out".kr, sig)
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

      view.infra = infra
    }

    aural.addClient(new AuralSystem.Client {
      def auralStarted(s: Server)(implicit tx: Txn): Unit = {
        tx.afterCommit(view.cursor.step { implicit tx => started(s) })
      }

      def auralStopped()(implicit tx: Txn): Unit = {
        println("AuralSystem stopped.")
        if (!orderlyQuit) {
          view.auralBoidsOption.foreach(_.debugPrint())
        }
      }
    })
    aural.start(sCfg)
  }
}