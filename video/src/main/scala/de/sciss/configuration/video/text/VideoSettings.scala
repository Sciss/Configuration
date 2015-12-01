/*
 *  VideoSettings.scala
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

package de.sciss.configuration.video.text

import de.sciss.file.File

import scala.language.implicitConversions

object VideoSettings {
  def apply() = new Builder

  implicit def build(b: Builder): VideoSettings = {
    import b._
    Impl(baseFile = baseFile, width = width, height = height,
      numFrames = numFrames, framesPerSecond = framesPerSecond,
      speedLimit = speedLimit, text = text, anim = anim, format = format, dpi = dpi)
  }

  private final case class Impl(baseFile: File, width: Int, height: Int, numFrames: Int, framesPerSecond: Int,
                                speedLimit: Double, text: String, anim: Anim, format: Format, dpi: Double)
    extends VideoSettings {

    override def productPrefix = "VideoSetting"
  }

  final class Builder extends VideoSettings {
    private var _baseFile: File = _
    def baseFile: File = {
      if (_baseFile == null) throw new IllegalStateException("baseFile has not been set")
      _baseFile
    }

    def baseFile_=(value: File): Unit = _baseFile = value

    var width               = 1080 // 1920
    var height              = 1920 // 1080
    var numFrames           = 10000
    var framesPerSecond     = 25
    var speedLimit          = 0.1
    var text                = "Foo Bar"
    var anim                = Vector.empty: Anim
    var format              = Format.PNG : Format
    var dpi                 = 300.0
  }

  object Format {
    def apply(name: String): Format = name match {
      case PNG.ext => PNG
      case PDF.ext => PDF
    }
    case object PNG extends Format { val ext = "png" }
    case object PDF extends Format { val ext = "pdf" }

    val all = Seq[Format](PNG, PDF)
  }
  sealed trait Format { def ext: String }
}
trait VideoSettings {
  def baseFile            : File
  def width               : Int
  def height              : Int
  def numFrames           : Int
  def framesPerSecond     : Int
  // def plopDur             : Double
  def speedLimit          : Double
  def text                : String
  def anim                : Anim

  def format              : VideoSettings.Format
  def dpi                 : Double
}
