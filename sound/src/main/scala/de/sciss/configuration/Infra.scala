/*
 *  Infra.scala
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

import de.sciss.lucre.synth.{Group, AudioBus, Server}

object Infra {
  class Channel(val index: Int, val group: Group, val bus: AudioBus)
}
class Infra(val server: Server, val channels: Vec[Infra.Channel], val normGroup: Group,
            val masterGroup: Group)