/*
 *  package.scala
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

package de.sciss.configuration.video

import de.sciss.kollflitz.Vec

package object text {
  type Anim = Vec[(Int, Map[String, Map[String, Float]])]
}
