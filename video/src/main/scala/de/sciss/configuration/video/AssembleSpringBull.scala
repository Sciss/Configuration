/*
 *  AssembleSpringBull.scala
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

import de.sciss.file._

object AssembleSpringBull extends AssembleLike with App {
  def BLACK_FRONT = 15.0
  def BLACK_MID   = 30.0
  def BLACK_BACK  = 15.0

  lazy val outDir = file("assembly_springbull")

  run()

  def run(): Unit = {
    testExistsOrMake()
    putBlack(BLACK_FRONT)
    putMovie(file("renderSpringMusic"))
    putBlack(BLACK_MID)
    putMovie(file("renderBullsEye"))
    putBlack(BLACK_BACK)
    finalizeH264()
  }
}
