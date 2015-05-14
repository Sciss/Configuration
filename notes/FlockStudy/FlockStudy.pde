/**
 * Flocking 
 * Original code by Daniel Shiffman.
 * Adapted by Hanns Holger Rutz. 
 * 
 * An implementation of Craig Reynold's Boids program to simulate
 * the flocking behavior of birds. Each boid steers itself based on 
 * rules of avoidance, alignment, and coherence.
 */

Flock flock;

// dimensions
int excess = 16;
int excessH = excess/2;
int extent = 256;
int side = 2 * extent;

float scaleFactor = 0.5f;

int numTransducers = 9;

// speed and force
float maxForce = 0.03f * scaleFactor;    // Maximum steering force
float maxSpeed = 2f * scaleFactor; // Maximum speed

// separation
float boidSeparation = 25.0f * scaleFactor;
float wallSeparation = excess * 1.5f; // * scaleFactor;

// coherence and alignment
float neighborDist = 50f * scaleFactor;

// weighted forces
float separationWeight = 1.5f;
float alignmentWeight  = 1.0f;
float coherenceWeight  = 1.0f;

void setup() {
  int full = (extent + excess) * 2;
  size(full, full);
  flock = new Flock();
  // Add an initial set of boids into the system
  int ix = round(random(width));
  int iy = round(random(height));
  for (int i = 0; i < numTransducers; i++) {
    flock.addBoid(new Boid(ix, iy));
    // flock.addBoid(new Boid(random(width), random(height)));
  }
  frameRate(4);
}

void draw() {
  background(50);
  noFill();
  rect(excess, excess, side, side);
  flock.run();
}
