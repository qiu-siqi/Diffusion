import javafx.scene.paint.Color;

/**
 * Created by Catz on 4/12/14.
 *
 * Atom is a Particle defined by its lack of charge
 */
public class Atom extends Particle {

    Atom(Color color, int size){
        setRadius(size);
        setFill(color);
    }

    /**
     * Handles collision with particles.
     *
     * @param particle Particle that was collided with.
     */
    @Override
    public void collidedWith(Particle particle) {
        if (getLastCollide() != particle ){
            if (particle instanceof Atom){
                collidedWith((Atom)particle);
            }
        }
    }

    /**
     * Handles collision with atoms.
     *
     * @param atom Atom that was collided with.
     */
    private void collidedWith(Atom atom){
        reflectBoth();
        setLastCollide(atom);
    }

    /**
     * Updates the position of the atom.
     *
     * @param multiplier Speed of simulation.
     */
    @Override
    public void update(double multiplier) {
        setTranslateX(getTranslateX()+getVX()*multiplier);
        setTranslateY(getTranslateY()+getVY()*multiplier);
    }

    /**
     * Handles close contact with particles.
     *
     * @param particle Particle that is close to this one.
     */
    @Override
    public void isCloseTo(Particle particle) {
        //Do nothing
    }
}
