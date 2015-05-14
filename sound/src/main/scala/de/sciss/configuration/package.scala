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

package de.sciss

import de.sciss.lucre.synth.InMemory
import de.sciss.synth.proc.Durable

package object configuration {
  type Vec[+A]  = scala.collection.immutable.IndexedSeq[A]
  val  Vec      = scala.collection.immutable.IndexedSeq

  // type I = InMemory
  type D = Durable

  def idleThread(): Unit = new Thread { override def run(): Unit = this.synchronized(this.wait()) } .start()
}
