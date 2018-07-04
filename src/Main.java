import java.util.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javax.swing.*;

/**
 * Created by Catz on 4/12/14.
 *
 * Main handles all GUI component of the simulation
 */
public class Main extends Application implements SimulationLimits{

    private BorderPane root;

    //Buttons for adding more particles
    private Button addParticleLeft, addParticleRight;
    //Fields for user to enter the variables of the simulation
    private TextField desiredHeight, desiredWidth, desiredParticleSize1, desiredParticleSize2;
    //Buttons for user to confirm the variables
    private Button setArea, setPores, setParticleSize;
    //Sliders to change some variables due to the small range of values accepted
    private Slider setSpeed, desiredPores;

    //Radiobuttons for user to choose between adding particle 1 or particle 2 into either of the division
    private RadioButton[] leftParticleChoice, rightParticleChoice;

    // Text to update in thread in controller.
    // countdown is to show user how long more till the direction of movement of all particles are reset
    // noOfParticleXinY - where X represents the particle number and Y represents the division (left:1 or right:2)
    private Text countdown = new Text("");
    private Text noOfParticle1in1 = new Text("");
    private Text noOfParticle1in2 = new Text("");
    private Text noOfParticle2in1 = new Text("");
    private Text noOfParticle2in2 = new Text("");

    //Menubar to display supported languages so that user can choose what language he wishes to view the simulation in
    private MenuBar menuBar;

    //Controller to implement workings of the simulation
    private Controller controller;
    //For fetching of information in different languages
    private Locale currentLocale;
    private ResourceBundle resourceBundle;

    //state 1: prompt for particle size
    //state 2: simulation
    private int state = 1;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) {
        Splash splash = new Splash();

        //SUPPORTED_LOCALE[0] = en_SG, default of simulation is in english
        currentLocale=SUPPORTED_LOCALE[0];
        resourceBundle = ResourceBundle.getBundle("MyResource",currentLocale);

        primaryStage.setTitle(resourceBundle.getString("title"));
        root = new BorderPane();
        Scene scene = new Scene(root);

        /* Setup of Menubar */
        menuBar = new MenuBar();
        Menu languages = new Menu("Select language");
        MenuItem[] supportedLanguages = new MenuItem[SUPPORTED_LOCALE.length];
        for (int i = 0; i < supportedLanguages.length; i++){
            supportedLanguages[i] = new MenuItem(SUPPORTED_LOCALE[i].getDisplayLanguage());
            languages.getItems().add(supportedLanguages[i]);
        }
        menuBar.getMenus().add(languages);
        //if user chooses english
        supportedLanguages[0].setOnAction(actionEvent -> {
            currentLocale = SUPPORTED_LOCALE[0];
            resourceBundle = ResourceBundle.getBundle("MyResource", currentLocale);
            repaintComponents(primaryStage);
        });
        //if user chooses chinese
        supportedLanguages[1].setOnAction(actionEvent -> {
            currentLocale = SUPPORTED_LOCALE[1];
            resourceBundle = ResourceBundle.getBundle("MyResource", currentLocale);
            repaintComponents(primaryStage);
        });

        //setup state 1
        paintPromptSize(primaryStage);

        //final touchups and showing of the frame
        root.setTop(menuBar);
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
        primaryStage.setResizable(false);
        primaryStage.requestFocus();
        primaryStage.toFront();
        primaryStage.show();
        splash.dispose();
    }

    /**
     * Sets up the simulation.
     *
     * @param primaryStage Stage to show simulation in.
     */
    private void setSimulation(final Stage primaryStage){

        //Top pane - change simulation area and number of pores
        VBox top = new VBox();
        top.setSpacing(10);
        top.getChildren().add(menuBar);

        HBox top1 = new HBox();
        top1.setPadding(new Insets(20,20,0,20));
        top1.setSpacing(10);
        top1.getChildren().add(new Text(resourceBundle.getString("simulationArea")+" -"));
        top1.getChildren().add(new Text(resourceBundle.getString("height")+":"));
        desiredHeight = new TextField(""+controller.getHeight());
        top1.getChildren().add(desiredHeight);
        top1.getChildren().add(new Text(resourceBundle.getString("width")+":"));
        desiredWidth = new TextField(""+controller.getWidth());
        top1.getChildren().add(desiredWidth);
        setArea = new Button(resourceBundle.getString("resetAndSetArea"));
        top1.getChildren().add(setArea);
        top.getChildren().add(top1);

        //When user wishes to change the area of simulation
        setArea.setOnAction(actionEvent -> {
            //Check that the area desired by user is within the limits of the simulation.
            //If area entered is accepted, pass to controller to setup a new map
            //If area is not accepted, message popup to tell user of what is wrong with his entry and continuation of previous simulation upon clicking ok
            try {
                if (Integer.parseInt(desiredHeight.getText())>=MIN_HEIGHT && Integer.parseInt(desiredHeight.getText())<=MAX_HEIGHT &&
                        Integer.parseInt(desiredWidth.getText())>=MIN_WIDTH && Integer.parseInt(desiredWidth.getText())<=MAX_WIDTH){
                    root.setCenter(controller.getNewSimulationArea(Integer.parseInt(desiredHeight.getText()),Integer.parseInt(desiredWidth.getText())));
                    primaryStage.sizeToScene();
                } else throw new Exception();
            } catch (RuntimeException e){
                JOptionPane.showMessageDialog(null,resourceBundle.getString("pleaseEnterValidNumbers")+"!");
            } catch (Exception e){
                JOptionPane.showMessageDialog(null,resourceBundle.getString("height")+" - "+resourceBundle.getString("min")+MIN_HEIGHT+" "+resourceBundle.getString("max")
                        +MAX_HEIGHT+"\n"+resourceBundle.getString("width")+" - "+resourceBundle.getString("min")+MIN_WIDTH+" "+resourceBundle.getString("max")+MAX_WIDTH);
            }
        });

        HBox top2 = new HBox();
        top2.setPadding(new Insets(0,20,20,20));
        top2.setSpacing(80);
        top2.getChildren().add(new Text(resourceBundle.getString("numberOfPores")+":"));
        desiredPores = new Slider();
        desiredPores.setMin(MIN_PORES);
        desiredPores.setMax(MAX_PORES);
        desiredPores.setValue(controller.getPores());
        top2.getChildren().add(desiredPores);
        final Text numberOfPores = new Text(Integer.toString((int)desiredPores.getValue()));
        top2.getChildren().add(numberOfPores);
        setPores = new Button(resourceBundle.getString("resetAndSetNumberOfPores"));
        top2.getChildren().add(setPores);
        top.getChildren().add(top2);

        //When user changes the desired pore size - USER HAVE NOT PRESSED SET
        desiredPores.valueProperty().addListener((observableValue, number, number2) -> {
            //Update value displayed by the text beside it
            numberOfPores.setText(String.format("%d",number2.intValue()));
        });

        //When user changes the desired pore size - USER HAVE PRESSED SET
        setPores.setOnAction(actionEvent -> {
            //Pass to controller to set up a new map
            root.setCenter(controller.getNewSimulationArea((int) desiredPores.getValue()));
            primaryStage.sizeToScene();
        });

        root.setTop(top);

        //Left pane - add particles to the left
        VBox left = new VBox();
        left.setPadding(new Insets(20,20,20,20));
        left.setSpacing(10);
        ToggleGroup leftGroup = new ToggleGroup();
        leftParticleChoice = new RadioButton[]{
                new RadioButton(resourceBundle.getString("particle1")),
                new RadioButton(resourceBundle.getString("particle2"))
        };
        leftParticleChoice[0].setToggleGroup(leftGroup);
        leftParticleChoice[0].setSelected(true);
        leftParticleChoice[1].setToggleGroup(leftGroup);
        left.getChildren().add(leftParticleChoice[0]);
        left.getChildren().add(leftParticleChoice[1]);
        addParticleLeft = new Button(resourceBundle.getString("add"));
        left.getChildren().add(addParticleLeft);
        GridPane temp3 = new GridPane();
        temp3.setPadding(new Insets(10,0,10,0));
        temp3.add(new Text(resourceBundle.getString("particle1InLeft")+": "),0,0);
        temp3.add(noOfParticle1in1,1,0);
        temp3.add(new Text(resourceBundle.getString("particle2InLeft")+": "),0,1);
        temp3.add(noOfParticle2in1,1,1);
        left.getChildren().add(temp3);
        root.setLeft(left);

        //When user confirms to add his chosen particle into the left division
        addParticleLeft.setOnAction(actionEvent -> {
            try {
                //Particle 1
                if (leftParticleChoice[0].isSelected()){
                    controller.addParticle(1,-1);
                //Particle 2
                } else if (leftParticleChoice[1].isSelected()){
                    controller.addParticle(2,-1);
                }
            //controller throws an Exception if no more particles can be added due to limits - handled by GUI class since only showing of message is needed
            //simulation is therefore not reset and continues upon clicking ok
            } catch (Exception e){
                controller.pauseSimulation();
                if (e.getMessage().equals("Max Particles")){
                    JOptionPane.showMessageDialog(null,resourceBundle.getString("maxParticles"),resourceBundle.getString("error"),JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null,resourceBundle.getString("noSpace"),resourceBundle.getString("error"),JOptionPane.ERROR_MESSAGE);
                }
                controller.playSimulation();
            }
        });

        //Right pane - add particles to the right
        VBox right = new VBox();
        right.setPadding(new Insets(20,20,20,20));
        right.setSpacing(10);
        ToggleGroup rightGroup = new ToggleGroup();
        rightParticleChoice = new RadioButton[]{
                new RadioButton(resourceBundle.getString("particle1")),
                new RadioButton(resourceBundle.getString("particle2"))
        };
        rightParticleChoice[0].setToggleGroup(rightGroup);
        rightParticleChoice[1].setSelected(true);
        rightParticleChoice[1].setToggleGroup(rightGroup);
        right.getChildren().add(rightParticleChoice[0]);
        right.getChildren().add(rightParticleChoice[1]);
        addParticleRight = new Button(resourceBundle.getString("add"));
        right.getChildren().add(addParticleRight);
        GridPane temp2 = new GridPane();
        temp2.setPadding(new Insets(10,0,10,0));
        temp2.add(new Text(resourceBundle.getString("particle1InRight") + ": "), 0, 0);
        temp2.add(noOfParticle1in2,1,0);
        temp2.add(new Text(resourceBundle.getString("particle2InRight")+": "),0,1);
        temp2.add(noOfParticle2in2,1,1);
        right.getChildren().add(temp2);
        GridPane temp1 = new GridPane();
        temp1.add(new Text(resourceBundle.getString("timeTillNextDirectionReset")+": "),0,0);
        temp1.add(countdown,1,0);
        right.getChildren().add(temp1);
        root.setRight(right);

        //When user confirms to add his chosen particle into the right division
        addParticleRight.setOnAction(actionEvent -> {
            try {
                //Particle 1
                if (rightParticleChoice[0].isSelected()){
                    controller.addParticle(1,1);
                //Particle 2
                } else if (rightParticleChoice[1].isSelected()){
                    controller.addParticle(2,1);
                }
            //controller throws an Exception if no more particles can be added due to limits - handled by GUI class since only showing of message is needed
            //simulation is therefore not reset and continues upon clicking ok
            } catch (Exception e){
                controller.pauseSimulation();
                if (e.getMessage().equals("Max Particles")){
                    JOptionPane.showMessageDialog(null, resourceBundle.getString("maxParticles"),
                            resourceBundle.getString("error"),JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, resourceBundle.getString("noSpace"),
                            resourceBundle.getString("error"),JOptionPane.ERROR_MESSAGE);
                }
                controller.playSimulation();
            }
        });

        //Bottom pane - set speed of particles
        HBox bottom = new HBox();
        bottom.setPadding(new Insets(20,20,20,20));
        bottom.setSpacing(50);
        bottom.setAlignment(Pos.CENTER);
        bottom.getChildren().add(new Text(resourceBundle.getString("speedOfParticles")+":"));
        setSpeed = new Slider();
        setSpeed.setMin(MIN_SPEED);
        setSpeed.setMax(MAX_SPEED);
        setSpeed.setValue(controller.getSpeed());
        bottom.getChildren().add(setSpeed);
        final Text particleSpeed = new Text(String.format("%.1f",setSpeed.getValue()));
        bottom.getChildren().add(particleSpeed);
        root.setBottom(bottom);

        //When slider value is changed and user intends to change the speed of particles
        setSpeed.valueProperty().addListener((observableValue, number, number2) -> {
            //Update both the showing of the speed and the speed variable in the controller
            controller.setSpeed(number2.doubleValue());
            particleSpeed.setText(String.format("%.1f",number2.doubleValue()));
        });
        root.setCenter(controller.getSimulationArea());
        primaryStage.sizeToScene();
    }

    /**
     * Sets up the prompt for particle size.
     *
     * @param primaryStage Stage to show the prompt in.
     */
    private void paintPromptSize(final Stage primaryStage){
        final GridPane pane = new GridPane();
        pane.setPadding(new Insets(30, 30, 30, 30));
        pane.setVgap(15);
        pane.setHgap(10);
        pane.add(new Text(resourceBundle.getString("promptSize")+": ("+resourceBundle.getString("min") +
                MIN_SIZE + " "+resourceBundle.getString("max") + MAX_SIZE + ")"), 0, 0, 2, 1);
        pane.add(new Text(resourceBundle.getString("particle1Size")+":"), 0, 2);
        desiredParticleSize1 = new TextField();
        pane.add(desiredParticleSize1,1,2);
        pane.add(new Text(resourceBundle.getString("particle2Size")+":"),0,3);
        desiredParticleSize2 = new TextField();
        pane.add(desiredParticleSize2,1,3);
        final Text error = new Text("");
        pane.add(error,0,4);
        HBox temp = new HBox();
        temp.setAlignment(Pos.BASELINE_RIGHT);
        setParticleSize = new Button(resourceBundle.getString("setSizes"));
        temp.getChildren().add(setParticleSize);
        pane.add(temp,1,4);
        root.setCenter(pane);

        //When user confirms the particle sizes
        setParticleSize.setOnAction(actionEvent -> {
            //Check whether the particle sizes are within the acceptable range
            //If particle sizes are acceptable, set up simulation controller with required information and change the state of the app into state 2
            //If particle sizes are not valid, error message is shown on the frame and nothing happens (User is expected to change the values and confirm again)
            try {
                if (Integer.parseInt(desiredParticleSize1.getText()) >= MIN_SIZE && Integer.parseInt(desiredParticleSize1.getText()) <= MAX_SIZE &&
                        Integer.parseInt(desiredParticleSize2.getText()) >= MIN_SIZE && Integer.parseInt(desiredParticleSize2.getText()) <= MAX_SIZE) {
                    controller = new Controller(Integer.parseInt(desiredParticleSize1.getText()), Integer.parseInt(desiredParticleSize2.getText()));
                    controller.setToUpdate(countdown, noOfParticle1in1, noOfParticle2in1, noOfParticle1in2, noOfParticle2in2);
                    controller.initializeSimulationArea();
                    setSimulation(primaryStage);
                    state = 2;

                } else throw new Exception();
            } catch (RuntimeException e) {
                error.setText(resourceBundle.getString("pleaseEnterTheSizes"));
                error.setFill(Color.rgb(new Random().nextInt(256), new Random().nextInt(256), new Random().nextInt(256)));
            } catch (Exception e) {
                error.setText(resourceBundle.getString("invalidSizes"));
                error.setFill(Color.rgb(new Random().nextInt(256), new Random().nextInt(256), new Random().nextInt(256)));
            }
        });

        primaryStage.sizeToScene();
        //When user closes application, call function in controller that stops necessary content to ensure complete closure of application
        primaryStage.setOnCloseRequest(windowEvent -> {
            if (controller != null){
                controller.clearUp();
            }
        });
    }

    /**
     * Resets the GUI shown.
     *
     * @param primaryStage Stage to reset.
     */
    private void repaintComponents(Stage primaryStage){

        if (state == 1){
            paintPromptSize(primaryStage);
        } else {
            if (controller != null) {
                controller.pauseSimulation();
            }

            setSimulation(primaryStage);

            if (controller != null) {
                controller.playSimulation();
            }
        }
    }
}
