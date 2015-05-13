package de.sciss.configuration
package video

import java.text.SimpleDateFormat
import java.util.{Date, Locale}

import de.sciss.configuration.video.text.VideoSettings
import de.sciss.file._
import de.sciss.kollflitz.Vec
import de.sciss.processor.Processor
import prefuse.util.ui.JForcePanel

import scala.collection.breakOut
import scala.swing.Swing._
import scala.swing.event.ButtonClicked
import scala.swing.{ScrollPane, BoxPanel, BorderPanel, Button, Component, FlowPanel, Frame, Orientation, ProgressBar, SplitPane, SwingApplication, ToggleButton}
import scala.util.{Failure, Success}

object Text extends SwingApplication {
  def startup(args: Array[String]): Unit = {
    val v = text.Visual()

    val textObj = args.headOption.getOrElse("text1") match {
      case "text1"  => text.Text1
      case "text2"  => text.Text2
    }

    val ggHead = Button("Head") {
      // v.initChromosome(100)
      v.text = textObj.text
    }

    val ggAutoZoom = new ToggleButton("Zoom") {
      selected = true
      listenTo(this)
      reactions += {
        case ButtonClicked(_) =>
          v.autoZoom = selected
      }
    }

    val ggRunAnim = new ToggleButton("Run Animation") {
      listenTo(this)
      reactions += {
        case ButtonClicked(_) =>
          v.runAnimation = selected
      }
    }

    val ggStepAnim = Button("Step Anim") {
      v.animationStep()
    }

    val ggSaveFrame = Button("Save Frame") {
      val fmt   = new SimpleDateFormat("'betanovuss_'yyMMdd'_'HHmmss'.png'", Locale.US)
      val name  = fmt.format(new Date)
      val f     = userHome / "Pictures" / name
      val vs    = VideoSettings()
      v.saveFrameAsPNG(f, width = vs.width, height = vs.height)
    }

    val ggParamSnap = Button("Parameter Snapshot") {
      val vec: Vec[(String, String)] = v.forceParameters.map { case (name, values) =>
        val pVec: Vec[(String, String)] = values.map { case (pName, pVal) =>
          (pName, s"${pVal}f")
        } (breakOut)
        val s = pVec.sorted.map { case (pName, pVal) => s""""$pName" -> $pVal""" } .mkString("Map(", ", ", ")")
        (name, s)
      } (breakOut)

      println(s"Layout count: ${v.layoutCounter}\n")
      val tx = vec.sorted.map { case (name, values) => s""""$name" -> $values""" } .mkString("Map(\n  ", ",\n  ", "\n)")
      println(tx)
    }

    var seriesProc = Option.empty[Processor[Unit]]

    val ggProgress = new ProgressBar

    val ggSaveFrameSeries = Button("Save Movie...") {
      seriesProc.fold[Unit] {
        val dir       = file("render")
        require(dir.isDirectory)
        val cfg       = VideoSettings()
        cfg.baseFile  = dir / "frame"
        cfg.anim      = textObj.anim
        cfg.text      = textObj.text
        cfg.numFrames = cfg.anim.last._1 + cfg.framesPerSecond * textObj.tail // 120
        cfg.baseFile

        val p         = v.saveFrameSeriesAsPNG(cfg)
        seriesProc    = Some(p)
        p.addListener {
          case prog @ Processor.Progress(_, _) => onEDT(ggProgress.value = prog.toInt)
          case Processor.Result(_, Success(_)) => println("Done.")
          case Processor.Result(_, Failure(ex)) =>
            println("Move rendering failed.")
            ex.printStackTrace()
        }

      } { p =>
        p.abort()
        seriesProc = None
      }
    }

    val pBottom = new BoxPanel(Orientation.Vertical) {
      contents += new FlowPanel(ggHead     , ggAutoZoom       , ggRunAnim  , ggStepAnim)
      contents += new FlowPanel(ggSaveFrame, ggSaveFrameSeries, ggParamSnap, ggProgress)
    }
    val fSim    = v.forceSimulator
    val fPanel  = new JForcePanel(fSim)
    fPanel.setBackground(null)
    val scroll = new ScrollPane(v.component)
    val split = new SplitPane(Orientation.Vertical, scroll, Component.wrap(fPanel))
    split.oneTouchExpandable  = true
    split.continuousLayout    = false
    split.dividerLocation     = 800
    split.resizeWeight        = 1.0

    new Frame {
      title     = "Text"
      contents  = new BorderPanel {
        add(split   , BorderPanel.Position.Center)
        add(pBottom , BorderPanel.Position.South)
      }
      pack()
      // size      = (640, 480)

      // v.display.panTo((-136 + 20, -470 + 20))   // XXX WAT -- where the heck do these values come from?
//      v.display.panTo((-100, 100))
//      v.display.zoomAbs((0, 0), 1.3333)

      open()

      override def closeOperation(): Unit = {
        try {
          // v.algorithm.system.close()
        } finally {
          sys.exit(0)
        }
      }
    }
  }
}
