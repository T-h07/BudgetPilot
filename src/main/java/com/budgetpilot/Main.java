package com.budgetpilot;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.ui.MainLayout;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;
import java.time.YearMonth;

public class Main extends Application {
    private static final double DEFAULT_WIDTH = 1280;
    private static final double DEFAULT_HEIGHT = 800;

    @Override
    public void start(Stage stage) {
        AppContext appContext = new AppContext("Taulanth H", YearMonth.now());
        MainLayout mainLayout = new MainLayout(appContext);

        Scene scene = new Scene(mainLayout, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        URL stylesheet = Main.class.getResource("/css/app.css");
        if (stylesheet == null) {
            throw new IllegalStateException("Missing stylesheet: /css/app.css");
        }
        scene.getStylesheets().add(stylesheet.toExternalForm());

        stage.setTitle("BudgetPilot");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
