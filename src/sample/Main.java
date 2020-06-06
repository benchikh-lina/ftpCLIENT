package sample;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class Main  extends Application {

    Stage window;



    @Override
    public void start(Stage stage) throws Exception {
        window = stage;
        Parent root1 =FXMLLoader.load(getClass().getResource("homePage.fxml"));
        window.setScene(new Scene(root1));
        window.show();

        FadeTransition fadeTransitionOUT=new FadeTransition(Duration.seconds(1),root1);

        fadeTransitionOUT.play();


        fadeTransitionOUT.setOnFinished((e) -> {
            try {
                Parent root2 =FXMLLoader.load(getClass().getResource("sample.fxml"));
                stage.setTitle("FTP");
                stage.setScene(new Scene(root2));
                stage.show();



            } catch (IOException ex) {
                ex.printStackTrace();
            }


        });



    }

    public static void main(String[] args){
        launch(args);
    }

}
