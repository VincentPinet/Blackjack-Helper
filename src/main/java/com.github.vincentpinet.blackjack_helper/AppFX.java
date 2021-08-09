package com.github.vincentpinet.blackjack_helper;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class AppFX extends Application {

	@Override
	public void start(Stage stage) throws Exception {
		Parent root = FXMLLoader.load(getClass().getResource("/View.fxml"));
		Scene scene = new Scene(root, 1280, 768);
		stage.setResizable(false);
		stage.setScene(scene);
		stage.show();
	}

	public static void main(String[] args) {
		launch();
	}
}
