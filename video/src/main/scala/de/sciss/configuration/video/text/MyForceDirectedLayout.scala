package de.sciss.configuration.video.text

import prefuse.action.layout.graph.ForceDirectedLayout
import prefuse.visual.EdgeItem

class MyForceDirectedLayout(main: Visual)
  extends ForceDirectedLayout(Visual.GROUP_GRAPH) {

  override def getSpringLength(e: EdgeItem): Float = {
    val nSrc = e.getSourceItem
    val nDst = e.getTargetItem

    (nSrc.get(Visual.COL_MUTA), nDst.get(Visual.COL_MUTA)) match {
      case (vSrc: VisualVertex, vDst: VisualVertex) =>
        if (vSrc.lineRef eq vDst.lineRef) {
          val res = vSrc.advance
          // println(s"ADVANCE = $res")
          res
        } else -1
      case _ =>
        // println("Oh noes!")
        -1f
    }
  }

  // this is used to mark horizontal springs (using coefficient zero)
  override def getSpringCoefficient(e: EdgeItem): Float = {
    val nSrc = e.getSourceItem
    val nDst = e.getTargetItem

    (nSrc.get(Visual.COL_MUTA), nDst.get(Visual.COL_MUTA)) match {
      case (vSrc: VisualVertex, vDst: VisualVertex) =>
        if (vSrc.wordRef eq vDst.wordRef) 0f else
        if (vSrc.lineRef eq vDst.lineRef) 1f else 2f
      case _ =>
        // println("Oh noes!")
        -1f
    }
  }

  private var _counter = 0

  override def run(frac: Double): Unit = {
    _counter += 1
    super.run(frac)
  }

  def counter: Int = _counter
}
