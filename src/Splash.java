import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Catz on 4/12/14.
 *
 * This class forms the startup screen of the application.
 */
class Splash extends Frame {

    Splash(){
        try {
            InputStream is = getClass().getResourceAsStream("SplashScreen.png");
            Image image = ImageIO.read(is);

            add(new JLabel(new ImageIcon(image)));
            setUndecorated(true);
            setVisible(true);
            pack();
            setLocationRelativeTo(null);
        } catch (IOException e){
            e.printStackTrace();
        }

        try {
            Thread.sleep(3000);
        }
        catch(InterruptedException ignored) {
        }
    }
}
