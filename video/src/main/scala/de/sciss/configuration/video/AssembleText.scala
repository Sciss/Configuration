package de.sciss.configuration.video

import de.sciss.file._

object AssembleText extends AssembleLike with App {
  def BLACK_FRONT = 15.0
  def BLACK_MID   = 30.0
  def BLACK_BACK  = 15.0

  lazy val outDir = file("assembly_text")

  run()

  def run(): Unit = {
    testExistsOrMake()
    putBlack(BLACK_FRONT)
    putMovie(file("renderText1"))
    putBlack(BLACK_MID)
    putMovie(file("renderText2"))
    putBlack(BLACK_BACK)
    finalizeH264()
  }
}
