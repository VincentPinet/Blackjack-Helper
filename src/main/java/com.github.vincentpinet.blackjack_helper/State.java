package com.github.vincentpinet.blackjack_helper;

import java.util.Objects;

public class State {

	private final long player;
	private final int dealer;
	private final long deck;

	public State(Cards player, int dealer, Cards deck) {
		this.player = player.raw();
		this.dealer = dealer;
		this.deck = deck.raw();
	}

	@Override
	public boolean equals(Object _o) {
		State o = (State) _o;
		return this.player == o.player && this.deck == o.deck && this.dealer == o.dealer;
	}

	@Override
	public int hashCode() {
		return Objects.hash(player, dealer, deck);
	}
}
