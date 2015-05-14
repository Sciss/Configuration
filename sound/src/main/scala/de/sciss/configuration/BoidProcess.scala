/*
 *  BoidProcess.scala
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

import de.sciss.lucre.event.Observable
import de.sciss.lucre.event.impl.ObservableImpl
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Disposable
import de.sciss.lucre.synth.Sys
import de.sciss.synth.proc.{Timeline, Scheduler}

import scala.concurrent.stm.Ref

object BoidProcess {
  val DEFAULT_PERIOD = 0.5

  type State = Vec[Boid]

  def apply[S <: Sys[S]](implicit tx: S#Tx, cursor: stm.Cursor[S]): BoidProcess[S] = {
    val state0 = Vec.fill(Configuration.numTransducers) {
      val x0      = util.Random.nextFloat * Boid.width
      val y0      = util.Random.nextFloat * Boid.height
      val angle0  = util.Random.nextFloat * math.Pi * 2
      Boid(x0, y0, angle0)
    }
    new Impl(state0, Scheduler[S])
  }

  private final class Impl[S <: Sys[S]](state0: State, sched: Scheduler[S]) 
    extends BoidProcess[S] with ObservableImpl[S, State] {
    
    private val stateRef      = Ref(state0)
    private val tokenRef      = Ref(-1)
    private val periodRef     = Ref(DEFAULT_PERIOD)
    // private val startFrameRef = Ref(0L)

    def cursor: stm.Cursor[S] = sched.cursor

    def state(implicit tx: S#Tx): State = stateRef.get(tx.peer)

    def step()(implicit tx: S#Tx): Unit = {
      implicit val itx = tx.peer
      val stateNew = stateRef.transformAndGet { boids =>
        boids.map(_.run(boids))
      }
      fire(stateNew)
    }

    def start()(implicit tx: S#Tx): Unit = {
      stop()
      sched1()
    }
    
    private def sched1()(implicit tx: S#Tx): Unit = {
      val nowFrame    = sched.time  // we don't care about jitter and cumulative drift
      val numFrames   = (periodRef.get(tx.peer) * Timeline.SampleRate).toLong
      val execFrame   = nowFrame + numFrames
      val token       = sched.schedule(execFrame) { implicit tx: S#Tx =>
        step()
        if (isRunning) sched1()
      }
      tokenRef.set(token)(tx.peer)
    }

    def stop ()(implicit tx: S#Tx): Unit = {
      val token = tokenRef.swap(-1)(tx.peer)
      if (token >= 0) sched.cancel(token)
    }

    def isRunning(implicit tx: S#Tx): Boolean = tokenRef.get(tx.peer) >= 0
    
    def period(implicit tx: S#Tx): Double = periodRef.get(tx.peer)

    def period_=(value: Double)(implicit tx: S#Tx): Unit = periodRef.set(value)(tx.peer)

    def dispose()(implicit tx: S#Tx): Unit = stop()
  }
}
trait BoidProcess[S <: Sys[S]] extends Observable[S#Tx, BoidProcess.State] with Disposable[S#Tx] {
  implicit def cursor: stm.Cursor[S]

  def state(implicit tx: S#Tx): BoidProcess.State

  def step()(implicit tx: S#Tx): Unit

  def start()(implicit tx: S#Tx): Unit
  def stop ()(implicit tx: S#Tx): Unit
  def isRunning(implicit tx: S#Tx): Boolean

  /** Iterations per second. */
  def period(implicit tx: S#Tx): Double
  def period_=(value: Double)(implicit tx: S#Tx): Unit
}