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

import de.sciss.lucre.synth.{BusNodeSetter, Synth, Bus, Group, Server, Txn}
import de.sciss.osc.TCP
import de.sciss.synth
import de.sciss.synth.{addToTail, addAfter, ugen, SynthGraph}
import de.sciss.synth.proc.AuralSystem

import scala.concurrent.ExecutionContext

object Configuration {
  final val numTransducers = 9

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

  def boot(view: ControlView[D])(implicit tx: D#Tx): Unit = {
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
    aural.addClient(new AuralSystem.Client {
      def auralStarted(s: Server)(implicit tx: Txn): Unit = {
        val graphNorm = SynthGraph {
          import synth._
          import ugen._
          val in      = In.ar("in".kr, 1)
          val normDur = 2.0
          val sig     = Normalizer.ar(in, level = -0.2.dbamp, dur = normDur)
          Out.ar("out".kr, sig)
        }

        val inGroup     = Group(s.defaultGroup)
        val normGroup   = Group(inGroup  , addAfter)
        val masterGroup = Group(normGroup, addAfter)
        val inBuses     = Vec.fill(numTransducers)(Bus.audio(s, 1))
        val infra       = new Infra(server = s, inGroup = inGroup, masterGroup = masterGroup,
          normGroup = normGroup, inBuses = inBuses)

        inBuses.zipWithIndex.foreach { case (inBus, idx) =>
          val syn = Synth.play(graphNorm, nameHint = Some("norm"))(target = normGroup, addAction = addToTail,
            args = List("out" -> idx))
          syn.read(inBus -> "in")
        }

        view.infra = infra
      }

      def auralStopped()(implicit tx: Txn): Unit = {
        println("AuralSystem stopped.")
      }
    })
    aural.start(sCfg)
  }
}