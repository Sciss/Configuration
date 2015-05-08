package de.sciss.configuration.video

import java.awt.Color
import java.awt.geom.AffineTransform
import java.awt.image.{AffineTransformOp, BufferedImage}
import javax.imageio.ImageIO

import com.jhlabs.image.{NoiseFilter, ThresholdFilter}
import de.sciss.file._
import de.sciss.numbers

import scala.concurrent.{ExecutionContext, Future}
import scala.swing.Swing._
import scala.swing.{Component, Graphics2D, MainFrame, SwingApplication}

object BullsEye extends SwingApplication {
  val IN_WIDTH    = 320   // d'oh
  val IN_HEIGHT   = 240

  val OUT_WIDTH   = 1080
  val OUT_HEIGHT  = 1920

  val FPS_IN      = 15
  val FPS_OUT     = 25
  val START_FRAME_IN = 13
  val NUM_FRAMES_IN  = 5573 - START_FRAME_IN
  val NUM_FRAMES_OUT = NUM_FRAMES_IN * FPS_OUT / FPS_IN

  val THRESH_LO   = 175
  val THRESH_HI   = 180

  val FADE_IN     = 180.0
  val FADE_OUT    = 180.0

  def startup(args: Array[String]): Unit = {
    val scale   = OUT_WIDTH.toDouble / IN_WIDTH

    val bW      = OUT_WIDTH
    val bH      = (IN_HEIGHT * scale).toInt
    val bScale  = new BufferedImage(bW, bH, BufferedImage.TYPE_INT_ARGB)

    val atScale = AffineTransform.getScaleInstance(scale, scale)
    val fScale  = new AffineTransformOp(atScale, AffineTransformOp.TYPE_BICUBIC)

    // val fEdge   = new EdgeFilter
    // fEdge.filter(bIn, bOut)
    val fThresh = new ThresholdFilter

    val fadeInFrames  = (FADE_IN  * FPS_OUT).toInt
    val fadeOutFrames = (FADE_OUT * FPS_OUT).toInt

    val fNoise = new NoiseFilter
    fNoise.setAmount(20)

    def perform(i: Int): Unit = {
      val j       = i * FPS_IN / FPS_OUT + START_FRAME_IN
      val bIn     = ImageIO.read(file("bulls_eye") / f"frame$j%d.png")
      val bOut    = new BufferedImage(OUT_WIDTH, OUT_HEIGHT, BufferedImage.TYPE_INT_ARGB)
      fScale.filter(bIn, bScale)
      fNoise.filter(bScale, bScale)

      import numbers.Implicits._
      val wIn  = i.clip(0, fadeInFrames).linlin(0, fadeInFrames, 0.0, 1.0)
      val wOut = i.clip(NUM_FRAMES_OUT - fadeOutFrames, NUM_FRAMES_OUT).linlin(NUM_FRAMES_OUT - fadeOutFrames, NUM_FRAMES_OUT, 1.0, 0.0)
      val w    = wIn * wOut
      fThresh.setLowerThreshold(w.linlin(0, 1, 256, THRESH_LO).toInt)
      fThresh.setUpperThreshold(w.linlin(0, 1, 256, THRESH_HI).toInt)
      fThresh.filter(bScale, bScale)

      val g       = bOut.createGraphics()
      g.setColor(Color.black)
      g.fill(0, 0, OUT_WIDTH, OUT_HEIGHT)
      g.drawImage(bScale, 0, (OUT_HEIGHT - bH)/2, null)
      g.dispose()

      ImageIO.write(bOut, "png", file("render") / f"frame$i%d.png")
    }

    // fThresh.filter(bScale, bScale)

    // perform(fadeInFrames/2)

    val view: Component = new Component {
      preferredSize = (bW, bH)

      override protected def paintComponent(g: Graphics2D): Unit =
        g.drawImage(bScale, 0,0, peer)
    }

    // val icon  = new ImageIcon(bScale)
    val frame = new MainFrame {
      contents = view
      pack().centerOnScreen()
      open()
    }

    import ExecutionContext.Implicits.global

    val fut = Future {
      var lastProg = -1
      for (i <- 1 to NUM_FRAMES_OUT) {
        perform(i)
        onEDT {
          view.repaint()
          import numbers.Implicits._
          val prog = i.linlin(1, NUM_FRAMES_OUT, 0, 100).toInt
          if (lastProg < prog) {
            lastProg = prog
            frame.title = s"$prog%"
          }
        }
      }
    }
    fut.onSuccess {
      case _ => onEDT(frame.title = "COMPLETED")
    }
  }
}