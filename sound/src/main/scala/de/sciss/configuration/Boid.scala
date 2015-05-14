package de.sciss.configuration

import java.awt.{Color, Graphics2D}
import java.awt.geom.{Path2D, GeneralPath}

import de.sciss.numbers

import scala.concurrent.stm.InTxn

object Vector2D {
  val empty = Vector2D(0f, 0f)
}
final case class Vector2D(x: Float, y: Float) {
  def + (that: Vector2D): Vector2D = Vector2D(this.x + that.x, this.y + that.y)

  def - (that: Vector2D): Vector2D = Vector2D(this.x - that.x, this.y - that.y)

  def * (scalar: Float): Vector2D = Vector2D(x * scalar, y * scalar)

  def / (scalar: Float): Vector2D = Vector2D(x / scalar, y / scalar)

  def mag: Float = math.sqrt(x*x + y*y).toFloat

  def magSq: Float = x*x + y*y

  def isEmpty: Boolean = x == 0f && y == 0f

  def nonEmpty: Boolean = !isEmpty

  def normalize: Vector2D = {
    val m = mag
    if (m != 0 && m != 1) this / m else this
  }

  def limit(max: Float): Vector2D = if (magSq <= max*max) this else normalize * max

  def clip(xMin: Float, yMin: Float, xMax: Float, yMax: Float): Vector2D = {
    import numbers.Implicits._
    val inside = x >= xMin && x <= xMax && y >= yMin && y <= yMax
    if (inside) this
    else Vector2D(x.clip(xMin, xMax), y.clip(yMin, xMax))
  }

  def distanceTo(that: Vector2D): Float = {
    val dx = x - that.x
    val dy = y - that.y
    math.sqrt(dx*dx + dy*dy).toFloat
  }

  def heading: Float = {
    val angle = math.atan2(-y, x).toFloat
    -angle
  }
}

/** Adapted from Processing Sketch by Daniel Shiffman. */
object Boid {
  def apply(x0: Float, y0: Float, angle0: Double): Boid = {
    val location = Vector2D(x0, y0)
    val velocity = Vector2D(math.cos(angle0).toFloat, math.sin(angle0).toFloat)
    new Boid(location = location, velocity = velocity)
  }

  // dimensions
  val excess  = 16
  val excessH = excess/2
  val extent  = QuadGraphDB.extent
  val side    = 2 * extent
  val width   = (extent + excess) * 2
  val height  = width

  val drawRadius  = 1.0f

  val scaleFactor = 0.5f

  // speed and force
  val maxForce = 0.03f * scaleFactor  // Maximum steering force
  val maxSpeed = 2f * scaleFactor     // Maximum speed

  // separation
  val boidSeparation = 25.0f * scaleFactor
  val wallSeparation = excess * 1.5f

  // coherence and alignment
  val neighborDist = 50f * scaleFactor

  val separationWeight  = 1.5f
  val alignmentWeight   = 1.0f
  val coherenceWeight   = 1.0f
}
final class Boid(val location: Vector2D, val velocity: Vector2D) {
  import Boid._

  def run(boids: Seq[Boid])(implicit tx: InTxn): Boid = {
    val accel = flock(boids)
    update(accel)
    // render()
  }

  // We accumulate a new acceleration each time based on three rules
  private def flock(boids: Seq[Boid]): Vector2D = {
    val sep = separate(boids)   // Separation
    val ali = align   (boids)   // Alignment
    val coh = cohesion(boids)   // Cohesion
    
    sep * separationWeight + ali * alignmentWeight + coh * coherenceWeight
  }

  private def update(accel: Vector2D): Boid = {
    val velNew = (velocity + accel).limit(maxSpeed)
    val locNew = (location + velNew).clip(excessH, excessH, side + excessH, side + excessH)
    new Boid(location = locNew, velocity = velNew)
  }

  // A method that calculates and applies a steering force towards a target
  // STEER = DESIRED MINUS VELOCITY
  private def seek(target: Vector2D): Vector2D  = {
    val desired0 = target - location   // A vector pointing from the location to the target
    // Scale to maximum speed
    val desired = desired0.normalize * maxSpeed

    // Steering = Desired minus Velocity
    val steer = desired - velocity
    steer.limit(maxForce)  // Limit to maximum steering force
  }

  def paint(g: Graphics2D): Unit = {
    // Draw a triangle rotated in the direction of velocity
    val theta = velocity.heading + math.Pi/2
    // heading2D() above is now heading() but leaving old syntax until Processing.js catches up

    val atOrig = g.getTransform
    g.translate(location.x, location.y)
    g.rotate(theta)
    val shape = new GeneralPath(Path2D.WIND_NON_ZERO, 4)
    shape.moveTo(0, -drawRadius*2)
    shape.lineTo(-drawRadius, drawRadius*2)
    shape.lineTo(drawRadius, drawRadius*2)
    shape.closePath()
    g.setColor(Color.white)
    g.draw(shape)
    g.setTransform(atOrig)
  }

  private def separationStep(steer: Vector2D, other: Vector2D, desiredSeparation: Float): Vector2D = {
    val d = location distanceTo other
    // If the distance is greater than 0 and less than an arbitrary amount (0 when you are yourself)
    if ((d > 0) && (d < desiredSeparation)) {
      // Calculate vector pointing away from neighbor
      val diff0 = location - other
      val diff  = diff0.normalize / d // Weight by distance
      diff
    } else Vector2D.empty
  }

  // Separation
  // Method checks for nearby boids and steers away
  private def separate(boids: Seq[Boid]): Vector2D = {
    // For every boid in the system, check if it's too close
    val steer0 = (Vector2D.empty /: boids) { case (steerIn, other) =>
      separationStep(steerIn, other.location, boidSeparation)
    }
    val walls =
      Vector2D(0, location.y) :: Vector2D(width, location.y) ::
      Vector2D(location.x, 0) :: Vector2D(location.x, height) :: Nil

    val steer = (steer0 /: walls) { case (steerIn, wall) =>
      separationStep(steerIn, wall, wallSeparation)
    }

    // As long as the vector is greater than 0
    if (steer.nonEmpty) {
      // First two lines of code below could be condensed with new Location setMag() method
      // Not using this method until Processing.js catches up
      // steer.setMag(maxspeed);

      // Implement Reynolds: Steering = Desired - Velocity
      (steer.normalize * maxSpeed - velocity).limit(maxForce)
    }
    else Vector2D.empty
  }

  // Alignment
  // For every nearby boid in the system, calculate the average velocity
  private def align(boids: Seq[Boid]): Vector2D = {
    val sum = (Vector2D.empty /: boids) { case (sumIn, other) =>
      val d = location distanceTo other.location
      if ((d > 0) && (d < neighborDist)) {
        sumIn + other.velocity
      } else sumIn
    }
    if (sum.nonEmpty) {
      // Implement Reynolds: Steering = Desired - Velocity
      val norm  = sum.normalize * maxSpeed
      val steer = norm - velocity
      steer.limit(maxForce)
    }
    else Vector2D.empty
  }

  // Cohesion
  // For the average location (i.e. center) of all nearby boids, calculate steering vector towards that location
  private def cohesion(boids: Seq[Boid]): Vector2D = {
    val (sum, count) = ((Vector2D.empty, 0) /: boids) { case (in @ (sumIn, countIn), other) =>
      val d = location distanceTo other.location
      if ((d > 0) && (d < neighborDist)) {
        (sumIn + other.location, countIn + 1)
      } else in
    }
    if (count > 0) {
      seek(sum / count) // Steer towards the location
    }
    else Vector2D.empty
  }
}