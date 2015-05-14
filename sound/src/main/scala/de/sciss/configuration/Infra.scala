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

class Infra(val server: Server, val inGroup: Group, val inBuses: Vec[AudioBus], val normGroup: Group,
            val masterGroup: Group)