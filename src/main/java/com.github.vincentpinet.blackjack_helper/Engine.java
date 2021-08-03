package com.github.vincentpinet.blackjack_helper;

public class Engine {

	private Solver solver;
	private Cards player;
	private int dealer;
	private Cards deck;

	public Engine(int n) {
		solver = new Solver();
		player = new Cards();
		dealer = 0;
		deck = new Cards(n);
	}

	public double run() {
		return (solver.compute_ev(this.deck));
	}
}
