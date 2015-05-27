/*
 *  AudioBusMeterImpl.scala
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
package impl

import de.sciss.audiowidgets.PeakMeter
import de.sciss.configuration.AudioBusMeter.Strip
import de.sciss.lucre.swing.deferTx
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.synth.{Txn, Synth, Sys}
import de.sciss.osc.Message
import de.sciss.synth
import de.sciss.synth.{SynthGraph, message}

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.stm.Ref
import scala.swing.{BoxPanel, Orientation, Swing}

object AudioBusMeterImpl {
  def apply[S <: Sys[S]](implicit tx: S#Tx): AudioBusMeter[S] = {
    new Impl[S].init()
  }

  private final class Impl[S <: Sys[S]]
    extends AudioBusMeter[S] with ComponentHolder[BoxPanel] {

    private val synths  = Ref(List.empty[Synth])
    private val resps   = Ref(List.empty[message.Responder])
    private var meters  = Vec .empty[PeakMeter]

    def dispose()(implicit tx: S#Tx): Unit = freeAll()

    private def freeAll()(implicit tx: Txn): Unit = {
      implicit val itx = tx.peer
      resps .swap(Nil).foreach(_.remove ())
      synths.swap(Nil).foreach(_.dispose())

      deferTx {
        component.contents.clear()
        meters.foreach(_.dispose())
        meters = Vector.empty
      }
    }

    def init()(implicit tx: S#Tx): this.type = {
      deferTx(guiInit())
      this
    }

    private val stripsRef = Ref(Vec.empty[Strip])

    def strips(implicit tx: Txn): Vec[Strip] = stripsRef.get(tx.peer)

    def strips_=(xs: Vec[Strip])(implicit tx: Txn): Unit = {
      implicit val itx = tx.peer
      freeAll()
      stripsRef() = xs

      // group to send out bundles per server
      strips.groupBy(_.bus.server).foreach { case (server, stripsByServer) =>
        // group to send out synth defs per channel num
        stripsByServer.groupBy(_.bus.numChannels).foreach { case (numChannels, stripsByChannels) =>
          val graph = SynthGraph {
            import synth._
            import ugen._
            val sig     = In.ar("bus".ir, numChannels)
            val tr      = Impulse.kr(20)
            val peak    = Peak.kr(sig, tr)
            val rms     = A2K.kr(Lag.ar(sig.squared, 0.1))
            val values  = Flatten(Zip(peak, rms))
            // CheckBadValues.kr(values)
            SendReply.kr(tr, values, "/$meter")
          }

          stripsByChannels.foreach { strip =>
            import strip._

            val syn = Synth.play(graph, nameHint = Some("meter"))(target = target, addAction = addAction)
            syn.read(bus -> "bus")

            lazy val meter = new PeakMeter
            deferTx {
              meter.numChannels   = numChannels
              meter.caption       = true
              meter.borderVisible = true
              meters :+= meter
              component.contents += meter
            }

            val SynID = syn.peer.id
            val resp  = message.Responder(syn.server.peer) {
              case Message("/$meter", SynID, _, vals @ _*) =>
                val pairs = vals.asInstanceOf[Seq[Float]].toIndexedSeq
                val time  = System.currentTimeMillis()
                Swing.onEDT(meter.update(pairs, 0, time))
            }
            resps .transform(resp :: _)
            synths.transform(syn  :: _)

            tx.afterCommit(resp.add())
          }
        }
      }
    }

    private def guiInit(): Unit =
      component = new BoxPanel(Orientation.Horizontal)
  }
}