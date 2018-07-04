import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Created by Catz on 4/12/14.
 *
 * Represents an obstacle existing between the diffusion sites which creates pores between such objects or the border.
 */
/*
*/
class Block extends Rectangle {
    private static final int width = 10;
    private static final Color color = Color.FORESTGREEN;

    Block(double height){
        setFill(color);
        setHeight(height);
        setWidth(width);
    }
}
