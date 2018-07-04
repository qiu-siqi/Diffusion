import javafx.scene.shape.Circle;

import java.util.Random;

/**
 * Created by Catz on 4/12/14.
 *
 * Particle is a template for the implementation of any type of moving circular objects that are involved in the diffusion process.
 */
public abstract class Particle extends Circle{

    //Stores the translation in the x direction (left right) of the object every update duration
    private double vX = 0;
    //Stores the translation in the y direction (up down) of the object every update duration
    private double vY = 0;

    //buffer is a countdown of the number of update durations that that needs to pass after the particle collides with another particle
    //before the same two particles can collide again
    //When buffer reaches 0, storage of this particle's last collision will be cleared such that it can collide with no limitations
    //*In real life diffusion, such a thing do not exist. buffer is for the purpose of minimizing the number of particle pairs that gets stuck together
    //due to multiple collisions that happen in short periods of time causing them to reflect back and forth (due to algorithm limitation).
    //However, such a buffer has minimal effect to the accuracy of the simulation since normally, no two particles would collide multiple times in short periods
    //(as they are reflected away from each other upon collision)*
    private int buffer;
    //lastCollide stores the reference to the particle which this particle last collided with which defines which particle it cannot collide again with
    //until the predetermined time period passes
    private Particle lastCollide;

    public Particle(){
        setDirection();
        resetBuffer();
    }

    /**
     * Clears this particle's last collision.
     */
    private void resetLastCollide(){
        lastCollide = null;
    }

    Particle getLastCollide(){
        return lastCollide;
    }

    void setLastCollide(Particle p){
        lastCollide = p;
    }

    /**
     * Moves simulation time forward.
     */
    void passedTime(){
        buffer--;
        //if buffer countdown to 0, remove lastCollide
        if (buffer == 0){
            resetBuffer();
            resetLastCollide();
        }
    }

    private void resetBuffer(){
        buffer = 20;
    }

    /**
     * Updates the particle upon the passing of simulation time.
     *
     * @param multiplier Speed of simulation.
     */
    public abstract void update(double multiplier);

    /**
     * Checks for collision with {@code other}.
     *
     * @param other The other particle.
     * @return whether this particle collided with {@code other}.
     */
    boolean collide(Particle other){
        double dx = (other.getTranslateX()+2000) - (getTranslateX()+2000);
        double dy = (other.getTranslateY()+2000) - (getTranslateY()+2000);
        double dist = Math.sqrt(dx*dx+dy*dy);
        double minDist = other.getRadius() + getRadius();

        return dist < minDist;
    }

    /**
     * Checks whether this particle is in close proximity with {@code other}.
     *
     * @param other The other particle.
     * @return whether this particle is in close proximity with {@code other}.
     */
    boolean closeTo(Particle other){
        double dx = (other.getTranslateX()+2000) - (getTranslateX()+2000);
        double dy = (other.getTranslateY()+2000) - (getTranslateY()+2000);
        double dist = Math.sqrt(dx*dx+dy*dy);
        double minDist = other.getRadius() + getRadius()+10;

        return dist < minDist;
    }

    /**
     * Updates this particle upon collision with {@code particle}.
     *
     * @param particle Particle that was collided with.
     */
    public abstract void collidedWith(Particle particle);

    /**
     * Updates this particle upon close contact with {@code particle}.
     *
     * @param particle Particle that is in close proximity.
     */
    public abstract void isCloseTo(Particle particle);

    /**
     * Resets the direction of this particle to a new randomized direction.
     */
    void setDirection(){
        int temp = new Random().nextInt((int)Math.PI*10000*2);
        vX = Math.cos(temp/10000.0);
        vY = Math.sin(temp/10000.0);
    }

    double getVX(){
        return vX;
    }

    double getVY(){
        return vY;
    }

    /**
     * Changes the direction of movement of this particle to its opposite direction.
     */
    void reflectBoth(){
        //there is a 1 in 8 chance whereby the particle will reflect off in a path that differs from the path the particle previously took
        if (new Random().nextInt(8)==0){
            //If the 1 in 8 chance is true, generate new values for vX and vY (through cos and sin since overall velocity has to be equal for all particles)
            //until both values match the general direction that the particle is supposed to travel in after the collision
            //For eg. if the particle was travelling diagonally upwards and leftwards, it is now supposed to travel downwards and rightwards, so the generated vX has to
            //be negative while the generated vY has to be positive
            //If the conditions are met, change the translational values of the particle to the new values
            while (true){
                boolean check1 = false, check2 = false;
                int temp = new Random().nextInt((int)Math.PI*10000*2);
                if (vX > 0 && Math.cos(temp/10000.0) < 0||vX<=0 && Math.cos(temp/10000.0)>0){
                    check1 = true;
                }
                if (vY > 0 && Math.sin(temp/10000.0) < 0 || vY <= 0 && Math.sin(temp/10000.0) > 0){
                    check2 = true;
                }
                if (check1 && check2){
                    vX = Math.cos(temp/10000.0);
                    vY = Math.sin(temp/10000.0);
                    break;
                }
            }
        //If not in the rare chance, particle travels back following the inverse of the path it previously took
        } else {
            vX = -vX;
            vY = -vY;
        }
    }

    /**
     * Changes the direction of the horizontal motion of the particle to its opposite direction.
     */
    void reflectX(){
        vX = -vX;
    }

    /**
     * Changes the direction of the lateral motion of the particle to its opposite direction.
     */
    void reflectY(){
        vY = -vY;
    }
}
