import javafx.scene.layout.StackPane;

/**
 * Created by Catz on 4/12/14.
 *
 * Represents the area containing the particles and other components.
 */
class SimulationArea extends StackPane {

    SimulationArea(int height, int width){
        setPrefHeight(height);
        setPrefWidth(width);
        setMinHeight(height);
        setMaxHeight(height);
        setMinWidth(width);
        setMaxWidth(width);

        setStyle("-fx-border-color: black; -fx-border-width:2px;");
    }
}
