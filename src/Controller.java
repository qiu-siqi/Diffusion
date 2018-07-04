import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javax.swing.*;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Catz on 4/13/14.
 *
 * Contains methods that handles all events that are related to changes in the simulation.
 */
class Controller implements SimulationLimits {

    //Determines the size of the pore. this is NOT a limit of the simulation (despite being static final)
    // - only arise due to an incomplete implementation of the simulation/controller
    //In future works, pore size can be varied too
    private static final int poreHeight = 30;

    //Necessary information of the simulation
    private SimulationArea area;
    private int height = 200;
    private int width = 400;
    private int pores = 3;
    private double speed = 1;
    private int particle1Size;
    private int particle2Size;
    private int[] noOfEachParticle;

    //To store all components of the simulation
    private ObjectManager<Particle> allParticles;
    private ObjectManager<Block> allBlocks;

    //Reference to GUI components which are to be updated at fixed intervals
    private Text countdownToUpdate;
    private Text particle1InLeft, particle1InRight, particle2InLeft, particle2InRight;

    //For algorithm - updating of objects directly involved in the simulation/diffusion
    private Timeline timeline;

    //For threading - updating of UI components that enhances viewing of simulation
    private ScheduledExecutorService executorService;

    Controller(int size1, int size2){
        area = new SimulationArea(height,width);
        particle1Size = size1;
        particle2Size = size2;
        allParticles = new ObjectManager<>();
        allBlocks = new ObjectManager<>();
        noOfEachParticle = new int[2];
    }

    /**
     * Stores references of the UI components which needs to be updated under the threads.
     * This method is called before initialization of the simulation area.
     *
     * @param countdown Text field to show the countdown till direction reset.
     * @param left1 Text field to show percentage of particle 1 in the left division.
     * @param left2 Text field to show percentage of particle 2 in the left division.
     * @param right1 Text field to show percentage of particle 1 in the right division.
     * @param right2 Text field to show percentage of particle 2 in the right division.
     */
    void setToUpdate(Text countdown, Text left1, Text left2, Text right1, Text right2){
        countdownToUpdate=countdown;
        particle1InLeft = left1;
        particle1InRight = right1;
        particle2InLeft = left2;
        particle2InRight = right2;
    }

    /**
     * Clears all information belonging to the previous simulation.
     */
    private void resetComponents(){
        area.getChildren().clear();
        if (executorService!= null){
            executorService.shutdown();
        }
        allParticles.clearAll();
        allBlocks.clearAll();
        noOfEachParticle[0] = 0;
        noOfEachParticle[1] = 0;
    }

    /**
     * Stops all ongoing threads/methods/algorithms which runs in the background.
     * This method is to be called when the user closes the application.
     */
    void clearUp(){
        if (executorService!= null){
            executorService.shutdown();
        }
    }

    /**
     * Sets up a new simulation.
     */
    void initializeSimulationArea(){
        //Clear previous data that is not required for the new simulation
        resetComponents();

        //Add blocks to the new simulation to create the user's desired pore number
        //If user wishes for there to be no pores, add a block which covers the whole of the mid-region
        if (pores == 0){
            Block temp = new Block(height);
            area.getChildren().add(temp);
            allBlocks.add(temp);
        //If user desires a single pore, add two blocks to the extremes which leaves a space between them
        } else if (pores == 1){
            Block temp = new Block((height-poreHeight)/2);
            temp.setTranslateY(height/2-(height-poreHeight)/4);
            Block temp2 = new Block((height-poreHeight)/2);
            temp2.setTranslateY(-height/2+(height-poreHeight)/4);
            area.getChildren().addAll(temp,temp2);
            allBlocks.add(temp);
            allBlocks.add(temp2);
        //If user desires more than one pore, make necessary calculations of block height and their position such that the two extremes are pores
        //and any remaining desired pores are formed between two subsequent blocks
        } else {
            double blockHeight = (height-pores*poreHeight)/(pores-1);
            for (int i = 1; i < pores; i++){
                Block temp = new Block(blockHeight);
                temp.setTranslateY(0-height/2+blockHeight/2+poreHeight*i+blockHeight*(i-1));
                area.getChildren().add(temp);
                allBlocks.add(temp);
            }
        }

        //Initializes and starts a nonstop Timeline to animate particles involved in the simulation
        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        //Defines a keyframe which makes necessary checks and updates the particles in 20 milliseconds time via an EventHandler
        //There is no keyvalue - simulation is not supposed to work towards any target value - diffusion is purely due to randomized motion
        KeyFrame keyFrame = new KeyFrame(Duration.millis(20), actionEvent -> {
            updateParticles();
            checkCollisions();
        });
        timeline.getKeyFrames().add(keyFrame);
        timeline.play();

        //Initializes a threading service which allows two threads to run concurrently
        //Start threads which are set up with relevant references to handle and transfer required data to the GUI to be viewable by users
        //The run method of these threads are invoked immediately upon run and subsequently at 1s intervals
        executorService = Executors.newScheduledThreadPool(2);
        executorService.scheduleWithFixedDelay(new ResetAllDirectionThread(allParticles, countdownToUpdate), 0, 1, TimeUnit.SECONDS);
        executorService.scheduleWithFixedDelay(new UpdateConcentrationThread(allParticles, particle1InLeft,
                particle1InRight, particle2InLeft, particle2InRight), 0, 1, TimeUnit.SECONDS);
    }

    //

    /**
     * Adds a new particle.
     *
     * @param particleNumber Particle number (1 or 2).
     * @param hint Left (-1) or Right (1).
     * @throws Exception if simulation reached the maximum number of particles.
     */
    void addParticle(int particleNumber, int hint) throws Exception {
        //If simulation reached the maximum number of particles, throw an Exception to be handled by the GUI class through showing user error message
        if (allParticles.getAll().size()>=MAX_PARTICLES){
            throw new Exception("Max Particles");
        //Simulation is not at its maximum number of particles
        } else {
            Atom newAtom;
            int translateX, translateY;
            //Create an atom object with particle 1's definitions
            if (particleNumber == 1){
                newAtom = new Atom(PARTICLE_COLOR_1,particle1Size);
            //Create an atom object with particle 2's definitions
            } else {
                newAtom = new Atom(PARTICLE_COLOR_2,particle2Size);
            }
            //Obtain random positions to place the new atom and check that the position is not occupied and is not too close to any other particles
            //Repeat 4 times if constantly unsuccessful. If still unsuccessful, the particle will not be added since the simulation area is too crowded
            boolean check = false;
            for (int i = 0; i < 5; i++){
                //Get random positions for the new atom at the side where it is being added into
                translateX = hint * (new Random().nextInt(width/2-2*(int)newAtom.getRadius())+(int)newAtom.getRadius());
                if (new Random().nextInt(2)==0){
                    translateY = -1 * (new Random().nextInt(height/2-2*(int)newAtom.getRadius())+(int)newAtom.getRadius());
                } else {
                    translateY = new Random().nextInt(height/2-2*(int)newAtom.getRadius())+(int)newAtom.getRadius();
                }

                //Set the atom's position properties to the random values
                newAtom.setTranslateY(translateY);
                newAtom.setTranslateX(translateX);

                //Check that this position is not close to any other existing particles
                check = true;
                for (int j =0 ; j < allParticles.getNumber();j++){
                    //if it is close to a particle, set check to false and proceed to regenerate a new position
                    if (newAtom.closeTo(allParticles.getAll().get(j))){
                        check = false;
                        break;
                    }
                }
                //check = true means that the position is not close to any other particles
                // - there is no need for new values - break out of the generation loop
                if (check){
                    break;
                }
            }
            //If a position is accepted, the new atom located at the position is added to the simulation area
            // and values and storage of all particles are updated
            if (check){
                area.getChildren().add(newAtom);
                allParticles.add(newAtom);
                if (particleNumber == 1){
                    noOfEachParticle[0]++;
                } else {
                    noOfEachParticle[1]++;
                }
            //If none of the 5 positions generated are accepted, throw an Exception to be handled by the GUI class through showing user error message
            } else {
                throw new Exception("No space");
            }
        }
    }

    /**
     * Changes the height and width and fetches the new {@code SimulationArea}.
     *
     * @param height New height.
     * @param width New width.
     * @return new area if successful and existing area if not.
     */
    SimulationArea getNewSimulationArea(int height, int width){
        //If more than one pore is desired, check that the new values of height and width is able to handle the previously set pore number
        if (pores != 1 && pores != 0){
            double blockHeight = (height-pores*poreHeight)/(pores-1);
            if (blockHeight <= 0){ //means that pores*poreHeight is bigger than height thus the newly desired height is invalid
                JOptionPane.showMessageDialog(null,
                        "Height of simulation area is too small to contain so many pores!", "Error",JOptionPane.ERROR_MESSAGE);
            } else { //if height is valid, change properties as per recorded by the controller and set up the new area
                this.height = height;
                this.width = width;
                area = new SimulationArea(height,width);
                initializeSimulationArea();
            }
        //If one or less pore is desired, all values for height and width is able to handle pore number and no checking is required
        } else { //set up the new area
            this.height = height;
            this.width = width;
            area = new SimulationArea(height,width);
            initializeSimulationArea();
        }
        //after setting up, return the newly set up area to be added to the GUI
        return getSimulationArea();
    }

    /**
     * Changes the number of pores and fetches the new {@code SimulationArea}.
     *
     * @param pores New number of pores.
     * @return new area if successful and existing area if not.
     */
    SimulationArea getNewSimulationArea(int pores){
        //If more than one pore is desired, check that the existing values of height and width is able to handle the new desired pore number
        if (pores != 1 && pores != 0){
            double blockHeight = (height-pores*poreHeight)/(pores-1);
            if (blockHeight <= 0){ //means that pores*poreHeight is bigger than height thus the newly desired pore is too large for the simulation area
                JOptionPane.showMessageDialog(null,
                        "Height of simulation area is too small to contain so many pores!","Error",JOptionPane.ERROR_MESSAGE);
            } else { //if pore number is acceptable, change pore number recorded by the controller and set up the new area
                this.pores = pores;
                initializeSimulationArea();
            }
        //If one or less pore is desired, all values for height and width is able to handle pore number and no checking is required
        } else { //set up the new area
            this.pores = pores;
            initializeSimulationArea();
        }
        return getSimulationArea();
    }

    /**
     * Update the positions of all particles in the simulation depending on the speed set
     * (speed multiplies the number of units that the particle is supposed to translate)
     * Also updates the countdown value for the min.time needed for collision with last collided particle
     */
    private void updateParticles(){
        for (int i =0; i < allParticles.getNumber();i++){
            allParticles.getAll().get(i).update(speed);
            allParticles.getAll().get(i).passedTime();
        }
    }

    /**
     * Checks collisions of all particles in the simulation with other components in the simulation.
     */
    private void checkCollisions(){
        //Check collisions between particles
        for (int i = 0; i < allParticles.getNumber();i++){
            for (int j = 0; j < allParticles.getNumber();j++){
                if (i==j) continue; //Particle cannot collide with itself!
                if (allParticles.getAll().get(i).collide(allParticles.getAll().get(j))){
                    allParticles.getAll().get(i).collidedWith(allParticles.getAll().get(j));
                }
            }
        }

        //Check collisions between particle and boundary of simulation area
        for (int i =0; i < allParticles.getNumber();i++){
            //if the X property of the particle + its radius is over the X property of the right boundary, the particle collided with the right boundary
            //if the particle is still moving to the right, flip its X translation per time period to go towards the left instead
            //also, if the X property of the particle - its radius is smaller than the X property of the left boundary, the particle collided with the left boundary
            //if the particle is still moving to the left, flip its X translation per time period to go towards the right instead
            if ((allParticles.getAll().get(i).getTranslateX() >= width/2-allParticles.getAll().get(i).getRadius() &&
                    allParticles.getAll().get(i).getVX()>0)||
                    (allParticles.getAll().get(i).getTranslateX() <= -width/2+allParticles.getAll().get(i).getRadius())&&
                            allParticles.getAll().get(i).getVX()<0){
                allParticles.getAll().get(i).reflectX();
            }
            //if the Y property of the particle + its radius is over the Y property of the top boundary, the particle collided with the top boundary
            //if the particle is still moving upwards, flip its Y translation per time period to go downwards instead
            //also, if the Y property of the particle - its radius is smaller than the Y property of the bottom boundary, the particle collided with the bottom boundary
            //if the particle is still moving downwards, flip its Y translation per time period to go upwards instead
            if ((allParticles.getAll().get(i).getTranslateY() >= height/2-allParticles.getAll().get(i).getRadius()&&
                    allParticles.getAll().get(i).getVY()>0)||
                    (allParticles.getAll().get(i).getTranslateY() <= -height/2+allParticles.getAll().get(i).getRadius())&&
                            allParticles.getAll().get(i).getVY()<0){
                allParticles.getAll().get(i).reflectY();
            }
        }

        double blockHeight;
        //Obtain height of each block based on the number of pores and formulas derived previously
        if (pores == 0){
            blockHeight = height;
        } else if (pores == 1){
            blockHeight = (height-poreHeight)/2;
        } else {
            blockHeight = (height-pores*poreHeight)/(pores-1);
        }

        //Check collision between particles and blocks in the equator of the simulation area
        for (int i = 0; i < allParticles.getNumber();i++){
            //Check if the particle is within the mid portion such that the particle's border is able to touch/intersect a block (with fixed width 10)
            //that exists in the same Y coordinate as the particle
            if (allParticles.getAll().get(i).getTranslateX()>=-5-allParticles.getAll().get(i).getRadius() &&
                    allParticles.getAll().get(i).getTranslateX()<=5+allParticles.getAll().get(i).getRadius()){
                //particle is within mid portion
                //Check if there is a block that is situated such that the particle/its borders is touching/intersecting the block
                //In other words, check that the particle is not between a pore in the mid portion
                for (int j = 0; j < allBlocks.getNumber();j++){
                    if (allParticles.getAll().get(i).getTranslateY() <
                            allBlocks.getAll().get(j).getTranslateY()+blockHeight/2+allParticles.getAll().get(i).getRadius() &&
                            allParticles.getAll().get(i).getTranslateY() >
                                    allBlocks.getAll().get(j).getTranslateY()-blockHeight/2-allParticles.getAll().get(i).getRadius()){
                        //particle collides with a block
                        //if the particle is within the left division and is traveling to the right, it bounces off the block and travels back towards the left
                        if (allParticles.getAll().get(i).getTranslateX()<0){
                            if (allParticles.getAll().get(i).getVX()>0){
                                allParticles.getAll().get(i).reflectX();
                            }
                        //if the particle is within the right division and is travelling to the left, it bounces off the block and travels back towards the right
                        } else {
                            if (allParticles.getAll().get(i).getVX()<0){
                                allParticles.getAll().get(i).reflectX();
                            }
                        }
                        //if the particle is located very close to the equator and is colliding with the block, it is assumed that it is right above/below the block
                        //since it would have been reflected away otherwise (it cannot exist between the blocks)
                        //as such, it collides with the top/bottom boundary of the block and thus bounces off with the inverse of its Y translation
                        if (allParticles.getAll().get(i).getTranslateX()>-5&& allParticles.getAll().get(i).getTranslateX()<5){
                            allParticles.getAll().get(i).reflectY();
                        }
                        break;
                    }
                }
            }
        }
    }

    void setSpeed(double speed){
        this.speed = speed;
    }

    SimulationArea getSimulationArea(){
        return area;
    }

    int getHeight(){
        return height;
    }

    int getWidth(){
        return width;
    }

    int getPores(){
        return pores;
    }

    double getSpeed(){
        return speed;
    }

    /**
     * Stops the movement of particles temporarily.
     */
    void pauseSimulation(){
        timeline.pause();
    }

    /**
     * Starts the movement of particles after stopping it.
     */
    void playSimulation(){
        timeline.play();
    }

    /**
     * Runnable which implements a countdown system and gives all existing particles new translation values whenever the count reaches 0.
     */
    public class ResetAllDirectionThread implements Runnable,SimulationLimits{
        private ObjectManager<Particle> target;
        private int countdown;
        private Text toUpdateCountdown;

        /**
         * Constructs a new instance.
         *
         * @param o ObjectManager with all the particles.
         * @param t Text to display countdown/time left.
         */
        ResetAllDirectionThread(ObjectManager<Particle> o, Text t){
            target = o;
            countdown = RESET_DELAY;
            toUpdateCountdown = t;
        }

        @Override
        public void run() {
            countdown--;
            if (countdown==0){
                for (int i = 0; i < target.getNumber();i++){
                    target.getAll().get(i).setDirection();
                }
                countdown=RESET_RATE;
            }
            toUpdateCountdown.setText(countdown+"");
        }
    }

    /**
     * Runnable which updates the concentration of each particle in each division every time the run method is invoked.
     */
    public class UpdateConcentrationThread implements Runnable{

        private ObjectManager<Particle> allParticles;
        private Text left1, right1;
        private Text left2, right2;

        /**
         * Constructs a new instance.
         *
         * @param o ObjectManager with all the particles.
         * @param left1 Text to display percentage of particle 1 in the left division.
         * @param right1 Text to display percentage of particle 1 in the right division.
         * @param left2 Text to display percentage of particle 2 in the left division.
         * @param right2 Text to display percentage of particle 2 in the right division.
         */
        UpdateConcentrationThread(ObjectManager<Particle> o, Text left1, Text right1, Text left2, Text right2){
            allParticles = o;
            this.left1=left1;
            this.left2=left2;
            this.right1=right1;
            this.right2=right2;
        }
        @Override
        public void run() {
            int l1=0,r1=0,l2=0,r2=0;
            //Obtain the total number of each particles in each division
            for (int i =0 ; i < allParticles.getNumber();i++){
                //Since color of particle is unique to the particle number, color is used to check for whether the particle belongs to grp 1 or 2
                if (allParticles.getAll().get(i).getFill()==PARTICLE_COLOR_1){
                    if (allParticles.getAll().get(i).getTranslateX()>=0){ // check if particle is at right
                        r1++;
                    } else { // left
                        l1++;
                    }
                } else if (allParticles.getAll().get(i).getFill()==PARTICLE_COLOR_2){
                    if (allParticles.getAll().get(i).getTranslateX()>=0){ //check if particle is at right
                        r2++;
                    } else { // left
                        l2++;
                    }
                }
            }
            //Project concentration in % of each particle in each division (left and right) onto the Text objects
            //If none of a certain particle exists, conc of both divisions will be shown as 0%
            //These Text objects are part of the GUI implemented in Main, therefore changes to the values will be shown in the GUI
            setPercentageText(l1, r1, left1, right1);
            setPercentageText(l2, r2, left2, right2);
        }

        private void setPercentageText(int l1, int r1, Text left1, Text right1) {
            if (l1 + r1 == 0){
                left1.setText("0%");
                right1.setText("0%");
            } else {
                left1.setText(String.format("%.2f%%",(double)l1/(l1+r1)*100));
                right1.setText(String.format("%.2f%%",(double)r1/(l1+r1)*100));
            }
        }
    }
}
