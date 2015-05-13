/*
 *  Text1.scala
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

object Text1 extends TextLike {
  // Text (C)opyright Hanns Holger Rutz. Provided through a Creative Commons license
  // 'Attribution-NonCommerical-NoDerivs 3.0 Unported'
  // https://creativecommons.org/licenses/by-nc-nd/3.0/
  //
  // Derived from my thesis, p. 5f
  val text =
    """Entanglement: My first encounter with sound art, at least
      |according to my leaky memory, was with a work by Rolf Julius in the Weserburg museum in
      |Bremen, Riga's twin city, near the place where I grew up. I cannot recall exactly which work
      |it was, but it was a sound installation inside a narrow severed corner, perhaps with small
      |sounds emanating from a pile of objects on the floor. I always enjoyed going to that museum
      |because of its rigorous industrial atmosphere, a very quiet place except for your own
      |footsteps, yet open and airy. Finding these tiny sounds among this quietness—maybe there
      |was the sound of the kinetic machines of a Kienholz environment or from a Rebecca Horn
      |swing—was a unique experience.
      |In hindsight it mixes with the effort of taking the bus from my hometown to travel there (I did
      |not have a driver’s licence yet). It also gets mixed up with things that happen later, for example
      |seeing Terry Fox’s "The Eye is Not the Only Glass that Burns the Mind" in Worpswede, probably
      |merely due to its regional proximity, or maybe because this was in autumn 2011, not so long
      |after Julius died. No matter what the reasons are, the important factor is that these coherences
      |are outside the control of the artist. My hypothesis is that just as the essence of an artwork’s
      |reception lies in the process of its traversal, the same goes for the production of an artwork.
      |""".stripMargin

  val tail = 60

  val anim: Anim = Vector(
  0 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.015f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 2.0E-4f, "Limit" -> 300.0f, "SpringCoefficient" -> 1.684E-4f, "VLength" -> 100.0f, "VSpring" -> 8.0E-5f, "VTorque" -> 2.0E-4f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.4f, "Distance" -> -1.0f, "GravitationalConstant" -> -0.2f)
  ),

  1747 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.015f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 2.0E-4f, "Limit" -> 300.0f, "SpringCoefficient" -> 1.684E-4f, "VLength" -> 200.8f, "VSpring" -> 2.4000001E-4f, "VTorque" -> 2.0E-4f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.4f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),

  2098 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.015f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 5.9999997E-4f, "Limit" -> 300.0f, "SpringCoefficient" -> 1.684E-4f, "VLength" -> 200.8f, "VSpring" -> 2.4000001E-4f, "VTorque" -> 2.0E-4f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.4f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),

  2850->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.02f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 5.9999997E-4f, "Limit" -> 300.0f, "SpringCoefficient" -> 2.1790001E-4f, "VLength" -> 200.8f, "VSpring" -> 8.8000007E-4f, "VTorque" -> 5.9999997E-4f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.4f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),

  3110 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.059f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 5.9999997E-4f, "Limit" -> 300.0f, "SpringCoefficient" -> 2.1790001E-4f, "VLength" -> 200.8f, "VSpring" -> 8.8000007E-4f, "VTorque" -> 5.9999997E-4f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.4f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),

  3448 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.1f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 5.9999997E-4f, "Limit" -> 300.0f, "SpringCoefficient" -> 2.1790001E-4f, "VLength" -> 200.8f, "VSpring" -> 0.008f, "VTorque" -> 5.9999997E-4f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.4f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),

  3977 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.014f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 5.9999997E-4f, "Limit" -> 300.0f, "SpringCoefficient" -> 0.001f, "VLength" -> 170.83f, "VSpring" -> 0.0016800001f, "VTorque" -> 5.9999997E-4f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.4f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),

  4255 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.014f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.005f, "Limit" -> 300.0f, "SpringCoefficient" -> 0.001f, "VLength" -> 150f /* 170.83f */, "VSpring" -> 0.0016800001f, "VTorque" -> 5.9999997E-4f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.4f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),

  4696 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.014f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.005f, "Limit" -> 300.0f, "SpringCoefficient" -> 0.001f, "VLength" -> 120f /* 150.85f */, "VSpring" -> 0.0016800001f, "VTorque" -> 0.00225f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.4f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),

  5244 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.014f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.005f, "Limit" -> 300.0f, "SpringCoefficient" -> 0.001f, "VLength" -> 90f /* 100.9f */, "VSpring" -> 0.0016800001f, "VTorque" -> 0.00225f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.4f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),

  6275 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.016f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.0025f, "Limit" -> 300.0f, "SpringCoefficient" -> 4.9510004E-4f, "VLength" -> 70f /* 100.9f */, "VSpring" -> 6.4000004E-4f, "VTorque" -> 7.5E-4f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.4f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),

  6556 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.016f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.0025f, "Limit" -> 300.0f, "SpringCoefficient" -> 4.9510004E-4f, "VLength" -> 40f /* 50.95f */, "VSpring" -> 6.4000004E-4f, "VTorque" -> 7.5E-4f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.4f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),

  7585 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.01f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.0025f, "Limit" -> 300.0f, "SpringCoefficient" -> 4.9510004E-4f, "VLength" -> 40f /* 50.95f */, "VSpring" -> 0.008f, "VTorque" -> 0.0025499999f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.4f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),

  8271 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.016f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.00185f, "Limit" -> 300.0f, "SpringCoefficient" -> 4.1590002E-4f, "VLength" -> 40f /* 50.95f */, "VSpring" -> 7.2E-4f, "VTorque" -> 0.0016999999f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.4f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),

  9022 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.016f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.00185f, "Limit" -> 300.0f, "SpringCoefficient" -> 4.1590002E-4f, "VLength" -> 30f /* 30.97f */, "VSpring" -> 5.6E-4f, "VTorque" -> 0.0016999999f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.4f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.2f)
  ),

  9379 ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.016f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.00185f, "Limit" -> 300.0f, "SpringCoefficient" -> 4.1590002E-4f, "VLength" -> 20f /* 30.97f */, "VSpring" -> 5.6E-4f, "VTorque" -> 0.0016999999f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.4f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  ),

  9600 /* 9732 */ ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.016f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.00185f, "Limit" -> 300.0f, "SpringCoefficient" -> 4.1590002E-4f, "VLength" -> 20f /* 30.97f */, "VSpring" -> 5.6E-4f, "VTorque" -> 0.0016999999f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.4f, "Distance" -> -1.0f, "GravitationalConstant" -> -0.2f)
  ),

//  9926 ->
//  Map(
//    "DragForce" -> Map("DragCoefficient" -> 0.016f),
//    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.00185f, "Limit" -> 300.0f, "SpringCoefficient" -> 4.1590002E-4f, "VLength" -> 20f /* 30.97f */, "VSpring" -> 5.6E-4f, "VTorque" -> 0.0016999999f),
//    "NBodyForce" -> Map("BarnesHutTheta" -> 0.4f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
//  ),

  9900 /* 10126 */ ->
  Map(
    "DragForce" -> Map("DragCoefficient" -> 0.022f),
    "MySpringForce" -> Map("DefaultSpringLength" -> 50.0f, "HTorque" -> 0.00185f, "Limit" -> 300.0f, "SpringCoefficient" -> 4.1590002E-4f, "VLength" -> 20f /* 30.97f */, "VSpring" -> 5.6E-4f, "VTorque" -> 0.0016999999f),
    "NBodyForce" -> Map("BarnesHutTheta" -> 0.4f, "Distance" -> -1.0f, "GravitationalConstant" -> 0.0f)
  )
  )
}