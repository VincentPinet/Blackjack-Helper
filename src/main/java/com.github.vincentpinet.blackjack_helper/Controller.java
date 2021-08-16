package com.github.vincentpinet.blackjack_helper;

import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.geometry.Insets;

import java.lang.Thread;
import javafx.concurrent.Task;

import javafx.scene.Node;
import javafx.scene.Cursor;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.control.Button;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CheckBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import javafx.scene.input.Dragboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.TransferMode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.DragEvent;
import javafx.event.ActionEvent;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

public class Controller {

	private final Solver solver;
	private final ObservableCards player, playerSplitted, dealer, deck;
	private final Rules rules;
	private final AtomicBoolean isComputing;

	@FXML
	private Node root;
	@FXML
	private Spinner<Integer> xdSpinner;
	@FXML
	private ComboBox<String> bjpaysCombobox;
	@FXML
	private CheckBox ss17, das, doa, hsa, es10;
	@FXML
	private Button shuffleButton, nextButton, splitButton;
	@FXML
	private TextFlow console;
	@FXML
	private StackPane playerZone, playerSplittedZone, dealerZone, binZone;
	@FXML
	private Rectangle playerSplittedLayout;
	@FXML
	private ArrayList<Rectangle> deckCards;
	@FXML
	private ArrayList<Text> deckCounters;

	public Controller() {
		this.rules = new Rules();
		this.solver = new Solver(rules);
		this.player = new ObservableCards();
		this.playerSplitted = new ObservableCards();
		this.deck = new ObservableCards();
		this.dealer = new ObservableCards();
		this.isComputing = new AtomicBoolean(false);
	}

	@FXML
	public void initialize() {

		xdSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 8, 8, 1));
		xdSpinner.valueProperty().addListener((observable, oldValue, newValue) -> { rules.xd = newValue; solver.clearCache(); });

		bjpaysCombobox.getItems().addAll("2/1", "3/2", "6/5", "1/1");
		bjpaysCombobox.getSelectionModel().select(1);
		bjpaysCombobox.valueProperty().addListener((observable, oldValue, newValue) -> {
			rules.bjpays = Double.parseDouble(newValue.split("/")[0]) / Double.parseDouble(newValue.split("/")[1]);
			solver.clearCache();
		});

		ss17.selectedProperty().addListener((observable, oldValue, newValue) -> { rules.ss17 = newValue; solver.clearCache(); });
		das.selectedProperty().addListener((observable, oldValue, newValue) -> { rules.das = newValue; solver.clearCache(); });
		doa.selectedProperty().addListener((observable, oldValue, newValue) -> { rules.doa = newValue; solver.clearCache(); });
		hsa.selectedProperty().addListener((observable, oldValue, newValue) -> { rules.hsa = newValue; solver.clearCache(); });
		es10.selectedProperty().addListener((observable, oldValue, newValue) -> { rules.es10 = newValue; solver.clearCache(); });

		{int i = 1;
		for (Rectangle card : deckCards) {
			final int rank = i;
			card.setOnDragDetected(e -> handleDetected(e, card, rank));
			card.setOnDragDone(e -> { binZone.setVisible(false); e.consume(); });
			i++;
		}};

		deck.addListener((observable, oldValue, newValue) -> {
			for (int i = 1; i <= 10; i++)
				deckCounters.get(i - 1).setText("" + newValue.contains(i));
		});

		shuffle(null);

		for (ObservableCards hand : new ObservableCards[]{dealer, player, playerSplitted})
			hand.addListener((observable, oldValue, newValue) -> showStrategy(hand));

		playerZone.setOnDragDropped(e -> handleDrop(e, playerZone, player));
		playerSplittedZone.setOnDragDropped(e -> handleDrop(e, playerSplittedZone, playerSplitted));
		dealerZone.setOnDragDropped(e -> handleDrop(e, dealerZone, dealer));

	}


	private void showStrategy(ObservableCards hand) {
		splitButton.setVisible(player.is_splitted() == 0 && player.is_splittable() > 0 && dealer.sum() != 0);
		if (hand.size() >= 2 && hand.sum() <= 21 && dealer.sum() != 0)
			computeActions(hand);
		else if (player.is_splitted() == 0 && player.size() >= 2 && player.sum() <= 21 && dealer.sum() != 0)
			computeActions(player);
	}


	private void handleDetected(MouseEvent event, Rectangle card, int rank) {
		if (deck.contains(rank) == 0) return;
		Dragboard db = card.startDragAndDrop(TransferMode.ANY);
		ClipboardContent cc = new ClipboardContent();
		cc.putString("" + rank);
		db.setContent(cc);
		binZone.setVisible(true);
		event.consume();
	}


	public void handleDrop(DragEvent event, StackPane zone, ObservableCards hand) {
		int rank = Integer.parseInt(event.getDragboard().getString());

		AnchorPane card = cardBuilder(rank);
		StackPane.setMargin(card, new Insets(0, 0, 24 * hand.size(), 32 * hand.size()));
		Button button = (Button) card.lookup(".button-small");
		button.setOnAction( e -> {
			zone.getChildren().remove(card);
			deck.add(rank);
			hand.remove(rank);
		});
		zone.getChildren().add(card);

		deck.remove(rank);
		hand.add(rank);

		event.setDropCompleted(true);
		event.consume();
	}

	@FXML
	private void handleOver(DragEvent event) {
		if (!player.is_bust())
			event.acceptTransferModes(TransferMode.ANY);
		event.consume();
	}

	@FXML
	private void handleOverSplitted(DragEvent event) {
		if (!playerSplitted.is_bust())
			event.acceptTransferModes(TransferMode.ANY);
		event.consume();
	}

	@FXML
	private void handleOverDealer(DragEvent event) {
		if (dealer.sum() == 0)
			event.acceptTransferModes(TransferMode.ANY);
		event.consume();
	}

	@FXML
	private void handleOverBin(DragEvent event) {
		for (Node node : binZone.getChildren())
			node.setStyle("-fx-opacity: 0.12");
		event.acceptTransferModes(TransferMode.ANY);
		event.consume();
	}

	@FXML
	private void handleExitBin(DragEvent event) {
		for (Node node : binZone.getChildren())
			node.setStyle("-fx-opacity: 0.06");
		event.consume();
	}

	@FXML
	public void handleDropBin(DragEvent event) {
		int rank = Integer.parseInt(event.getDragboard().getString());
		deck.remove(rank);
		event.setDropCompleted(true);
		event.consume();
	}

	@FXML
	private void next(ActionEvent event) {
		if (!isComputing.compareAndSet(false, true)) return;

		clear();
		root.setCursor(Cursor.WAIT);

		Task<Double> task = new Task<Double>() {
			@Override
			public Double call() {
				return solver.compute_ev(new Cards(deck));
			}
		};

		task.setOnSucceeded(e -> {
			double ev = task.getValue();
			Text text = new Text("EV : " + String.format("%+.4f", ev * 100) + "%\n\n");
			if (ev > 0) text.setStyle("-fx-fill: rgba(96, 255, 96, 1);");
			else text.setStyle("-fx-fill: rgba(255, 192, 0, 1);");
			console.getChildren().add(text);
			root.setCursor(Cursor.DEFAULT);
			isComputing.set(false);
		});

		new Thread(task).start();
	}

	@FXML
	private void shuffle(ActionEvent event) {
		clear();
		deck.init(rules.xd);
	}

	@FXML
	private void split(ActionEvent event) {

		for (Node card : playerZone.getChildren())
			((AnchorPane)card).getChildren().remove(card.lookup(".button-small"));

		AnchorPane card = (AnchorPane) playerZone.getChildren().remove(playerZone.getChildren().size() - 1);
		StackPane.setMargin(card, new Insets(0, 0, 0, 0));
		playerSplittedZone.getChildren().add(card);
		playerSplittedZone.setVisible(true);
		playerSplittedLayout.setVisible(true);

		player.do_split();
		playerSplitted.init(player);
	}


	private void computeActions(Cards p) {
		Map<String, Double> actions = solver.getAns(new Cards(p), dealer.sum(), new Cards(deck));
		ArrayList< Map.Entry<String, Double> > sorted = new ArrayList<Map.Entry<String, Double>>(actions.entrySet());

		sorted.sort(Map.Entry.<String, Double>comparingByValue().reversed());

		for (int i = 0; i < sorted.size(); i++) {
			Text text = new Text(String.format("%6s", sorted.get(i).getKey()) + " = " + String.format("%+.4f", sorted.get(i).getValue()) + "\n");
			if (i == 0) text.setStyle("-fx-fill: rgba(64, 128, 255, 1);");
			console.getChildren().add(text);
		}
		console.getChildren().add(new Text("\n"));
	}


	private AnchorPane cardBuilder(int rank) {
		try {
			AnchorPane res = FXMLLoader.load(getClass().getResource("/Card.fxml"));
			Text text = (Text) res.lookup(".letter");
			text.setText(rank==1 ? "A" : rank==10 ? "T" : ""+rank);
			return res;
		} catch (Exception e) {
			return null;
		}
	}


	private void clear() {
		player.init(0);
		playerSplitted.init(0);
		dealer.init(0);
		playerZone.getChildren().clear();
		playerSplittedZone.getChildren().clear();
		playerSplittedZone.setVisible(false);
		playerSplittedLayout.setVisible(false);
		dealerZone.getChildren().clear();
		console.getChildren().clear();
		console.getChildren().add(new Text(">_\n"));
	}
}
