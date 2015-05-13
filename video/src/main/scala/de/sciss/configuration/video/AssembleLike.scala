package de.sciss.configuration.video

import de.sciss.file._

trait AssembleLike {
  def FPS = 25

  var frameCount = 0

  def incFrame(): Unit = frameCount += 1

  def outDir: File

  lazy val outFrameDir: File = outDir / "frames"

  def testExistsOrMake(): Unit = {
    if (outDir.exists()) {
      println(s"Directory '$outDir' already exists. Not re-rendering.")
      sys.exit()
    }

    outFrameDir.mkdirs()
  }

  def put(in: File): Unit = {
    incFrame()
    val out = outFrameDir / s"frame$frameCount.png"
    import sys.process._
    val res = Seq("ln", "-s", in.absolutePath, out.absolutePath).!
    require(res == 0, s"ln failed for '$in' -> '$out' ($res)")
  }

  lazy val blackFile = file("black_frame.png")

  var USE_LOG = true

  def log(what: => String): Unit = if (USE_LOG) println(what)

  def putBlack(dur: Double): Unit = {
    val num = (dur * FPS + 0.5).toInt
    log(s"Putting $num frames of black.")
    for (i <- 0 until num) put(blackFile)
  }

  def putMovie(movieDir: File): Unit = {
    val Reg = """frame(\d+).png""".r

    def getFrame(s: String): Option[Int] = s match {
      case Reg(n) => Some(n.toInt)
      case _ => None
    }

    val frames = movieDir.children.flatMap(f => getFrame(f.name).map(_ -> f)).sortBy(_._1)
    val startFrame = frames.head._1
    require(startFrame == 0 || startFrame == 1)
    val num = frames.last._1 - startFrame + 1
    require(frames.size == num)  // fail if frames are missing

    log(s"Putting $num frames of '${movieDir.name}'.")
    frames.foreach { case (i, f) =>
      put(f)
    }
  }

  def finalizeH264(): Unit = {
    log(s"Converting $frameCount frame images into h.264 file.")
    val cmd = Seq("avconv",
      "-i", s"${outFrameDir.absolutePath}/frame%d.png", // input image sequence
      "-vcodec", "libx264", // codec
      "-r", FPS.toString,   // frame rate
      "-q", "100",          // quality
      "-pass", "1",         // stage at which to estimate bit rate
      "-s", "1920x1080",    // size
      "-vb", "6M",          //
      "-threads", "0",      //
      "-f", "mp4",          // container format
      "-vf", "transpose=2", // rotate from portrait to landscape
      (outDir / "assembly.mp4").absolutePath) // output file
    import sys.process._
    println(cmd.mkString(" "))
    val res = cmd.!
    require(res == 0, s"avconv failed ($res)")

    Seq("rm", "-r", outFrameDir.absolutePath).!  // no need to keep these sym-links

    log("Done.")
  }
}
