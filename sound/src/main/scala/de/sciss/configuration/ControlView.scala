/*
 *  View.scala
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

import java.awt.Color

import de.sciss.audiowidgets.Transport
import de.sciss.lucre.data.gui.{QuadView, SkipQuadtreeView}
import de.sciss.lucre.geom.{IntDistanceMeasure2D, IntPoint2D}
import de.sciss.lucre.stm
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.swing.{View, deferTx}
import de.sciss.lucre.synth.Sys
import de.sciss.numbers
import de.sciss.swingplus.OverlayPanel
import de.sciss.synth.impl.DefaultUGenGraphBuilderFactory
import de.sciss.synth.swing.ServerStatusPanel
import de.sciss.synth.{Server, ServerConnection, Synth, SynthDef}

import scala.swing.event.{ButtonClicked, MousePressed}
import scala.swing.{Graphics2D, ToggleButton, Swing, Label, BorderPanel, Button, Component, FlowPanel, Frame}
import scala.util.Try
import Swing._

object ControlView {
  import QuadGraphDB.{PlacedNode, Tpe, extent}
  
  def apply[S <: Sys[S]](boids: BoidProcess[S], quad: QuadGraphDB[S])(implicit tx: S#Tx): ControlView[S] = {
    val res = new Impl(boids, quad).init()
    deferTx {
      new Frame {
        title     = "Configuration"
        contents  = res.component
        pack().centerOnScreen()
        open()

        override def closeOperation(): Unit = {
          quad.cursor.step { implicit tx =>
            res   .dispose()
            boids .dispose()
            quad  .dispose()
          }
          sys.exit()
        }
      }
    }
    res
  }

  private final class Impl[S <: Sys[S]](boids: BoidProcess[S], quad: QuadGraphDB[S])
    extends ControlView[S] with ComponentHolder[Component] {

    import quad.cursor

    private[this] var boidsState: BoidProcess.State = _
    private[this] var quadView  : SkipQuadtreeView[S, PlacedNode] = _
    private[this] var boidsComp : Component = _

    def init()(implicit tx: S#Tx): this.type = {
      val quadH = quad.handles.head // XXX TODO
      boidsState = boids.state
      deferTx(guiInit(quadH))
      boids.react { implicit tx => state =>
        boidsState = state
        deferTx(boidsComp.repaint())
      }
      this
    }

    def dispose()(implicit tx: S#Tx): Unit = ()

    private def startBoids(): Unit = cursor.step { implicit tx => boids.start() }
    private def stopBoids (): Unit = cursor.step { implicit tx => boids.stop () }

    private def guiInit(quadH: stm.Source[S#Tx, Tpe[S]]): Unit = {

      var synthOpt = Option.empty[Synth]

      import de.sciss.synth.Ops._

      quadView  = new SkipQuadtreeView[S, PlacedNode](quadH, cursor, _.coord)
      quadView.setBorder(Swing.EmptyBorder(Boid.excess, Boid.excess, Boid.excess, Boid.excess))

      def stopSynth(): Unit = synthOpt.foreach { synth =>
        synthOpt = None
        if (synth.server.isRunning) synth.release(3.0) // free()
      }

      def playSynth(): Unit = {
        stopSynth()
        for {
          s    <- Try(Server.default).toOption
          node <- quadView.highlight.headOption
        } {
          val graph = node.node.input.graph // node.chromosome.graph
          val df    = SynthDef("test", graph.expand(DefaultUGenGraphBuilderFactory))
          val x     = df.play(s, args = Seq("out" -> 1))
          synthOpt = Some(x)
        }
      }

      val pStatus = new ServerStatusPanel
      def boot(): Unit = {
        val cfg = Server.Config()
        cfg.memorySize = 256 * 1024
        cfg.pickPort()
        val connect = Server.boot(config = cfg) {
          case ServerConnection.Running(s) =>
          case ServerConnection.Aborted    =>
        }
        pStatus.booting = Some(connect)
      }

      val butKill = Button("Kill") {
        import scala.sys.process._
        Try(Server.default).toOption.foreach(_.dispose())
        "killall scsynth".!
      }

      pStatus.bootAction = Some(boot)
      val boidsTransport = Transport.makeButtonStrip(Seq(Transport.Stop(stopBoids()), Transport.Play(startBoids())))
      val soundTransport = Transport.makeButtonStrip(Seq(Transport.Stop(stopSynth()), Transport.Play(playSynth())))

      // quadView.scale = 240.0 / extent
      val quadComp  = Component.wrap(quadView)
      quadComp.visible = false

      val ggQuadVis = new ToggleButton("Quad Vis") {
        listenTo(this)
        reactions += {
          case ButtonClicked(_) =>
            quadComp.visible = selected
        }
      }

      val ggPrintBoids = Button("Print") {
        boidsState.zipWithIndex.foreach { case (boid, id) =>
          println(s"$id: $boid")
        }
      }

      val tp = new FlowPanel(ggQuadVis, new Label("Boids:"), ggPrintBoids, boidsTransport, pStatus, butKill,
        new Label("Sound:"), soundTransport)

      boidsComp = new Component {
        preferredSize = (Boid.width, Boid.height)

        override protected def paintComponent(g: Graphics2D): Unit = {
          g.setColor(Color.lightGray)
          g.drawRect(Boid.excess, Boid.excess, Boid.side, Boid.side)
          boidsState.foreach(_.paint(g))
        }
      }

      val overlay = new OverlayPanel {
        preferredSize = (Boid.width, Boid.height)
        contents += boidsComp
        contents += quadComp
      }

      val mainPane = new BorderPanel {
        add(tp      , BorderPanel.Position.North )
        add(overlay , BorderPanel.Position.Center)
      }

      quadComp.listenTo(quadComp.mouse.clicks)
      val insets = quadView.getInsets
      quadComp.reactions += {
        case MousePressed(_, pt, mod, clicks, _) =>
          import numbers.Implicits._
          val x = ((pt.x - insets.left) / quadView.scale + 0.5).toInt.clip(0, extent << 1)
          val y = ((pt.y - insets.top ) / quadView.scale + 0.5).toInt.clip(0, extent << 1)

          val nodeOpt = cursor.step { implicit tx =>
            val q = quadH()
            q.nearestNeighborOption(IntPoint2D(x, y), IntDistanceMeasure2D.euclideanSq)
          }
          nodeOpt.foreach { node =>
            // println(node.node.input.graph)
            // --- the highlight doesn't seem to be working,
            // probably because SynthGraph is not equal to itself after de-serialization?
            quadView.highlight = Set(node)
            quadView.repaint()
            soundTransport.button(Transport.Play).foreach(_.doClick(100))
          }
      }

      //      def topPaint(h: QuadView.PaintHelper): Unit = {
      //        import h.g2
      //        //        g2.setColor(Color.green)
      //        //        g2.drawLine(0, 0, 100,   0)
      //        //        g2.drawLine(0, 0,   0, 100)
      //        // h.translate(-Boid.excess, -Boid.excess)
      //        boidsState.foreach(_.paint(g2))
      //      }

      // quadView.topPainter = Some(topPaint _)

      component = mainPane
    }
  }
}
trait ControlView[S <: Sys[S]] extends View[S] {

}