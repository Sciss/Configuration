/*
 *  Configuration.scala
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

import scala.concurrent.ExecutionContext

object Configuration {
  def main(args: Array[String]): Unit = {
    if (args.headOption == Some("--make")) {
      val proc = QuadGraphDB.make()
      idleThread()
      import ExecutionContext.Implicits.global
      proc.onComplete(_ => sys.exit())
    } else {
      run()
    }
  }

  def run(): Unit = {
    val quad = QuadGraphDB.open()
    import quad.cursor
    cursor.step { implicit tx =>
      val boids = BoidProcess[D]
      boids.period = 0.01 // 5
      ControlView(boids, quad)
    }
  }

  def boot(): Unit = {

  }

  val numTransducers = 9
}
//trait Configuration {
//  def mainBus: AudioBus
//}
