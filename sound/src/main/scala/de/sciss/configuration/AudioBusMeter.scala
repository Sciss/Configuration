/*
 *  AudioBusMeter.scala
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

import de.sciss.lucre.swing.View
import de.sciss.lucre.synth.{Txn, AudioBus, Group, Sys}
import de.sciss.synth.AddAction

import scala.swing.Component

object AudioBusMeter {
  /** Specification of a meter strip.
    *
    * @param bus        the audio bus to meter
    * @param target     the target point at which the meter synth will sit
    * @param addAction  the relation of the meter synth with respect to its target node.
    */
  final case class Strip(bus: AudioBus, target: Group, addAction: AddAction)

  /** Creates a new audio bus meter. */
  def apply[S <: Sys[S]](implicit tx: S#Tx): AudioBusMeter[S] = impl.AudioBusMeterImpl[S]
}
trait AudioBusMeter[S <: Sys[S]] extends View[S] {
  import AudioBusMeter.Strip

  /** The buses and targets to meter. */
  def strips(implicit tx: Txn): Vec[Strip]
  def strips_=(xs: Vec[Strip])(implicit tx: Txn): Unit

  /** The swing component showing the meter. */
  def component: Component
}