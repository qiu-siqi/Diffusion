import javafx.scene.paint.Color;

import java.util.Locale;

/**
 * Created by Catz on 4/12/14.
 *
 * SimulationLimits is an interface that solely stores constants that determines what the simulation is able to handle as well as
 * general data that does not/will not/need not differ throughout the implementation and development of the simulation and project.
 */
public interface SimulationLimits {
    int MAX_PORES =  10;
    int MAX_PARTICLES = 100;
    int MAX_SIZE = 20;
    double MAX_SPEED =  10;
    int MAX_HEIGHT = 600;
    int MAX_WIDTH = 1200;

    int MIN_PORES = 0;
    int MIN_SIZE = 1;
    double MIN_SPEED = 0.1;
    int MIN_HEIGHT = 100;
    int MIN_WIDTH = 200;

    int RESET_DELAY = 101;
    int RESET_RATE = 50;

    Color PARTICLE_COLOR_1 = Color.RED;
    Color PARTICLE_COLOR_2 = Color.BLUE;

    Locale[] SUPPORTED_LOCALE = {new Locale("en","SG"),new Locale("zh","CN")};
}
