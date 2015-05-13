/*
 *  Text2.scala
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

object Text2 extends TextLike {
  // Text (C)opyright Hanns Holger Rutz. Provided through a Creative Commons license
  // 'Attribution-NonCommerical-NoDerivs 3.0 Unported'
  // https://creativecommons.org/licenses/by-nc-nd/3.0/
  val text =
    """Eggshells, many of them, the breeding of things. Some die; a batch dies; some hatch, a
      |collection of animals. For her, in her honour. Verticality—things are on the floor,
      |a staircase going up and leading out. A turtle. On the top, there is earth, a ground,
      |a piece of land. A gate; two gates, next to each other, one for adults to open and
      |close, it can be safely left open. The other for kids or animals to be left shut
      |whenever you use it. It locks by force and friction. The algorithm becomes real. What
      |is that? The field of eggshells. Cocoa shells? The concept of the train station,
      |purely by sound of containers and not having seen it, someone says it is kilometres
      |away. Through the obsessive exposure, computational things become real, effective and
      |agential. The way after-images disappear, the irrevocable fading. An owl, one has to
      |be extremely careful as it is not domesticated, it bites strongly with the bill, but
      |more severely, when it tries to fly it will use its claws and push them into your
      |hands, your flesh, causing deep wounds. You need to protected yourself. Her favourite
      |animal. Another animal catches the words you speak and repeats them. You have to
      |listen very carefully to hear the difference between origin and echo. Is that me, is
      |that her speaking, or just the animal's imitation? You can tell by the slurs, there
      |are some subtle echos, a graininess in the sound.
      |""".stripMargin

  val tail = 120

  val anim: Anim = Vector(
  0 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.015f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 2.0E-4f, "Limit" -> 300.0f, "SpringCoefficient" -> 1.0E-4f, "VLength" -> 100.0f, "VSpring" -> 8.0E-5f, "VTorque" -> 2.0E-4f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.4f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),
  1000 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.015f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 2.0E-4f, "Limit" -> 300.0f, "SpringCoefficient" -> 1.0E-4f, "VLength" -> 100.0f, "VSpring" -> 8.0E-5f, "VTorque" -> 2.0E-4f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.4f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),
  1506 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.0f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 2.0E-4f, "Limit" -> 300.0f, "SpringCoefficient" -> 1.0E-4f, "VLength" -> 100.0f, "VSpring" -> 8.0E-5f, "VTorque" -> 2.0E-4f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.41f, "Distance" -> -1.0f, "GravitationalConstant" -> 1.4f)
  ),
  2359 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.022f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 2.0E-4f, "Limit" -> 300.0f, "SpringCoefficient" -> 6.139E-4f, "VLength" -> 100.0f, "VSpring" -> 8.0E-5f, "VTorque" -> 2.0E-4f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.41f, "Distance" -> -1.0f, "GravitationalConstant" -> 10.0f)
  ),
  3005 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.022f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 2.0E-4f, "Limit" -> 300.0f, "SpringCoefficient" -> 6.139E-4f, "VLength" -> 100.0f, "VSpring" -> 8.0E-5f, "VTorque" -> 0.005f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.41f, "Distance" -> -1.0f, "GravitationalConstant" -> 10.0f)
  ),
  3176 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.022f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 2.0E-4f, "Limit" -> 300.0f, "SpringCoefficient" -> 6.139E-4f, "VLength" -> 100.0f, "VSpring" -> 0.008f, "VTorque" -> 0.005f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.41f, "Distance" -> -1.0f, "GravitationalConstant" -> 10.0f)
  ),
  3440 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.022f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 2.0E-4f, "Limit" -> 300.0f, "SpringCoefficient" -> 6.139E-4f, "VLength" -> 100.0f, "VSpring" -> 0.008f, "VTorque" -> 0.005f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.41f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),
  4281 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.053f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.00225f, "Limit" -> 300.0f, "SpringCoefficient" -> 2.1790001E-4f, "VLength" -> 100.0f, "VSpring" -> 0.008f, "VTorque" -> 0.005f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.41f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),
  4831 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.01f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.00315f, "Limit" -> 300.0f, "SpringCoefficient" -> 6.3370005E-4f, "VLength" -> 100.0f, "VSpring" -> 0.0051200003f, "VTorque" -> 0.0033999998f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.41f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),
  5707 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.01f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.00195f, "Limit" -> 300.0f, "SpringCoefficient" -> 6.3370005E-4f, "VLength" -> 100.0f, "VSpring" -> 0.0051200003f, "VTorque" -> 0.0018999999f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.41f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),
  6087 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.032f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.00195f, "Limit" -> 300.0f, "SpringCoefficient" -> 4.9510004E-4f, "VLength" -> 100.0f, "VSpring" -> 0.0029600002f, "VTorque" -> 0.0018999999f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.41f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),
  6256 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.032f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.00195f, "Limit" -> 300.0f, "SpringCoefficient" -> 4.9510004E-4f, "VLength" -> 50.95f, "VSpring" -> 0.0029600002f, "VTorque" -> 0.0018999999f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.41f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),
  6763 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.01f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.00195f, "Limit" -> 300.0f, "SpringCoefficient" -> 4.9510004E-4f, "VLength" -> 50.95f, "VSpring" -> 0.0029600002f, "VTorque" -> 0.0018999999f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.41f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),
  7257 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.021f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.00195f, "Limit" -> 300.0f, "SpringCoefficient" -> 3.6640003E-4f, "VLength" -> 50.95f, "VSpring" -> 0.00152f, "VTorque" -> 0.0f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.41f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),
  7504 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.021f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.005f, "Limit" -> 300.0f, "SpringCoefficient" -> 3.6640003E-4f, "VLength" -> 50.95f, "VSpring" -> 0.00152f, "VTorque" -> 0.0f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.41f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),
  7610 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.021f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.0017499999f, "Limit" -> 300.0f, "SpringCoefficient" -> 3.6640003E-4f, "VLength" -> 50.95f, "VSpring" -> 0.00152f, "VTorque" -> 0.0f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.41f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),
  7695 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.021f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.0017499999f, "Limit" -> 300.0f, "SpringCoefficient" -> 3.6640003E-4f, "VLength" -> 50.95f, "VSpring" -> 0.0f, "VTorque" -> 0.0f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.41f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),
  8389 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.021f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.0017499999f, "Limit" -> 300.0f, "SpringCoefficient" -> 3.6640003E-4f, "VLength" -> 50.95f, "VSpring" -> 0.0f, "VTorque" -> 1.0E-4f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.41f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),
  9052 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.021f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.0017499999f, "Limit" -> 300.0f, "SpringCoefficient" -> 3.6640003E-4f, "VLength" -> 50.95f, "VSpring" -> 8.0000005E-5f, "VTorque" -> 1.0E-4f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.41f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),
  9677 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.021f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.0017499999f, "Limit" -> 300.0f, "SpringCoefficient" -> 3.6640003E-4f, "VLength" -> 20.98f, "VSpring" -> 8.0000005E-5f, "VTorque" -> 1.0E-4f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.41f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  )
  )
}