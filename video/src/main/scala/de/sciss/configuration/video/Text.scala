/*
 *  Text.scala
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
package video

import java.text.SimpleDateFormat
import java.util.{Date, Locale}

import de.sciss.configuration.video.text.{Text2, Text1, TextLike, VideoSettings}
import de.sciss.file._
import de.sciss.kollflitz.Vec
import de.sciss.processor.Processor
import prefuse.util.ui.JForcePanel

import scala.collection.breakOut
import scala.swing.Swing._
import scala.swing.event.ButtonClicked
import scala.swing.{BorderPanel, BoxPanel, Button, Component, FlowPanel, Frame, Orientation, ProgressBar, ScrollPane, SplitPane, Swing, ToggleButton}
import scala.util.{Failure, Success}

object Text  {
  case class Config(text: TextLike = Text1, format: VideoSettings.Format = VideoSettings.Format.PNG,
                    dpi: Double = 72.0)

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Config]("Configuration - Text") {
      opt[Int]('t', "text").validate { x => if (x == 1 || x == 2) success else failure("--text must be 1 or 2") }
        .action { (x, c) =>
          val t = x match {
            case 1 => Text1
            case 2 => Text2
          }
          c.copy(text = t)
        }.text("text selection (1 or 2)")

      opt[String]('f', "format").validate { x => if (x == "png" || x == "pdf") success else failure ("--format must be png or pdf") }
        .action { (x, c) =>
          val f = VideoSettings.Format(x)
          c.copy(format = f)
        }
    }
    parser.parse(args, Config()).fold(sys.exit(1)) { config =>
      Swing.onEDT(startup(config))
    }
  }

  def startup(config: Config): Unit = {
    val v = text.Visual()

    val textObj = config.text

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
      val fmt   = new SimpleDateFormat(s"'betanovuss_'yyMMdd'_'HHmmss'.${config.format.ext}'", Locale.US)
      val name  = fmt.format(new Date)
      val f     = userHome / "Pictures" / name
      val vs    = VideoSettings()
      v.saveFrame(f, width = vs.width, height = vs.height, format = config.format, dpi = config.dpi)
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
        cfg.format    = config.format
        cfg.dpi       = config.dpi
        cfg.baseFile

        val p         = v.saveFrameSeries(cfg)
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