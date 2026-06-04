package ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ui.controller.MainController;

import java.net.URL;
import java.nio.file.Path;

public class TravelRouteApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        URL fxmlUrl = Path.of("resources", "fxml", "main-view.fxml").toUri().toURL();
        loader.setLocation(fxmlUrl);

        Scene scene = new Scene(loader.load(), 1400, 900);
        scene.getStylesheets().add(Path.of("resources", "styles", "app.css").toUri().toString());

        MainController controller = loader.getController();
        controller.setHostStage(primaryStage);

        primaryStage.setTitle("Akıllı Gezi Rota Planlama Uygulaması");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1300);
        primaryStage.setMinHeight(820);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
