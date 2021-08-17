package com.github.vincentpinet.blackjack_helper;

import java.util.Arrays;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

public class Solver {

	private final HashMap<State, HashMap<String, Double>> solution;
	private final HashMap<State, double[]> dealer_cache;
	private final Rules rules;


	public Solver(Rules rules) {
		this.solution = new HashMap<>();
		this.dealer_cache = new HashMap<>();
		this.rules = rules;
	}


	private double eval(Cards player, int dealer) {
		if (player.is_blackjack()) return dealer==0 ? 0 : rules.bjpays;
		if (dealer == 0 || player.sum() > 21) return -1;
		if (dealer > 21) return 1;
		return (player.sum() >= dealer?1:0) - (dealer >= player.sum()?1:0);
	}


	private void dealer_score_rec(Cards hand, Cards deck, double weight, double[] res) {
		for (int i = 1; i <= 10; i++) {
			if (deck.contains(i) == 0) continue;
			double adj_weight = weight * deck.contains(i) / deck.size();
			hand.add(i);
			if (hand.sum() >= 17 && !(!rules.ss17 && hand.sum() == 17 && hand.is_soft())) {
				if (hand.is_blackjack()) res[0] += adj_weight;
				else if (hand.is_bust()) res[22] += adj_weight;
				else res[hand.sum()] += adj_weight;
			} else {
				deck.remove(i);
				dealer_score_rec(hand, deck, adj_weight, res);
				deck.add(i);
			}
			hand.remove(i);
		}
	}


	private double[] dealer_score(int card, Cards deck) {
		Cards hand = new Cards();
		hand.add(card);

		State state = new State(hand, 0, deck);
		if (dealer_cache.containsKey(state))
			return dealer_cache.get(state);

		double[] res = new double[23];
		Arrays.fill(res, 0.0);

		dealer_score_rec(hand, deck, 1.0, res);

		dealer_cache.put(state, res);
		return res;
	}


	private double stand_ev(Cards player, int dealer, Cards deck) {
		double res = 0;
		double[] a = dealer_score(dealer, deck);
		for (int i = 0; i <= 22; i++)
			res += eval(player, i) * a[i];
		return res;
	}


	private HashMap<Double, Double> stand_distribution(Cards player, int dealer, Cards deck) {
		HashMap<Double, Double> res = new HashMap<>();
		double[] a = dealer_score(dealer, deck);
		for (int i = 0; i <= 22; i++)
			res.merge(eval(player, i), a[i], Double::sum);
		return res;
	}


	private double optimal(HashMap<String, Double> actions) {
		return actions.values()
			.stream()
			.max(Double::compare)
			.get();
	}


	private double compute(Cards player, int dealer, Cards deck) {
		State state = new State(player, dealer, deck);
		if (solution.containsKey(state))
			return optimal(solution.get(state));

		HashMap<String, Double> res = new HashMap<>();

		// BUST
		if (player.sum() > 21)
			return -1;

		// STAND
		res.put("STAND", stand_ev(player, dealer, deck));

		// HIT
		double ev = 0;
		for (int i = 1; i <= 10; i++) {
			if (deck.contains(i) == 0) continue;
			double weight = 1.0 * deck.contains(i) / deck.size();
			player.draw(deck, i);
			ev += compute(player, dealer, deck) * weight;
			deck.draw(player, i);
		}
		res.put("HIT", ev);

		// SPLIT
		if (player.is_splitted() == 0 && player.is_splittable() > 0) {
			ev = 0;
			Cards player_splitted = new Cards(player);
			player_splitted.do_split();
			for (int i = 1; i <= 10; i++) {
				if (deck.contains(i) == 0) continue;
				double weight = 1.0 * deck.contains(i) / deck.size();
				player_splitted.draw(deck, i);
				if (player.contains(1) > 0 && !rules.hsa)
					ev += 2 * stand_ev(player_splitted, dealer, deck) * weight;
				else
					ev += 2 * compute(player_splitted, dealer, deck) * weight;
				deck.draw(player_splitted, i);
			}
			res.put("SPLIT", ev);
		}

		// DOUBLE
		if (player.size() == 2 && (rules.das || player.is_splitted() == 0) && (rules.doa || 9 <= player.sum() && player.sum() <= 11 )) {
			ev = 0;
			for (int i = 1; i <= 10; i++) {
				if (deck.contains(i) == 0) continue;
				double weight = 1.0 * deck.contains(i) / deck.size();
				player.draw(deck, i);
				ev += 2 * stand_ev(player, dealer, deck) * weight;
				deck.draw(player, i);
			}
			res.put("DOUBLE", ev);
		}

		// SURRENDER ES10
		if (player.size() == 2 && player.is_splitted() == 0 && rules.es10 && dealer != 1)
			res.put("SURR", -0.5);

		solution.put(state, res);
		return optimal(res);
	}


	private HashMap<Double, Double> compute_distribution(Cards player, int dealer, Cards deck) {
		HashMap<Double, Double> res = new HashMap<>();

		if (player.is_bust()) {
			res.put(-1.0, 1.0);
			return res;
		}

		String action = solution.get(new State(player, dealer, deck)).entrySet()
			.stream()
			.max(Map.Entry.comparingByValue())
			.get()
			.getKey();

		if (action.equals("STAND")) {
			res = stand_distribution(player, dealer, deck);

		} else if (action.equals("SURR")) {
			res.put(-0.5, 1.0);

		} else if (action.equals("HIT")) {
			for (int i = 1; i <= 10; i++) {
				if (deck.contains(i) == 0) continue;
				double weight = 1.0 * deck.contains(i) / deck.size();
				player.draw(deck, i);
				for (var kv : compute_distribution(player, dealer, deck).entrySet())
					res.merge(kv.getKey(), kv.getValue() * weight, Double::sum);
				deck.draw(player, i);
			}

		} else if (action.equals("DOUBLE")) {
			for (int i = 1; i <= 10; i++) {
				if (deck.contains(i) == 0) continue;
				double weight = 1.0 * deck.contains(i) / deck.size();
				player.draw(deck, i);
				for (var kv : stand_distribution(player, dealer, deck).entrySet())
					res.merge(2.0 * kv.getKey(), kv.getValue() * weight, Double::sum);
				deck.draw(player, i);
			}

		} else if (action.equals("SPLIT")) {
			Cards player_splitted = new Cards(player);
			player_splitted.do_split();
			ArrayList<HashMap<Double, Double>> dists = new ArrayList<HashMap<Double, Double>>();
			ArrayList<Double> weights = new ArrayList<>();
			for (int i = 1; i <= 10; i++) {
				if (deck.contains(i) == 0) continue;
				weights.add(1.0 * deck.contains(i) / deck.size());
				player_splitted.draw(deck, i);
				if (player.contains(1) > 0 && !rules.hsa)
					dists.add(stand_distribution(player_splitted, dealer, deck));
				else
					dists.add(compute_distribution(player_splitted, dealer, deck));
				deck.draw(player_splitted, i);
			}
			for (int i = 0; i < dists.size(); i++)
				for (var h1 : dists.get(i).entrySet())
					for (int j = 0; j < dists.size(); j++)
						for (var h2 : dists.get(j).entrySet())
							res.merge(h1.getKey() + h2.getKey(), (h1.getValue() * h2.getValue()) * weights.get(i) * weights.get(j), Double::sum);

		}

		return res;
	}


	public double computeEV(HashMap<Double, Double> distribution) {
		return distribution.entrySet()
			.stream()
			.mapToDouble(e -> e.getKey() * e.getValue())
			.sum();
	}


	public HashMap<Double, Double> getDistribution(Cards deck) {
		final HashMap<Double, Double> res = new HashMap<>();

		double weight = 1;
 		Cards player = new Cards();

		for (int i = 1; i <= 10; i++) {
			if (deck.contains(i) == 0) continue;
			weight *= 1.0 * deck.contains(i) / deck.size();
			player.draw(deck, i);

			for (int j = 1; j <= i; j++) {
				if (deck.contains(j) == 0) continue;
				weight *= 1.0 * deck.contains(j) / deck.size();
				if (j != i) weight *= 2;
				player.draw(deck, j);

				for (int k = 1; k <= 10; k++) {
					if (deck.contains(k) == 0) continue;
					weight *= 1.0 * deck.contains(k) / deck.size();
					deck.remove(k);
					compute(player, k, deck);
					for (var e : compute_distribution(player, k, deck).entrySet())
						res.merge(e.getKey(), e.getValue() * weight, Double::sum);

					deck.add(k);
					weight /= 1.0 * deck.contains(k) / deck.size();
				}

				deck.draw(player, j);
				if (j != i) weight /= 2;
				weight /= 1.0 * deck.contains(j) / deck.size();
			}

			deck.draw(player, i);
			weight /= 1.0 * deck.contains(i) / deck.size();
		}

		return res;
	}

	public HashMap<String, Double> getAns(Cards player, int dealer, Cards deck) {
		compute(player, dealer, deck);
		return solution.get(new State(player, dealer, deck));
	}

	public void clearCache() {
		solution.clear();
		dealer_cache.clear();
	}
}
