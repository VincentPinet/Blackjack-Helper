package com.github.vincentpinet.blackjack_helper;

import java.util.ArrayList;

import javafx.beans.value.ObservableValue;
import javafx.beans.value.ChangeListener;
import javafx.beans.InvalidationListener;

public class ObservableCards extends Cards implements ObservableValue<Cards> {

		private ArrayList<ChangeListener<? super Cards>> listeners;

		public ObservableCards() {
			super();
			this.listeners = new ArrayList<>();
		}

		public ObservableCards(Cards o) {
			super(o);
			this.listeners = new ArrayList<>();
		}

		@Override
		public void init(int n) {
			Cards oldValue = new Cards(this);
			super.init(n);
			notifyAll(oldValue, this);
		}

		@Override
		public void init(Cards o) {
			Cards oldValue = new Cards(this);
			super.init(o);
			notifyAll(oldValue, this);
		}

		@Override
		public void remove(int rank) {
			Cards oldValue = new Cards(this);
			super.remove(rank);
			notifyAll(oldValue, this);
		}

		@Override
		public void add(int rank) {
			Cards oldValue = new Cards(this);
			super.add(rank);
			notifyAll(oldValue, this);
		}

		@Override
		public void do_split() {
			Cards oldValue = new Cards(this);
			super.do_split();
			notifyAll(oldValue, this);
		}

		@Override
		public void addListener(ChangeListener<? super Cards> listener) {
			listeners.add(listener);
		}

		@Override
		public void removeListener(ChangeListener<? super Cards> listener) {
			listeners.remove(listener);
		}

		@Override
		public Cards getValue() {
			return this;
		}

		private void notifyAll(Cards oldValue, Cards newValue) {
			for (ChangeListener<? super Cards> listener : listeners)
				listener.changed(this, oldValue, newValue);
		}

		@Override public void addListener(InvalidationListener listener) {}
		@Override public void removeListener(InvalidationListener listener) {}
}
