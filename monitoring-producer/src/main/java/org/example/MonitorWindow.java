package org.example;

import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class MonitorWindow extends Application {
    @Override
    public void start(Stage stage){

        Label tempTitle = new Label("Température");
        Label loadTitle = new Label("Charge CPU");
        Label tempValue = new Label("--");
        Label loadValue = new Label("--");
        ProgressBar tempBar = new ProgressBar();
        ProgressBar loadBar = new ProgressBar();
        Button buttonStop = new Button("Stop Engine");
        Button buttonStart = new Button("Start Engine");
        JsonFileManager jsonFileManager = new JsonFileManager("data/metrics.json");
        OshiEngine engine = new OshiEngine();



        GridPane grid = new GridPane();
        grid.setHgap(50);
        grid.setVgap(20);
        loadBar.setMinWidth(150);
        tempBar.setMinWidth(150);
        buttonStop.setOnAction(actionEvent -> {
            engine.stop();
        });
        buttonStart.setOnAction(actionEvent -> {
            engine.start();
        });

        grid.add(tempTitle, 0, 0);
        grid.add(loadTitle,1, 0);
        grid.add(tempValue, 0, 1);
        grid.add(loadValue, 1, 1);
        grid.add(tempBar,0, 2 );
        grid.add(loadBar, 1, 2);
        grid.add(buttonStop,1, 3 );
        grid.add(buttonStart, 0, 3);
        GridPane.setHalignment(tempTitle, HPos.CENTER);
        GridPane.setHalignment(tempValue, HPos.CENTER);
        GridPane.setHalignment(tempBar, HPos.CENTER);
        GridPane.setHalignment(loadTitle, HPos.CENTER);
        GridPane.setHalignment(loadValue, HPos.CENTER);
        GridPane.setHalignment(loadBar, HPos.CENTER);

        engine.addListener(metric -> {javafx.application.Platform.runLater(()-> {
            tempValue.setText(String.format("%.1f°C", metric.cpu().temperature()));
            loadValue.setText(String.format("%.1f %%", metric.cpu().cpuLoad() * 100));
            tempBar.setProgress(metric.cpu().temperature() / 100);
            loadBar.setProgress(metric.cpu().cpuLoad());

            if (metric.cpu().isOverheating()){
                    tempValue.setTextFill(Color.RED);
                    tempBar.setStyle("-fx-accent: red;");
            }else{
                tempValue.setTextFill(Color.GREEN);
                tempBar.setStyle("-fx-accent: green;");
            }
            if (metric.cpu().isOverloaded()){
                loadValue.setTextFill(Color.RED);
                loadBar.setStyle("-fx-accent: red;");
            }else {
                loadValue.setTextFill(Color.GREEN);
                loadBar.setStyle("-fx-accent: green;");
            }
        });
        });

        engine.start();

        stage.setOnCloseRequest(event -> engine.stop());

        Scene scene = new Scene(grid, 400, 300);
        stage.setTitle("Moniteur CPU");
        stage.setScene(scene);
        stage.show();
    }
}
