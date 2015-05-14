/*
 *  ControlView.scala
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
import javax.swing.{JFrame, SwingUtilities, SpinnerNumberModel}

import de.sciss.audiowidgets.Transport
import de.sciss.lucre.data.gui.SkipQuadtreeView
import de.sciss.lucre.geom.{IntDistanceMeasure2D, IntPoint2D}
import de.sciss.lucre.stm
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.swing.{View, deferTx}
import de.sciss.lucre.synth.{Bus, Synth, Sys, Txn}
import de.sciss.swingplus.{OverlayPanel, Spinner}
import de.sciss.synth.swing.j.JServerStatusPanel
import de.sciss.synth.{addAfter, Server, SynthGraph, addToHead, addToTail}
import de.sciss.{numbers, synth}

import scala.concurrent.stm.Ref
import scala.swing.Swing._
import scala.swing.event.{ButtonClicked, MousePressed, ValueChanged}
import scala.swing.{BorderPanel, BoxPanel, Button, Component, FlowPanel, Frame, Graphics2D, Label, Orientation, Swing, ToggleButton}
import scala.util.Try

object ControlView {
  import QuadGraphDB.{PlacedNode, Tpe}
  
  def apply[S <: Sys[S]](boids: BoidProcess[S], quad: QuadGraphDB[S])(implicit tx: S#Tx): ControlView[S] = {
    val meterView = AudioBusMeter[S]
    val res = new Impl(boids, quad, meterView).init()
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

  private final class Impl[S <: Sys[S]](boids: BoidProcess[S], quad: QuadGraphDB[S], meterView: AudioBusMeter[S])
    extends ControlView[S] with ComponentHolder[Component] {

    def cursor: stm.Cursor[S] = quad.cursor

    private[this] var boidsState: BoidProcess.State = _
    private[this] var quadView  : SkipQuadtreeView[S, PlacedNode] = _
    private[this] var boidsComp : Component = _
    private[this] var pStatus   : JServerStatusPanel = _
    private[this] var hpSynth   = Option.empty[Synth]

    def init()(implicit tx: S#Tx): this.type = {
      val quadH = quad.handles.head // XXX TODO
      boidsState = boids.state
      val boidRate0 = boids.period
      deferTx(guiInit(quadH, boidRate0 = boidRate0))
      boids.react { implicit tx => state =>
        boidsState = state
        deferTx(boidsComp.repaint())
      }
      this
    }

    def dispose()(implicit tx: S#Tx): Unit = {
      meterView.dispose()
    }

    private def startBoids(): Unit = cursor.step { implicit tx => boids.start() }
    private def stopBoids (): Unit = cursor.step { implicit tx => boids.stop () }

    var synthsPlaying = List.empty[Synth]

    private def stopSynth(): Unit = if (synthsPlaying.nonEmpty) {
      cursor.step { implicit tx =>
        synthsPlaying.foreach { synth =>
          if (synth.server.peer.isRunning) synth.release(3.0) // free()
        }
      }
      synthsPlaying = Nil
    }

    private def playSynth(): Unit = {
      stopSynth()
      for {
        s    <- Try(Server.default).toOption
        node <- quadView.highlight.headOption
      } {
        val graph = node.node.input.graph // node.chromosome.graph
        // val df    = SynthDef("test", graph.expand(DefaultUGenGraphBuilderFactory))
        val x = cursor.step { implicit tx =>
            val syn = Synth.play(graph)(target = infra.channels.head.group, addAction = addToHead)
            syn.write(infra.channels.head.bus -> "out")
            syn
          }
        synthsPlaying = x :: Nil
      }
    }

    private def guiInit(quadH: stm.Source[S#Tx, Tpe[S]], boidRate0: Double): Unit = {

      quadView  = new SkipQuadtreeView[S, PlacedNode](quadH, cursor, _.coord)
      quadView.setBorder(Swing.EmptyBorder(Boid.excess, Boid.excess, Boid.excess, Boid.excess))

      pStatus = new JServerStatusPanel(JServerStatusPanel.COUNTS)
      //      def boot(): Unit = {
      //        val cfg = Server.Config()
      //        cfg.memorySize = 256 * 1024
      //        cfg.pickPort()
      //        val connect = Server.boot(config = cfg) {
      //          case ServerConnection.Running(s) =>
      //          case ServerConnection.Aborted    =>
      //        }
      //        pStatus.booting = Some(connect)
      //      }

      val butKill = Button("Kill") {
        import scala.sys.process._
        Try(Server.default).toOption.foreach(_.dispose())
        "killall scsynth".!
      }

      // pStatus.bootAction = Some(boot)
      // val boidsTransport = Transport.makeButtonStrip(Seq(Transport.Stop(stopBoids()), Transport.Play(startBoids())))
      val boidsTransport = new ToggleButton("Sim") {
        listenTo(this)
        reactions += {
          case ButtonClicked(_) =>
            if (selected) startBoids() else stopBoids()
        }
      }

      val soundTransport = Transport.makeButtonStrip(Seq(Transport.Stop(stopSynth()), Transport.Play(playSynth())))

      // quadView.scale = 240.0 / extent
      val quadComp  = Component.wrap(quadView)
      quadComp.visible = false

      val mBoidRate = new SpinnerNumberModel(boidRate0, 0.01, 1, 0.01)
      val ggBoidRate = new Spinner(mBoidRate) {
        listenTo(this)
        reactions += {
          case ValueChanged(_) =>
            val r = mBoidRate.getNumber.doubleValue()
            quad.cursor.step { implicit tx =>
              boids.period = r
            }
        }
      }

      val ggAuralBoids = new ToggleButton("Aural") {
        listenTo(this)
        reactions += {
          case ButtonClicked(_) =>
            val sel = selected
            quad.cursor.step { implicit tx =>
              implicit val itx = tx.peer
              auralBoidsOn() = sel
              auralBoidsRef().foreach { ab =>
                if (sel) ab.start() else ab.stop()
              }
            }
        }
      }

      val ggQuadVis = new ToggleButton("Quad Vis") {
        listenTo(this)
        reactions += {
          case ButtonClicked(_) =>
            quadComp.visible = selected
        }
      }

      val ggHP = new ToggleButton("Headphones") {
        listenTo(this)
        reactions += {
          case ButtonClicked(_) =>
            hpSynth.foreach { syn =>
              quad.cursor.step { implicit tx =>
                syn.release()
              }
              hpSynth = None
            }
            if (selected) {
              val graphHP = SynthGraph {
                import synth._
                import ugen._
                val in    = In.ar(0, Configuration.numTransducers)
                val pan   = SplayAz.ar(numChannels = 2, in = in)
                val limDur = 0.01
                val env    = new Env(0.0, Vec[Env.Segment](
                  (limDur, 0.0, Curve.linear),
                  (0.2, 1.0, Curve.sine), (0.2, 0.0, Curve.sine), (limDur, 0.0, Curve.linear)), 2)
                // val fade  = EnvGen.ar(Env.asr(0.2, 1, 0.2, Curve.linear), gate = "gate".kr(1f), doneAction = freeSelf)
                val fade = EnvGen.ar(env, gate = "gate".kr(1f), doneAction = freeSelf)
                XOut.ar(0, Limiter.ar(pan / Configuration.numTransducers, level = -0.2.dbamp, dur = limDur), fade)
              }
              val syn = quad.cursor.step { implicit tx =>
                Synth.play(graphHP, nameHint = Some("hp"))(target = infra.masterGroup, addAction = addAfter /* addToTail */)
              }
              hpSynth = Some(syn)
            }
        }
      }

      val ggPrintBoids = Button("Print") {
        boidsState.zipWithIndex.foreach { case (boid, id) =>
          println(s"$id: $boid")
        }
        cursor.step { implicit tx =>
          infraOption.foreach { infra =>
            infra.server.peer.dumpTree(controls = true)
          }
        }
      }

      val tp1 = new FlowPanel(ggQuadVis, new Label("Boids:"), ggAuralBoids, ggBoidRate, ggPrintBoids, boidsTransport)
      val tp2 = new FlowPanel(new Label("Server:"), ggHP, Component.wrap(pStatus), butKill, new Label("Sound:"),
        soundTransport)
      val tp = new BoxPanel(Orientation.Vertical) {
        contents += tp1
        contents += tp2
      }

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

      val pMeter = new BoxPanel(Orientation.Vertical) {
        contents += Swing.VGlue
        contents += meterView.component
        contents += Swing.VGlue
      }

      val mainPane = new BorderPanel {
        add(tp      , BorderPanel.Position.North )
        add(overlay , BorderPanel.Position.Center)
        add(pMeter  , BorderPanel.Position.East  )
      }

      val mouseComp = boidsComp // quadComp
      mouseComp.listenTo(mouseComp.mouse.clicks)
      // val insets = quadView.getInsets
      mouseComp.reactions += {
        case MousePressed(_, pt, mod, clicks, _) =>
          import numbers.Implicits._
          val x = (pt.x / quadView.scale + 0.5).toInt.clip(0, Boid.width )
          val y = (pt.y / quadView.scale + 0.5).toInt.clip(0, Boid.height)

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

      component = mainPane
    }

    private val infraRef      = Ref(Option.empty[Infra])
    private val auralBoidsRef = Ref(Option.empty[AuralBoids[S]])
    private val auralBoidsOn  = Ref(initialValue = false)

    private def infraOption(implicit tx: Txn): Option[Infra] = infraRef.get(tx.peer)

    def infra(implicit tx: S#Tx): Infra = infraRef.get(tx.peer).getOrElse(sys.error("infra was not set yet"))

    def infra_=(value: Infra)(implicit tx: S#Tx): Unit = {
      implicit val itx = tx.peer
      infraRef() = Some(value)
      val strip = AudioBusMeter.Strip(Bus.soundOut(value.server, Configuration.numTransducers), value.masterGroup, addToTail)
      meterView.strips = Vector(strip)
      val auralBoids = AuralBoids(infra, boids, quad)
      auralBoidsRef.swap(Some(auralBoids)).foreach(_.dispose())
      if (auralBoidsOn()) auralBoids.start()
      deferTx {
        pStatus.server = Some(value.server.peer)
        SwingUtilities.getWindowAncestor(component.peer) match {
          case jf: JFrame => jf.pack()
        }
      }
    }
  }
}
trait ControlView[S <: Sys[S]] extends View.Cursor[S] {
  def infra(implicit tx: S#Tx): Infra
  def infra_=(value: Infra)(implicit tx: S#Tx): Unit
}