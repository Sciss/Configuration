/**
 * Flocking 
 * by Daniel Shiffman.  
 * 
 * An implementation of Craig Reynold's Boids program to simulate
 * the flocking behavior of birds. Each boid steers itself based on 
 * rules of avoidance, alignment, and coherence.
 * 
 * Click the mouse to add a new boid.
 */

Flock flock;

int excess = 16;
int excessH = excess/2;
int extent = 256;
int side = 2 * extent;
int numTransducers = 9;

void setup() {
  int full = (extent + excess) * 2;
  size(full, full);
  flock = new Flock();
  // Add an initial set of boids into the system
  for (int i = 0; i < numTransducers; i++) {
    flock.addBoid(new Boid(width/2,height/2));
  }
}

void draw() {
  background(50);
  flock.run();
}
