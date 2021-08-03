package com.github.vincentpinet.blackjack_helper;

public class Main {

	public static void main(String[] args) {

		// AppFX.main(args);

		Engine engine = new Engine(8);
		System.out.println("Player's edge = " + engine.run() * 100 + "%");

		System.exit(0);
	}
}
