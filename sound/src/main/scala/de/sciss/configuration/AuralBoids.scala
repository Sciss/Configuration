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

import java.util.Date

import de.sciss.configuration.QuadGraphDB.PlacedNode
import de.sciss.lucre.event.Observable
import de.sciss.lucre.event.impl.ObservableImpl
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
    new Impl(infra, boids, quad, layer0 = util.Random.nextInt(QuadGraphDB.numLayers))
  }

  private final class Impl[S <: Sys[S]](infra: Infra, boids: BoidProcess[S], quad: QuadGraphDB[S], layer0: Int)
    extends AuralBoids[S] with ObservableImpl[S, Update] {

    private val scheduled     = TSet.empty[Int]
    private val layerRef      = Ref(layer0)
    private val playingNodes  = TSet.empty[(Long, PlacedNode)] // _1 = time stamp

    def start()(implicit tx: S#Tx): Unit = {
      stop()

      infra.channels.foreach { ch =>
        stepChan(ch)
      }

      stepLayer()
    }

    private def stepLayer()(implicit tx: S#Tx): Unit = {
      implicit val itx = tx.peer
      import numbers.Implicits._
      // XXX TODO - DRY
      val lyr           = layer
      val short         = lyr > 2
      // val dlyMinMins    = if (short)  7.5 else 10.0
      val dlyMinMins    = if (short)  5.0 else  7.5
      val dlyMaxMins    = if (short) 10.0 else 15.0
      val delaySeconds  = math.random.linlin(0, 1, dlyMinMins, dlyMaxMins) * 60   // 10 to 15 minutes
      val delayFrames   = (delaySeconds * Timeline.SampleRate).toLong
      val sched         = boids.scheduler
      val absFrames     = sched.time + delayFrames
      val tokenRef      = Ref(-1)
      val token         = sched.schedule(absFrames) { implicit tx =>
        implicit val itx = tx.peer
        scheduled -= tokenRef()
        clearSched()  // don't pick up yet

        infra.channels.foreach { ch =>
          releaseChan(ch, dur = math.random.linlin(0, 1, 4.0, 8.0))
        }

        // now wait at least eight seconds so all is calm
        swapLayer()
      }
      tokenRef() = token
      scheduled += token
    }

    private def swapLayer()(implicit tx: S#Tx): Unit = {
      implicit val itx = tx.peer
      val lyr0 = util.Random.nextInt(QuadGraphDB.numLayers - 1)
      val old  = layer
      val lyr  = if (lyr0 < old) lyr0 else lyr0 + 1 // do not repeat previous layer
      layer = lyr
      fire(Update(newLayer = lyr))
      // println(s"NEW LAYER = $lyr")

      // XXX TODO - DRY
      import numbers.Implicits._
      val delaySeconds  = math.random.linlin(0, 1, 12.0, 18.0)  // >= 8
      val delayFrames   = (delaySeconds * Timeline.SampleRate).toLong
      val sched         = boids.scheduler
      val absFrames     = sched.time + delayFrames
      val tokenRef      = Ref(-1)
      val token         = sched.schedule(absFrames) { implicit tx =>
        implicit val itx = tx.peer
        scheduled -= tokenRef()
        start()
      }
      tokenRef() = token
      scheduled += token
    }

    def layer(implicit tx: S#Tx): Int = layerRef.get(tx.peer)

    def layer_=(value: Int)(implicit tx: S#Tx): Unit = layerRef.set(value)(tx.peer)

    private def stepChan(ch: Infra.Channel)(implicit tx: S#Tx): Unit = {
      implicit val itx = tx.peer
      val lyr           = layerRef()
      val q             = quad.handles(lyr).apply()
      val loc           = boids.state.apply(ch.index).location
      val pt            = IntPoint2D((loc.x + 0.5f).toInt, (loc.y + 0.5f).toInt)
      q.nearestNeighborOption(pt, IntDistanceMeasure2D.euclideanSq).foreach { pn =>
        val node  = pn.node
        val graph = node.input.graph
        val syn = Synth.playOnce(graph)(target = ch.group, addAction = addToHead)
        syn.write(ch.bus -> "out")
//        val stamp = System.currentTimeMillis()
//        val entry = (stamp, pn)
//        playingNodes += entry
//        syn.onEndTxn { implicit tx =>
//          // syn.definition.dispose()  // XXX TODO - safe to do this here?
//          implicit val itx = tx.peer
//          playingNodes -= entry
//          // println(s"playingNodes.size = ${playingNodes.size}")
//        }
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

    private def releaseChan(ch: Infra.Channel, dur: Double = 4.0)(implicit tx: S#Tx): Unit =
      ch.group.release(dur)

    def stop()(implicit tx: S#Tx): Unit = {
      infra.channels.foreach(_.group.freeAll())
      clearSched()
    }

    private def clearSched()(implicit tx: S#Tx): Unit = {
      implicit val itx = tx.peer
      import boids.scheduler
      scheduled.foreach(scheduler.cancel)
      scheduled.clear()
    }

    def dispose()(implicit tx: S#Tx): Unit = stop()

    def debugPrint()(implicit tx: TxnLike): Unit = {
      implicit val itx = tx.peer
      println("------- LAST ACTIVE NODES -------")
      playingNodes.toIndexedSeq.sortBy(_._1).takeRight(4).foreach { case (stamp, pn) =>
        println(new Date(stamp).toString)
        println(pn)
        val ugens = pn.node.input.graph.sources.collect {
          case p: Product => p.productPrefix
        } .toSet.toIndexedSeq.sorted
        println(ugens.mkString("[", ", ", "]"))
      }
    }
  }

  case class Update(newLayer: Int)
}
trait AuralBoids[S <: Sys[S]] extends Disposable[S#Tx] with Observable[S#Tx, AuralBoids.Update] {
  def start()(implicit tx: S#Tx): Unit
  def stop ()(implicit tx: S#Tx): Unit

  def debugPrint()(implicit tx: TxnLike): Unit

  def layer(implicit tx: S#Tx): Int
  def layer_=(value: Int)(implicit tx: S#Tx): Unit
}