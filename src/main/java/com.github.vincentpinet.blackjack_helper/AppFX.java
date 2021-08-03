package com.github.vincentpinet.blackjack_helper;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;

public class AppFX extends Application {

	@Override
	public void start(Stage stage) throws Exception {
		Scene scene = new Scene(new BorderPane(), 1024, 768);
		stage.setScene(scene);
		stage.show();
	}

	public static void main(String[] args) {
		launch();
	}
}
