/*
 *  TextLike.scala
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

package de.sciss.configuration.video.text

trait TextLike {
  def text: String
  def anim: Anim

  /** Seconds */
  def tail: Int
}
