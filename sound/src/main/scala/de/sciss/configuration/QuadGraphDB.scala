/*
 *  QuadGraphDB.scala
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

import de.sciss.file._
import de.sciss.lucre.data.DeterministicSkipOctree
import de.sciss.lucre.geom.IntSpace.TwoDim
import de.sciss.lucre.geom.{IntPoint2D, IntSpace, IntSquare}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Disposable
import de.sciss.lucre.stm.store.BerkeleyDB
import de.sciss.lucre.synth.Sys
import de.sciss.processor.Processor
import de.sciss.serial.{Serializer, DataInput, DataOutput, ImmutableSerializer}
import de.sciss.synth.SynthGraph
import de.sciss.synth.proc.{Durable, SynthGraphs}

import scala.concurrent.{ExecutionContext, blocking}

object QuadGraphDB {
  type Tpe[S <: Sys[S]] = DeterministicSkipOctree[S, Dim, PlacedNode]
  type Dim              = IntSpace.TwoDim

  private val singleBaseDirectory = userHome / "Documents" / "devel" / "MutagenTx" / "database"

  final val extent  = 256

  final val layerNames = Vec(
    "betanovuss0",          // 0
    "betanovuss1",          // 1
    "betanovuss2",          // 2
    "betanovussSchrauben",  // 3
    "betanovussStairs",     // 4
    "zaubes2"               // 5
  )

  final val numLayers = layerNames.size

  private def pointView[S <: Sys[S]]: (PlacedNode, S#Tx) => IntPoint2D = (n, tx) => n.coord

  private implicit def treeSerializer[S <: Sys[S]]: Serializer[S#Tx, S#Acc, DeterministicSkipOctree[S, Dim, PlacedNode]] = {
    implicit val placedNodeSer = PlacedNode.serializer // why is it not found found, Scala!?
    implicit val pv = pointView[S]
    DeterministicSkipOctree.serializer[S, Dim, PlacedNode]
  }

  private def openSingle(name: String): SingleDB = {
    val somDir = singleBaseDirectory / s"${name}_som"
    implicit val dur = Durable(BerkeleyDB.factory(somDir))
    val quadH = dur.root { implicit tx =>
      emptyTree[D]
    }

    new SingleDB(dur, quadH)
  }

  private def emptyTree[S <: Sys[S]](implicit tx: S#Tx): Tpe[S] = {
    implicit val pv = pointView[S]
    DeterministicSkipOctree.empty[S, Dim, PlacedNode](IntSquare(extent, extent, extent))
  }

  private final class SingleDB(val system: D, handle: stm.Source[D#Tx, Tpe[D]]) {
    def getAll: Vec[PlacedNode] = system.step { implicit tx =>
      handle().iterator.toIndexedSeq
    }
  }

  private val baseDirectory = file("database")
  private val dbFile        = baseDirectory / "quad"

  def make(): Processor[Unit] = {
    import ExecutionContext.Implicits.global
    if (dbFile.exists()) {
      println(s"QuadGraphDB - database file '$dbFile' already exists. Not overwriting.")
      return Processor[Unit]("no-op")(_ => ())
    }

    implicit val dur = Durable(BerkeleyDB.factory(dbFile))
    val vecH = dur.root { implicit tx =>
      Vec.fill(numLayers)(emptyTree[D])
    }
    val res = Processor[Unit]("make-quad") { self =>
      layerNames.zipWithIndex.foreach { case (name, idx) =>
        blocking {
          val nodes = openSingle(name).getAll
          dur.step { implicit tx =>
            val tree = vecH().apply(idx)
            nodes.foreach(tree.add)
          }
        }
        self.progress = (idx + 1).toDouble / numLayers
        self.checkAborted()
      }
    }

    res.monitor()
    res
  }

  def open(): QuadGraphDB[D] = {
    implicit val dur = Durable(BerkeleyDB.factory(dbFile))
    val vecH = dur.root { implicit tx =>
      sys.error("should exists") : Vec[Tpe[D]]
    }
    new QuadGraphDB[D] {
      val system: D             = dur
      val cursor: stm.Cursor[D] = dur

      val handles: Vec[stm.Source[D#Tx, Tpe[D]]] = dur.step { implicit tx =>
        vecH().map(tx.newHandle(_))
      }
    }
  }

  ////////

  object Input {
    implicit object serializer extends ImmutableSerializer[Input] {
      private final val COOKIE = 0x496E7000 // "Inp\0"

      def read(in: DataInput): Input = {
        val cookie  = in.readInt()
        if (cookie != COOKIE) throw new IllegalStateException(s"Expected cookie ${COOKIE.toHexString} but found ${cookie.toHexString}")
        val graph   = SynthGraphs.ValueSerializer.read(in)
        val iter    = in.readShort()
        val fitness = in.readFloat()
        Input(graph, iter = iter, fitness = fitness)
      }

      def write(input: Input, out: DataOutput): Unit = {
        out.writeInt(COOKIE)
        SynthGraphs.ValueSerializer.write(input.graph, out)
        out.writeShort(input.iter)
        out.writeFloat(input.fitness)
      }
    }
  }
  case class Input(graph: SynthGraph, iter: Int, fitness: Float) {
    override def toString = {
      val fitS  = f"$fitness%1.3f"
      s"Input(graph size = ${graph.sources.size}, iter = $iter, fitness = $fitS)"
    }
  }

  object Weight {
    implicit object serializer extends ImmutableSerializer[Weight] {
      private final val COOKIE = 0x57656900 // "Wei\0"

      private def readArray(in: DataInput): Array[Double] = {
        val sz = in.readShort()
        val a  = new Array[Double](sz)
        var i = 0
        while (i < sz) {
          a(i) = in.readDouble()
          i += 1
        }
        a
      }

      def read(in: DataInput): Weight = {
        val cookie = in.readInt()
        if (cookie != COOKIE) throw new IllegalStateException(s"Expected cookie ${COOKIE.toHexString} but found ${cookie.toHexString}")
        val spectral = readArray(in)
        val temporal = readArray(in)
        new Weight(spectral = spectral, temporal = temporal)
      }

      private def writeArray(a: Array[Double], out: DataOutput): Unit = {
        out.writeShort(a.length)
        var i = 0
        while (i < a.length) {
          out.writeDouble(a(i))
          i += 1
        }
      }

      def write(w: Weight, out: DataOutput): Unit = {
        out.writeInt(COOKIE)
        writeArray(w.spectral, out)
        writeArray(w.temporal, out)
      }
    }
  }
  class Weight(val spectral: Array[Double], val temporal: Array[Double]) {
    override def toString = spectral.map(d => f"$d%1.3f").mkString("[", ", ", "]")
  }

  object Node {
    implicit object serializer extends ImmutableSerializer[Node] {
      private final val COOKIE = 0x4E6F6400 // "Nod\0"

      def read(in: DataInput): Node = {
        val cookie  = in.readInt()
        if (cookie != COOKIE) throw new IllegalStateException(s"Expected cookie ${COOKIE.toHexString} but found ${cookie.toHexString}")
        val input   = Input.serializer.read(in)
        val weight  = Weight.serializer.read(in)
        Node(input = input, weight = weight)
      }

      def write(node: Node, out: DataOutput): Unit = {
        out.writeInt(COOKIE)
        Input.serializer.write(node.input, out)
        Weight.serializer.write(node.weight, out)
      }
    }
  }
  case class Node(input: Input, weight: Weight)

  object PlacedNode {
    implicit object serializer extends ImmutableSerializer[PlacedNode] {
      private final val COOKIE = 0x506C6100 // "Pla\0"

      def write(pn: PlacedNode, out: DataOutput): Unit = {
        out.writeInt(COOKIE)
        IntPoint2D.serializer.write(pn.coord, out)
        Node      .serializer.write(pn.node , out)
      }

      def read(in: DataInput): PlacedNode = {
        val cookie = in.readInt()
        if (cookie != COOKIE) sys.error(s"Unexpected cookie, found ${cookie.toHexString} expected ${COOKIE.toHexString}")
        val coord = IntPoint2D.serializer.read(in)
        val node  = Node      .serializer.read(in)
        new PlacedNode(coord, node)
      }
    }
  }
  case class PlacedNode(coord: IntPoint2D, node: Node)
}
trait QuadGraphDB[S <: Sys[S]] extends Disposable[S#Tx] {
  implicit val system: S
  implicit val cursor: stm.Cursor[S]

  def handles: Vec[stm.Source[S#Tx, QuadGraphDB.Tpe[S]]]

  def dispose()(implicit tx: S#Tx): Unit = tx.afterCommit(system.close())
}