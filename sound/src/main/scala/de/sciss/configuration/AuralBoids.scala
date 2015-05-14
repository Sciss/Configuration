/*
 *  AuralBoids.scala
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

import de.sciss.configuration.QuadGraphDB.PlacedNode
import de.sciss.lucre.geom.{IntDistanceMeasure2D, IntPoint2D}
import de.sciss.lucre.stm.{TxnLike, Disposable}
import de.sciss.lucre.synth.{Synth, Sys}
import de.sciss.numbers
import de.sciss.synth.addToHead
import de.sciss.synth.proc.Timeline

import scala.concurrent.stm.{Ref, TSet}

object AuralBoids {
  def apply[S <: Sys[S]](infra: Infra, boids: BoidProcess[S], quad: QuadGraphDB[S])
                        (implicit tx: S#Tx): AuralBoids[S] = {
    new Impl(infra, boids, quad)
  }

  private final class Impl[S <: Sys[S]](infra: Infra, boids: BoidProcess[S], quad: QuadGraphDB[S])
    extends AuralBoids[S] {

    private val scheduled = TSet.empty[Int]

    def start()(implicit tx: S#Tx): Unit = {
      stop()

      infra.channels.foreach { ch =>
        stepChan(ch)
      }
    }

    private val playingNodes = TSet.empty[PlacedNode]

    private def stepChan(ch: Infra.Channel)(implicit tx: S#Tx): Unit = {
      implicit val itx = tx.peer
      val layer         = 0 // XXX TODO
      val q             = quad.handles(layer).apply()
      val loc           = boids.state.apply(ch.index).location
      val pt            = IntPoint2D((loc.x + 0.5f).toInt, (loc.y + 0.5f).toInt)
      q.nearestNeighborOption(pt, IntDistanceMeasure2D.euclideanSq).foreach { node =>
        val graph = node.node.input.graph
        val syn = Synth.play(graph)(target = ch.group, addAction = addToHead)
        syn.write(ch.bus -> "out")
        playingNodes += node
        syn.onEndTxn { implicit tx =>
          // syn.definition.dispose()  // XXX TODO - safe to do this here?
          implicit val itx = tx.peer
          playingNodes -= node
          // println(s"playingNodes.size = ${playingNodes.size}")
        }
      }
      import numbers.Implicits._
      val delaySeconds  = math.random.linexp(0, 1, 4.0, 30.0)
      val delayFrames   = (delaySeconds * Timeline.SampleRate).toLong
      val sched         = boids.scheduler
      val absFrames     = sched.time + delayFrames
      val tokenRef      = Ref(-1)
      val token         = sched.schedule(absFrames) { implicit tx =>
        implicit val itx = tx.peer
        scheduled -= tokenRef()
        releaseChan(ch)
        stepChan(ch)
      }
      tokenRef() = token
      scheduled += token
    }

    private def releaseChan(ch: Infra.Channel)(implicit tx: S#Tx): Unit =
      ch.group.release(4.0) // XXX TODO

    def stop()(implicit tx: S#Tx): Unit = {
      implicit val itx = tx.peer
      infra.channels.foreach(_.group.freeAll())
      import boids.scheduler
      scheduled.foreach(scheduler.cancel)
      scheduled.clear()
    }

    def dispose()(implicit tx: S#Tx): Unit = stop()

    def debugPrint()(implicit tx: TxnLike): Unit = {
      implicit val itx = tx.peer
      println("------- LAST ACTIVE NODES -------")
      playingNodes.foreach { n =>
        println(n)
        val ugens = n.node.input.graph.sources.collect {
          case p: Product => p.productPrefix
        } .toSet.toIndexedSeq.sorted
        println(ugens.mkString("[", ", ", "]"))
      }
    }
  }
}
trait AuralBoids[S <: Sys[S]] extends Disposable[S#Tx] {
  def start()(implicit tx: S#Tx): Unit
  def stop ()(implicit tx: S#Tx): Unit

  def debugPrint()(implicit tx: TxnLike): Unit
}