package de.sciss.configuration.video.text

trait TextLike {
  def text: String
  def anim: Anim

  /** Seconds */
  def tail: Int
}
