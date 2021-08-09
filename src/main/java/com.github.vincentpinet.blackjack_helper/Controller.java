package com.github.vincentpinet.blackjack_helper;

import java.util.Map;
import java.util.ArrayList;

import javafx.geometry.Insets;

import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Button;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import javafx.scene.input.Dragboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.TransferMode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.DragEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

public class Controller {

	private Solver solver;
	private Cards player, deck;
	private int dealer;

	@FXML
	private Button shuffleButton, nextButton;
	@FXML
	private TextFlow console;
	@FXML
	private StackPane playerZone, dealerZone;
	@FXML
	private ArrayList<Rectangle> deckCards;
	@FXML
	private ArrayList<Text> deckCounters;

	public Controller() {
		solver = new Solver();
		player = new Cards();
		deck = new Cards(8);
		dealer = 0;
	}

	@FXML
	public void initialize() {

		int _rank = 1;
		for (Rectangle card : deckCards) {
			final String rank = "" + _rank;
			card.setOnDragDetected(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					if (deck.contains(Integer.parseInt(rank)) == 0) return;
					Dragboard db = card.startDragAndDrop(TransferMode.ANY);
					ClipboardContent cc = new ClipboardContent();
					cc.putString(rank);
					db.setContent(cc);
					event.consume();
				}
			});
			_rank++;
		}
		clear();
		updateCounters();
	}

	@FXML
	private void handleOver(DragEvent event) {
		int card = Integer.parseInt(event.getDragboard().getString());

		player.add(card);
		if (player.sum() <= 21)
			event.acceptTransferModes(TransferMode.ANY);
		player.remove(card);

		event.consume();
	}

	@FXML
	private void handleOverDealer(DragEvent event) {
		if (dealer == 0)
			event.acceptTransferModes(TransferMode.ANY);
		event.consume();
	}

	@FXML
	public void handleDrop(DragEvent event) {
		int rank = Integer.parseInt(event.getDragboard().getString());

		Parent card = cardBuilder(rank);
		StackPane.setMargin(card, new Insets(0, 0, 24 * player.size(), 32 * player.size()));
		Button button = (Button) card.lookup(".button-small");
		button.setOnAction( e -> { playerZone.getChildren().remove(card); deck.draw(player, rank); updateCounters(); });
		playerZone.getChildren().add(card);

		player.draw(deck, rank);
		updateCounters();
		if (player.size() >= 2 && dealer != 0) computeActions();

		event.setDropCompleted(true);
		event.consume();
	}

	@FXML
	public void handleDropDealer(DragEvent event) {
		int rank = Integer.parseInt(event.getDragboard().getString());

		if (dealer == rank) return;

		Parent card = cardBuilder(rank);
		Button button = (Button) card.lookup(".button-small");
		button.setOnAction( e -> { dealerZone.getChildren().remove(card); deck.add(rank); dealer = 0; updateCounters();});
		dealerZone.getChildren().add(card);

		dealer = rank;
		deck.remove(rank);
		updateCounters();
		if (player.size() >= 2 && dealer != 0) computeActions();

		event.setDropCompleted(true);
		event.consume();
	}

	@FXML
	private void next(ActionEvent event) {
		clear();
		// console.getScene().getRoot().setCursor(Cursor.WAIT);
		double ev = solver.compute_ev(deck);

		Text text = new Text("EV : " + String.format("%+.4f", ev * 100) + "%\n\n");
		if (ev > 0)
			text.setStyle("-fx-fill: rgba(96, 255, 96, 1);");
		else
			text.setStyle("-fx-fill: rgba(255, 192, 0, 1);");

		console.getChildren().add(text);
		// console.getScene().getRoot().setCursor(Cursor.DEFAULT);
	}

	@FXML
	private void shuffle(ActionEvent event) {
		clear();
		deck = new Cards(8);
		updateCounters();
	}

	private Parent cardBuilder(int rank) {
		try {
			Parent res = FXMLLoader.load(getClass().getResource("/Card.fxml"));
			Text text = (Text) res.lookup(".char");
			text.setText(rank==1 ? "A" : rank==10 ? "T" : ""+rank);
			return res;
		} catch (Exception e) {
			return null;
		}
	}

	private void updateCounters() {
		for (int rank = 1; rank <= 10; rank++)
			deckCounters.get(rank - 1).setText("" + deck.contains(rank));
	}

	private void clear() {
		player = new Cards();
		dealer = 0;
		playerZone.getChildren().clear();
		dealerZone.getChildren().clear();
		console.getChildren().clear();
		console.getChildren().add(new Text(">_\n"));
	}

	private void computeActions() {
		Map<String, Double> actions = solver.getAns(player, dealer, deck);
		ArrayList< Map.Entry<String, Double> > sorted = new ArrayList<Map.Entry<String, Double>>(actions.entrySet());

		sorted.sort(Map.Entry.<String, Double>comparingByValue().reversed());

		for (int i = 0; i < sorted.size(); i++) {
			Text text = new Text(String.format("%6s", sorted.get(i).getKey()) + " = " + String.format("%+.4f", sorted.get(i).getValue()) + "\n");
			if (i == 0) text.setStyle("-fx-fill: rgba(64, 128, 255, 1);");
			console.getChildren().add(text);
		}
		console.getChildren().add(new Text("\n"));
	}
}
