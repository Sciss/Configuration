package de.sciss.configuration.video.text

import prefuse.data.{Node => PNode}
import prefuse.visual.VisualItem

import scala.swing.Graphics2D

//object VisualUGen {
//  def apply(main: Visual, v: Vertex.UGen)(implicit tx: S#Tx): VisualUGen = impl.VisualUGenImpl(main, v)
//}
//trait VisualUGen extends VisualVertex {
//  def info: UGenSpec
//}
//
//object VisualConstant {
//  def apply(main: Visual, v: Vertex.Constant)(implicit tx: S#Tx): VisualConstant = impl.VisualConstantImpl(main, v)
//}
//trait VisualConstant extends VisualVertex {
//  var value: Float
//}

object VisualVertex {
  def apply(main: Visual, character: Char): VisualVertex = new Impl(main, character)

  private final class Impl(val main: Visual, val character: Char)
    extends VisualVertex with VisualVertexImpl {

    protected def renderDetail(g: Graphics2D, vi: VisualItem): Unit = {
      drawLabel(g, vi, /* diam * vi.getSize.toFloat * 0.5f, */ name)
    }

    protected def boundsResized(): Unit = ()

    def name: String = character.toString

    private var _pNode: PNode = _

    def pNode: PNode = _pNode

    def init(): Unit = {
      _pNode = mkPNode()
    }

    def dispose(): Unit = {
      if (_pNode.isValid) main.graph.removeNode(_pNode)
    }

    init()
  }
}
sealed trait VisualVertex extends VisualNode {
  def character: Char

  def advance: Int

  def pNode: PNode
}