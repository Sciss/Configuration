package de.sciss.configuration

import java.awt.{Color, Graphics2D}
import java.awt.geom.{Path2D, GeneralPath}

import de.sciss.lucre.confluent.TxnRandom
import de.sciss.numbers

import scala.concurrent.stm.InTxn

object Vector2D {
  val empty = Vector2D(0f, 0f)
}
final case class Vector2D(x: Float, y: Float) {
  def + (that: Vector2D): Vector2D =
    if (this.isEmpty) that else if (that.isEmpty) this
    else Vector2D(this.x + that.x, this.y + that.y)

  def - (that: Vector2D): Vector2D =
    if (that.isEmpty) this
    else Vector2D(this.x - that.x, this.y - that.y)

  def * (scalar: Float): Vector2D =
    if (isEmpty) this else if (scalar == 0f) Vector2D.empty
    else Vector2D(x * scalar, y * scalar)

  def / (scalar: Float): Vector2D =
    if (isEmpty) this
    else Vector2D(x / scalar, y / scalar)

  def magSq: Float = x*x + y*y
  def mag  : Float = if (isEmpty) 0f else math.sqrt(magSq).toFloat

  def isEmpty : Boolean = x == 0f && y == 0f
  def nonEmpty: Boolean = !isEmpty

  def normalize: Vector2D = {
    val m = mag
    if (m == 0 || m == 1) this else this / m
  }

  def limit(max: Float): Vector2D = if (magSq <= max*max) this else normalize * max

  //  def flipX: Vector2D = if (x == 0) this else Vector2D(-x,  y)
  //  def flipY: Vector2D = if (y == 0) this else Vector2D( x, -y)

  def clip(xMin: Float, yMin: Float, xMax: Float, yMax: Float): Vector2D = {
    import numbers.Implicits._
    val inside = x >= xMin && x <= xMax && y >= yMin && y <= yMax
    if (inside) this
    else Vector2D(x.clip(xMin, xMax), y.clip(yMin, xMax))
  }

  def distanceTo(that: Vector2D): Float = math.sqrt(distanceToSqr(that)).toFloat

  def distanceToSqr(that: Vector2D): Float = {
    val dx = x - that.x
    val dy = y - that.y
    dx*dx + dy*dy
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

  val paintSize  = 2.0f

  val scaleFactor = 0.5f

  // speed and force
  val maxForce = 0.03f * scaleFactor  // Maximum steering force
  val maxSpeed = 2f * scaleFactor     // Maximum speed

  // separation
  val boidSeparation = 25.0f * scaleFactor
  val boidSeparationSqr = boidSeparation * boidSeparation
  val wallSeparation = excess * 1.0f // 1.5f

  // coherence and alignment
  val neighborDist = 50f * scaleFactor
  val neighborDistSqr = neighborDist * neighborDist

  val separationWeight  = 1.5f
  val alignmentWeight   = 1.0f
  val coherenceWeight   = 1.0f
}
final case class Boid(location: Vector2D, velocity: Vector2D) {
  import Boid._

  def run(boids: Seq[Boid])(implicit tx: InTxn, random: TxnRandom[InTxn]): Boid = {
    val accel = flock(boids)
    update(accel)
    // render()
  }

  // We accumulate a new acceleration each time based on three rules
  private def flock(boids: Seq[Boid])(implicit tx: InTxn, random: TxnRandom[InTxn]): Vector2D = {
    val sep = separate(boids)   // Separation
    val ali = align   (boids)   // Alignment
    val coh = cohesion(boids)   // Cohesion
    
    sep * separationWeight + ali * alignmentWeight + coh * coherenceWeight
  }

  private def update(accel: Vector2D)(implicit tx: InTxn, random: TxnRandom[InTxn]): Boid = {
    var vel   = (velocity + accel).limit(maxSpeed)
    var loc   = location + vel
    if (loc.x < excessH) {
      loc = Vector2D(excess - loc.x, loc.y)
      vel = Vector2D(-vel.x, vel.y)
    }
    if (loc.x > width - excessH) {
      loc = Vector2D(2*width - excess - loc.x, loc.y)
      vel = Vector2D(-vel.x, vel.y)
    }
    if (loc.y < excessH) {
      loc = Vector2D(loc.x, excess - loc.y)
      vel = Vector2D(vel.x, -vel.y)
    }
    if (loc.y > height - excessH) {
      loc = Vector2D(loc.x, 2*height - excess - loc.y)
      vel = Vector2D(vel.x, -vel.y)
    }

    vel = Vector2D(vel.x + (random.nextFloat() - 0.5f) * maxSpeed * 0.01f,
                   vel.y + (random.nextFloat() - 0.5f) * maxSpeed * 0.01f)
    new Boid(location = loc, velocity = vel)
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

    val atOrig = g.getTransform
    g.translate(location.x, location.y)
    g.rotate(theta)
    val shape = new GeneralPath(Path2D.WIND_NON_ZERO, 4)
    shape.moveTo(0, 0)
    shape.lineTo(-paintSize, paintSize*4)
    shape.lineTo( paintSize, paintSize*4)
    shape.closePath()
    g.setColor(Color.magenta)
    // g.draw(shape)
    g.fill(shape)
    g.setTransform(atOrig)
  }

  private def separationStep(steer: Vector2D, other: Vector2D, sepSqr: Float): Vector2D = {
    val d = location distanceToSqr other
    // If the distance is greater than 0 and less than an arbitrary amount (0 when you are yourself)
    if ((d > 0) && (d < sepSqr)) {
      // Calculate vector pointing away from neighbor
      val diff0 = location - other
      val diff  = diff0.normalize / d // Weight by distance
      steer + diff
    } else steer
  }

  // Separation
  // Method checks for nearby boids and steers away
  private def separate(boids: Seq[Boid]): Vector2D = {
    // For every boid in the system, check if it's too close
    val steerB = (Vector2D.empty /: boids) { case (steerIn, other) =>
      separationStep(steerIn, other.location, boidSeparationSqr)
    }

    //    val xt = location.x // + random.nextFloat() - 0.5f
    //    val yt = location.x // + random.nextFloat() - 0.5f
    //
    //    val walls =
    //      Vector2D(0f, yt) :: Vector2D(width, yt) ::
    //      Vector2D(xt, 0f) :: Vector2D(xt, height) :: Nil
    //
    //    val steerW = (Vector2D.empty /: walls) { case (steerIn, wall) =>
    //      separationStep(steerIn, wall, wallSeparation)
    //    }

    // As long as the vector is greater than 0
    val steerBN = if (steerB.nonEmpty) {
      // Implement Reynolds: Steering = Desired - Velocity
      (steerB.normalize * maxSpeed - velocity).limit(maxForce)
    }
    else Vector2D.empty

    //    val steerWN = if (steerW.nonEmpty) {
    //      (steerW.normalize * maxSpeed - velocity).limit(maxForce)
    //    }
    //    else Vector2D.empty
    //
    //    steerBN + steerWN

    steerBN
  }

  // Alignment
  // For every nearby boid in the system, calculate the average velocity
  private def align(boids: Seq[Boid]): Vector2D = {
    val sum = (Vector2D.empty /: boids) { case (sumIn, other) =>
      val d = location distanceToSqr other.location
      if ((d > 0) && (d < neighborDistSqr)) {
        sumIn + other.velocity
      } else sumIn
    }

    //    val xt = location.x // + random.nextFloat() - 0.5f
    //    val yt = location.x // + random.nextFloat() - 0.5f
    //
    //    val walls =
    //      (Vector2D(0f, yt), Vector2D(maxSpeed, 0f)) :: (Vector2D(width, yt), Vector2D(-maxSpeed, 0f)) ::
    //      (Vector2D(xt, 0f), Vector2D(0f, maxSpeed)) :: (Vector2D(xt, height), Vector2D(0f, -maxSpeed)) :: Nil
    //
    //    val sum = (sum0 /: walls) { case (sumIn, (wallLoc, wallVel)) =>
    //      val d = location distanceToSqr wallLoc
    //      if ((d > 0) && (d < neighborDistSqr)) {
    //        sumIn + wallVel
    //      } else sumIn
    //    }

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
      val d = location distanceToSqr other.location
      if ((d > 0) && (d < neighborDistSqr)) {
        (sumIn + other.location, countIn + 1)
      } else in
    }
    if (count > 0) {
      seek(sum / count) // Steer towards the location
    }
    else Vector2D.empty
  }
}