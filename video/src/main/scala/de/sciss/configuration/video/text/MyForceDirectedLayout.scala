package de.sciss.configuration.video.text

import prefuse.action.layout.graph.ForceDirectedLayout
import prefuse.visual.EdgeItem

class MyForceDirectedLayout(main: Visual) extends ForceDirectedLayout(Visual.GROUP_GRAPH) {
  override def getSpringLength(e: EdgeItem): Float = {
    val nSrc = e.getSourceItem
    val nDst = e.getTargetItem

    (nSrc.get(Visual.COL_MUTA), nDst.get(Visual.COL_MUTA)) match {
      case (vSrc: VisualVertex, vDst: VisualVertex) =>
        vSrc.advance
      case _ => -1
    }
  }
}
