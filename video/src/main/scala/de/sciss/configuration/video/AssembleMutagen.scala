package de.sciss.configuration.video

import de.sciss.file._

object AssembleMutagen extends AssembleLike with App {
  def BLACK_FRONT = 15.0
  def BLACK_MID   = 30.0
  def BLACK_BACK  = 15.0

  lazy val outDir = file("assembly_mutagen")

  run()

  def run(): Unit = {
    testExistsOrMake()
    putBlack(BLACK_FRONT)
    putMovie(file("renderBetanovuss0_100"))
    putBlack(BLACK_MID)
    putMovie(file("renderBetanovuss1"))
    putBlack(BLACK_MID)
    putMovie(file("renderBetanovuss2"))
    putBlack(BLACK_MID)
    //    putMovie(file("renderBetanovussSchrauben"))
    //    putBlack(BLACK_MID)
    //    putMovie(file("renderBetanovussStairs"))
    //    putBlack(BLACK_MID)
    putMovie(file("renderZaubes2"))
    putBlack(BLACK_BACK)
    finalizeH264()
  }
}
